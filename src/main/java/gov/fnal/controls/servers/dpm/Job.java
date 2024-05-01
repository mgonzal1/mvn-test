// $Id: Job.java,v 1.111 2024/03/27 21:06:49 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.text.ParseException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.ResultSet;
import java.sql.SQLException;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.PoolUser;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.LoggedDevice;
import gov.fnal.controls.servers.dpm.pools.AcceleratorPool;
import gov.fnal.controls.servers.dpm.pools.LoggerRequest;
import gov.fnal.controls.servers.dpm.pools.LoggerEvent;

import gov.fnal.controls.servers.dpm.pools.acnet.DaqPoolUserRequests;
import gov.fnal.controls.servers.dpm.pools.acnet.SavedDataSource;
import gov.fnal.controls.servers.dpm.pools.acnet.DataLoggerFetchJob;
import gov.fnal.controls.servers.dpm.pools.acnet.LoggerConfigCache;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.OnceImmediateEvent;
import gov.fnal.controls.servers.dpm.events.SavedDataEvent;
import gov.fnal.controls.servers.dpm.events.MonitorChangeEvent;

import static gov.fnal.controls.db.DbServer.getDbServer;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

abstract class Job implements AcnetErrors, TimeNow
{
	final DPMList list;
	final StringBuffer description;
	final private ArrayList<WhatDaq> whatDaqs;

	Job(DPMList list)
	{
		this.list = list;
		this.description  = new StringBuffer(getClass().getSimpleName());
		this.whatDaqs = new ArrayList<>();
	}

	Collection<WhatDaq> whatDaqs()
	{
		return whatDaqs;
	}

	WhatDaq whatDaq(int id)
	{
		try {
			return whatDaqs.get(id);
		} catch (Exception ignore) { }

		return null;
	}

	WhatDaq WhatDaq(long refId, DPMRequest req) throws AcnetStatusException
	{
		final WhatDaq whatDaq = new WhatDaq(list, refId, req);

		whatDaqs.add(whatDaq);

		return whatDaq;
	}

	WhatDaq WhatDaq(long refId, DPMRequest req, DataEvent event) throws AcnetStatusException
	{
		final WhatDaq whatDaq = new WhatDaq(list, refId, req, event);

		whatDaqs.add(whatDaq);

		return whatDaq;
	}

	public boolean completed()
	{
		return false;
	}

	@Override
	public String toString()
	{
		return description.toString();
	}

	abstract void start(Map<Long, DPMRequest> requests) throws InterruptedException, IOException, AcnetStatusException;
	abstract void stop();
}

class NullJob extends Job
{
	final static NullJob instance;

	private NullJob()
	{
		super(null);
	}

	static {
		instance = new NullJob();
	}

	@Override
	public boolean completed()
	{
		return true;
	}

	@Override
	void start(Map<Long, DPMRequest> requests) throws AcnetStatusException { }

	@Override
	void stop() { }
}

class AcnetStatusJob extends Job
{
	private final int status;

	AcnetStatusJob(DPMList list, int status)
	{
		super(list);
		this.status = status;
	}

	@Override
	void start(Map<Long, DPMRequest> requests) throws InterruptedException, IOException, AcnetStatusException 
	{ 
		final long now = now();

		for (Map.Entry<Long, DPMRequest> entry : requests.entrySet()) {
			list.sendStatus(entry.getKey(), status, now);
		}
	}

	@Override
	public boolean completed()
	{
		return true;
	}

	@Override
	void stop() { }
}

class ReceiveDataCallback implements ReceiveData, AcnetErrors, TimeNow
{
	final DPMList list;
	final WhatDaq whatDaq;
	final DataReplier dataReplier;
	final boolean monitorChange;
	boolean monitorChangeSendImmediate;
	long seqNo = 0;

