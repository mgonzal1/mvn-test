// $Id: PVAPool.java,v 1.7 2024/03/05 17:42:33 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.epics;

import java.util.Set;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

import gov.fnal.controls.servers.dpm.DPMServer;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.pools.AcceleratorPool;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.client.GetRequest2;
import org.epics.pva.client.PutRequest2;
import org.epics.pva.client.PVAClient;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.MonitorListener;
import org.epics.pva.client.ClientChannelState;
import org.epics.pva.client.ClientChannelListener;

public class PVAPool extends TimerTask
{
	private static class Channel extends TimerTask implements ClientChannelListener, AcnetErrors
	{
		private abstract class Request implements ClientChannelListener, MonitorListener, AcnetErrors
		{
			final String request;

			Request(String request)
			{
				this.request = request;
			}

			private void sendStatus(int status)
			{
				final BitSet bs = new BitSet();
				final PVAStructure struct = new PVAStructure("", "fermi:nt/NTAcnetStatus:1.0", new PVAInt("value", status));

				handleMonitor(channel, bs, bs, struct);
			}

			@Override
			public void channelStateChanged(PVAChannel channel, ClientChannelState state)
			{
				switch (state) {
					case SEARCHING:
						sendStatus(DPM_PEND);
						break;

					case CONNECTED:
						try {
							start();
						} catch (Exception e) { 
							sendStatus(ACNET_SYS);
							logger.log(Level.FINE, "PVAPool channel [" + channel.getName() + 
											"] request: [" + request + "] exception:", e);
						}
						break;
				}
			}

			boolean start()
			{
				try {
					startImpl();
					return true;
				} catch (Exception e) { 
					sendStatus(ACNET_SYS);
					logger.log(Level.FINE, "PVAPool channel [" + channel.getName() + 
									"] request: [" + request + "] exception:", e);
				}
				return false;
			}

			abstract void startImpl() throws Exception;
			abstract public void handleMonitor(PVAChannel channel, BitSet changes, BitSet overruns, PVAStructure data);
			abstract EpicsListener[] listeners();
		}

		private class Get extends Request
		{
			private final EpicsListener listener;

			Get(String request, EpicsListener listener)
			{
				super(request);
				this.listener = listener;
			} 

			@Override
			void startImpl() throws Exception
			{
				if (channel.isConnected()) {
					new GetRequest2(channel, request, this);
					logger.fine("PVAPool.channelget '" + request + "'");
				}
			}

			@Override
			public void handleMonitor(PVAChannel channel, BitSet changes, BitSet overruns, PVAStructure data)
			{
				listener.handleData(channel, changes, overruns, data);
				removeChannelListener(this);
			}

			@Override
			EpicsListener[] listeners()
			{
				return new EpicsListener[] { listener };
			}

			@Override
			public String toString()
			{
				return "GET [" + request + "]";
			}
		}

		private class Put extends Request
		{
			private final Object value;
			private final EpicsListener listener;

			Put(String request, Object value, EpicsListener listener)
			{
				super(request);
				this.value = value;
				this.listener = listener;
			}

			@Override
			void startImpl() throws Exception
			{
				if (channel.isConnected()) {
					new PutRequest2(channel, request, value, this);
					logger.fine("PVAPool.channelput '" + request + "' => " + value);
				}
			}

			@Override
			public void handleMonitor(PVAChannel channel, BitSet changes, BitSet overruns, PVAStructure data)
			{
				listener.handleData(channel, changes, overruns, data);
				logger.fine("PVAPool.channelput '" + request + "' complete => " + value);
				removeChannelListener(this);
			}

			@Override
			EpicsListener[] listeners()
			{
				return new EpicsListener[] { listener };
			}

			@Override
			public String toString()
			{
				return "PUT [" + request + "]";
			}
		}

		private class Monitor extends Request
		{
			private volatile AutoCloseable monitorCloseable = null;
			private final ConcurrentLinkedQueue<EpicsListener> listeners = new ConcurrentLinkedQueue<>();

