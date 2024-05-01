// $Id: AcnetPoolImpl.java,v 1.15 2024/04/11 19:17:07 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.SettingData;
import gov.fnal.controls.servers.dpm.pools.PoolUser;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.PoolType;
import gov.fnal.controls.servers.dpm.pools.PoolInterface;

import gov.fnal.controls.servers.dpm.scaling.ScalingFactory;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class AcnetPoolImpl implements PoolInterface, SettingData.Handler, AcnetErrors
{
	static Timer sharedTimer = new Timer("ACNET pool timer");

	// All requests added to this AcceleratorPool view

	private final ArrayList<WhatDaq> requests = new ArrayList<>();

	// Live pool entries

	private final PoolUser user = new PoolUser() { };
	private final Set<DaqPoolUserRequests> repetitive = new HashSet<>();
	private final Map<Integer, OneShotDaqPool> oneshot = new HashMap<>();

	public static void init() throws Exception
	{
		//Lookup.init();
		LoggerConfigCache.init();
		States.init();
		//DBNews.init();
	}

	@Override
	public PoolType type()
	{
		return PoolType.ACNET;
	}

	@Override
	public void addRequest(WhatDaq whatDaq)
	{
		requests.add(whatDaq);
	}

	@Override
	public void addSetting(WhatDaq whatDaq, SettingData setting) throws AcnetStatusException
	{
		setting.deliverTo(whatDaq, this);
	}

	@Override
	public void handle(WhatDaq whatDaq, byte[] setting) throws AcnetStatusException
	{
		//try {
		//logger.log(Level.FINER, "adding " + setting.length + " byte setting to " + whatDaq);

			whatDaq.setSetting(setting);
			requests.add(whatDaq);
		//} catch (AcnetStatusException e) {
		//	whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		//} catch (Exception e) {
		//	whatDaq.getReceiveData().receiveStatus(ACNET_NXE, System.currentTimeMillis(), 0);
		//}
	}

	@Override
	public void handle(WhatDaq whatDaq, double setting) throws AcnetStatusException
	{
		//try {
			//whatDaq.setSetting(setting);
			whatDaq.setSetting(ScalingFactory.get(whatDaq).unscale(setting, whatDaq.length()));
			requests.add(whatDaq);
		//} catch (AcnetStatusException e) {
		//	whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		//} catch (Exception e) {
		//	whatDaq.getReceiveData().receiveStatus(DIO_SCALEFAIL, System.currentTimeMillis(), 0);
		//}
	}

	@Override
	public void handle(WhatDaq whatDaq, double[] setting) throws AcnetStatusException
	{
		//try {
			whatDaq.setSetting(ScalingFactory.get(whatDaq).unscale(setting, whatDaq.defaultLength()));
			requests.add(whatDaq);
		//} catch (AcnetStatusException e) {
		//	whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		//} catch (Exception e) {
		//	whatDaq.getReceiveData().receiveStatus(DIO_SCALEFAIL, System.currentTimeMillis(), 0);
		//}
	}

	@Override
	public void handle(WhatDaq whatDaq, String setting) throws AcnetStatusException
	{
		//try {
			whatDaq.setSetting(ScalingFactory.get(whatDaq).unscale(setting, whatDaq.length()));
			requests.add(whatDaq);
		//} catch (AcnetStatusException e) {
		//	whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		//} catch (Exception e) {
		//	whatDaq.getReceiveData().receiveStatus(DIO_SCALEFAIL, System.currentTimeMillis(), 0);
		//}
	}

	@Override
	public void handle(WhatDaq whatDaq, String[] setting) throws AcnetStatusException
	{
		//try {
			whatDaq.setSetting(ScalingFactory.get(whatDaq).unscale(setting, whatDaq.defaultLength()));
			requests.add(whatDaq);
		//} catch (AcnetStatusException e) {
		//	whatDaq.getReceiveData().receiveStatus(e.status, System.currentTimeMillis(), 0);
		//} catch (Exception e) {
		//	whatDaq.getReceiveData().receiveStatus(DIO_SCALEFAIL, System.currentTimeMillis(), 0);
		//}
	}

	@Override
	public void processRequests()
	{
		for (WhatDaq whatDaq : requests) {
			whatDaq.setUser(user);
			//whatDaq.setUser(poolUser);

			//logger.log(Level.FINER, "AcnetPoolImpl: " + whatDaq);

			if (whatDaq.getEvent().isRepetitive()) { //|| whatDaq.isSettingReady()) {
				//final DaqPoolUserRequests<WhatDaq> pool =  DaqPool.getPool(whatDaq.getTrunk(), whatDaq.getNode(),
				//												whatDaq.getEvent(), whatDaq.isSettingReady());
				final DaqPoolUserRequests<WhatDaq> pool =  DaqPool.get(whatDaq.node(), whatDaq.event());

				repetitive.add(pool);
				pool.insert(whatDaq);
			} else {
				final int key = whatDaq.node().value(); ///(whatDaq.trunk() << 16) + whatDaq.node(); 

				//DPMOneShotDaqPool pool = oneshot.get(key);
				OneShotDaqPool pool = oneshot.get(key);

				if (pool == null) {
					//pool = new DPMOneShotDaqPool(whatDaq.getTrunk(), whatDaq.getNode(), 0, whatDaq.getEvent(), "", true);
					//pool = new OneShotDaqPool(whatDaq.getTrunk(), whatDaq.getNode(), whatDaq.getEvent());
					pool = whatDaq.isSettingReady() ? new SettingDaqPool(whatDaq.node()) : 
														new OneShotDaqPool(whatDaq.node());
					oneshot.put(key, pool);
				}

				//logger.log(Level.FINER, "AcnetPoolImpl: " + whatDaq + " pool: " + pool);
				//pool.insert(whatDaq, DPMOneShotDaqPool.NORMAL);
				pool.insert(whatDaq);
			}
		}

		for (OneShotDaqPool pool : oneshot.values()) {
			pool.process(true);
		}

		for (DaqPoolUserRequests pool : repetitive) {
			if (pool instanceof RepetitiveDaqPool)
				//((RepetitiveDaqPool) pool).doProcessUserRequests(false);
				pool.process(false);
			else
				pool.process(true);
		}
	}

	@Override
	public void cancelRequests()
	{
		for (DaqPoolUserRequests pool : repetitive) {
			pool.cancel(user, 0);
			//pool.cancel(poolUser, false, 0);
			pool.process(true);
		}

		repetitive.clear();
		oneshot.clear();
		requests.clear();
	}

	public static void nodeUp(Node node)
	{
		DaqPool.nodeUp(node);
	}

	@Override
	public List<WhatDaq> requests()
	{
		return requests;
	}

	@Override
	public String dumpPool()
	{
		return RepetitiveDaqPool.dumpPools() + "\n" + OneShotDaqPool.dumpPools();
	}
}