	ReceiveDataCallback(DPMList list, WhatDaq whatDaq)
	{
		this.list = list;
		this.whatDaq = whatDaq;
		this.dataReplier = list.getDataReplier(whatDaq);

		final DataEvent event = whatDaq.getEvent();

		if (event instanceof MonitorChangeEvent) {
			final MonitorChangeEvent mcEvent = (MonitorChangeEvent) event;

			this.monitorChange = true;
			this.monitorChangeSendImmediate = mcEvent.sendImmediate();
		} else {
			this.monitorChange = false;
			this.monitorChangeSendImmediate = false;
		}
	}

	@Override
	public void receiveData(ByteBuffer data, long timestamp, long cycle)
	{
		try {
			dataReplier.sendReply(data, timestamp, cycle);
			list.incrementReplies();
		} catch (AcnetStatusException e) {
			logger.log(e.status == ACNET_NO_SUCH ? Level.FINEST : Level.FINE, "exception sending reply", e);
			list.sendStatusNoEx(whatDaq, e.status, timestamp, cycle);
		} catch (IOException e) {
			logger.log(Level.FINE, "exception sending reply", e);
			list.dispose(ACNET_DISCONNECTED);
		} catch (BufferOverflowException e) {
			logger.log(Level.FINE, "exception sending reply", e);
			list.sendStatusNoEx(whatDaq, DPM_REPLY_OVERFLOW, timestamp, cycle);
		} catch (Exception e) {
			logger.log(Level.FINE, "exception sending reply", e);
			list.dispose(ACNET_DISCONNECTED);
		}
	}

	@Override
    public void receiveData(byte[] data, int offset, long timestamp, long cycle)
	{
		boolean isChanged = true;

		if (!monitorChange || isChanged || monitorChangeSendImmediate) {
			try {
				dataReplier.sendReply(data, offset, timestamp, cycle);
				list.incrementReplies();
			} catch (AcnetStatusException e) {
				logger.log(e.status == ACNET_NO_SUCH ? Level.FINEST : Level.FINE, "exception sending reply", e);
				list.sendStatusNoEx(whatDaq, e.status, timestamp, cycle);
			} catch (IOException e) {
				logger.log(Level.FINE, "exception sending reply", e);
				list.dispose(ACNET_DISCONNECTED);
			} catch (BufferOverflowException e) {
				logger.log(Level.FINE, "exception sending reply", e);
				list.sendStatusNoEx(whatDaq, DPM_REPLY_OVERFLOW, timestamp, cycle);
			} catch (Exception e) {
				logger.log(Level.FINE, "exception sending reply", e);
				list.dispose(ACNET_DISCONNECTED);
			}
		}
		monitorChangeSendImmediate = false;
	}

	@Override
	public void receiveStatus(int status, long timestamp, long cycle)
	{
		try {
			list.sendStatus(whatDaq, status, timestamp, cycle);
		} catch (AcnetStatusException e) {
			list.sendStatusNoEx(whatDaq, e.status, timestamp, cycle);
		} catch (IOException e) {
			list.dispose(ACNET_DISCONNECTED);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception during status delivery", e); 
		}
	}

	@Override
	public void receiveData(double value, long timestamp, long cycle)
	{
		try {
			dataReplier.sendReply(value, timestamp, cycle);
			list.incrementReplies();
		} catch (AcnetStatusException e) {
			list.sendStatusNoEx(whatDaq, e.status, timestamp, cycle);
		} catch (IOException e) {
			list.dispose(ACNET_DISCONNECTED);
		} catch (BufferOverflowException e) {
			list.sendStatusNoEx(whatDaq, DPM_REPLY_OVERFLOW, timestamp, cycle);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception during data delivery", e); 
		}
	}

	@Override
	public void receiveData(double[] values, long timestamp, long cycle)
	{
		try {
			dataReplier.sendReply(values, timestamp, cycle);
			list.incrementReplies();
		} catch (AcnetStatusException e) {
			list.sendStatusNoEx(whatDaq, e.status, timestamp, cycle);
		} catch (IOException e) {
			list.dispose(ACNET_DISCONNECTED);
		} catch (BufferOverflowException e) {
			list.sendStatusNoEx(whatDaq, DPM_REPLY_OVERFLOW, timestamp, cycle);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception during data delivery", e); 
		}
	}

