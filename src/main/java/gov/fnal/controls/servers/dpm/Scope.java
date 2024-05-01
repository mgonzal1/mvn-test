// $Id: Scope.java,v 1.63 2024/04/17 14:34:40 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.reflect.Method;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ietf.jgss.GSSException;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetCancel;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequest;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetMessage;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequestHandler;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetMessageHandler;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.service.proto.DPMScope;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.PoolType;
import gov.fnal.controls.servers.dpm.pools.AcceleratorPool;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class RefreshTask extends TimerTask
{
	private static volatile Timer timer = null;
	private static final Map<String, RefreshTask> activeTasks = new HashMap<>();
	private static final String packageName = RefreshTask.class.getPackage().getName();

	final String className;
	final Method method;
	final int delay;

	private RefreshTask(String className) throws Exception
	{
		this.className = className;
		this.method = Class.forName(packageName + "." + className).getDeclaredMethod("refresh");
		this.delay = randomDelay(5, 60);

		logger.log(Level.INFO, className + ": refresh scheduled in " + delay + " seconds"); 
	}

	@Override
	public void run()
	{
		try {
			method.invoke(null);
			logger.log(Level.INFO, className + ": refresh complete");
		} catch (Exception e) {
			logger.log(Level.INFO, className + ": refresh failed", e);
		}

		synchronized (activeTasks) {
			activeTasks.remove(className);
			if (activeTasks.size() == 0) {
				timer.cancel();
				timer = null;
			}
		}
	}

	private int randomDelay(int min, int max)
	{
		return (int) (Math.random() * (max - min + 1) + min);
	}

	synchronized static int activeCount()
	{
		synchronized (activeTasks) {
			return activeTasks.size();
		}
	}

	synchronized static boolean schedule(String className) throws Exception
	{
		synchronized (activeTasks) {
			if (timer == null)
				timer = new Timer("Scope refresh timer", true);

			if (!activeTasks.containsKey(className)) {
				final RefreshTask task = new RefreshTask(className);

				activeTasks.put(className, task);
				timer.schedule(task, task.delay * 1000);

				return true;
			}

			return false;
		}
	}
}

public class Scope implements AcnetMessageHandler, AcnetRequestHandler, DPMScope.Request.Receiver, AcnetErrors
{
/*
	class ScopeServer extends Thread
	{
		private final InetSocketAddress address = new InetSocketAddress(0);
		private final ServerSocketChannel serverChannel;

		ScopeServer() throws IOException
		{
			this.serverChannel = ServerSocketChannel.open();
			this.serverChannel.bind(address);
			this.setName("Scope accept");
			this.start();
		}

		public void run()
		{
			try {
				while (true)
					new HandlerThread(serverChannel.accept());
			} catch (Exception ignore) { }
		}

		void close() throws IOException
		{
			serverChannel.close();
		}
	}
	*/

	private static AcnetConnection connection;
	//private static ScopeServer scopeServer;
	private static ConcurrentLinkedQueue<HandlerThread> handlers = new ConcurrentLinkedQueue<>();

	private final ByteBuffer rpyBuf = ByteBuffer.allocate(8 * 1024);
	private final DPMScope.Reply.ServiceDiscovery discoveryReply = new DPMScope.Reply.ServiceDiscovery(); 

	private int repliesPerSecondMax;
	private AcnetRequest request;

	private static volatile int listOpenedCount;

	static void init() throws AcnetStatusException
	{
		new Scope(AcnetInterface.open("SCOPE"));
	}

	static void close() throws IOException
	{
		//scopeServer.close();

		for (HandlerThread handler : handlers)
			handler.close();
	}

	private Scope(AcnetConnection connection) throws AcnetStatusException
	{
		this.repliesPerSecondMax = 0;
		this.connection = connection;
		this.connection.handleRequests(this);
		this.connection.handleMessages(this);
		
		try {
			this.discoveryReply.hostName = DPMServer.localHostName();
			//this.scopeServer = new ScopeServer();
			this.discoveryReply.codeVersion = DPMServer.codeVersion();
		} catch (Exception ignore) {
			this.discoveryReply.hostName = "-";
		}
		this.discoveryReply.nodeName = this.connection.getLocalNodeName();
		this.discoveryReply.date = System.currentTimeMillis();
	}

	@Override
    public void handle(AcnetRequest r)
	{
		request = r;

		try {
			DPMScope.Request.unmarshal(r.data()).deliverTo(this);
		} catch (Exception e) {
			request.sendLastStatusNoEx(ACNET_SYS);
			logger.log(Level.WARNING, "exception delivering request", e);
		}
	}

