// $Id: HttpRequest.java,v 1.1 2021/09/27 20:58:16 kingc Exp $

package gov.fnal.controls.servers.dpm.protocols.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.HashMap;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class HttpRequest 
{
	private String method;
	private String url;
	private String protocol;
	final Map<String, String> headers;
	final private StringBuilder buf;

	HttpRequest(SocketChannel channel) throws IOException
	{
		this.method = "";
		this.url = "";
		this.protocol = "";
		this.headers = new HashMap<>();
		this.buf = new StringBuilder();

		final InputStream is = channel.socket().getInputStream();

		while (readLine(is) > 0) {
			if (method.isEmpty()) {
				final String[] split = buf.toString().split(" ");
				
				if (split.length == 3) {
					this.method = split[0];
					this.url = split[1];
					this.protocol = split[2];
				} else
					throw new IOException();
			} else {
				final String[] split = buf.toString().split(": ");

				if (split.length == 2) {
					this.headers.put(split[0].trim().toLowerCase(), split[1]);
				} else
					throw new IOException();
			}
		}
	}

	private int readLine(InputStream is) throws IOException
	{
		buf.setLength(0);

		while (true) {
			final char v = (char) is.read();

			if (v == '\n')
				return buf.length();
			else if (v != '\r')
				buf.append(v);

			if (buf.length() > 4096)
				throw new IOException();
		}
	}

	@Override
	public String toString()
	{
		final StringBuilder buf = new StringBuilder();

		buf.append(method).append(' ').append(url).append(' ').append(protocol).append("\r\n");

		for (Map.Entry<String, String> entry : headers.entrySet())
			buf.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");

		return buf.append("\r\n").toString();
	}
}