			// Cached data for merges

			private PVAStructure lastData;
			private BitSet lastChanges, lastOverruns;

			Monitor(String request)
			{
				super(request);
			} 

			@Override
			void startImpl() throws Exception
			{
				if (channel.isConnected()) {
					monitorCloseable = channel.subscribe(request, this);
					logger.fine("PVAPool.channelsubscribe '" + request + "'");
				}
			}

			@Override
			public void handleMonitor(PVAChannel channel, BitSet changes, BitSet overruns, PVAStructure data)
			{
				for (EpicsListener l : listeners)
					l.handleData(channel, changes, overruns, data);

				lastData = data;
				lastChanges = changes;
				lastOverruns = overruns;
			}

			void addListener(EpicsListener listener)
			{
				listeners.add(listener);

				if (lastData != null)
					listener.handleData(channel, lastChanges, lastOverruns, lastData);
			}

			void removeListener(EpicsListener listener)
			{
				listeners.removeIf(l -> l == listener);

				if (listeners.size() == 0) {
					logger.fine("PVAPool.channelstop '" + request + "'");

					if (monitorCloseable != null) {
						try {
							monitorCloseable.close();
							logger.fine("PVAPool.channelunsubscribe '" + request + "'");
						} catch (Exception e) {
							logger.log(Level.FINE, "exception closing pva channel", e);
						}
					}
				}
			}

			@Override
			public boolean equals(Object o)
			{
				if (o instanceof Monitor)
					return ((Monitor) o).request.equals(this.request);

				return false;
			}

			@Override
			EpicsListener[] listeners()
			{
				return listeners.toArray(new EpicsListener[listeners.size()]);
			}

			@Override
			public String toString()
			{
				return "MON [" + request + "]";
			}
		}

		// Channel

		private final PVAChannel channel;
		private final HashMap<String, Monitor> monitors = new HashMap<>(128); 
		private final Set<Request> channelListeners = ConcurrentHashMap.newKeySet();
		private volatile long timeLastUsed = System.currentTimeMillis();;

		Channel(String pv)
		{
			this.channel = pool.pvaClient.getChannel(pv, this);
			pool.timer.schedule(this, 2000);
		}

		// TimerTask timeout

		@Override
		public void run()
		{
			sendStatus(ACNET_REQTMO);
		}

		private void sendStatus(int status)
		{
			for (Request r : channelListeners)
				r.sendStatus(status);
		}

		@Override
		public void channelStateChanged(PVAChannel channel, ClientChannelState state)
		{
			logger.fine("PVAPool.channelstate " + channel.getName() + " " + state);

			for (Request r : channelListeners)
				r.channelStateChanged(channel, state);

			switch (state) {
				case CONNECTED:

					// Cancel search timeout when successfully connected

					cancel();
					break;
			}
		}

		synchronized void subscribe(String request, EpicsListener listener)
		{
			Monitor monitor = monitors.get(request);

			if (monitor == null) {
				monitor = new Monitor(request);
				channelListeners.add(monitor);
				monitors.put(request, monitor);
				monitor.start();
			} else
				AcceleratorPool.consolidationHit();

			monitor.addListener(listener);

			timeLastUsed = System.currentTimeMillis();;
		}

		synchronized void unsubscribe(String request, EpicsListener listener)
		{
			Monitor monitor = monitors.get(request);
				
			if (monitor != null) {
				monitor.removeListener(listener);

				if (monitor.listeners.size() == 0) {
					channelListeners.remove(monitor);
					monitors.remove(request);
				}
			}

			timeLastUsed = System.currentTimeMillis();;
		}

		synchronized void get(String request, EpicsListener listener)
		{
			Get get = new Get(request, listener);

			channelListeners.add(get);
			get.start();
			timeLastUsed = System.currentTimeMillis();;
		}