	@Override
	public void receiveData(String value, long timestamp, long cycle)
	{
		try {
			dataReplier.sendReply(value, timestamp, cycle);
			list.incrementReplies();
		} catch (AcnetStatusException e) {
			list.sendStatusNoEx(whatDaq, e.status, timestamp, cycle);
		} catch (IOException e) {
			list.dispose(ACNET_DISCONNECTED);
		} catch (BufferOverflowException e) {
			list.sendStatusNoEx(whatDaq, DPM_REPLY_OVERFLOW, timestamp, cycle);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception during data delivery", e); 
		}
	}

	@Override
	public void receiveEnum(String value, int index, long timestamp, long cycle)
	{
		try {
			dataReplier.sendReply(value, index, timestamp, cycle);
			list.incrementReplies();
		} catch (AcnetStatusException e) {
			list.sendStatusNoEx(whatDaq, e.status, timestamp, cycle);
		} catch (IOException e) {
			list.dispose(ACNET_DISCONNECTED);
		} catch (BufferOverflowException e) {
			list.sendStatusNoEx(whatDaq, DPM_REPLY_OVERFLOW, timestamp, cycle);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception during data delivery", e); 
		}
	}

	@Override
	public void plotData(long timestamp, int error, int __, long[] micros, int[] nanos, double[] values)
	{
		try {
			if (error != 0)
				list.sendStatus(whatDaq, error, timestamp, 0);
			else {
				dataReplier.sendReply(values, micros, seqNo++);
				list.incrementReplies();
			}
		} catch (AcnetStatusException e) {
			list.sendStatusNoEx(whatDaq, e.status, timestamp, 0);
			logger.log(Level.FINE, "exception delivering plot data", e);
		} catch (IOException e) {
			list.sendStatusNoEx(whatDaq, ACNET_NXE, timestamp, 0);
			logger.log(Level.FINE, "exception delivering plot data", e);
		} catch (Exception e) {
			list.sendStatusNoEx(whatDaq, ACNET_NXE, timestamp, 0);
			logger.log(Level.FINE, "exception delivering plot data", e);
		}
	}

	private int translateError(int error)
	{
		switch (error) {
			case ACNET_NO_TASK:
				return ACNET_NODE_DOWN;
		}

		return error;
	}
}

class AcceleratorJob extends Job
{
	private final AcceleratorPool pool = new AcceleratorPool();

	AcceleratorJob(DPMList list)
	{
		super(list);
	}

	@Override
	void start(Map<Long, DPMRequest> requests) throws InterruptedException, IOException, AcnetStatusException
	{
		for (Map.Entry<Long, DPMRequest> entry : requests.entrySet()) {
			final long refId = entry.getKey();
			final DPMRequest req = entry.getValue();

			try {
				final WhatDaq whatDaq = WhatDaq(refId, req);

				list.sendDeviceInfo(whatDaq);

				if (whatDaq.hasEvent()) {
					whatDaq.setReceiveData(new ReceiveDataCallback(list, whatDaq));
					pool.addRequest(whatDaq);
				}

				if (!whatDaq.getEvent().isRepetitive())
					requests.remove(refId);	
			} catch (AcnetStatusException e) {
				list.sendStatus(refId, e.status, now());
			} catch (Exception e) {
				logger.log(Level.WARNING, "exception during start", e);
			}
		}

		pool.processRequests();
	}

	@Override
	void stop()
	{
		pool.cancelRequests();
	}
}

class RedirectAcceleratorJob extends AcceleratorJob
{
	private final AcceleratorPool pool = new AcceleratorPool();
	private final Node srcNode, dstNode;

	RedirectAcceleratorJob(DPMList list, Node srcNode, Node dstNode)
	{
		super(list);
		this.srcNode = srcNode;
		this.dstNode = dstNode;
	}

