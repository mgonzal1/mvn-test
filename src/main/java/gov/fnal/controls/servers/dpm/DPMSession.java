// $Id: DPMSession.java,v 1.2 2022/06/30 20:51:37 kingc Exp $
package gov.fnal.controls.servers.dpm;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class DPMSession
{
	static final HashMap<String, DPMSession> activeSessions = new HashMap<>();

	final UUID id;
	volatile GSSContext gss;
	volatile GSSName gssName;
	volatile ConsoleUser consoleUser;
	HashSet<String> activeRoles;

	public DPMSession()
	{
		this.id = UUID.randomUUID();
		this.gss = null;
		this.gssName = null;
		this.consoleUser = null;
		this.activeRoles = null;
	}

	synchronized public static DPMSession open()
	{
		final DPMSession newSession = new DPMSession();

		activeSessions.put(newSession.id.toString(), newSession);
		return newSession;
	}

	synchronized public static DPMSession get(String id) throws Exception
	{
		final DPMSession session = activeSessions.get(id);

		if (session != null)
			return session;

		throw new Exception("Invalid session id");
	}

	synchronized public void close()
	{
		activeSessions.remove(id);

		try {
			gss.dispose();
		} catch (Exception ignore) { }
	}

	public GSSName userName()
	{
		return gssName;
	}

	public int userId()
	{
		return consoleUser.id();
	}

	public Set<String> allowedDevices()
	{
		return consoleUser.allowedDevices();
	}

	public ConsoleUser consoleUser() throws GSSException
	{
		if (consoleUser != null)
			return consoleUser;

		throw new GSSException(GSSException.NO_CRED);
	}

	public void addRole(String role) throws Exception
	{
		if (consoleUser != null) {
			if (consoleUser.roles().contains(role))
				activeRoles.add(role);
			else
				throw new Exception("Invalid role [" + role + "] for [" + consoleUser.name() + "]");	
		} else
			throw new GSSException(GSSException.NO_CRED);
	}

	public void removeRole(String role) throws Exception
	{
		if (consoleUser != null) {
			if (activeRoles.contains(role))
				activeRoles.remove(role);
			else
				throw new Exception("Role [" + role + "] is not active for [" + consoleUser.name() + "]");	
		} else
			throw new GSSException(GSSException.NO_CRED);
	}

	public String id()
	{
		return id.toString();
	}

	public boolean authenticated()
	{
		return gss != null && gss.isEstablished();
	}

	public byte[] authenticateStep(byte[] token) throws GSSException
	{
		if (gss == null)
			gss = DPMCredentials.createContext();

		token = gss.acceptSecContext(token, 0, token.length);

		if (gss.isEstablished()) {
			gssName = gss.getSrcName();

			logger.info(String.format("User %s authenticated for service %s", 
											gssName, gss.getTargName()));

			if ((consoleUser = ConsoleUserManager.get(gssName)) == null) {
				gss.dispose();
				gss = null;

				throw new GSSException(GSSException.UNAVAILABLE);
			}

			activeRoles = new HashSet<>();
		}

		return token;
	}
}
