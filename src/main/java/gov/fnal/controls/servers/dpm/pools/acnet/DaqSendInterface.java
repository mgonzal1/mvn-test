// $Id: DaqSendInterface.java,v 1.5 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Collection;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;

interface DaqSendInterface
{
	void begin();
	boolean add(WhatDaq req);
	void send(boolean forceResend);
	void send(Collection<WhatDaq> items);
	int requestCount();
	void cancel();
}
