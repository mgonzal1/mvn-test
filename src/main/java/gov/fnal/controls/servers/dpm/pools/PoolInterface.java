// $Id: PoolInterface.java,v 1.6 2024/03/27 21:04:42 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import java.util.List;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.SettingData;

public interface PoolInterface
{
	PoolType type();
	void addRequest(WhatDaq whatDaq);
	void addSetting(WhatDaq whatDaq, SettingData setting) throws AcnetStatusException;
	void processRequests();
	void cancelRequests();
	List<WhatDaq> requests();
	String dumpPool();
}