	@Override
    public void handle(AcnetCancel c) { }

	@Override
	public void handle(AcnetMessage r)
	{
		request = null;

		try {
			DPMScope.Request.unmarshal(r.data()).deliverTo(this);
		} catch (Exception e) {
			logger.log(Level.FINE, "exception unmarshaling request", e);
		}
	}

	@Override
	public void handle(DPMScope.Request.ServiceDiscovery m)
	{
		try {
			final Collection<DPMList> lists = DPMServer.activeLists();

			discoveryReply.activeListCount = lists.size();
			discoveryReply.totalListCount = listOpenedCount;
			discoveryReply.requestCount = 0;
			discoveryReply.totalRequestCount = DPMList.totalRequestCount;
			discoveryReply.consolidationHits = AcceleratorPool.consolidationHits();
			discoveryReply.repliesPerSecond = 0;
			discoveryReply.repliesPerSecondMax = 0;
			discoveryReply.loadPct = DPMServer.loadPercentage();
			discoveryReply.logLevel = DPMServer.getLogLevel().getName();
			discoveryReply.pid = DPMServer.pid();
			discoveryReply.heapFreePct = DPMServer.heapFreePercentage();
			discoveryReply.activeRefresh = RefreshTask.activeCount() > 0;
			discoveryReply.restartScheduled = DPMServer.restartScheduled();
			discoveryReply.inDebugMode = DPMServer.debug();
			discoveryReply.scopePort = DPMServer.scopeChannel.socket().getLocalPort();

			for (DPMList list : lists) {
				//discoveryReply.requestCount += list.requestCount();
				discoveryReply.requestCount += list.whatDaqCount();
				discoveryReply.repliesPerSecond += list.repliesPerSecond;
				discoveryReply.repliesPerSecondMax += list.repliesPerSecondMax;
			}

			if (discoveryReply.repliesPerSecondMax > repliesPerSecondMax)
				repliesPerSecondMax = discoveryReply.repliesPerSecondMax;
			else
				discoveryReply.repliesPerSecondMax = repliesPerSecondMax;

			rpyBuf.clear();
			discoveryReply.marshal(rpyBuf).flip();
			request.sendLastReply(rpyBuf, 0);
		} catch (Exception ignore) { 
			request.ignoreNoEx();
		}
	}

	@Override
	public void handle(DPMScope.Request.Restart m)
	{
	}

	@Override
	public void handle(DPMScope.Request.Refresh m)
	{
		try {
			if (request != null) {
				RefreshTask.schedule(m.name);
				request.sendLastStatus(0);
			} else
				RefreshTask.schedule(m.name);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception handling refresh", e);
			request.sendLastStatusNoEx(0);
		}
	}

	@Override
	public void handle(DPMScope.Request.DumpPools m)
	{
		request.sendLastStatusNoEx(ACNET_SYS);
	}

	@Override
	public void handle(DPMScope.Request.SetLogLevel m)
	{
		request.sendLastStatusNoEx(ACNET_SYS);
	}

	@Override
	public void handle(DPMScope.Request.DisposeList m)
	{
		request.sendLastStatusNoEx(ACNET_SYS);
	}

	@Override
	public void handle(DPMScope.Request.List m)
	{
		request.sendLastStatusNoEx(ACNET_SYS);
	}

	static String userName(DPMList list)
	{
		try {
			return list.userName().toString();
		} catch (Exception e) { 
			return "";
		}
	}

	public static void listOpened(DPMList list)
	{
		listOpenedCount++;

		if (!handlers.isEmpty()) {
			final DPMScope.Reply.ListOpened m = new DPMScope.Reply.ListOpened();

			m.id = list.id().value();
			m.date = list.createdTime;

			for (HandlerThread handler : handlers)
				handler.queueReply(m);
		}
	}

	public static void listStarted(DPMList list)
	{
		if (!handlers.isEmpty()) {
			final DPMScope.Reply.ListStarted m = new DPMScope.Reply.ListStarted();

			m.id = list.id().value();
			m.requestCount = list.requestCount();
			m.date = System.currentTimeMillis();

			for (HandlerThread handler : handlers)
				handler.queueReply(m);
		}
	}

	static void listSettingsStarted(DPMList list)
	{
		if (!handlers.isEmpty()) {
			final DPMScope.Reply.ListSettingsStarted m = new DPMScope.Reply.ListSettingsStarted();

			m.id = list.id().value();
			m.requestCount = list.requestCount();
			m.date = System.currentTimeMillis();

			for (HandlerThread handler : handlers)
				handler.queueReply(m);
		}
	}

