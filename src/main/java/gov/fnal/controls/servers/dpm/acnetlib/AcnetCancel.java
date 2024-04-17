// $Id: AcnetCancel.java,v 1.3 2024/02/22 16:29:45 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.logging.Level;

public class AcnetCancel extends AcnetPacket
{
	final ReplyId replyId;

	AcnetCancel(Buffer buf)
	{
		super(buf);

		// A received cancel contains the reply id in the status field

		this.replyId = status == 0 ? new ReplyId(client, id) : new ReplyId(status);
	}

	@Override
	final public int status()
	{
		return 0;
	}

	@Override
	final public int hashCode()
	{
		return replyId.hashCode();
	}

	final public ReplyId replyId()
	{
		return replyId;
	}

	@Override
	void handle(AcnetConnection connection)
	{
		final AcnetRequest request;
		
		synchronized (connection.requestsIn) {
			if ((request = connection.requestsIn.remove(replyId)) != null)
				request.cancel();
		}

		try {
			connection.requestHandler.handle(this);
		} catch (Exception e) {
			AcnetInterface.logger.log(Level.WARNING, "ACNET cancel callback exception for connection '" + connection.connectedName() + "'", e);
		} finally {
			data = null;
			buf.free();
		}
	}
}