	@Override
	void start(Map<Long, DPMRequest> requests) throws IOException, AcnetStatusException
	{
		for (Map.Entry<Long, DPMRequest> entry : requests.entrySet()) {
			final long refId = entry.getKey();

			try {
				final WhatDaq whatDaq = WhatDaq(refId, entry.getValue());

				list.sendDeviceInfo(whatDaq);

				whatDaq.setReceiveData(new ReceiveDataCallback(list, whatDaq));

				if (srcNode == null || whatDaq.node().equals(srcNode)) {
					whatDaq.redirect(dstNode);
					pool.addRequest(whatDaq);
				} else
					pool.addRequest(whatDaq);

				if (!whatDaq.getEvent().isRepetitive())
					requests.remove(refId);	

			} catch (AcnetStatusException e) {
				list.sendStatusNoEx(refId, e.status, now());
			} catch (Exception e) {
				logger.log(Level.WARNING, "exception during start", e);
			}
		}

		pool.processRequests();
	}

	@Override
	void stop()
	{
		pool.cancelRequests();
	}
}

class SavefileJob extends Job implements PoolUser, TimeNow
{
	private final SavedDataSource source; 

	SavefileJob(DPMList list, int fileAlias) throws AcnetStatusException
	{
		super(list);

		try {
			this.source = new SavedDataSource(fileAlias);

			source.open();
			source.setUser(this);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception creating saveDataSource", e);
			throw new AcnetStatusException(DPM_BAD_DATASOURCE_FORMAT);
		}

		description.append(":");
		description.append(fileAlias);
	}

	SavefileJob(DPMList list, int usageNo, int fileAlias, int sdaCase, int sdaSubcase) throws AcnetStatusException
	{
		super(list);

		String usage = "";

		switch (usageNo) {
			case 1:
				usage = "ColliderShot";
				break;

			case 2:
				usage = "E835Store";
				break;

			case 3:
				usage = "PbarTransferShot";
				break;

			case 4:
				usage = "RecyclerShot";
				break;
		}

		int rsCollectionIndex = 0, rsFileIndex = 0;

		try {
			this.source = new SavedDataSource(usage, fileAlias, sdaCase, sdaSubcase);

			final String query = "SELECT DISTINCT B.file_alias, A.file_index, A.collection_index"
									+ " FROM srdb.sda_data A, srdb.save_header B WHERE B.owner LIKE '" + usage
									+ "' AND B.file_alias = " + fileAlias 
									+ " AND A.file_index = B.file_index "
									+ " AND A.collection_alias = " + sdaCase
									+ " AND A.set_alias = " + sdaSubcase;
			final ResultSet rs = getDbServer("srdb").executeQuery(query);

			while (rs.next()) {
				rsFileIndex = rs.getInt(2);
				rsCollectionIndex = rs.getInt(3);
			}

			rs.close();

			final DataEvent event = new SavedDataEvent(rsFileIndex, rsCollectionIndex);

			this.source.open(event);
			this.source.setUser(this);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception with savedDataSource", e);
			throw new AcnetStatusException(DPM_BAD_DATASOURCE_FORMAT);
		}

		description.append(":" + usage + ":" + fileAlias);
	}

	@Override
	void start(Map<Long, DPMRequest> requests) throws IOException, AcnetStatusException
	{
		final DaqPoolUserRequests<WhatDaq> pool = ((DaqPoolUserRequests<WhatDaq>) source);

		for (Map.Entry<Long, DPMRequest> entry : requests.entrySet()) {
			final long refId = entry.getKey();

			try {
				final WhatDaq whatDaq = WhatDaq(refId, entry.getValue());

				list.sendDeviceInfo(whatDaq);
				whatDaq.setReceiveData(new ReceiveDataCallback(list, whatDaq));
				pool.insert(whatDaq);
			} catch (AcnetStatusException e) {
				list.sendStatusNoEx(refId, e.status, now());
			} catch (Exception e) {
				logger.log(Level.WARNING, "exception during save file start", e);
			}
		}

		pool.process(true);
	}

