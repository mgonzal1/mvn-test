// $Id: AuthenticateReply.java,v 1.3 2024/09/26 14:07:16 kingc Exp $

package gov.fnal.controls.servers.dpm;

public class AuthenticateReply
{
	public final String serviceName;
	public final byte[] token;

	AuthenticateReply(final String serviceName, final byte[] token)
	{
		this.serviceName = serviceName;
		this.token = token;
	}

	AuthenticateReply(final String serviceName)
	{
		this(serviceName, null);
	}
}
