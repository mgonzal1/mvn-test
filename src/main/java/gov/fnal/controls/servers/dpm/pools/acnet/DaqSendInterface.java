// $Id: DaqSendInterface.java,v 1.4 2023/06/12 16:22:10 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Collection;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;

interface DaqSendInterface
{
	void begin();
	boolean add(WhatDaq req);
	void send(boolean forceResend);
	void send(Collection<WhatDaq> items);
	//void transactionLimit(int limit);	
	int requestCount();
	void cancel();
}
