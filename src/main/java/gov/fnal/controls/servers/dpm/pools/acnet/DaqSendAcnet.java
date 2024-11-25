// $Id: DaqSendAcnet.java,v 1.11 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Collection;
import java.util.ArrayList;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.drf3.Event;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

class DaqSendAcnet implements DaqSendInterface
{
	final Node node;
	final Event event;
	final boolean isSetting;
	final Completable listCompletion;
	final ArrayList<DaqRequestList> reqLists = new ArrayList<>();

	DaqSendAcnet(Node node, Event event, boolean isSetting, Completable listCompletion)
	{
		this.node = node;
		this.event = event;
		this.isSetting = isSetting;
		this.listCompletion = listCompletion;
	}

	@Override
	synchronized public void send(Collection<WhatDaq> whatDaqs)
	{
		cancel();

		for (WhatDaq whatDaq : whatDaqs)
			add(whatDaq);

		send(false);
	}

	@Override
	synchronized public void send(boolean forceResend)
	{
		for (DaqRequestList reqList : reqLists)
			reqList.send(forceResend);
	}

	@Override
	public void begin()
	{
	}

	@Override
	synchronized public boolean add(WhatDaq whatDaq)
	{
		for (DaqRequestList reqList : reqLists)
			if (reqList.add(whatDaq))
				return true;

		final DaqRequestList reqList = new DaqRequestList(node, event, isSetting, listCompletion);

		reqLists.add(reqList);

		return reqList.add(whatDaq);
	}

	@Override
	synchronized public void cancel()
	{
		for (DaqRequestList reqList : reqLists)
			reqList.cancel();

		reqLists.clear();
	}

	@Override
	synchronized public int requestCount()
	{
		int count = 0;

		for (DaqRequestList l : reqLists)
			count += l.requestCount();

		return count;
	}

	@Override
	public String toString()
	{
		final StringBuilder buf = new StringBuilder(String.format("DaqSendAcnet: isSetting:%s list count:%-3d\n", 
																	isSetting, reqLists.size()));

		int ii = 1;
		for (DaqRequestList reqList : reqLists)
			buf.append(String.format("%3d) %s\n", ii++, reqList));

		return buf.toString();
	}
}