	static void listSettingsComplete(DPMList list)
	{
		if (!handlers.isEmpty()) {
			final DPMScope.Reply.ListSettingsComplete m = new DPMScope.Reply.ListSettingsComplete();

			m.id = list.id().value();

			for (HandlerThread handler : handlers)
				handler.queueReply(m);
		}
	}

	static void listProperties(DPMList list)
	{
		if (!handlers.isEmpty()) {
			final DPMScope.Reply.ListProperties m = new DPMScope.Reply.ListProperties();

			m.id = list.id().value();
			m.properties = list.properties();

			for (HandlerThread handler : handlers)
				handler.queueReply(m);
		}
	}

	public static void listHost(DPMList list)
	{
		if (!handlers.isEmpty()) {
			final DPMScope.Reply.ListHost m = new DPMScope.Reply.ListHost();

			m.id = list.id().value();
			m.hostName = list.clientHostName();

			for (HandlerThread handler : handlers)
				handler.queueReply(m);
		}
	}

	static void listUser(DPMList list)
	{
		if (!handlers.isEmpty()) {
			final DPMScope.Reply.ListUser m = new DPMScope.Reply.ListUser();

			m.id = list.id().value();
			m.userName = userName(list);

			for (HandlerThread handler : handlers)
				handler.queueReply(m);
		}
	}

	static void listRepliesPerSecond(DPMList list)
	{
		if (!handlers.isEmpty()) {
			final DPMScope.Reply.ListReplyCount m = new DPMScope.Reply.ListReplyCount();

			m.id = list.id().value();
			m.repliesPerSecond = list.repliesPerSecond;
			m.repliesPerSecondMax = list.repliesPerSecondMax;

			for (HandlerThread handler : handlers)
				handler.queueReply(m);
		}
	}

	public static void listDisposed(DPMList list)
	{
		if (!handlers.isEmpty()) {
			final DPMScope.Reply.ListDisposed m = new DPMScope.Reply.ListDisposed();

			m.id = list.id().value();
			m.date = list.disposedTime;
			m.status = list.disposedStatus;

			for (HandlerThread handler : handlers)
				handler.queueReply(m);
		}
	}

	static class HandlerThread extends Thread implements DPMScope.Request.Receiver
	{
		final Selector selector;
		final ConcurrentLinkedQueue<DPMScope.Reply> queue = new ConcurrentLinkedQueue<>();
		final ByteBuffer ibuf = ByteBuffer.allocate(128 * 1024);
		final ByteBuffer obufs[] = { ByteBuffer.allocate(4), ByteBuffer.allocate(128 * 1024) };
		final SocketChannel channel;
		final String remoteHostName;
		final SelectionKey selectKey;

		boolean done = false;

		HandlerThread(SocketChannel channel) throws IOException
		{
			this.channel = channel;
			this.setName("Scope connection - " + channel);

			this.selector = Selector.open();
			this.channel.configureBlocking(false);
			this.channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
			this.channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			this.selectKey = this.channel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

			final SocketAddress addr = this.channel.getRemoteAddress();
			
			if (addr instanceof InetSocketAddress)
				this.remoteHostName = ((InetSocketAddress) addr).getHostName();
			else
				this.remoteHostName = addr.toString();

			this.channel.finishConnect();
			this.setDaemon(true);

			this.ibuf.clear().limit(4);
			this.obufs[0].clear().limit(0);
			this.obufs[1].clear().limit(0);

			this.start();
		}

		synchronized void close()
		{
			handlers.remove(this);
			done = true;
			selector.wakeup();
		}

		synchronized private void queueReply(DPMScope.Reply m)
		{
			queue.offer(m);
			selectKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			selector.wakeup();
		}

		private void queueList(DPMList list)
		{
			final DPMScope.Reply.List m = new DPMScope.Reply.List();

			m.id = list.id().value();
			m.date = list.createdTime;
			m.hostName = list.clientHostName();
			m.userName = userName(list);
			m.properties = list.properties();
			m.repliesPerSecond = list.repliesPerSecond;
			m.repliesPerSecondMax = list.repliesPerSecondMax;
			m.disposedDate = list.disposedTime;
			//m.requestCount = list.requestCount();
			m.requestCount = list.whatDaqCount();

			queueReply(m);
		}