	@Override
	void stop()
	{
		source.cancel(this, 0);
	}
}

class LoggerSingleJob extends Job implements PoolUser
{
	private final String loggerName;
	private final long t1, t2;
	private long timestamp;
	private final ArrayList<DataLoggerFetchJob> fetchJobs = new ArrayList<>();
	private boolean completed = false;
	
	LoggerSingleJob(DPMList list, String loggerName, long timestamp, int accuracy)
	{
		super(list);
		this.loggerName = loggerName;
		this.timestamp = timestamp;
		this.t1 = timestamp - accuracy;
		this.t2 = timestamp + accuracy;
	}

	private class Callback extends ReceiveDataCallback implements TimeNow
	{
		private long bestDiff = Long.MAX_VALUE;
		private long bestTimestamp;
		private double bestValue;

		Callback(DPMList list, WhatDaq whatDaq)
		{
			super(list, whatDaq);
		}

		@Override
		public void plotData(long timestamp, int error, int count, long[] micros, int[] nanos, double[] values)
		{
			try {
				if (error != 0)
					list.sendStatus(whatDaq, error, now(), 0);
				else {
					if (count > 0) {
						for (int ii = 0; ii < count; ii++) {
							long ts = micros[ii] / 1000;
							long thisDiff = Math.abs(ts - LoggerSingleJob.this.timestamp);

							if (thisDiff < bestDiff) {
								bestDiff = thisDiff;
								bestTimestamp = ts;
								bestValue = values[ii];
							}
						}
					} else {
						if (bestDiff == Long.MAX_VALUE)
							list.sendStatus(whatDaq, DAE_LJ_NO_DATA, now(), 0);
						else {
							dataReplier.sendReply(bestValue, bestTimestamp, 0);
							list.incrementReplies();
						}

						completed = true;
						list.jobCompleted(LoggerSingleJob.this);
					}
				}
			} catch (BufferOverflowException e) {
				list.sendStatusNoEx(whatDaq, DPM_REPLY_OVERFLOW, timestamp, 0);
			} catch (Exception e) {
				logger.log(Level.WARNING, "exception delivering plot results", e);
				try {
					list.sendStatus(whatDaq, DIO_SCALEFAIL, timestamp, 0);
				} catch (Exception ee) { }
			}
		}
	}

	@Override
	public boolean completed()
	{
		return completed;
	}

	@Override
	synchronized void start(Map<Long, DPMRequest> requests) throws InterruptedException, IOException, AcnetStatusException
	{
		for (Map.Entry<Long, DPMRequest> entry : requests.entrySet()) {
			final long refId = entry.getKey();
			final DPMRequest req = entry.getValue();

			try {
				final WhatDaq whatDaq = WhatDaq(refId, req);
				final LoggerRequest loggerRequest = new LoggerRequest(whatDaq.loggedName, new Callback(list, whatDaq));

				list.sendDeviceInfo(whatDaq);
				fetchJobs.add((new DataLoggerFetchJob(this, loggerName, loggerRequest, new LoggerEvent(t1, t2, null))).start());
			} catch (AcnetStatusException e) {
				list.sendStatus(refId, e.status, now());
			}
		}
	}

	@Override
	synchronized void stop()
	{
		for (DataLoggerFetchJob fetchJob : fetchJobs)
			fetchJob.stop();
	}
}

class LoggerJob extends Job implements Runnable
{
	private final String loggerName;
	private final long t1, t2;
	private final LinkedBlockingQueue<FetchInfo> fetchQ = new LinkedBlockingQueue<>();
	private final Thread fetchThread;
	private boolean completed = false;

	private volatile DataLoggerFetchJob currentJob = null;
	
