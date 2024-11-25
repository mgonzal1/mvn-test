// $Id: JobInfo.java,v 1.21 2024/11/19 22:34:43 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.Map;
import java.util.Collection;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.SQLException;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.DeviceCache;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class JobInfo
{
	final Map<Long, DPMRequest> requests = new ConcurrentHashMap<>();
	Job job = NullJob.instance;

	private DataSource dataSource = LiveDataSource.instance;
	private boolean needsStart = true;

	protected JobInfo()
	{
	}

	protected JobInfo(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	boolean addRequest(long refId, DPMRequest req)
	{
		final DPMRequest old = requests.get(refId);

		if (!req.equals(old)) {
			requests.put(refId, req);
			needsStart = true;

			return true;
		}

		return false;
	}

	boolean restartable()
	{
		return dataSource.restartable() || job.completed();
	}

	void setDataSource(DataSource newDataSource)
	{
		if (!newDataSource.equals(dataSource)) {
			dataSource = newDataSource;
			needsStart = true;
		}
	}

	void setDataSource(String newDataSourceStr)
	{
		setDataSource(DataSource.parse(newDataSourceStr));
	}

	void removeRequest(long refId)
	{
		if (requests.remove(refId) != null)
			needsStart = true;
	}

	void clearRequests()
	{
		if (!requests.isEmpty()) {
			requests.clear();
			needsStart = true;
		}
	}

	void start(DPMList list) throws InterruptedException, IOException, AcnetStatusException, SQLException
	{
		if (needsStart && (job == NullJob.instance || dataSource.restartable())) {
			DeviceCache.add(requests.values());

			needsStart = false;

			final Job newJob = dataSource.createJob(list);

	    	logger.log(Level.FINE, String.format("%s StartJob source:%s requests:%d", 
										list.id(), dataSource, requests.size()));

			newJob.start(requests);
			job.stop();
			job = newJob;
		}
	}

	void stop()
	{
		job.stop();
	}

	boolean completed()
	{
		return job.completed();
	}

	final public WhatDaq whatDaq(int id)
	{
		return job.whatDaq(id);
	}

	final public  Collection<WhatDaq> whatDaqs()
	{
		return job.whatDaqs();
	}

	final public long devicePropertyCount(int dipi)
	{
		return job.whatDaqs().stream().filter(w -> w.dipi() == dipi).count();
	}

	final public long nodeCount(int node)
	{
		return job.whatDaqs().stream().filter(w -> w.node().value() == node).count();
	}
}
