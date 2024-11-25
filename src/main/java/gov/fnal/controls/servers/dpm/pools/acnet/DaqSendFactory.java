// $Id: DaqSendFactory.java,v 1.11 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Collection;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.drf3.Event;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class DaqSendFactory
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

	static DaqSendInterface getDaqSendInterface(Node node, Event event, boolean isSetting, Completable completable)
	{
		return new DaqSendAcnet(node, event, isSetting, completable);
	}
}
