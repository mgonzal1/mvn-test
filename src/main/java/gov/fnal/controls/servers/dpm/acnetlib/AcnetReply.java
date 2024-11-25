// $Id: AcnetReply.java,v 1.4 2024/05/30 18:11:14 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

public class AcnetReply extends AcnetPacket implements AcnetConstants, AcnetErrors
{
	static final AcnetReply nil = new AcnetReply() { void handle() { } };

	final RequestId requestId;
	final boolean last;

	AcnetReply()
	{
		super();

		this.requestId = null;
		this.last = true;
	}

	AcnetReply(int flags, Buffer buf)
	{
		super(buf);

		this.requestId = new RequestId(id);
		this.last = (flags & ACNET_FLG_MLT) == 0;
	}

	AcnetReply(int status, RequestId requestId, int server, boolean last) 
	{
		super(ACNET_FLG_RPY, status, server, 0, 0, 0, 0, ACNET_HEADER_SIZE, null);

		this.requestId = requestId;
		this.last = last;
	}

	@Override
	public boolean isReply()
	{
		return true;
	}

	@Override
	public String toString()
	{
		return String.format("REPLY 0x%04x", status());
	}

	public boolean success()
	{
		return status == ACNET_SUCCESS;
	}

	public final RequestId requestId()
	{
		return requestId;
	}

	public boolean last() 
	{
	    return last;
	}

	@Override
	void handle(AcnetConnection connection)
	{
		if (connection.replyThread != null) {
			if (!connection.replyThread.queue.offer(this)) {
				data = null;
				buf.free();
				connection.droppedReply(this);
			}
		} else {
			try {
				connection.handleReply(this);
			} catch (Exception e) {
			} finally {
				data = null;
				buf.free();
			}
		}

		AcnetInterface.stats.replyReceived();
		connection.stats.replyReceived();
	}

	@Override
	public int hashCode()
	{
		return requestId.hashCode();
	}
}
