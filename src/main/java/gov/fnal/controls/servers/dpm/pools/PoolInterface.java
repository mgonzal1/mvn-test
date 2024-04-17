// $Id: PoolInterface.java,v 1.5 2023/10/04 19:40:29 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import gov.fnal.controls.servers.dpm.SettingData;

import java.util.List;

public interface PoolInterface
{
	PoolType type();
	void addRequest(WhatDaq whatDaq);
	void addSetting(WhatDaq whatDaq, SettingData setting);
	void processRequests();
	void cancelRequests();
	List<WhatDaq> requests();
	String dumpPool();
}

