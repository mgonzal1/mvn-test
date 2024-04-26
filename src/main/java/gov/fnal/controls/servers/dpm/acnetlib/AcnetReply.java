// $Id: AcnetReply.java,v 1.3 2024/02/22 16:29:45 kingc Exp $
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

	//AcnetReply(AcnetConnection connection, int flags, Buffer buf)
	AcnetReply(int flags, Buffer buf)
	{
		//super(connection, buf);
		super(buf);

		this.requestId = new RequestId(id);
		this.last = (flags & ACNET_FLG_MLT) == 0;
	}

	//AcnetReply(AcnetConnection connection, int status, RequestId requestId, int server) 
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
		//System.out.println("RECEIVED A REPLY");

		if (connection.replyThread != null) {
			//System.out.println("PUT ON REPLY QUEUE");
			connection.replyThread.queue.offer(this);

			AcnetInterface.stats.replyReceived();
			connection.stats.replyReceived();
		} else {
			try {
				connection.handleReply(this);
			} catch (Exception e) {
			} finally {
				data = null;
				buf.free();
			}
		}
	}

	@Override
	public int hashCode()
	{
		return requestId.hashCode();
	}
}
