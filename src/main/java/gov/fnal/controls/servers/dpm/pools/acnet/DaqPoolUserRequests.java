// $Id: DaqPoolUserRequests.java,v 1.3 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.PoolUser;

public interface DaqPoolUserRequests<R>
{
	public void insert(R request);
	public void cancel(PoolUser user, int error);
	public boolean process(boolean forceRetransmission);
}
