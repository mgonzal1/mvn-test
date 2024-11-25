// $Id: DPMCredentials.java,v 1.20 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

import gov.fnal.controls.kerberos.login.ServiceName;
import gov.fnal.controls.kerberos.KerberosLoginContext;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

final public class DPMCredentials
{
    static final Pattern PRINCIPAL_NAME_PATTERN = Pattern.compile("^(.*?)/([^/]+)@.+$");

	static volatile List<KerberosPrincipal> kerberosServicePrincipals = new ArrayList<>();
	static String serviceName;

	private static void login(final Level level)
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

	public static String serviceName()
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

		return matcher.group(1) + "@" + matcher.group(2);
	}

	static GSSContext createContext() throws GSSException
	{
		return createContext(1, Level.FINE);
	}

	private static final GSSContext createContext(int retry, Level level) throws GSSException
	{
		final KerberosLoginContext kerberosLoginContext = KerberosLoginContext.getInstance();

		if (kerberosServicePrincipals.isEmpty())
			throw new GSSException(GSSException.NO_CRED);

		final ServiceName serviceName = new ServiceName(kerberosServicePrincipals.get(0));

		logger.log(level, "Subject: " + kerberosLoginContext.getSubject());

		try {
			return kerberosLoginContext.createAcceptorContext(serviceName);
		} catch (GSSException e) {
			if (retry > 0)
				return createContext(--retry, Level.INFO);
			else
				throw e;
		}
	}

	static byte[] accept(final GSSContext gss, final byte[] token) throws GSSException
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

	public static void main(final String[] args) throws GSSException
	{
		DPMCredentials.init();
		logger.log(Level.INFO, "Service Name: " + serviceName());
	}
}