		private DPMScope.DrfRequest to(WhatDaq whatDaq)
		{
			final DPMScope.DrfRequest r = new DPMScope.DrfRequest();

			r.refId = whatDaq.refId;
			r.name = whatDaq.getDeviceName();
			r.property = whatDaq.property.toString();
			r.foreignName = whatDaq.pInfo.foreignName;
			r.foreignType = whatDaq.dInfo.type.toString();
			r.length = whatDaq.getLength();
			r.offset = whatDaq.getOffset();
			r.event = whatDaq.getEvent().toString();

			return r;
		}

		private void queueRequests(DPMList list, ArrayList<DPMScope.DrfRequest> reqArr)
		{
			final DPMScope.Reply.Requests m = new DPMScope.Reply.Requests();

			m.id = list.id().value();
			m.date = list.createdTime;
			m.requests = (DPMScope.DrfRequest[]) reqArr.toArray(new DPMScope.DrfRequest[reqArr.size()]);
			queueReply(m);
		}

		private void queueRequests(DPMList list)
		{
			final int RequestsPerMessage = 100;
			final ArrayList<DPMScope.DrfRequest> reqArr = new ArrayList<DPMScope.DrfRequest>(RequestsPerMessage);

			for (Map.Entry<Model, JobInfo> entry : list.jobs.entrySet()) {
				for (WhatDaq whatDaq : entry.getValue().whatDaqs()) {
					if (reqArr.size() == RequestsPerMessage) {
						queueRequests(list, reqArr);
						reqArr.clear();
					}
		
					final DPMScope.DrfRequest r = to(whatDaq);
					r.model = entry.getKey().toString();

					reqArr.add(r);
				}
			}

			if (reqArr.size() > 0)
				queueRequests(list, reqArr);
		}

		private void queueSettingRequests(DPMList list, ArrayList<DPMScope.DrfRequest> reqArr)
		{
			final DPMScope.Reply.SettingRequests m = new DPMScope.Reply.SettingRequests();

			m.id = list.id().value();
			m.date = list.createdTime;
			m.requests = (DPMScope.DrfRequest[]) reqArr.toArray(new DPMScope.DrfRequest[reqArr.size()]);

			queueReply(m);
		}

		private void queueSettingRequests(DPMList list)
		{
			final int RequestsPerMessage = 100;
			final ArrayList<DPMScope.DrfRequest> reqArr = new ArrayList<DPMScope.DrfRequest>(RequestsPerMessage);

			//if (list.settingsHandler != null) {
				//for (SettingStats stats : list.settingsHandler.settingStats()) {

				/*
				for (SettingStats stats : list.settingStats()) {
					if (reqArr.size() == RequestsPerMessage) {
						queueSettingRequests(list, reqArr);
						reqArr.clear();
					}

					final DPMScope.DrfRequest r = to(stats.whatDaq);
					
					r.setCount = stats.count;
					r.model = "";

					reqArr.add(r);
				}
				*/

			//}

			if (reqArr.size() > 0)
				queueSettingRequests(list, reqArr);
		}

		private void handleRequest(DPMScope.Request request) throws IOException
		{
			request.deliverTo(this);
		}

		private final void handleChannel()
		{
			int readLen = -1;

			try {
				while (!done) {
					final int count = selector.select();

  					if (count > 0) {
						final Set<SelectionKey> keys = selector.selectedKeys();
						final Iterator<SelectionKey> iter = keys.iterator();

						while (iter.hasNext()) {
							final SelectionKey key = iter.next();

							if (key.isReadable()) {

								// Channel is ready for reading

								if (readLen == -1) {
									if (channel.read(ibuf) != -1) {
										if (ibuf.remaining() == 0) {
											ibuf.flip();
											readLen = ibuf.getInt();
											ibuf.clear().limit(readLen);
										}
									} else
										return;
								}

								if (readLen != -1) {
									if (channel.read(ibuf) != -1) {
										if (ibuf.remaining() == 0) {
											ibuf.flip();
											readLen = -1;
											handleRequest(DPMScope.Request.unmarshal(ibuf));
											ibuf.clear().limit(4);
										}
									} else
										return;
								}
							} else if (key.isWritable()) {

								// Channel is ready for writing

								while (true) {
									if (obufs[1].remaining() == 0) {
										DPMScope.Reply m = queue.poll();

										if (m != null) {
											obufs[1].clear();
											m.marshal(obufs[1]);
											obufs[1].flip();

											obufs[0].clear();
											obufs[0].putInt(obufs[1].remaining());
											obufs[0].flip();
										} else {
											key.interestOps(SelectionKey.OP_READ);
											break;
										}
									}

									channel.write(obufs);

									if (obufs[1].remaining() > 0)
										break;
								}
							}

							iter.remove();
						}
					}
				}
			} catch (Exception e) {
				logger.log(Level.FINE, "exception handling scope channel", e);
			}
		}

