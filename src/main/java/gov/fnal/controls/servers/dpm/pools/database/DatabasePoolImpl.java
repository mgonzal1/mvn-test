// $Id: DatabasePoolImpl.java,v 1.6 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.database;

import java.util.ArrayList;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;

import gov.fnal.controls.servers.dpm.SettingData;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.PoolType;
import gov.fnal.controls.servers.dpm.pools.PoolInterface;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class DatabasePoolImpl implements PoolInterface, AcnetErrors
{
	private final ArrayList<WhatDaq> requests = new ArrayList<>();

	@Override
	public PoolType type()
	{
		return PoolType.DATABASE;
	}

	@Override
	public void addRequest(WhatDaq whatDaq)
	{
		requests.add(whatDaq);
	}

	@Override
	public void addSetting(WhatDaq whatDaq, SettingData __)
	{
		whatDaq.getReceiveData().receiveStatus(DPM_BAD_REQUEST, System.currentTimeMillis(), 0);
	}

	@Override
	public void processRequests()
	{
		final long t = System.currentTimeMillis();

		for (WhatDaq whatDaq : requests) {
			final ReceiveData r = whatDaq.getReceiveData();

			switch (whatDaq.property) {
			 case DESCRIPTION:
			 	r.receiveData(whatDaq.dInfo.description, t, 0);
				break;

			 case INDEX:
			 	r.receiveData((double) whatDaq.dInfo.di, t, 0);
			 	break;

			 case LONG_NAME:
			 	r.receiveData(whatDaq.dInfo.longName, t, 0);
			 	break;

			 case ALARM_LIST_NAME:
			 	r.receiveData(whatDaq.alarmListName, t, 0);
			 	break;
			}
		}
	}

	@Override
	public void cancelRequests()
	{
		requests.clear();
	}

	@Override
	public ArrayList<WhatDaq> requests()
	{
		return new ArrayList<WhatDaq>();
	}

	@Override
	public String dumpPool()
	{
		return "";
	}
}
