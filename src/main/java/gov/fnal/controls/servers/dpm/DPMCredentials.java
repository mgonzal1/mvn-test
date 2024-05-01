// $Id: DPMCredentials.java,v 1.16 2024/03/20 20:17:43 kingc Exp $
package gov.fnal.controls.servers.dpm;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSName;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

import gov.fnal.controls.kerberos.login.ServiceName;
import gov.fnal.controls.kerberos.KerberosLoginContext;

public class DPMCredentials
{
    static final Pattern PRINCIPAL_NAME_PATTERN = Pattern.compile("^(.*?)/([^/]+)@.+$");

	static volatile List<KerberosPrincipal> kerberosServicePrincipals = new ArrayList<>();
	static String serviceName; 

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
			serviceName = getServiceName();

			logger.log(level, "Kerberos login successful in " + (System.currentTimeMillis() - begin) + "ms");
			logger.log(level, "Kerberos service name: '" + serviceName + "'");
		} catch (Exception e) {
			serviceName = "";
			kerberosServicePrincipals.clear();

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

	static public String serviceName()
	{
		return serviceName;
	}

	static String getServiceName() throws GSSException
	{
		if (kerberosServicePrincipals.isEmpty())
			throw new GSSException(GSSException.NO_CRED);

        final String principalName = kerberosServicePrincipals.get(0).getName();
        final Matcher matcher = PRINCIPAL_NAME_PATTERN.matcher(principalName);

        if (!matcher.matches())
			throw new GSSException(GSSException.NO_CRED);

		//final GSSManager manager = GSSManager.getInstance();
		//final String name = matcher.group(1) + "@" + matcher.group(2);
		return matcher.group(1) + "@" + matcher.group(2);

		//logger.log(Level.FINE, "service name: '" + name + "'");

		//try {
			//return manager.createName(name, GSSName.NT_HOSTBASED_SERVICE);
		//} catch (GSSException e) {
			//logger.log(Level.WARNING, "exception in serviceName()", e);
			//login(Level.FINE);
			//return manager.createName(name, GSSName.NT_HOSTBASED_SERVICE);
		//}
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

