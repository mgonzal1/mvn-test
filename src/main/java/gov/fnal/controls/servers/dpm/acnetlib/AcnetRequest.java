// $Id: AcnetRequest.java,v 1.5 2024/11/21 22:02:19 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class AcnetRequest extends AcnetPacket implements AcnetConstants
{
	final ReplyId replyId;
	final boolean multipleReply;
	volatile boolean cancelled;
	Object object;
	AcnetConnection connection;

	AcnetRequest(int flags, Buffer buf)
	{
		//super(c, buf);
		super(buf);

		// A received request contains the reply id in the status field

		this.connection = null;
		this.replyId = status == 0 ? new ReplyId(client, id) : new ReplyId(status);
		this.multipleReply = (flags & ACNET_FLG_MLT) > 0;
		this.cancelled = false;
		this.object = null;
	}

	@Override
	public boolean isRequest()
	{
		return true;
	}

	@Override
	public String toString()
	{
		return String.format("%s - %s mult:%s cancelled:%s]", connection.connectedName(), replyId, multipleReply, cancelled);
	}

	void cancel()
	{
		cancelled = true;
	}	

	boolean isCancelled()
	{
		return cancelled;
	}

	public AcnetConnection connection()
	{
		return connection;
	}

	public int status()
	{
		return 0;
	}

	public final ReplyId replyId()
	{
		return replyId;
	}

	public final boolean isMulticast()
	{
		return (server() & 0xffff) == 0xff;
	}

	public void ignore() throws AcnetStatusException
	{
		connection.ignoreRequest(this);
	}

	public void ignoreNoEx()
	{
		try {
			connection.ignoreRequest(this);
		} catch (AcnetStatusException ignore) { }
	}

	public boolean multipleReply()
	{
		return multipleReply;
	}

    public void sendReply(ByteBuffer buf, int status) throws AcnetStatusException 
	{
		connection.sendReply(this, REPLY_NORMAL, buf, status);
    }

    public void sendReplyNoEx(ByteBuffer buf, int status)
	{
		try {
			connection.sendReply(this, REPLY_NORMAL, buf, status);
		} catch (AcnetStatusException ignore) { }
    }

    public void sendLastReply(ByteBuffer buf, int status) throws AcnetStatusException 
	{
		connection.sendReply(this, REPLY_ENDMULT, buf, status);
    }

    public void sendLastReplyNoEx(ByteBuffer buf, int status)
	{
		try {
			connection.sendReply(this, REPLY_ENDMULT, buf, status);
		} catch (AcnetStatusException ignore) { }
    }

    public void sendStatus(int status) throws AcnetStatusException 
	{
		connection.sendReply(this, REPLY_NORMAL, null, status);
    }

    public void sendStatusNoEx(int status)
	{
		try {
			connection.sendReply(this, REPLY_NORMAL, null, status);
		} catch (AcnetStatusException ignore) { }
    }

    public void sendLastStatus(int status) throws AcnetStatusException 
	{
		connection.sendReply(this, REPLY_ENDMULT, null, status);
    }

    public void sendLastStatusNoEx(int status)
	{
		try {
			connection.sendReply(this, REPLY_ENDMULT, null, status);
		} catch (AcnetStatusException ignore) { }
    }

	public void sendReply(ByteBuffer buf, int status, boolean endMult) throws AcnetStatusException
	{
		connection.sendReply(this, endMult ? REPLY_ENDMULT : REPLY_NORMAL, buf, status);
	}

	public void sendReplyNoEx(ByteBuffer buf, int status, boolean endMult)
	{
		try {
			connection.sendReply(this, endMult ? REPLY_ENDMULT : REPLY_NORMAL, buf, status);
		} catch (AcnetStatusException ignore) { }
	}

	public void setObject(Object data)
	{
		this.object = data;
	}

	public Object getObject()
	{
		return object;
	}

	@Override
	void handle(AcnetConnection connection)
	{
		final ReplyId replyId = replyId();

		try {	
			connection.requestAck(replyId);

			synchronized (connection.requestsIn) {
				connection.requestsIn.put(replyId, this);
			}

			AcnetInterface.stats.requestReceived();
			connection.stats.requestReceived();

			try {
				this.connection = connection;
				connection.requestHandler.handle(this);
			} catch (Exception e) {
				logger.log(Level.WARNING, "ACNET request callback exception for connection '" + connection.connectedName() + "'", e);
			}
		} catch (AcnetStatusException e) {
		} catch (Exception e) {
			logger.log(Level.WARNING, "ACNET request ack exception for connection '" + connection.connectedName() + "'", e);
		} finally {
			data = null;
			buf.free();
		}
	}
}
