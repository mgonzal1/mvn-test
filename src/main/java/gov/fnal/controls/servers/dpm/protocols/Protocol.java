// $Id: Protocol.java,v 1.3 2021/12/17 20:27:54 kingc Exp $

package gov.fnal.controls.servers.dpm.protocols;

public enum Protocol
{
	GRPC("Google RPC using protocol buffers", "application/grpc"),
	PC("Fermi Protocol Compiler", "application/pc"),
	JSON("Javascript Object Notation", "application/json"),
	RAW("Raw Bytes", "");

	private String description;
	private String contentType;

	Protocol(String description, String contentType)
	{
		this.description = description;
		this.contentType = contentType;
	}

	public static Protocol fromContentType(String contentType) throws Exception
	{
		for (Protocol p : Protocol.values()) {
			if (contentType.equalsIgnoreCase(p.contentType))
				return p;
		}

		throw new Exception("Bad protocol content-type: '" + contentType + "'");
	}

	public String description()
	{
		return description;
	}
}