		public void run()
		{
			handlers.add(this);

			logger.log(Level.INFO, String.format("Scope thread %s begin - %d active thread(s)", 
													channel, handlers.size()));

			// Send over the current state of the DPM 

			for (DPMList list : DPMServer.activeLists()) {
				queueList(list);
				queueRequests(list);
				queueSettingRequests(list);
			}

			for (DPMList list : DPMServer.disposedLists()) {
				queueList(list);
				queueRequests(list);
				queueSettingRequests(list);
			}

			handleChannel();

			handlers.remove(this);

			try {
				selector.close();
			} catch (Exception ignore) { }
			try {
				channel.close();
			} catch (Exception ignore) { }

			logger.log(Level.INFO, String.format("Scope thread %s end - %d active thread(s)", 
													channel, handlers.size()));
		}

		@Override
		public void handle(DPMScope.Request.Restart m)
		{
			logger.log(Level.INFO, "Restart requested");
			queueReply(new DPMScope.Reply.Restart());
			DPMServer.scheduleRestart();
		}

		@Override
		public void handle(DPMScope.Request.SetLogLevel m)
		{
			try {
				final DPMScope.Reply.SetLogLevel rpy = new DPMScope.Reply.SetLogLevel();

				DPMServer.setLogLevel(Level.parse(m.newLogLevel));
				rpy.newLogLevel = DPMServer.getLogLevel().getName();

				queueReply(rpy);
			} catch (IllegalArgumentException ignore) {
			}
		}

		@Override
		public void handle(DPMScope.Request.DisposeList m)
		{
			final DPMScope.Reply.DisposeList rpy = new DPMScope.Reply.DisposeList();

			try {
				DPMServer.list(m.id).dispose(m.status);
				rpy.succeeded = true;
			} catch (Exception e) {
				rpy.succeeded = false;
			} finally {
				queueReply(rpy);
			}
		}

		@Override
		public void handle(DPMScope.Request.List m)
		{
			logger.log(Level.FINE, String.format("Scope: request list %04x", m.id));

			final DPMScope.Reply.List rpy = new DPMScope.Reply.List();

			try {
				final DPMList list = DPMServer.list(m.id);

				rpy.id = list.id().value();
				rpy.date = list.createdTime;
				rpy.hostName = list.clientHostName();

				try {
					rpy.userName = list.userName().toString();
				} catch (GSSException e) {
					rpy.userName = "";
				}

				rpy.properties = list.properties();
				rpy.repliesPerSecond = list.repliesPerSecond;
				rpy.repliesPerSecondMax = list.repliesPerSecondMax;
				rpy.disposedDate = list.disposedTime;
				rpy.disposedStatus = list.disposedStatus;
				rpy.requestCount = list.requestCount();

				queueReply(rpy);
				queueRequests(list);
				queueSettingRequests(list);
			} catch (Exception e) {
				logger.log(Level.FINE, "exception in list reply", e);
			}
		}

		@Override
		public void handle(DPMScope.Request.DumpPools m)
		{
			String poolDumpText = "";

			switch (m.poolType) {
			 case AcnetRepetitive:
				poolDumpText = AcceleratorPool.dumpPool(PoolType.ACNET);
				break;

			 case AcnetOneshot:
				poolDumpText = AcceleratorPool.dumpPool(PoolType.ACNET);
				break;
			 
			 case PVAMonitors:
				poolDumpText = AcceleratorPool.dumpPool(PoolType.EPICS);
				break;
			}

			
			int startIndex = 0;

			while (startIndex < poolDumpText.length()) {
				final DPMScope.Reply.DumpPools rpy = new DPMScope.Reply.DumpPools();

				rpy.poolType = m.poolType;

				int endIndex = startIndex + (16 * 1024); 

				if (endIndex > poolDumpText.length())
					endIndex = poolDumpText.length();

				rpy.text = poolDumpText.substring(startIndex, endIndex); 
				queueReply(rpy);
				startIndex = endIndex;
			}

			final DPMScope.Reply.DumpPools rpy = new DPMScope.Reply.DumpPools();

			rpy.poolType = m.poolType;
			rpy.text = "";
			queueReply(rpy);
		}

		@Override
		public void handle(DPMScope.Request.ServiceDiscovery m)
		{
		}

		@Override
		public void handle(DPMScope.Request.Refresh m)
		{
		}
	}
}