	LoggerJob(DPMList list, long t1, long t2, String loggerName)
	{
		super(list);

		this.t1 = t1;
		this.t2 = t2;
		this.loggerName = loggerName;

		this.fetchThread = new Thread(this, "LoggerFetch - " + list.id());
		this.fetchThread.start();

		logger.log(Level.FINE, () -> String.format("Logger job: %tc to %tc loggerName:'%s'", this.t1, this.t2, loggerName));
	}

	private class FetchInfo
	{
		final WhatDaq whatDaq;
		final List<LoggedDevice> loggers;

		FetchInfo()
		{
			this.whatDaq = null;
			this.loggers = null;
		}

		FetchInfo(WhatDaq whatDaq, List<LoggedDevice> loggers)
		{
			this.whatDaq = whatDaq;
			this.loggers = loggers;
		}
	}

	private class Callback extends ReceiveDataCallback implements PoolUser//, DataLoggerFetchTroubleCallback
	{
		private int error = 0;
		private int count = 0;

		Callback(DPMList list, WhatDaq whatDaq)
		{
			super(list, whatDaq);
		}

		@Override
		public void plotData(long timestamp, int error, int __, long[] micros, int[] nanos, double[] values)
		{
			try {
				if (error != 0)
					this.error = error;
				else {
					count++;
					dataReplier.sendReply(values, micros, seqNo++);
					list.incrementReplies();
				}
			} catch (Exception e) {
				logger.log(Level.FINE, "exception during plot data delivery", e);
				try {
					list.sendStatus(whatDaq, DIO_SCALEFAIL, timestamp, 0);
				} catch (Exception ignore) { }
			}
		}

		int error()
		{
			return error & 0xffff;
		}

		int count()
		{
			return count;
		}
	}

	@Override
	public boolean completed()
	{
		return completed;
	}

	final private boolean fetch(final FetchInfo fInfo) throws InterruptedException, AcnetStatusException
	{
		for (LoggedDevice e : fInfo.loggers) {
			final Callback callback = new Callback(list, fInfo.whatDaq);
			final LoggerRequest loggerRequest = new LoggerRequest(fInfo.whatDaq.loggedName, callback);
			final LoggerEvent loggerEvent = new LoggerEvent(t1, t2, e.event());

			currentJob = new DataLoggerFetchJob(callback, e.loggerName(), loggerRequest, loggerEvent);

			logger.log(Level.FINE, String.format("%s fetching '%s' from logger on '%s' list:%d event:'%s'", 
													list.id(), fInfo.whatDaq.loggedName, e.loggerName(), e.id(), e.event()));

			synchronized (callback) {
				currentJob.start();
				callback.wait();
			}

			if (callback.error() == 0 && callback.count() > 0) {
				logger.log(Level.FINE, String.format("%s fetch complete for '%s' from logger on '%s' list:%d event:'%s'", 
														list.id(), fInfo.whatDaq.loggedName, e.loggerName(), e.id(), e.event()));
				return true;
			} else
				logger.log(Level.FINE, String.format("%s No data found in logger '%s' for '%s'. (Error=0x%04x) Trying next logger...", 
											list.id(), e.loggerName(), fInfo.whatDaq.loggedName, callback.error()));
		}

		return false;
	}

	@Override
	public void run()
	{
		try {
			while (true) {	
				final FetchInfo fetchInfo = fetchQ.take();

				if (fetchInfo.loggers == null)
					break;

				if (!fetch(fetchInfo))
					list.sendStatus(fetchInfo.whatDaq, ACNET_NO_NODE, now(), 0);
			}
		} catch (InterruptedException e) {
			logger.log(Level.INFO, list.id() + " logger job interrupted", e);
		} catch (AcnetStatusException e) {
			logger.log(Level.INFO, list.id() + " logger job exception", e);
		} catch (Exception e) {
			logger.log(Level.FINE, list.id() + " logger job exception", e);
		}

		logger.log(Level.FINE, list.id() + " logger job fetch thread exit");

		completed = true;
		list.jobCompleted(this);
	}

