// $Id: AcceleratorPool.java,v 1.25 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.drf3.Property;
import gov.fnal.controls.servers.dpm.SettingData;
import gov.fnal.controls.servers.dpm.ConsoleUser;
import gov.fnal.controls.servers.dpm.DataSource;
import gov.fnal.controls.servers.dpm.pools.acnet.AcnetPoolImpl;
import gov.fnal.controls.servers.dpm.pools.acnet.FTPPoolImpl;
import gov.fnal.controls.servers.dpm.pools.epics.EpicsPoolImpl;
import gov.fnal.controls.servers.dpm.pools.epics.PVAPool;
import gov.fnal.controls.servers.dpm.pools.database.DatabasePoolImpl;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class AcceleratorPool
{
	private static AcnetConnection connection;
	volatile static int consolidationHits;

	private final ByteBuffer buf;

	final PoolInterface pools[] = { new AcnetPoolImpl(), new EpicsPoolImpl(), 
									new DatabasePoolImpl(), new FTPPoolImpl() };

	public static void init() throws Exception
	{
		consolidationHits = 0;
		DeviceCache.init();

		AcnetPoolImpl.init();
		EpicsPoolImpl.init();
		FTPPoolImpl.init();

		connection = AcnetInterface.open("SETSDB");
	}

	public static void consolidationHit()
	{
		consolidationHits++;
	}

	public static int consolidationHits()
	{
		return consolidationHits;
	}

	private PoolInterface getPool(WhatDaq whatDaq)
	{
		final DataSource dataSource = whatDaq.request.dataSource();

		if (dataSource instanceof DatabaseSource) {
			return pools[2];
		} else if (dataSource instanceof AcnetSource) {
			if ((whatDaq.property == Property.READING) && (whatDaq.getEventFrequency() > 20.0)) {
				logger.log(Level.FINE, whatDaq + " was upgraded to FTP protocol for frequency - " + whatDaq.getEventFrequency() + "Hz for event: " + whatDaq.event());
				return pools[3];
			} else {
				return pools[0];
			}
		} else if (dataSource instanceof EpicsSource) {
			logger.log(Level.FINE, whatDaq + " is an Epics type device" );
			return pools[1];	
		}

		return pools[0];
	}

	public static void nodeUp(Node node)
	{
		AcnetPoolImpl.nodeUp(node);
		EpicsPoolImpl.nodeUp(node);
		FTPPoolImpl.nodeUp(node);
	}

	public AcceleratorPool()
	{
		this.buf = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);
	}

	synchronized public void addRequest(WhatDaq whatDaq)
	{
		getPool(whatDaq).addRequest(whatDaq);
	}

	synchronized public void addSetting(WhatDaq whatDaq, SettingData setting) throws AcnetStatusException
	{
		getPool(whatDaq).addSetting(whatDaq, setting);
	}

	public ArrayList<WhatDaq> requests()
	{
		ArrayList<WhatDaq> requests = new ArrayList<>();

		for (PoolInterface p : pools)
			requests.addAll(p.requests());

		return requests;
	}

	synchronized public void processRequests()
	{
		for (PoolInterface p : pools)
			p.processRequests();
	}

	synchronized public void cancelRequests()
	{
		for (PoolInterface p : pools)
			p.cancelRequests();
	}

	synchronized public void logSettings(ConsoleUser user, String location, List<WhatDaq> settings) throws AcnetStatusException
	{
		buf.clear();

		// Type code 1000 for Java UserSettingSource

		buf.putShort((short) 1000);

		// This is thrown away when received

		buf.putInt(0);

		// Server time of setting

		buf.putLong(System.currentTimeMillis());

		// Console user id

		buf.putInt(user.id());

		// Number of setting entries

		buf.putInt(settings.size());

		// App id that doesn't apply here but maybe could XXX

		buf.putInt(0);

		// "DAE" name

		try {
			final String srvNode = connection.getLocalNodeName();

			buf.put((byte) srvNode.length());
			buf.put(srvNode.getBytes());
		} catch (Exception e) {
			buf.put((byte) 0);
		}

		// "User" node

		//final String clientNode = list.clientHostName();
		buf.put((byte) location.length());
		buf.put(location.getBytes());

		// Account name

		buf.put((byte) user.name().length());
		buf.put(user.name().getBytes());

		// Setting entries

		for (WhatDaq whatDaq : settings) {
			buf.putInt(whatDaq.dipi());
			buf.putShort((short) whatDaq.length());
			buf.putInt(whatDaq.offset());
			buf.put(whatDaq.setting(), 0, whatDaq.length() > 4 ? 4 : whatDaq.length()); 
		}

		buf.flip();

		connection.send("SETSDB", "SETSDB", buf);
	}

	public static String dumpPool(PoolType type)
	{
		final StringBuilder buf = new StringBuilder();
		final AcceleratorPool pool = new AcceleratorPool();

		for (PoolInterface p : pool.pools) {
			if (p.type() == type)
				buf.append(p.dumpPool());
		}

		return buf.toString();
	}
}
