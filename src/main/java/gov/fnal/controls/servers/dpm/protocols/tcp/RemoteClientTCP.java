// $Id: RemoteClientTCP.java,v 1.6 2022/06/21 20:55:42 kingc Exp $

package gov.fnal.controls.servers.dpm.protocols.tcp;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

import gov.fnal.controls.servers.dpm.protocols.Protocol;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class RemoteClientTCP 
{
	final SocketChannel channel;
	final String hostName;
	final InetAddress hostAddress;
	final HttpRequest httpRequest;

	public RemoteClientTCP(SocketChannel channel) throws IOException
	{
		this.channel = channel;
		this.channel.finishConnect();
		this.channel.configureBlocking(true);
		this.channel.socket().setSoTimeout(500);
		this.httpRequest = new HttpRequest(channel);

		logger.fine("HTTP Request:\n" + httpRequest.toString());

		final String forward = httpRequest.headers.get("x-forwarded-for");

		if (forward == null) {
			final SocketAddress addr = this.channel.getRemoteAddress();
			
			if (addr instanceof InetSocketAddress) {
				final InetSocketAddress sa = (InetSocketAddress) addr;
				this.hostName = sa.getHostName() + ":" + sa.getPort();
				this.hostAddress = ((InetSocketAddress) addr).getAddress();
			} else {
				throw new IOException();
			}
		} else {
			this.hostAddress = InetAddress.getByName(forward);	
			this.hostName = hostAddress.getHostName();
		}
	}

	public SocketChannel channel()
	{
		return channel;
	}

	public String hostName()
	{
		return hostName;
	}

	public InetAddress hostAddress()
	{
		return hostAddress;
	}

	Protocol protocol() throws Exception
	{
		final String s = httpRequest.headers.get("content-type");

		return s == null ? Protocol.PC : Protocol.fromContentType(s);
	}

	boolean hasHeader(String headerName)
	{
		return httpRequest.headers.containsKey(headerName);
	}

	String getHeader(String headerName)
	{
		return httpRequest.headers.get(headerName);
	}
}
