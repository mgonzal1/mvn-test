// $Id: SharedWhatDaq.java,v 1.9 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Iterator;
import java.util.LinkedList;
import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;

class SharedWhatDaq extends WhatDaq implements ReceiveData
{
	final LinkedList<WhatDaq> users = new LinkedList<>();

	SharedWhatDaq(WhatDaq whatDaq)
	{
		super(whatDaq);

		setReceiveData(this);
		users.add(whatDaq);
	}

	synchronized public boolean addUser(WhatDaq whatDaq)
	{
		if (whatDaq == null)
			throw new NullPointerException();

		boolean replace = false;

		for (WhatDaq tmp : users) {
			if (tmp.list().equals(whatDaq.list())) 
				replace = true;
		}

		users.add(whatDaq);

		return replace;
	}

	synchronized public int removeDeletedUsers()
	{
		int totalDeleted = 0;
		final Iterator<WhatDaq> iter = users.iterator();

		while (iter.hasNext()) {
			if (iter.next().isMarkedForDelete()) {
				iter.remove();
				totalDeleted++;
			}
		}

		return totalDeleted;
	}

	public Iterator<WhatDaq> iterator()
	{
		return users.iterator();	
	}

	synchronized public boolean isEmpty()
	{
		return users.isEmpty();
	}

	@Override
	synchronized public void receiveData(byte[] data, int offset, long timestamp, long cycle)
	{
		for (final WhatDaq user : users) {
			try {
				user.getReceiveData().receiveData(data, offset, timestamp, cycle);
			} catch (Exception e) {
				user.setMarkedForDelete();
			}
		}
	}

	@Override
	synchronized public void receiveData(ByteBuffer data, long timestamp, long cycle)
	{
		data.mark();

		for (final WhatDaq user : users) {
			try {
				user.getReceiveData().receiveData(data, timestamp, cycle);
			} catch (Exception e) {
				user.setMarkedForDelete();
			}
			data.reset();
		}
	}


	@Override
	synchronized public void receiveStatus(int status, long timestamp, long cycle)
	{
		for (final WhatDaq user : users) {
			try {
				user.getReceiveData().receiveStatus(status, timestamp, cycle);
			} catch (Exception e) {
				user.setMarkedForDelete();
			}
		}
	}
}
