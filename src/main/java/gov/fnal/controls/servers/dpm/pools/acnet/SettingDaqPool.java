// $Id: SettingDaqPool.java,v 1.14 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.List;
import java.util.Iterator;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;

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
	public void insert(WhatDaq newWhatDaq)
	{
		if (newWhatDaq.length() > DaqDefinitions.MaxReplyDataLength()) {
			PoolSegmentAssembly.insert(newWhatDaq, this, 8192, false);
			return;
		}

		final Iterator<WhatDaq> iter = queuedReqs.iterator();

		while (iter.hasNext()) {
			final WhatDaq whatDaq = iter.next();

			if (whatDaq.isEqual(newWhatDaq)) {
				whatDaq.getReceiveData().receiveStatus(DAE_SETTING_SUPERCEDED, System.currentTimeMillis(), 0);
				iter.remove();
				break;
			}
		}

		queuedReqs.add(newWhatDaq);
	}

	@Override
	public String toString()
	{
		return "SettingDaqPool: " + super.toString();
	}
}
