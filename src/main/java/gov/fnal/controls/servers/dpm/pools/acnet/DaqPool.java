// $Id: DaqPool.java,v 1.11 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.HashMap;
import java.util.TreeMap;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.drf3.Event;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

abstract class DaqPool implements DaqPoolUserRequests<WhatDaq>
{
	volatile private static int nextId = 1;
	static final HashMap<String, DaqPool> pools = new HashMap<>();

	final int id;	
	final Node node;

	final Event event;

	DaqPool(Node node, Event event)
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

	static DaqPool get(Node node, Event event)
	{
		final String key = node.name() + "-" + event + "-" + (event.isRepetitive() ? "R" : "O");

		synchronized (pools) {
			DaqPool pool = pools.get(key);

			if (pool == null) {
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
