// $Id: AcnetCancel.java,v 1.4 2024/11/21 22:02:19 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.logging.Level;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

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
			logger.log(Level.WARNING, "ACNET cancel callback exception for connection '" + connection.connectedName() + "'", e);
		} finally {
			data = null;
			buf.free();
		}
	}
}

