// $Id: RepetitiveMultiShotPool.java,v 1.7 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.PoolUser;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class RepetitiveMultiShotPool extends RepetitiveDaqPool implements Completable, AcnetErrors
{
	static final Timer sharedTimer = new Timer("RepetitiveMultiShotPoolTimer", true);

	class ShotTask extends TimerTask
	{
		public void run()
		{
			doShot();
		}
	}

	ShotTask shotTask;

    final OneShotDaqPool pool;
	final long delayTime;
	final long repeatRate;

    public RepetitiveMultiShotPool(Node node, long delayTime, long repeatRate)
	{
        super(node, null);

		this.shotTask = new ShotTask();
    	this.pool = new OneShotDaqPool(node);
		this.delayTime = delayTime;
		this.repeatRate = repeatRate;
    }

	//@Override
    //public void update(DataEvent userEvent, DataEvent currentEvent)
	//{
	//	process();
	//}

	private void doShot()
	{
		procTime = System.currentTimeMillis();

		logger.log(Level.FINE, "Pool " + id + " " + repeatRate / 1000 + " second shot");
		logger.log(Level.FINE, toString());

		final int remaining = removeDeletedRequests();

		//if (remaining == 0)
		//	event.deleteObserver(this);
		//else
		//	event.addObserver(this);

		synchronized (requests) {
			pool.insert(requests);
			pool.process(true);
			//updateTime = System.currentTimeMillis();
			++totalTransactions;

			//if (!event.isRepetitive()) {
			//	requests.clear();
			//	event.deleteObserver(this);
			//}

		}

		//return true;
	}

	@Override
	public boolean process(boolean forceRetransmission)
	{
		shotTask.cancel();
		shotTask = new ShotTask();
		sharedTimer.schedule(shotTask, delayTime, repeatRate);

		return true;
    }

	@Override
    public void cancel(PoolUser user, int error)
	{
		logger.log(Level.FINE, "Pool " + id + " " + repeatRate / 1000 + " second shot cancelled");
		logger.log(Level.FINE, toString());

		shotTask.cancel();
		super.cancel(user, error);
	}

	@Override
    public void completed(int status)
	{
        if (status != ACNET_PEND) {
			lastCompletedStatus = status;

			//if (status == ACNET_UTIME) //{
			//	doShot();

			updateTime = System.currentTimeMillis();

			//++totalTransactions;

			//if (!event.isRepetitive())
			//	process();

			//multiShotPool.process(false);
		}
    }

	@Override
	public String toString()
	{
		final String header =  String.format("RepetitiveMultiShotPool %-4d %-10s event:%-16s status:%04x procd:%tc",
												id, node.name(), event, lastCompletedStatus & 0xffff, procTime);

		final StringBuilder buf = new StringBuilder(header);

		buf.append("\n\n");

		int ii = 1;
		for (WhatDaq whatDaq : requests)
			buf.append(String.format(" %3d) %s\n", ii++, whatDaq));

		buf.append('\n');
		buf.append(xtrans);

        buf.append("\ninserts: ").append(totalInserts);
        buf.append(", procs: ").append(totalProcs);
        buf.append(", packets: ").append(totalTransactions);

		return buf.append('\n').toString();
	}
}
