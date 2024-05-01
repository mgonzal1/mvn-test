// $Id: OneShotDaqPool.java,v 1.12 2024/03/27 20:58:32 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.OnceImmediateEvent;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.PoolUser;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class OneShotDaqPool extends DaqPool implements Completable, AcnetErrors
{
    private static final DataEvent event = new OnceImmediateEvent();

	int lastCompletedStatus = 0;
	long updateTime = 0;
	long procTime = 0;

    ArrayList<WhatDaq> activeReqs = new ArrayList<>();
    LinkedList<WhatDaq> queuedReqs = new LinkedList<>();

	DaqSendInterface xtrans = null;

    public OneShotDaqPool(Node Node)
	{
		super(Node, event);
    }

	@Override
    synchronized public void insert(WhatDaq whatDaq)
	{
        if (whatDaq.length() > DaqDefinitions.MaxReplyDataLength()) {
            PoolSegmentAssembly.insert(whatDaq, this, 2048, false);
            return;
        }

        queuedReqs.add(whatDaq);
    }

	synchronized void insert(Collection<WhatDaq> whatDaqs)
	{
		for (WhatDaq whatDaq : whatDaqs)
			insert(whatDaq);
	}

	@Override
    synchronized public void cancel(PoolUser user, int error)
	{
		Iterator<WhatDaq> iter = activeReqs.iterator();

		while (iter.hasNext()) {
			final WhatDaq whatDaq = iter.next();

			if (user == null || whatDaq.getUser() == user) {
				whatDaq.getReceiveData().receiveStatus(error);
				iter.remove();
			}
        }

        iter = queuedReqs.iterator();

		while (iter.hasNext()) {
			final WhatDaq whatDaq = (WhatDaq) iter.next();

			if (user == null || whatDaq.getUser() == user) {
				whatDaq.getReceiveData().receiveStatus(error);
			}
        }
    }

	@Override
    synchronized public boolean process(boolean __)
	{
		procTime = System.currentTimeMillis();
            
		try {
			if (xtrans != null) {
				try {
					for (WhatDaq whatDaq : activeReqs)
						whatDaq.getReceiveData().receiveStatus(ACNET_UTIME);

					activeReqs.clear();
					xtrans.cancel();
					xtrans = null;
				} catch (Exception e) {
				}
			}

			xtrans = DaqSendFactory.getDaqSendInterface(node, event, isSetting(), this, event.defaultTimeout());
			xtrans.begin();
		} catch (Exception e) {
			logger.log(Level.FINE, "OneShotDaqPool: exception", e);
			return false;
		}

		WhatDaq whatDaq;

		while ((whatDaq = queuedReqs.peekFirst()) != null) {
			if (xtrans.add(whatDaq)) {
				queuedReqs.removeFirst();
				activeReqs.add(whatDaq);
			} else
				break;
		}

		if (activeReqs.size() > 0) {
			xtrans.send(true);
			return true;
		} else {
			xtrans = null;
			return false;
		}
    }

	@Override
    synchronized public void completed(int status)
	{
        lastCompletedStatus = status;
        updateTime = System.currentTimeMillis();

		if (status != 0) {
			for (WhatDaq whatDaq : activeReqs)
				whatDaq.getReceiveData().receiveStatus(status);
		}

		activeReqs.clear();
        xtrans = null;

		process(false);
    }

    public static String dumpPools()
	{
		final StringBuilder buf = new StringBuilder("[OneShotDaqPool]\n");

		return buf.toString();
    }
}
