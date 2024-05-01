// $Id: DaqPool.java,v 1.9 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.HashMap;
import java.util.TreeMap;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

abstract class DaqPool implements DaqPoolUserRequests<WhatDaq>
{
	volatile private static int nextId = 1;
	static final HashMap<String, DaqPool> pools = new HashMap<>();

	final int id;	
	final Node node;

	final DataEvent event;

	DaqPool(Node node, DataEvent event)
	{
		this.id = nextId++;
		this.node = node;
		this.event = event;
	}

	static void nodeUp(Node node)
	{
		synchronized (pools) {
			for (DaqPool pool : pools.values()) {
				if (pool.node.equals(node))
					pool.process(true);
			}
		}
	}

	static DaqPool get(Node node, DataEvent event)
	{
		final String key = node.name() + "," + event.toString().toUpperCase() +
								(event.isRepetitive() ? "T" : "F");

		synchronized (pools) {
			DaqPool pool = pools.get(key);

			if (pool == null) {
/*
				if (event instanceof DeltaTimeEvent) {
					//final DeltaTimeEvent dtEvent = (DeltaTimeEvent) event;
					final long repeatRate = ((DeltaTimeEvent) event).getRepeatRate();

					if (repeatRate >= 20000) {
						final long delayTime = ((DeltaTimeEvent) event).immediate() ? 0 : repeatRate;

						pool = new RepetitiveMultiShotPool(node, delayTime, repeatRate);
						pools.put(key, pool);
						return pool;
					}
				}
*/

				pool = new RepetitiveDaqPool(node, event);
				pools.put(key, pool);
			}

			return pool;
		}
	}

	boolean isSetting()
	{
		return false;
	}

    static String dumpPools()
	{
		final String dash50 = "-----------------------------------------------------------";
		final TreeMap<String, DaqPool> map = new TreeMap<>();

		synchronized (pools) {
			for (DaqPool pool :  pools.values())
				map.put(pool.node.name() + "," + pool.event, pool);
		}

        final StringBuilder buf = new StringBuilder("[Pools]\n\n");

        for (DaqPool pool : map.values())
			buf.append(dash50).append(dash50).append('\n').append(pool).append('\n');

		buf.append(dash50).append(dash50);

        return buf.append('\n').toString();
    }
}