	@Override
	void start(Map<Long, DPMRequest> requests) throws InterruptedException, IOException, AcnetStatusException
	{
		for (Map.Entry<Long, DPMRequest> entry : requests.entrySet()) {
			final long refId = entry.getKey();
			final DPMRequest req = entry.getValue();

			try {
				final WhatDaq whatDaq = WhatDaq(refId, req);
				final List<LoggedDevice> bestLoggers = LoggerConfigCache.bestLoggers(whatDaq, loggerName);

				whatDaq.setOption(WhatDaq.Option.FLOW_CONTROL);

				if (logger.isLoggable(Level.FINE)) {
					logger.log(Level.FINE, list.id() + " best loggers:");
					for (LoggedDevice e : bestLoggers) {
						logger.log(Level.FINE, list.id() + "    " + e.loggerName());
					}
				}

				list.sendDeviceInfo(whatDaq);

				fetchQ.offer(new FetchInfo(whatDaq, bestLoggers));
			} catch (AcnetStatusException e) {
				list.sendStatus(refId, e.status, now());
			} catch (Exception e) {
				list.sendStatus(refId, ACNET_NO_NODE, now());
			}
		}

		// Signal end of logger request jobs with empty info

		fetchQ.offer(new FetchInfo());
	}

	@Override
	void stop()
	{
		if (currentJob != null)
			currentJob.stop();
		
		fetchQ.clear();
	}
}

class SettingJob extends Job
{
	private static class AllDevicesSet extends HashSet<String>
	{
		@Override
		public boolean contains(Object o) { return true; }
	}

	private class ReceiveSettingStatus implements ReceiveData
	{
		private final SettingStatus settingStatus;
		private final WhatDaq whatDaq;

		ReceiveSettingStatus(SettingStatus settingStatus)
		{
			this.settingStatus = settingStatus;
			this.whatDaq = settingStatus.whatDaq;
		}

		@Override
		public void receiveData(ByteBuffer buf, long timestamp, long cycle)
		{
			Thread.dumpStack();
		}

		@Override
		public void receiveStatus(int status, long timestamp, long cycle)
		{
			setReceivedStatus(status);
		}

		synchronized private void setReceivedStatus(int status)
		{
			settingStatus.setStatus(status);

			completionCheck();
		}
	}

	private static final Set<String> emptyDeviceSet = new HashSet<>();
	private static final AllDevicesSet allDevicesSet = new AllDevicesSet();

	private final ConsoleUser user;
	private final List<SettingData> settings;
	private final ArrayList<SettingStatus> settingStatus = new ArrayList<>();

	private final AcceleratorPool pool = new AcceleratorPool();
	private volatile Set<String> allowedDevices = emptyDeviceSet;

	volatile boolean completed = false;

	SettingJob(List<SettingData> settings, DPMList list, ConsoleUser user)
	{
		super(list);

		this.settings = settings;
		this.user = user;

		Scope.listUser(this.list);
	}

	@Override
	public boolean completed()
	{
		return completed;
	}

	@Override
	void start(Map<Long, DPMRequest> requests)
	{
		Scope.listSettingsStarted(list);

		allowedDevices = list.allowPrivilegedSettings() ? allDevicesSet : 
							user.allowedDevices(list.properties.get("ROLE"));

		for (SettingData setting : settings) {
			final long refId = setting.refId;
			final DPMRequest request = requests.get(refId);

			if (request != null) {
				try {
					final WhatDaq whatDaq = WhatDaq(refId, request, new OnceImmediateEvent());

					if (allowedDevices.contains(whatDaq.getDeviceName())) {
						final SettingStatus status = new SettingStatus(whatDaq);

						whatDaq.setReceiveData(new ReceiveSettingStatus(status));
						pool.addSetting(whatDaq, setting);
						settingStatus.add(status);
					} else
						settingStatus.add(new SettingStatus(refId, DPM_PRIV));
				} catch (AcnetStatusException e) {
					logger.log(Level.FINER, "Setting exception: " + e.status + " for " + request);
					settingStatus.add(new SettingStatus(refId, e.status));
				} catch (Exception e) {
					settingStatus.add(new SettingStatus(refId, ACNET_SYS));
				}
			} else
				settingStatus.add(new SettingStatus(refId, DPM_BAD_REQUEST));
		}

		pool.processRequests();

		logger.log(Level.FINE, String.format("%s ApplySettings - %d setting(s) - %d in pool", list.id(), settingStatus.size(), pool.requests().size()));

		completionCheck();

	}

