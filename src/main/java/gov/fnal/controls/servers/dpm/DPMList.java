// $Id: DPMList.java,v 1.90 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm;
 
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.BitSet;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.net.InetAddress;

import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.MessageProp;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.drf3.EventFormatException;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public abstract class DPMList implements AcnetErrors, TimeNow
{
	static final Timer timer = new Timer("List stats timer", true);
	static final IdPool idPool = new IdPool(4096, 2048);

	static volatile long totalRequestCount = 0;

	abstract public String clientHostName();
	abstract public InetAddress fromHostAddress() throws AcnetStatusException;

	private static class IdPool implements AcnetErrors
	{
		class Id extends ListId
		{
			Id(int value)
			{
				super(value);
			}
		}

		private int firstId;
		private int size;
		private int fromIndex;
		private int freeCount;
		final private BitSet inUse;

		IdPool(final int firstIndex, final int size)
		{
			this.firstId = firstIndex;
			this.size = size;
			this.fromIndex = 0;
			this.freeCount = size;
			this.inUse = new BitSet(size);
		}

		synchronized Id alloc() throws AcnetStatusException
		{
			int nextIndex = inUse.nextClearBit(fromIndex);

			if (nextIndex < size) {
				inUse.set(nextIndex);
				fromIndex = (nextIndex + 1) % size;
				freeCount--;

				return new Id(nextIndex + firstId);
			} else {
				fromIndex = 0;
				nextIndex = inUse.nextClearBit(fromIndex);
				if (nextIndex < size) {
					inUse.set(nextIndex);
					fromIndex = (nextIndex + 1) % size;
					freeCount--;

					return new Id(nextIndex + firstId);
				}
			}

			throw new AcnetStatusException(DPM_OUTOMEM);
		}

		synchronized void free(final Id id)
		{
			final int index = id.value() - firstId;

			if (inUse.get(index)) {
				inUse.clear(id.value() - firstId);
				freeCount++;
			} else
				logger.log(Level.WARNING, "attempt to free unallocated id " + id.value());
		}

		synchronized int freeCount()
		{
			return freeCount;
		}
	}

	private class StatsTimer extends TimerTask implements AcnetErrors
	{
		public void run()
		{
			final int count = replyCounter.getAndSet(0);
			final int newRepliesPerSecond = (count + (StatsRateSeconds / 2)) / StatsRateSeconds;

			if (count == 0) {
				try {
					protocolReplier.sendReply(id, 0);
					if (logger.isLoggable(Level.FINEST))
						logger.log(Level.FINEST, id + " send list status");
				} catch (AcnetStatusException e) {
					dispose(ACNET_DISCONNECTED);
					logger.log(Level.FINE, String.format("%s list - exception: %s", id, e.getMessage()));
				} catch (IOException e) {
					dispose(ACNET_DISCONNECTED);
					logger.log(Level.FINE, String.format("%s list - exception: %s", id, e.getMessage()), e);
				} catch (Exception e) {
					dispose(DPM_INTERNAL_ERROR);
					logger.log(Level.WARNING, String.format("%s list - exception: %s in run()", id, e.getMessage()), e);
				}
			}

			if (repliesPerSecond != newRepliesPerSecond) {
				repliesPerSecond = newRepliesPerSecond;
				if (repliesPerSecond > repliesPerSecondMax)
					repliesPerSecondMax = repliesPerSecond;
				Scope.listRepliesPerSecond(DPMList.this);
			}
		}
	}

	private final IdPool.Id id;

	protected final DPMProtocolHandler owner;
	protected final long createdTime;
	protected final Map<String, String> properties = new ConcurrentHashMap<>();

	protected final AtomicInteger replyCounter = new AtomicInteger();
	protected final AtomicInteger droppedReplyCounter = new AtomicInteger();
	volatile int repliesPerSecond = 0;
	volatile int repliesPerSecondMax = 0;

	private volatile GSSContext gss = null;
	protected volatile GSSName gssName = null;

	protected final Map<Long, DPMRequest> requests = new ConcurrentHashMap<>();
	protected final AtomicReference<List<SettingData>> settings = new AtomicReference<>();

	protected final Map<DataSource, JobInfo> jobs = new ConcurrentHashMap<>();
	protected DPMProtocolReplier protocolReplier;
	volatile long disposedTime = 0;
	protected volatile int disposedStatus = 0;

	static final int StatsRateSeconds = 2;

	final StatsTimer statsTimer = new StatsTimer();

	public DPMList(final DPMProtocolHandler owner) throws AcnetStatusException
	{
		this.owner = owner;
		this.id = idPool.alloc();
		this.createdTime = now();
		this.settings.set(new ArrayList<SettingData>());

		this.protocolReplier = new DPMProtocolReplier() { };

		timer.schedule(this.statsTimer, StatsRateSeconds * 1000, StatsRateSeconds * 1000);
	}

	final public ListId id()
	{
		return id;
	}
	
	final public void incrementReplies()
	{
		replyCounter.incrementAndGet();
	}

	public final void sendStatus(final long refId, final int status, final long timestamp) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(refId, status, timestamp, 0L);
		replyCounter.incrementAndGet();
	}

	public final void sendStatus(final long refId, final int status, final long timestamp, final long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(refId, status, timestamp, cycle);
		replyCounter.incrementAndGet();
	}

	public final void sendStatus(final WhatDaq whatDaq, final int status, final long timestamp, final long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		sendStatus(whatDaq.refId(), status, timestamp, cycle);
	}

	public final void sendStatusNoEx(final long refId, final int status, final long timestamp)
	{	
		try {
			sendStatus(refId, status, timestamp);
		} catch (Exception ignore) { }
	}

	public final void sendStatusNoEx(final WhatDaq whatDaq, final int status, final long timestamp, final long cycle)
	{
		try {
			sendStatus(whatDaq, status, timestamp, cycle);
		} catch (Exception ignore) { }
	}

	public final void sendStatus(final int status) throws InterruptedException, IOException, AcnetStatusException
	{
		final long now = now();

		for (JobInfo job : jobs.values()) {
			for (long refId : job.requests.keySet())
				sendStatus(refId, status, now);
		}
	}

	public final void sendStatusNoEx(final int status)
	{
		try {
			sendStatus(status);
		} catch (Exception ignore) { }
	}

	public final void sendDeviceInfo(final WhatDaq whatDaq) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq);
		replyCounter.incrementAndGet();
	}
	
	public final Collection<JobInfo> jobs()
	{
		return jobs.values();
	}

	public final String property(final String name)
	{
		return properties.get(name);
	}

	GSSName userName() throws GSSException
	{
		if (gssName == null)
			throw new GSSException(GSSException.NO_CRED);

		return gssName;
	}

	String[] properties()
	{
		final ArrayList<String> p = new ArrayList<>();

		for (Map.Entry<String, String> e : properties.entrySet())
			p.add(e.getKey() + ":" + e.getValue());

		return p.toArray(new String[p.size()]);
	}

	public boolean disposed()
	{
		return disposedTime != 0;
	}

	public boolean restartable()
	{
		for (JobInfo jobInfo : jobs.values())
			if (!jobInfo.restartable())
				return false;

		return true;
	}

	public int jobCount()
	{
		return jobs.size();
	}

	void jobCompleted(final Job job)
	{
		setRestartableProperty();
		Scope.listProperties(this);
	}

	private void setRestartableProperty()
	{
		properties.put("RESTARTABLE", restartable() ? "Y" : "N");
	}

	private boolean disposeImpl(final int status)
	{
		if (!disposed()) {
			if (gss != null) {
				try {
					gss.dispose();
				} catch (Exception ignore) { }
			}
			
			disposedTime = now();
			disposedStatus = status;

			try {
				statsTimer.cancel();
				logger.log(Level.FINE, String.format("%s Purged %d timer(s)", id, timer.purge()));
			} catch (Exception ignore) { }

			logger.log(Level.FINE, id + " Disposed");

			owner.listDisposed(this);
			idPool.free(id);

			return true;
		}

		logger.log(Level.FINER, id + " List already disposed");
		return false;
	}

	synchronized public boolean dispose(final int status)
	{
		if (disposeImpl(status)) {
			stop();
			return true;
		}

		return false;
	}

	public boolean dispose()
	{
		return dispose(ACNET_CANCELLED);
	}

	private void addToJob(final long refId, final DPMRequest req)
	{
		final DataSource dataSource = req.dataSource();
		JobInfo jInfo = jobs.get(dataSource);
		
		if (jInfo == null) {
			jInfo = new JobInfo();
			
			if (dataSource != DefaultDataSource.instance)
				jInfo.setDataSource(dataSource);

			jobs.put(dataSource, jInfo);
		}

		if (jInfo.addRequest(refId, req))
			totalRequestCount++;
	}

	private void removeFromJob(final long refId, final DPMRequest req)
	{
		JobInfo jInfo = jobs.get(req.dataSource());

		if (jInfo != null)
			jInfo.removeRequest(refId);
	}

	DPMRequest getRequest(final long refId) throws AcnetStatusException
	{
		final DPMRequest req = requests.get(refId);	

		if (req == null)
			throw new AcnetStatusException(DPM_BAD_REQUEST);

		return req;
	}

	public void authenticate(final byte[] token)
	{
		authenticate(token, protocolReplier);
	}

	public void authenticate(byte[] token, final DPMProtocolReplier protocolReplier)
	{
        logger.log(Level.FINER, String.format("%s Authenticate from:%-6s token[%d]", id, clientHostName(), token.length));

		if (token.length == 0) {
			try {
				final String serviceName = DPMCredentials.serviceName().toString();

				logger.log(Level.FINER, String.format("%s Authenticate GSS service name: %s", id, serviceName));	

				if (serviceName != null) {
					if (gss != null) {
						gss.dispose();
						gss = null;
					}
				}

				try {
					protocolReplier.sendReply(new AuthenticateReply(serviceName, token));
				} catch (AcnetStatusException e) {
					authenticationFailed(e.status, e, protocolReplier);
        		} catch (Exception e) {
					authenticationFailed(DPM_INTERNAL_ERROR, e, protocolReplier);
					logger.log(Level.WARNING, "exception in authenticate()", e);
        		}
			} catch (GSSException e) {
				authenticationFailed(DPM_PRIV, e, protocolReplier);
			}
		} else {
			try {
				if (gss == null)
					gss = DPMCredentials.createContext();
				token = DPMCredentials.accept(gss, token);
			} catch (GSSException e) {
				authenticationFailed(DPM_PRIV, e, protocolReplier);
			}

			if (gss != null && gss.isEstablished()) {
				try {
					logger.log(Level.INFO, String.format("%s User %s authenticated from %s for service %s",
															id, gss.getSrcName(), clientHostName(), gss.getTargName()));
					protocolReplier.sendReply(new AuthenticateReply(DPMCredentials.serviceName()));
				} catch (GSSException e) {
					authenticationFailed(DPM_PRIV, e, protocolReplier);
				} catch (AcnetStatusException e) {
					authenticationFailed(e.status, e, protocolReplier);
				} catch (Exception e) {
					authenticationFailed(DPM_INTERNAL_ERROR, e, protocolReplier);
					logger.log(Level.WARNING, "exception in authenticate()", e);
				}
			} else if (token != null) {
				try {
					protocolReplier.sendReply(new AuthenticateReply(DPMCredentials.serviceName(), token));
				} catch (AcnetStatusException e) {
					authenticationFailed(e.status, e, protocolReplier);
				} catch (Exception e) {
					authenticationFailed(DPM_INTERNAL_ERROR, e, protocolReplier);
					logger.log(Level.WARNING, "exception in authenticate()", e);
				}
			}
		}
	}

	private final void authenticationFailed(final int status, final Exception e, final DPMProtocolReplier protocolReplier)
	{
		logger.log(Level.FINE, String.format("%s Authentication failed - %s", id, e), e);

		try {
			gss.dispose();
		} catch (Exception ignore) { }
		gss = null;

		protocolReplier.sendStatusNoEx(status);
	}

	public void enableSettings(final byte[] MIC, final byte[] message)// throws AcnetStatusException
	{
		enableSettings(MIC, message, protocolReplier);
	}

	public void enableSettings(final byte[] MIC, final byte[] message, final DPMProtocolReplier protocolReplier)// throws AcnetStatusException
	{
	    logger.log(Level.FINER, String.format("%s EnableSettings from:%-6s", id, clientHostName()));

		int status = DPM_PRIV;

		try {
			if (gss != null) {
				gss.verifyMIC(MIC, 0, MIC.length, message, 0, message.length, new MessageProp(true));
				gssName = gss.getSrcName();
				logger.log(Level.INFO, String.format("%s Settings enabled for user %s from %s",
														id, gssName, clientHostName()));		
			}

			protocolReplier.sendReplyNoEx(0L, 0, 0, -1L);
		} catch (Exception e) {
			protocolReplier.sendReplyNoEx(0L, DPM_PRIV, 0, -1L);
		}
	}

	public void addToProperties(final String key, final String value)
	{
		properties.put(key, value);
		Scope.listProperties(this);
	}

	public void addRequest(final String drf, final long refId)
	{
		try {
			// Check for list property format.  This will eventually be removed
			// when an actual message is added to the protocol for properties.

			if (drf.charAt(0) == '#' && drf.length() > 4) {
				try {
					String[] prop = drf.substring(1).split(":");

					if (!prop[0].isEmpty()) {
						if (logger.isLoggable(Level.FINE))
	    					logger.log(Level.FINE, String.format("%s AddProperty '%s'='%s'", id, prop[0], prop[1]));
						addToProperties(prop[0], prop[1]);
						return;
					}
				} catch (Exception e) {
					logger.log(Level.FINE, "bad property format", e);
				}
			}
			
			// If it fails as a property then go ahead and try to parse
			// as a DRF3 request

			if (logger.isLoggable(Level.FINE))
	    		logger.log(Level.FINE, String.format("%s AddRequest %6d '%s'", id, refId, drf));

			final DPMRequest req = new DPMRequest(drf, refId);
			
			requests.put(refId, req);
			addToJob(refId, req);
		} catch (EventFormatException e) {
			sendStatusNoEx(refId, DPM_BAD_EVENT, now());
		} catch (Exception e) {
			sendStatusNoEx(refId, DPM_BAD_REQUEST, now());
		}
	}

	public void removeRequest(final long refId)
	{
        logger.log(Level.FINE, String.format("%s RemoveFromList %6d", id, refId));

		requests.remove(refId);

		for (JobInfo jInfo : jobs.values())
			jInfo.removeRequest(refId);
	}

	public void clear()
	{
		logger.log(Level.FINE, String.format("%s ClearList from:%-6s", id, clientHostName()));

		requests.clear();

		for (JobInfo jInfo : jobs.values())
			jInfo.clearRequests();
	}

	public int requestCount()
	{
		return requests.size();
	}

	public int whatDaqCount()
	{
		int count = 0;

		for (JobInfo jInfo : jobs.values())
			count += jInfo.whatDaqs().size();

		return count;
	}

	synchronized public final void start(final String newDefaultDataSource)
	{
	    logger.log(Level.FINE, String.format("%s StartList from:%-6s source:%s jobs:%d",
									id, clientHostName(), newDefaultDataSource, jobCount()));

		final JobInfo jobInfo = jobs.get(DefaultDataSource.instance);

		if (jobInfo != null)
			jobInfo.setDataSource(newDefaultDataSource);

		// Start all the jobs associated with this list

		for (JobInfo jInfo : jobs.values()) {
			try {
				jInfo.start(this);
			} catch (AcnetStatusException e) {
				jInfo.stop();
				protocolReplier.sendStatusNoEx(e.status);
			} catch (Exception e) {
				jInfo.stop();
				protocolReplier.sendStatusNoEx(DPM_INTERNAL_ERROR);
				logger.log(Level.WARNING, "exception in start()", e);
				dispose(DPM_INTERNAL_ERROR);
			}
		}

		setRestartableProperty();

		// Calc request count based on running jobs

		Scope.listProperties(this);
		Scope.listStarted(this);
		Scope.listUser(this);
	}

	synchronized public void stop()
	{
		logger.log(Level.FINE, String.format("%s StopList from:%-6s", id, clientHostName()));

		for (JobInfo jInfo : jobs.values())
			jInfo.stop();
	}

    private boolean isFromPrivilegedHost()
    {
        try {
            final byte[] addrBytes = fromHostAddress().getAddress();

            if ((addrBytes[0] & 0xff) == 131 && (addrBytes[1] & 0xff) == 225) {
                final int subnet = addrBytes[2] & 0xff;

                return subnet == 120 || subnet == 121;
            }
        } catch (Exception e) {
			logger.log(Level.WARNING, "unable to get host address for privilege check", e);
		}

        return false;
    }

    private boolean hasStandardConsoleProperties()
    {
        return properties.containsKey("TASK") && properties.containsKey("SLOTINDEX");
    }

    private boolean hasDaqUserProperties()
    {
        return properties.containsKey("TASK") && properties.containsKey("DAQUSER");
    }

    public boolean allowPrivilegedSettings()
    {
        return isFromPrivilegedHost() && (hasStandardConsoleProperties() || hasDaqUserProperties());
    }

	public ConsoleUser getAuthenticatedUser() throws AcnetStatusException
	{
		if (gssName == null) {
			if (allowPrivilegedSettings()) {
				try {
					gssName = GSSManager.getInstance().createName(property("USER"), GSSName.NT_USER_NAME);
				} catch (Exception e) {
					logger.log(Level.FINE, "Unable to create GSSName from '" + property("USER") + "'");
					throw new AcnetStatusException(DPM_PRIV);

					//try {
					//	gssName = GSSManager.getInstance().createName("neswold@FNAL.GOV", GSSName.NT_USER_NAME);
					//} catch (Exception e2) {
					//	logger.log(Level.FINE, "Unable to create GSSName from '" + property("USER") + "'");
					//	throw new AcnetStatusException(DPM_PRIV);
					//}
				}
			} else {
				throw new AcnetStatusException(DPM_PRIV);
			}
		}
		
		final ConsoleUser consoleUser = ConsoleUserManager.get(gssName);

		if (consoleUser == null)
			throw new AcnetStatusException(DPM_PRIV);
		
		return consoleUser;
	}

	public void addSetting(SettingData setting)
	{
		settings.get().add(setting);
	}

	protected void sendStatus(List<SettingData> settings, int status) throws InterruptedException, IOException, AcnetStatusException
	{
		final ArrayList <SettingStatus> settingStatus = new ArrayList<>();

		for (SettingData setting : settings)
			settingStatus.add(new SettingStatus(setting.refId, status));

		protocolReplier.sendReply(settingStatus);
	}

	protected void sendStatusNoEx(List<SettingData> settings, int status)
	{
		try {
			sendStatus(settings, status);
		} catch (Exception ignore) { }
	}

	public void applySettings()
	{
		final List<SettingData> settings = this.settings.getAndSet(new ArrayList<SettingData>());

		try {
			final SettingDataSource dataSource = new SettingDataSource(settings, getAuthenticatedUser());
			final JobInfo jobInfo = new JobInfo(dataSource);

			for (SettingData setting : settings) {
				final DPMRequest tmp = requests.get(setting.refId);

				if (tmp != null)
					jobInfo.addRequest(setting.refId, tmp);
			}

			jobInfo.start(this);
			jobs.put(dataSource, jobInfo);
		} catch (AcnetStatusException e) {
			sendStatusNoEx(settings, e.status);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception in applySettings()", e);
			sendStatusNoEx(settings, DPM_INTERNAL_ERROR);
		}
	}

	public static int remaining()
	{
		return idPool.freeCount();
	}
}
