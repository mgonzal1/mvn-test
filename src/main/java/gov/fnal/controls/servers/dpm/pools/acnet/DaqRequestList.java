// $Id: DaqRequestList.java,v 1.13 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.ArrayList;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.drf3.Event;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class DaqRequestList
{
	final Node node;
    final Event event;
    final boolean isSetting;
    final int maxMsgSize;
    final long timeout;
	final DaqDefinitions daqDefs;
    final ArrayList<WhatDaq> requests = new ArrayList<>();
    final Completable listCompletion;

    int rpySize;
    int reqSize;
    boolean modified = false;

	volatile DaqSendTransaction transaction;

    DaqRequestList(Node node, Event event, boolean isSetting, Completable listCompletion) 
    {
		this.node = node;
        this.event = event;
		this.isSetting = isSetting;
		this.listCompletion = listCompletion;
		this.timeout = event.defaultTimeout();
		this.daqDefs = DaqDefinitions.get(node);
		this.maxMsgSize = MaxReplySizeOverride.get(node.name(), daqDefs.MaxAcnetMessageSize); 
        this.reqSize = daqDefs.RequestOverhead;
        this.rpySize = daqDefs.ReplyOverhead;
		this.transaction = null;
    }

	int requestCount()
	{ 
		return requests.size();
	}

	int replySize()
	{
		return rpySize;
	}
	
    boolean add(WhatDaq whatDaq) 
    {
		final int length = whatDaq.length() + (whatDaq.length() & 1);
		final int reqRemaining = maxMsgSize - reqSize;
		final int rpyRemaining = maxMsgSize - rpySize;

		final boolean reqTooBig = isSetting ? (daqDefs.DeviceOverhead + length) >= reqRemaining : daqDefs.DeviceOverhead >= reqRemaining;
		final boolean rpyTooBig = isSetting ? daqDefs.StatusOverhead >= rpyRemaining : (daqDefs.StatusOverhead + length) >= rpyRemaining;

		if (reqTooBig || rpyTooBig) {
			logger.log(Level.FINER, "DaqRequestList.add(too big): reqSize:" + reqSize + " rpySize:" + rpySize + " " + whatDaq);
			return false;
		}

		if (isSetting) {
			reqSize += daqDefs.DeviceOverhead + length; 
			rpySize += daqDefs.StatusOverhead;
		} else {
			reqSize += daqDefs.DeviceOverhead; 
			rpySize += length + daqDefs.StatusOverhead;
		}

		logger.log(Level.FINER, "DaqRequestList.add: reqSize:" + reqSize + " rpySize:" + rpySize + " " + whatDaq);

		requests.add(whatDaq);
		modified = true;

		return true;
    }

    void send(boolean forceSend) 
    {
        if (modified || forceSend) {
			final DaqSendTransaction oldTransaction = transaction;

        	transaction = daqDefs.newSendTransaction(this);

			logger.log(Level.FINER, "DaqRequestList.send: reqSize:" + reqSize + " rpySize:" + rpySize);
			transaction.transmit();

			modified = false;

			if (oldTransaction != null)
				oldTransaction.cancel();
		}
    }

    void cancel() 
    {
		if (transaction != null)
			transaction.cancel();
    }
    
	@Override
    public String toString() 
    {
		final String hdr = String.format("DaqRequestList: count:=%-3d reqSize:%-5d rpySize:%-5d\n\n", 
											requests.size(), reqSize, rpySize);
		final StringBuilder buf = new StringBuilder(hdr);

		int ii = 1;
		for (WhatDaq whatDaq : requests)
			buf.append(String.format("   %3d) %s\n", ii++, whatDaq));

		return buf.toString();
    }
}
