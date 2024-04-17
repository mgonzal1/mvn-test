// $Id: DPMCredentials.java,v 1.14 2024/03/04 16:54:02 kingc Exp $
package gov.fnal.controls.servers.dpm;

import gov.fnal.controls.kerberos.KerberosLoginContext;
import gov.fnal.controls.kerberos.login.ServiceName;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;

import javax.security.auth.kerberos.KerberosPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class DPMCredentials
{
    private static final Pattern PRINCIPAL_NAME_PATTERN = Pattern.compile("^(.*?)/([^/]+)@.+$");

	static private volatile List<KerberosPrincipal> kerberosServicePrincipals = new ArrayList<>();

	private static void login(Level level)
	{
		final KerberosLoginContext kerberosLoginContext = KerberosLoginContext.getInstance();

		logger.log(level, "Kerberos login begin");
	
		final long begin = System.currentTimeMillis();

		try {
			kerberosLoginContext.login();

			kerberosServicePrincipals.clear();

			final List<KerberosPrincipal> tmp = new ArrayList<>();

			for (KerberosPrincipal kerberosPrincipal : kerberosLoginContext.getSubject().getPrincipals(KerberosPrincipal.class)) {
				tmp.add(kerberosPrincipal);
			}

			kerberosServicePrincipals = tmp;

			logger.log(level, "Kerberos login successful in " + (System.currentTimeMillis() - begin) + "ms");
		} catch (Exception e) {
			logger.log(Level.WARNING, "Kerberos login failed", e);
		}
	}

	static void init()
	{
		System.setProperty("gov.fnal.controls.kerberos.service", "true");

		if (System.getProperty("gov.fnal.controls.kerberos.keytab") == null)
			System.setProperty("gov.fnal.controls.kerberos.keytab", "/usr/local/etc/daeset");

		login(Level.CONFIG);
	}

	private DPMCredentials() { }

	static public GSSName serviceName() throws GSSException
	{
		if (kerberosServicePrincipals.isEmpty())
			throw new GSSException(GSSException.NO_CRED);

        final String principalName = kerberosServicePrincipals.get(0).getName();
        final Matcher matcher = PRINCIPAL_NAME_PATTERN.matcher(principalName);

        if (!matcher.matches())
			throw new GSSException(GSSException.NO_CRED);

		final GSSManager manager = GSSManager.getInstance();
		final String name = matcher.group(1) + "@" + matcher.group(2);

		logger.log(Level.FINE, "service name: '" + name + "'");

		try {
			return manager.createName(name, GSSName.NT_HOSTBASED_SERVICE);
		} catch (GSSException e) {
			logger.log(Level.WARNING, "exception in serverName()", e);
			login(Level.FINE);
			return manager.createName(name, GSSName.NT_HOSTBASED_SERVICE);
		}
	}

	static GSSContext createContext() throws GSSException
	{
		final KerberosLoginContext kerberosLoginContext = KerberosLoginContext.getInstance();

		if (kerberosServicePrincipals.isEmpty())
			throw new GSSException(GSSException.NO_CRED);

		final ServiceName serviceName = new ServiceName(kerberosServicePrincipals.get(0));

		logger.log(Level.FINE, "Subject: " + kerberosLoginContext.getSubject());

		try {
			return kerberosLoginContext.createAcceptorContext(serviceName);
		} catch (GSSException e) {
			logger.log(Level.WARNING, "exception in createContext()", e);
			login(Level.FINE);
			return kerberosLoginContext.createAcceptorContext(serviceName);
		}
	}

	static byte[] accept(GSSContext gss, byte[] token) throws GSSException
	{
		try {
			return gss.acceptSecContext(token, 0, token.length);
		} catch (GSSException e) {
			logger.log(Level.WARNING, "exception in accept()", e);
			login(Level.FINE);
			return gss.acceptSecContext(token, 0, token.length);
		}
	}

	@Override
	public String toString()
	{
		return KerberosLoginContext.getInstance().toString();
	}

	public static void main(String[] args) throws GSSException
	{
		DPMCredentials.init();
		System.out.println("Service Name: " + serviceName());
	}
}

