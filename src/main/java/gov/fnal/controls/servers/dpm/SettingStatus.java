// $Id: SettingStatus.java,v 1.8 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;

public class SettingStatus
{
	final public long refId;

	WhatDaq whatDaq;
	int status;
	boolean completed;

	SettingStatus(long refId)
	{
		this.whatDaq = null;
		this.refId = refId;
		this.status = AcnetErrors.ACNET_UTIME;
		this.completed = false;
	}

	SettingStatus(WhatDaq whatDaq)
	{
		this.whatDaq = whatDaq;
		this.refId = whatDaq.refId;
		this.status = AcnetErrors.ACNET_UTIME;
		this.completed = false;
	}

	SettingStatus(long refId, int status)
	{
		this.refId = refId;
		this.whatDaq = null;
		this.status = status;
		this.completed = (status != 0);
	}

	void setStatus(int status)
	{
		this.status = status;
		this.completed = true;
	}

	public int status()
	{
		return status;
	}

	public boolean completed()
	{
		return completed;
	}

	public boolean successful()
	{
		return whatDaq != null && status == 0;
	}
}
