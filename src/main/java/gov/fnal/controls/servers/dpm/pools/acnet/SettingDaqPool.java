// $Id: SettingDaqPool.java,v 1.11 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import java.util.Iterator;

public class SettingDaqPool extends OneShotDaqPool implements Completable, AcnetErrors
{
	public SettingDaqPool(Node node)
	{
		super(node);
	}

	@Override
	public boolean isSetting()
	{
		return true;
	}

	@Override
	public void insert(WhatDaq whatDaq)
	{
		if (whatDaq.getLength() > DaqDefinitions.MaxReplyDataLength()) {
			PoolSegmentAssembly.insert(whatDaq, this, 8192, false);
			return;
		}

		Iterator<WhatDaq> url = queuedReqs.iterator();

		WhatDaq userPerhaps;
		while (url.hasNext()) {
			userPerhaps = (WhatDaq) url.next();
			if (userPerhaps.isEqual(whatDaq)) {
				userPerhaps.getReceiveData().receiveStatus(DAE_SETTING_SUPERCEDED, System.currentTimeMillis(), 0);
				url.remove();
				break;
			}
		}
		queuedReqs.add(whatDaq);
	}

	@Override
	public String toString()
	{
		return "SettingDaqPool " + super.toString();
	}
}
