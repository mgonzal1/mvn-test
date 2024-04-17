// $Id: AcnetMessage.java,v 1.3 2024/02/22 16:29:45 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.logging.Level;

public class AcnetMessage extends AcnetPacket
{
	//AcnetMessage(AcnetConnection connection, Buffer buf)
	AcnetMessage(Buffer buf)
	{
		//super(connection, buf);
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
			AcnetInterface.logger.log(Level.WARNING, "ACNET message callback exception for connection '" + connection.connectedName() + "'", e);
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