	@Override
	void stop() 
	{
		completed = true;
	}

	private void completionCheck()
	{
		if (!completed) {
			logger.log(Level.FINE, String.format("%s CompletionCheck - %d setting(s)", list.id(), settingStatus.size()));

			for (SettingStatus ss : settingStatus) {
				if (!ss.completed())
					return;
			}

			logger.log(Level.FINE, String.format("%s CompletionCheck - setting(s) complete", list.id()));

			completed = true;

			// XXX Let consoles (privileged) do the logging for now

			if (!list.allowPrivilegedSettings())
				logSuccessfulSettings();

			Scope.listSettingsComplete(list);

			try {
				list.protocolReplier.sendReply(settingStatus);
			} catch (IOException e) {
				list.dispose(ACNET_DISCONNECTED);
			} catch (AcnetStatusException e) {
				list.dispose(e.status);
			} catch (Exception e) {
				logger.log(Level.WARNING, "exception handling settings complete", e);
				list.dispose(ACNET_SYS);
			}
		}
	}

	synchronized private void logSuccessfulSettings()
	{
		final ArrayList<WhatDaq> successful = new ArrayList<>();

		for (SettingStatus ss : settingStatus) {
			if (ss.successful()) {
				successful.add(ss.whatDaq);
			}
		}

		logger.log(Level.FINE, String.format("%s LogSettings - %d setting(s)", list.id(), successful.size()));

		try {
			pool.logSettings(user, list.clientHostName(), successful); 
		} catch (Exception e) {
			logger.log(Level.FINE, list.id() + " unable to log settings - " + e);
		}
	}
}

class SpeedTestJob extends Job
{
	final int replyCount;
	final int arraySize;
	boolean completed = false;

	SpeedTestJob(DPMList list, int replyCount, int arraySize)
	{
		super(list);
		this.replyCount = replyCount;
		this.arraySize = arraySize;
	}

	@Override
	public boolean completed()
	{
		return completed;
	}

	@Override
	void start(Map<Long, DPMRequest> requests) throws InterruptedException, IOException, AcnetStatusException
	{
		for (Map.Entry<Long, DPMRequest> entry : requests.entrySet()) {
			final long refId = entry.getKey();
			final DPMRequest req = entry.getValue();

			try {
				final Random random = new Random();
				final WhatDaq whatDaq = WhatDaq(refId, req);
				final DataReplier dataReplier = list.getDataReplier(whatDaq);
				final double[] values = new double[arraySize];
				final long[] micros = new long[arraySize];

				logger.log(Level.FINE, "arraySize: " + arraySize);

				for (int ii = 0; ii < arraySize; ii++) {
					values[ii] = random.nextDouble();
					values[ii] = random.nextLong();
				}

				list.sendDeviceInfo(whatDaq);

				final Runnable task = () -> {
					try {
						int seqNo = 0;
						while (seqNo < replyCount && !completed) {
							dataReplier.sendReply(values, micros, seqNo++);
							list.incrementReplies();
						}

						completed = true;
						list.jobCompleted(this);
						dataReplier.sendReply(new double[0], new long[0], seqNo);
						list.incrementReplies();
					} catch (Exception e) {
						logger.log(Level.FINE, "exception delivering reply in speed test", e);
					}

					logger.log(Level.FINE, "thread exit");
				};

				(new Thread(task)).start();

				break;
			} catch (AcnetStatusException e) {
				list.sendStatus(refId, e.status, now());
			}
		}
	}

	@Override
	void stop() 
	{
		completed = true;
	}
}
