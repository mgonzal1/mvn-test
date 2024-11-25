// $Id: ConsoleUserManager.java,v 1.31 2024/11/19 22:34:43 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.Collections;
import java.util.logging.Level;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.ietf.jgss.GSSName;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.db.DbServer;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import static gov.fnal.controls.db.DbServer.getDbServer;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

final class ConsoleUserManager extends TimerTask implements DbServer.Constants, AcnetErrors
{
	enum ConsoleClass {
		MCR(0x00000002),
		RemoteMCR(0x00000004),
		CHL(0x00000008),
		MCRCrewChief(0x00000010),
		ASTA(0x00000020),
		CDF(0x00000040),
		WebUser(0x00000080),
		Minimal(0x00000100),
		Operations(0x00000200),
		KTev(0x00000400),
		AccelProgrammer(0x00000800),
		RF(0x00001000),
		NOvA(0x00002000),
		CentralOther(0x00004000),
		CentralServices(0x00008000),
		Muon(0x00010000),
		FSWrite(0x00020000),
		Development(0x00040000),
		Collider(0x00080000),
		Linac(0x00100000),
		Booster(0x00200000),
		MainInjector(0x00400000),
		Switchyard(0x00800000),
		Tevatron(0x01000000),
		NuMI(0x02000000),
		MiniBooNE(0x04000000),
		Meson(0x08000000),
		Frig(0x10000000),
		AccelRnD(0x20000000),
		RemoteSet(0x40000000),
		Manager(0x80000000);

		final int mask;

		ConsoleClass(int mask)
		{
			this.mask = mask;
		}

		static String maskString(int mask)
		{
			final StringBuilder buf = new StringBuilder();

			for (ConsoleClass c : ConsoleClass.values())
				if ((mask & c.mask) > 0) {
					buf.append(c);
					buf.append(' ');
				}

			if (buf.length() > 0)
				buf.deleteCharAt(buf.length() - 1);

			return buf.toString();
		}
	}

	enum UserPrivilege
	{
		NoSettings(0x00000002),
		SetForever(0x00000004),
		SetForRead(0x00000008),
		SetUnlock1Hour(0x00000010),
		SetUnlock8Hours(0x00000020),
		ComfortDisplay(0x00000040),
		RunSlot7(0x00000080),
		IdleStop(0x00000100),
		RunAlarms(0x00000200),
		AlarmsOnly(0x00000800),
		OffsiteSettings(0x00008000),
		AppTimeout30Min(0x00001000),
		AppTimeout1Hour(0x00002000),
		AppTimeout4Hours(0x00004000),
		TimeoutPA(0x00010000),
		TimeoutSA(0x00020000),
		TimeoutAlarms(0x00040000),
		CHGPGMLocked(0x00080000),
		AUXPlotPriority(0x00100000),
		MCRPlotPriority(0x00200000),
		NoDotCursor(0x00400000);

		final int mask;

		UserPrivilege(int mask)
		{
			this.mask = mask;
		}

		static String maskString(int mask)
		{
			final StringBuilder buf = new StringBuilder();

			for (UserPrivilege p : UserPrivilege.values())
				if ((mask & p.mask) > 0) {
					buf.append(p);
					buf.append(' ');
				}

			if (buf.length() > 0)
				buf.deleteCharAt(buf.length() - 1);

			return buf.toString();
		}
	}

	private static class ConsoleUserImpl implements ConsoleUser
	{
		final int id;
		final String name;
		final int permissions;
		final int classes;

		Set<String> roles;
		Set<String> allowedDevices;

		ConsoleUserImpl(final ResultSet rs) throws SQLException
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
		public Set<String> allowedDevices(final String role)
		{
			return roles.contains(role) ? devicesForRole(role) : EmptyDeviceSet;
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

	static final Set<String> EmptyDeviceSet = Collections.unmodifiableSet(new HashSet<>());
	static final int UPDATE_RATE = 1000 * 60 * 30;

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
		}
	}
	
	synchronized static Set<String> devicesForRole(final String role)
	{
		final Set<String> s = manager.roles.get(role);

		return s == null ? EmptyDeviceSet : s;
	}

	private static final String extractName(final String principal)
	{
		final String[] split1 = principal.split("@");
		final String[] split2 = split1[0].split("/");
		final String[] split3 = split2[split2.length - 1].split("[.]");
		
		return split3[0];
	}

	synchronized static ConsoleUser get(final String principal) throws AcnetStatusException
	{
		synchronized (manager) {
			final ConsoleUserImpl user = manager.usersByName.get(extractName(principal));

			if (user != null)
				return user;

			throw new AcnetStatusException(DPM_PRIV);
		}
	}
	
	synchronized static ConsoleUser get(final GSSName gssName)
	{
		synchronized (manager) {
			final String fqName = gssName.toString();

			return manager.usersByName.get(extractName(fqName));
		}
	}

	private static void print(final ConsoleUserImpl user)
	{
		logger.log(Level.INFO, String.format("%-18s id:%6d permissions:0x%08x classes:0x%08x %s %d devices\n",
							user.name, user.id, user.permissions,
							user.classes, user.roles, user.allowedDevices.size()));

		logger.log(Level.INFO, String.format("Classes [%s]\n", ConsoleClass.maskString(user.classes)));
		logger.log(Level.INFO, String.format("Privileges [%s]\n", UserPrivilege.maskString(user.permissions)));
	}

	public static void main(final String[] args) throws Exception
	{
		final DPMServer server = new DPMServer();

		logger.setLevel(Level.OFF);
		ConsoleUserManager.init();

		if (args[0].equals("list")) {
			logger.log(Level.INFO, String.format("ConsoleUserManager: %d users, %d defined roles\n\n",
								manager.usersByName.size(), manager.roles.size()));

			for (ConsoleUserImpl user : manager.usersByName.values())
				print(user);

			logger.log(Level.INFO, "");

			for (Map.Entry<String, Set<String>> e : manager.roles.entrySet()) {
				logger.log(Level.INFO, "[" + e.getKey() + "] " + e.getValue().size() + " devices");
				logger.log(Level.INFO, "------------------------------------------------------------------------");

				int pCnt = 0;
				for (String deviceName : e.getValue()) {
					logger.log(Level.INFO, deviceName);
					if (++pCnt == 8) {
						pCnt = 0;
						logger.log(Level.INFO, "");
					} else
						logger.log(Level.INFO, "  ");
				}
				logger.log(Level.INFO, "");
				logger.log(Level.INFO, "");
			}
		} else if (args[0].equals("user"))
			print((ConsoleUserImpl) get(args[1]));

		Thread.sleep(1500);
		System.exit(0);
	}
}
