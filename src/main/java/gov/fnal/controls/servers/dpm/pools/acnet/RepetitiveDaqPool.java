// $Id: RepetitiveDaqPool.java,v 1.21 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.logging.Level;
import java.nio.ByteBuffer;
import java.util.TimerTask;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.acnetlib.NodeFlags;

import gov.fnal.controls.servers.dpm.drf3.Event;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.PoolUser;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.AcceleratorPool;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class RepetitiveDaqPool extends DaqPool implements NodeFlags, Completable, AcnetErrors
{
    private static Recovery recovery = new Recovery();

	int totalProcs = 0;
	int numInserts = 0;
	int totalInserts = 0;
	int totalTransactions = 0;
	volatile int lastCompletedStatus = 0;
	long procTime = 0;
	long updateTime = 0;

	final LinkedList<WhatDaq> requests = new LinkedList<>();
	DaqSendInterface xtrans = DaqSendFactory.init();

    RepetitiveDaqPool(Node node, Event event)
	{
        super(node, event);
    }

	boolean recovery(long now)
	{
        if (requests.size() != 0) {
			if (lastCompletedStatus != 0) {
                process(true);
				return true;
            } else if (node.has(EVENT_STRING_SUPPORT)) {
				if (procTime != 0 && (now - procTime > 120000L) && updateTime == 0 || (now - updateTime > 120000L)) {
                	process(true);
					return true;
				}
            }
        }

		return false;
	}

	@Override
    public void insert(WhatDaq whatDaq)
	{
        ++totalInserts;

        if (whatDaq.length() > DaqDefinitions.MaxReplyDataLength()) {
            PoolSegmentAssembly.insert(whatDaq, this, DaqDefinitions.SegmentSize(), true);
            return;
        }

        synchronized (requests) {
            Iterator<WhatDaq> url = requests.iterator();

            while (url.hasNext()) {
                final SharedWhatDaq shared = (SharedWhatDaq) url.next();

                if (shared.isEqual(whatDaq)) {

                    synchronized (shared) {
                        if (!shared.addUser(whatDaq)) {
							AcceleratorPool.consolidationHit();
						}
                    }
                    return;
                }
            }
            
            requests.add(new SharedWhatDaq(whatDaq));
            numInserts++;
        }
    }

	@Override
    public void cancel(PoolUser user, int error)
	{
        final long now = System.currentTimeMillis();

        synchronized (requests) {
            final Iterator<WhatDaq> url = requests.iterator();

            while (url.hasNext()) {
                final SharedWhatDaq shared = (SharedWhatDaq) url.next();

                synchronized (shared) {
                    final Iterator<WhatDaq> whatDaqs = shared.iterator();

                    for (WhatDaq whatDaq; whatDaqs.hasNext();) {
                        whatDaq = whatDaqs.next();

                        if (user == null || whatDaq.user() == user) {
                            if (error != 0)
								whatDaq.getReceiveData().receiveStatus(error, now, 0);

                            whatDaq.setMarkedForDelete();
                        }
                    }
                }
            }
        }

		recovery.remove(this);
    }

	protected final int removeDeletedRequests()
	{
		int totalDeleted = 0;

        synchronized (requests) {
            final Iterator<WhatDaq> iter = requests.iterator();

            while (iter.hasNext()) {
				final SharedWhatDaq shared = (SharedWhatDaq) iter.next();
				final int deleted = shared.removeDeletedUsers();

				if (shared.isEmpty())
					iter.remove();

				totalDeleted += deleted;
            }
        }

		return totalDeleted;
	}

	@Override
	public boolean process(boolean forceXmit)
	{
		final long now = System.currentTimeMillis();
        final boolean inserts = numInserts > 0;
		final boolean deletes = removeDeletedRequests() > 0;
		final boolean changes = inserts || deletes;

        numInserts = 0;

		if (requests.size() == 0) {
			xtrans.cancel();
			recovery.remove(this);
            return false;
        }
        
        if (!changes) {
            if (forceXmit) {
                lastCompletedStatus = 0;
				++totalProcs;
				procTime = now;
				xtrans.send(true);
				return true;
            } else {
                return false;
            }
        }

        ++totalProcs;

		synchronized (requests) {
			xtrans.cancel();
			lastCompletedStatus = 0;

			xtrans = DaqSendFactory.getDaqSendInterface(node, event, false, this);
			procTime = now;
			xtrans.send(requests);
			recovery.add(this);

			return true;
		}
    }

	@Override
    public void completed(int status)
	{
        if (status != ACNET_PEND) {
			lastCompletedStatus = status;
			updateTime = System.currentTimeMillis();

			++totalTransactions;
		}
    }

	@Override
	public String toString()
	{
		final String header =  String.format("RepetitiveDaqPool %-4d %-10s event:%-16s status:%04x procd:%tc",
												id, node.name(), event, lastCompletedStatus & 0xffff, procTime);

		final StringBuilder buf = new StringBuilder(header);

		buf.append("\n");
		buf.append(xtrans);
        buf.append("\ninserts: ").append(totalInserts);
        buf.append(", procs: ").append(totalProcs);
        buf.append(", packets: ").append(totalTransactions);

		return buf.append('\n').toString();
	}

    private static class Recovery extends TimerTask
	{
		final static long RecoveryPeriod = 3 * 60 * 1000;
    	final HashMap<Integer, RepetitiveDaqPool> pools = new HashMap<>();

        Recovery()
		{
			AcnetPoolImpl.sharedTimer.scheduleAtFixedRate(this, RecoveryPeriod, RecoveryPeriod);
        }

        synchronized void add(RepetitiveDaqPool pool)
		{
            pools.put(pool.id, pool);
        }

        synchronized void remove(RepetitiveDaqPool pool)
		{
            pools.remove(pool.id);
        }

		@Override
        synchronized public void run()
		{
			final long now = System.currentTimeMillis();

			for (RepetitiveDaqPool pool : pools.values()) {
				final boolean resent = pool.recovery(now);

				logger.log(Level.FINER, () -> String.format("Repetitive pool %6d recovery for %-10s event:%-16s entries:%5d action:[%6s]", 
															pool.id, pool.node.name(), pool.event, pool.requests.size(), resent ? "RESENT" : "")); 
			}
        }
    }
}