		synchronized void put(String request, Object value, EpicsListener listener)
		{
			Put put = new Put(request, value, listener);

			channelListeners.add(put);
			put.start();
			timeLastUsed = System.currentTimeMillis();;
		}

		protected synchronized void removeChannelListener(Request req)
		{
			channelListeners.remove(req);
		}

		synchronized int requestCount()
		{
			return channelListeners.size();
		}

		void dump(StringBuilder buf)
		{
			for (Request r : channelListeners) {
				buf.append("        ");
				buf.append(r.toString());
				buf.append(r.request);
				buf.append(" ");

				final EpicsListener[] listeners = r.listeners();
				buf.append(listeners.length);
				buf.append(" client(s)"); 
				buf.append('\n');

				for (EpicsListener l : listeners) {
					buf.append("            ");
					buf.append(l.toString());
					buf.append('\n');
				}
			}
		}
	}

	private static PVAPool pool = null;

	private final PVAClient pvaClient;
	private final Timer timer;
	private final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>(2048);

	private PVAPool() throws Exception
	{
		this.pvaClient = new PVAClient();
		this.timer = new Timer("PVAPool", true);

		this.timer.scheduleAtFixedRate(this, 5000, 5000);
	}

	// TimerTask run()

	@Override
	public void run()
	{
		final long now = System.currentTimeMillis();

		final Set<Entry<String, Channel>> channelSet = channels.entrySet();
		final Iterator<Entry<String, Channel>> ii = channelSet.iterator();

		while (ii.hasNext()) {
			Channel channel = ii.next().getValue();
			if (channel.requestCount() == 0 && (now - channel.timeLastUsed) > 15000) {
				logger.finer("PVAPool.closechannel [" + channel.channel.getName() + "]");
				channel.channel.close();
				ii.remove();
			}				
		}
	}

	static private void init()
	{
		try {
			if (pool == null)
				pool = new PVAPool();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "exception initializing pva pool", e);
		}
	}

	static synchronized public void subscribe(EpicsRequest eReq)
	{
		init();

		Channel channel = pool.channels.get(eReq.channel);
		
		if (channel == null) {
			channel = new Channel(eReq.channel);
			logger.fine("PVAPool.openchannel " + eReq.channel);
			pool.channels.put(eReq.channel, channel);
		}

		channel.subscribe(eReq.request, eReq.listener);
	}

	static synchronized public void unsubscribe(EpicsRequest eReq)
	{
		init();

		final Channel channel = pool.channels.get(eReq.channel);

		if (channel != null)
			channel.unsubscribe(eReq.request, eReq.listener);
	}

	static synchronized public void get(EpicsRequest eReq)
	{
		init();

		Channel channel = pool.channels.get(eReq.channel);

		if (channel == null) {
			channel = new Channel(eReq.channel);
			logger.fine("PVAPool.openchannel " + eReq.channel);
			pool.channels.put(eReq.channel, channel);
		}

		channel.get(eReq.request, eReq.listener);
	}

	static synchronized public void put(EpicsRequest eReq, Object value)
	{
		init();

		Channel channel = pool.channels.get(eReq.channel);

		if (channel == null) {
			channel = new Channel(eReq.channel);
			logger.fine("PVAPool.openchannel " + eReq.channel);
			pool.channels.put(eReq.channel, channel);
		}

		channel.put(eReq.request, value, eReq.listener);
	}

	static synchronized public void put(EpicsRequest eReq)
	{
		put(eReq, eReq.newValue);
	}

	public static synchronized String dump()
	{
		init();

		final StringBuilder buf = new StringBuilder();

		buf.append("PVAPool:");
		buf.append('\n');

		for (Channel chan : pool.channels.values()) {
			buf.append("    ");
			buf.append("CHANNEL [");
			buf.append(chan.channel.getName());
			buf.append("] - ");
			buf.append(chan.channel.getState());
			buf.append('\n');

			chan.dump(buf);
			buf.append('\n');
		}

		return buf.toString();
	}
}
