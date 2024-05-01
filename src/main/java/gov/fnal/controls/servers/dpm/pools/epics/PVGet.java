// $Id: PVGet.java,v 1.4 2023/09/26 20:52:04 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.epics;

import java.util.BitSet;

import org.epics.pva.data.PVAStructure;
import org.epics.pva.client.PVAChannel;

import gov.fnal.controls.servers.dpm.pools.epics.PVAPool;
import gov.fnal.controls.servers.dpm.pools.epics.EpicsRequest;
import gov.fnal.controls.servers.dpm.pools.epics.EpicsListener;

public class PVGet implements EpicsListener
{
	static {
		gov.fnal.controls.servers.dpm.DPMServer.setLogLevel(java.util.logging.Level.parse(System.getProperty("loglevel", "INFO")));
	}

	@Override
	synchronized public void handleData(final PVAChannel channel, final BitSet changes, final BitSet overruns, final PVAStructure data)
	{
		System.out.println(data.toString());
		notify();
	}

	public static void main(String[] args) throws Exception
	{
		//PVAPool.init();

		final PVGet listener = new PVGet();

		synchronized (listener) {
			PVAPool.get(new EpicsRequest(args[0], listener));
			listener.wait();
		}
	}
}
