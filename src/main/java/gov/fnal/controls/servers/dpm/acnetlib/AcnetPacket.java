// $Id: AcnetPacket.java,v 1.3 2024/02/22 16:29:45 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.util.logging.Level;

abstract public class AcnetPacket implements AcnetConstants
{
	static final AcnetPacket nil = new AcnetPacket() { void handle(AcnetConnection connection) { } };

	//final AcnetConnection connection;

	//protected final int flags;
	final Buffer buf;
	final int status;
	final int server;
	final int client;
	final int serverTask;
	//protected final String serverTaskName;
	final int clientTaskId;
	final int id;
	final int length;

	volatile ByteBuffer data;

	abstract void handle(AcnetConnection connection);

	AcnetPacket()
	{
		//this.connection = null;
		this.buf = null;
		this.status = 0;
		this.server = 0;
		this.client = 0;
		this.serverTask = 0;
		//this.serverTaskName = "";
		this.clientTaskId = 0;
		this.id = 0;
		this.length = 0;
		this.data = null;
	}

	//AcnetPacket(AcnetConnection connection, Buffer buf)
	AcnetPacket(Buffer buf)
	{
		//this.connection = connection;
		this.buf = buf;

		//this.flags = buf.getUnsignedShort(); //buf.getShort() & 0xffff;
	    this.status = buf.getShort();
	    this.server = buf.bigEndian().getUnsignedShort(); //buf.order(ByteOrder.BIG_ENDIAN).getShort() & 0xffff;
	    this.client = buf.getUnsignedShort(); //buf.getShort() & 0xffff;
	    this.serverTask = buf.littleEndian().getInt(); //buf.order(ByteOrder.LITTLE_ENDIAN).getInt();
	    //this.serverTaskName = Rad50.decode(serverTask);
	    this.clientTaskId = buf.getUnsignedShort(); //buf.getShort() & 0xffff;
	    this.id = buf.getUnsignedShort(); //buf.getShort() & 0xffff;
	    this.length = buf.getUnsignedShort(); //buf.getShort() & 0xffff;

	    this.data = buf.slice(); //.order(ByteOrder.LITTLE_ENDIAN);
	}

	//AcnetPacket(AcnetConnection connection, int flags, int status, int server, int client, int serverTask,
	//				int clientTaskId, int id, int length, Buffer buf)
	AcnetPacket(int flags, int status, int server, int client, int serverTask,
					int clientTaskId, int id, int length, Buffer buf)
	{
		//this.connection = connection;
		this.buf = buf;

		//this.flags = flags;
		this.status = status;
		this.server = server;
		this.client = client;
		this.serverTask = serverTask;
		//this.serverTaskName = Rad50.decode(serverTask);
		this.clientTaskId = clientTaskId;
		this.id = id;
		this.length = length;
		this.data = buf.slice();
	}

	public boolean isRequest()
	{
		return false;
	}

	public boolean isReply()
	{
		return false;
	}

	int id()
	{
		return id;
	}

	public int status()
	{
		return status;
	}

	public int server()
	{
		return server;
	}

	public String serverName() throws AcnetStatusException
	{
		return Node.get(server).name();
	}

	public int client()
	{
		return client;
	}

	public boolean isMulticast() throws AcnetStatusException
	{
		return Node.get(client).isMCAST();
	}

	public String clientName() throws AcnetStatusException
	{
		return Node.get(client).name();
	}

	public int serverTask()
	{
		return serverTask;
	}

	public String serverTaskName()
	{
		return Rad50.decode(serverTask);
	}

	public int clientTaskId()
	{
		return clientTaskId;
	}

	public int length()
	{
		return length;
	}

	synchronized public ByteBuffer data()
	{
		return data;
	}

	public int getNode(String name) throws AcnetStatusException
	{
		return Node.get(name).value();
	}

	public String getName(int node) throws AcnetStatusException
	{
		return Node.get(node).name();
	}

	public int getLocalNode() throws AcnetStatusException
	{
		return AcnetInterface.localNode().value();
	}

	static AcnetPacket create(Buffer buf)
	{
		final int flags = buf.getUnsignedShort();

		switch (flags & (ACNET_FLG_TYPE | ACNET_FLG_CAN)) {
			case ACNET_FLG_RPY:  
				//return new AcnetReply(connection, flags, buf);
				return new AcnetReply(flags, buf);

			case ACNET_FLG_USM: 
				//return new AcnetMessage(connection, buf);
				return new AcnetMessage(buf);

			case ACNET_FLG_REQ:
				//return new AcnetRequest(connection, flags, buf);
				return new AcnetRequest(flags, buf);

			case ACNET_FLG_CAN:
				//return new AcnetCancel(connection, buf);
				return new AcnetCancel(buf);

			default:
				return AcnetPacket.nil;
		}
	}
}
