// $Id: AcnetMessage.java,v 1.4 2024/11/21 22:02:19 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.logging.Level;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class AcnetMessage extends AcnetPacket
{
	AcnetMessage(Buffer buf)
	{
		super(buf);
	}

	@Override
	void handle(AcnetConnection connection)
	{
		try {
			connection.messageHandler.handle(this);

			AcnetInterface.stats.messageReceived();
			connection.stats.messageReceived();
		} catch (Exception e) {
			logger.log(Level.WARNING, "ACNET message callback exception for connection '" + connection.connectedName() + "'", e);
		} finally {
			data = null;
			buf.free();
		}
	}

	@Override
	final public int status()
	{
		return 0;
	}
}
