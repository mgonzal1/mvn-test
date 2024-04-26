// $Id: DaqSendAcnet.java,v 1.9 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Collection;
import java.util.ArrayList;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

class DaqSendAcnet implements DaqSendInterface
{
	private final Node node;
	private final DataEvent event;
	private final boolean isSetting;
	private final long timeout;
	private final Completable listCompletion;
	private final ArrayList<DaqRequestList> reqLists = new ArrayList<>();

	DaqSendAcnet(Node node, DataEvent event, boolean isSetting, 
					Completable listCompletion, long timeout)
	{
		this.node = node;
		this.event = event;
		this.isSetting = isSetting;
		this.listCompletion = listCompletion;
		this.timeout = timeout;
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

		final DaqRequestList reqList = new DaqRequestList(node, event, isSetting, listCompletion, timeout);

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

		//final StringBuilder buf = new StringBuilder("DaqSendAcnet:\n");

		//buf.append("\tevent:").append(event).append('\n');
		//buf.append("\tisSet:").append(isSetting).append('\n');
		//buf.append("\t").append("timeout:").append(timeout).append('\n');
		//buf.append("\trequest count:").append(requestCount()).append('\n');
		//buf.append("\tlist count:").append(reqLists.size()).append('\n');
		//buf.append("\tcompletion:").append(listCompletion).append('\n');

		return buf.toString();
	}
}
