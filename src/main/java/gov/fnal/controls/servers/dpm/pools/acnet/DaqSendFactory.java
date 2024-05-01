// $Id: DaqSendFactory.java,v 1.9 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Collection;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.acnetlib.NodeFlags;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class DaqSendFactory implements NodeFlags
{
	private static DaqSendInterface nullSendInterface = new DaqSendInterface()
		{
			@Override
			public void begin() { }

			@Override
			public boolean add(WhatDaq req) { return false; }

			@Override
			public void send(boolean forceResend) { }

			@Override
			public void send(Collection<WhatDaq> items) { }
			
			@Override
			public int requestCount() { return 0; }

			@Override
			public void cancel() { }

			@Override
			public String toString() { return "NullSendInterface"; }
		};

	static DaqSendInterface init()
	{
		return nullSendInterface;
	}

	static DaqSendInterface getDaqSendInterface(Node node, DataEvent event, boolean isSetting, 
														Completable completable, long timeout)
	{
		return new DaqSendAcnet(node, event, isSetting, completable, timeout);

		//logger.log(Level.FINE, "using RETDAT for " + node);

		//return new DaqSendAcnet(node, event, isSetting, completable, timeout);
	}
}
