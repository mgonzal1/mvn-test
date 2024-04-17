// $Id: AuthenticateReply.java,v 1.2 2022/09/27 18:24:37 kingc Exp $

package gov.fnal.controls.servers.dpm;

public class AuthenticateReply
{
	public final String serviceName;
	public final byte[] token;

	AuthenticateReply(String serviceName, byte[] token)
	{
		this.serviceName = serviceName;
		this.token = token;
	}

	AuthenticateReply(String serviceName)
	{
		this(serviceName, null);
	}
}
