// $Id: SharedWhatDaq.java,v 1.5 2023/06/20 20:52:51 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

/*
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import gov.fnal.controls.servers.dpm.CollectionContext;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
//import gov.fnal.controls.servers.dpm.pools.ReceiveData;

public class SharedWhatDaq implements ReceiveData //extends WhatDaq //implements ReceiveData
{
	private final ArrayList<WhatDaq> users = new ArrayList<>();
	private final int di;
	private final int pi;
	private final boolean isSetting;
	private final int length;
	private final int offset;

	public SharedWhatDaq(WhatDaq whatDaq) //, boolean nonRepetitive)
	{
		this.di = whatDaq.di;
		this.pi = whatDaq.pi;
		this.isSetting = whatDaq.isSetting;
		this.length = whatDaq.length;
		this.offset = whatDaq.offset;
		//super(whatDaq, 0);

		//this.receiveData = this;
		addUser(whatDaq);
	}

	@Override
	public boolean isEqual(WhatDaq whatDaq)
	{
		if (di != whatDaq.di() || pi != whatDaq.pi || isSetting != whatDaq.isSetting
				|| length != whatDaq.length || offset != whatDaq.offset)
			return false;
	}

	synchronized public void addUser(WhatDaq whatDaq)
	{
		if (whatDaq == null)
			throw new NullPointerException();

		users.add(whatDaq);
	}

	public List<WhatDaq> getUsers()
	{
		return users;
	}

	synchronized int removeDeletedUsers()
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

	synchronized boolean isEmpty()
	{
		return users.isEmpty();
	}

	@Override
	synchronized public void receiveData(byte[] data, int offset, long timestamp, long cycle)
	{
		final boolean delete = !event().isRepetitive();

		for (final WhatDaq user : users) {
			try {
				user.getReceiveData().receiveData(data, offset, timestamp, cycle);
				if (delete)
					user.setMarkedForDelete();
			} catch (Exception e) {
				user.setMarkedForDelete();
			}
		}
	}

	@Override
	synchronized public void receiveStatus(int status, long timestamp, long cycle)
	{
		final boolean delete = !event().isRepetitive();

		for (final WhatDaq user : users) {
			try {
				user.getReceiveData().receiveData(status, timestamp, cycle);
				if (delete)
					user.setMarkedForDelete();
			} catch (Exception e) {
				user.setMarkedForDelete();
			}
		}
	}

	@Override
	synchronized public void receiveData(WhatDaq whatDaq, int error, int offset, byte[] data, 
											long timestamp, CollectionContext context)
	{
		for (final WhatDaq user : users) {
			try {
				user.getReceiveData().receiveData(user, error, offset, data, timestamp, context);
				if (!whatDaq.event().isRepetitive())
					user.setMarkedForDelete();
			} catch (Exception e) {
				user.setMarkedForDelete();
			}
		}
	}
}
*/
