// $Id: ConsoleUserManager.java,v 1.27 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.Collections;
import java.util.logging.Level;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.ietf.jgss.GSSName;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.db.DbServer;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.service.proto.DPMScope;

import static gov.fnal.controls.db.DbServer.getDbServer;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class ConsoleUserManager extends TimerTask implements DbServer.Constants, AcnetErrors
{
	static private class ConsoleUserImpl implements ConsoleUser
	{
		final int id;
		final String name;
		final int permissions;
		final int classes;

		Set<String> roles;
		Set<String> allowedDevices;	

		ConsoleUserImpl(ResultSet rs) throws SQLException
		{
			this.id = rs.getInt("console_user_id");
			this.name = rs.getString("name").trim().toLowerCase();
			this.classes = rs.getInt("classes");
			this.permissions = rs.getInt("permissions");
			this.roles = new HashSet<>();
			this.allowedDevices = new HashSet<>();
		}

		@Override
		public final int id()
		{
			return id;
		}

		@Override
		public final String name()
		{
			return name;
		}

		@Override
		public final int permissions()
		{
			return permissions;
		}

		@Override
		public final int classes()
		{
			return classes;
		}

		@Override
		public Set<String> roles()
		{
			return roles;
		}

		@Override
		public Set<String> allowedDevices(String role)
		{
			return roles.contains(role) ? devicesForRole(role) : emptyDeviceSet;
		}

		@Override
		public Set<String> allowedDevices()
		{
			return allowedDevices;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	static final Set<String> emptyDeviceSet = Collections.unmodifiableSet(new HashSet<>());

	private final int UPDATE_RATE = 1000 * 60 * 30;
	private final HashMap<String, ConsoleUserImpl> usersByName = new HashMap<>();	
	private final HashMap<Integer, ConsoleUserImpl> usersById = new HashMap<>();	
	private final HashMap<String, Set<String>> roles = new HashMap<>();

	private static ConsoleUserManager manager;

	private ConsoleUserManager()
	{
		DPMServer.sharedTimer().scheduleAtFixedRate(this, UPDATE_RATE, UPDATE_RATE);	
	}

	static void init()
	{
		manager = new ConsoleUserManager();
		manager.update();
	}

	@Override
	public void run()
	{
		update();
	}

	static void refresh()
	{
		manager.update();
	}

	private synchronized void update()
	{
		usersByName.clear();
		usersById.clear();
		roles.clear();

		try {
			final String query = "SELECT console_user_id, name, classes, permissions FROM accdb.console_user WHERE inactive = 0";
			ResultSet rs = getDbServer("adbs").executeQuery(query);

			while (rs.next()) {
				final ConsoleUserImpl user = new ConsoleUserImpl(rs);

				usersByName.put(user.name, user);
				usersById.put(user.id, user);
			}

			rs.close();
			rs = getDbServer("adbs").executeQuery("SELECT console_user_id, role_name, DRF FROM beau.allowed");

			final HashSet<Integer> badConsoleUserIds = new HashSet<>();

			while (rs.next()) {
				final int userId = rs.getInt("console_user_id");
				final ConsoleUserImpl user = usersById.get(userId);
				
				if (user != null) {
					final String role = rs.getString("role_name").trim();
					final String drf = rs.getString("DRF").trim();

					try {
						final String device = (new DPMRequest(drf)).getDevice();

						Set<String> deviceSet = roles.get(role);

						if (deviceSet == null) {
							deviceSet = new HashSet<>();
							roles.put(role, deviceSet);
						}

						deviceSet.add(device);
						user.allowedDevices.add(device);
					} catch (Exception e) {
						logger.log(Level.FINE, "console manager: ignoring user:'" + user + "' role:'" + role + "' drf:'" + drf + "' ", e);
					}

					user.roles.add(role);

				} else if (!badConsoleUserIds.contains(userId)) {
					badConsoleUserIds.add(userId);
					logger.log(Level.FINE, "console manager: bad console user id: " + userId); 
				}
			}

			rs.close();
			rs = null;

			for (Map.Entry<String, Set<String>> e : roles.entrySet())
				roles.put(e.getKey(), Collections.unmodifiableSet(e.getValue()));

			for (ConsoleUserImpl user : manager.usersByName.values()) {
				user.roles = Collections.unmodifiableSet(user.roles);
				user.allowedDevices = Collections.unmodifiableSet(user.allowedDevices);
			}

			logger.log(Level.FINER, "updated console user info");
			
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception updating console user info", e);
			usersByName.clear();
			usersById.clear();
			roles.clear();
		} finally {
		}
	}
	
	synchronized static Set<String> devicesForRole(String role)
	{
		final Set<String> s = manager.roles.get(role);

		return s == null ? emptyDeviceSet : s;
	}

	private static final String extractName(String principal)
	{
		final String[] split1 = principal.split("@");
		final String[] split2 = split1[0].split("/");
		final String[] split3 = split2[split2.length - 1].split("[.]");
		
		return split3[0];
	}

	synchronized static ConsoleUser get(String principal) throws AcnetStatusException
	{
		synchronized (manager) {
			final ConsoleUserImpl user = manager.usersByName.get(extractName(principal));

			if (user != null)
				return user;

			throw new AcnetStatusException(DPM_PRIV);
		}
	}
	
	synchronized static ConsoleUser get(GSSName gssName)
	{
		synchronized (manager) {
			final String fqName = gssName.toString();

			return manager.usersByName.get(extractName(fqName));
		}
	}

	private static void print(ConsoleUserImpl user)
	{
		System.out.printf("%-18s id:%6d 0x%08x 0x%08x %s %d devices\n", 
							user.name, user.id, user.permissions, 
							user.classes, user.roles, user.allowedDevices.size());
	}

/*
	public static void main(String[] args) throws Exception
	{
		final DPMServer server = new DPMServer();

		logger.setLevel(Level.OFF);
		ConsoleUserManager.init();

		if (args[0].equals("list")) {
			System.out.printf("ConsoleUserManager: %d users, %d defined roles\n\n",
								manager.usersByName.size(), manager.roles.size());

			for (ConsoleUserImpl user : manager.usersByName.values())
				print(user);

			System.out.println();

			for (Map.Entry<String, Set<String>> e : manager.roles.entrySet()) {
				System.out.println("[" + e.getKey() + "] " + e.getValue().size() + " devices");
				System.out.println("------------------------------------------------------------------------");

				int pCnt = 0;
				for (String deviceName : e.getValue()) {
					System.out.print(deviceName);
					if (++pCnt == 8) {
						pCnt = 0;
						System.out.println();
					} else
						System.out.print("  ");
				}
				System.out.println();
				System.out.println();
			}	
		} else if (args[0].equals("refresh")) {
			final AcnetConnection c = AcnetConnection.open();
			final DPMScope.Request.Refresh m = new DPMScope.Request.Refresh();
			final ByteBuffer buf = ByteBuffer.allocate(1024);

			m.name = "ConsoleUserManager";
			m.marshal(buf).flip();

			c.send("MCAST", "SCOPE", buf);
		} else if (args[0].equals("user")) {
			print((ConsoleUserImpl) get(args[1]));
		}

		Thread.sleep(1500);
		System.exit(0);
	}
*/
}
