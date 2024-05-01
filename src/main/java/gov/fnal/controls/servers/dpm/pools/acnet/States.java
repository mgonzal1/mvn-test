// $Id: States.java,v 1.9 2024/04/01 15:28:45 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.HashSet;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.concurrent.LinkedBlockingQueue;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetMessage;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetMessageHandler;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.pools.AcceleratorPool;
import gov.fnal.controls.servers.dpm.pools.DeviceCache;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class States extends Thread implements AcnetMessageHandler
{
	class State
	{
		final int di;
		final int state;
		final long date;

		State(int di, int state, long date)
		{
			this.di = di;
			this.state = state;
			this.date = date;
		}
	}

	final AcnetConnection connection;
	final LinkedBlockingQueue<State> msgQ = new LinkedBlockingQueue<>();
	final int v_feup, v_fesu, v_acnet;

	static void init() throws AcnetStatusException
	{
		new States(AcnetInterface.open("STATES"));
	}

	private int di(String name)
	{
		try {
			return DeviceCache.di(name);
		} catch (Exception ignore) {
			logger.log(Level.WARNING, "Device name '" + name + "' not found");
		}
	
		return 0;
	}

	private States(AcnetConnection connection) throws AcnetStatusException
	{
		this.connection = connection;

		this.v_feup = di("V:FEUP");
		this.v_fesu = di("V:FESU");
		this.v_acnet = di("V:ACNET");

		this.connection.handleMessages(this);
		setName("STATES handler");
		start();
	}

	@Override
	public void run()
	{
		while (true) {
			try {
				final State report = msgQ.take();
				final int di = report.di;
				final int state = report.state;
				
				if (di == v_feup || di == v_fesu) {
					logger.log(Level.FINE, "Node " + Node.get(report.state) + " is up");
					AcceleratorPool.nodeUp(Node.get(report.state));
				} else if (di == v_acnet && (state == 1 || state == 2))
					Node.cacheNodes();
			} catch (InterruptedException e) {
				break;
			} catch (Exception ignore) { }
		}
	}

	@Override
    public void handle(AcnetMessage r)
	{
		try {
			final ByteBuffer buf = r.data().order(ByteOrder.LITTLE_ENDIAN);
			final int version = buf.getShort() & 0xffff;
			
			if (version == 1) {
				final int seqNo = buf.getShort() & 0xffff;
				final int count = buf.getInt();

				for (int ii = 0; ii < count; ii++) {
					final int typeCode = buf.getShort() & 0xffff;

					if (typeCode == 31) {
						final int di = buf.getInt();
						final int state = buf.getShort() & 0xffff;
						final long seconds = buf.getInt() & 0xffffffff;
						final long nanoSeconds = buf.getInt() & 0xffffffff;
						final long date = (seconds * 1000) + (nanoSeconds / 1000000);

						msgQ.add(new State(di, state, date));
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception handling states message", e);
		}
	}
}

