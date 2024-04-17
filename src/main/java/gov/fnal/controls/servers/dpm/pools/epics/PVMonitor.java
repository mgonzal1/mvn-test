// $Id: PVMonitor.java,v 1.2 2023/05/25 16:57:54 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.epics;

import org.epics.pva.client.PVAChannel;
import org.epics.pva.data.PVAStructure;

import java.util.BitSet;

public class PVMonitor implements EpicsListener
{
	static {
		gov.fnal.controls.servers.dpm.DPMServer.setLogLevel(java.util.logging.Level.parse(System.getProperty("loglevel", "INFO")));
	}

	@Override
	synchronized public void handleData(final PVAChannel channel, final BitSet changes, final BitSet overruns, final PVAStructure data)
	{
		System.out.println(data.toString());
	}

	public static void main(String[] args) throws Exception
	{
		final PVMonitor listener = new PVMonitor();

		synchronized (listener) {
			PVAPool.subscribe(new EpicsRequest(args[0], listener));
			listener.wait();
		}
	}
}
