// $Id: LoggerConfigCache.java,v 1.21 2024/11/21 22:07:18 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.ArrayList;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.DPMServer;
import gov.fnal.controls.servers.dpm.DPMRequest;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.DeviceCache;
import gov.fnal.controls.servers.dpm.pools.LoggedDevice;
import gov.fnal.controls.servers.dpm.drf3.AcnetRequest;
import gov.fnal.controls.servers.dpm.drf3.NeverEvent;
import gov.fnal.controls.servers.dpm.drf3.PeriodicEvent;
import gov.fnal.controls.servers.dpm.drf3.Event;

import static gov.fnal.controls.db.DbServer.getDbServer;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class LoggerConfigCache extends TimerTask implements AcnetErrors
{
	static private final int UPDATE_RATE = 1000 * 60 * 30;

	static private HashMap<Device, List<LoggedDeviceEntry>> loggerDeviceMap = new HashMap<>();
	static private HashMap<String, Integer> loggerNameMap = new HashMap<>();
	static private HashMap<Integer, String> loggerNodeMap = new HashMap<>();
	static private HashSet<String> badEntries = new HashSet<>();

	private static class Device
	{
		final String name;
		final int property;

		Device(String name, int property)
		{
			this.name = name;
			this.property = property;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(name, property);
		}

		@Override
		public boolean equals(Object o)
		{
			if (o == this)
				return true;

			if (o instanceof Device) {
				final Device d = (Device) o;

				return d.name.equalsIgnoreCase(name) &&
						d.property == property;
			}

			return false;
		}
	}

	private static class LoggedDeviceEntry implements LoggedDevice
	{
		private final Node node;
		private final String loggerName;
		private final String loggedDrf;
		private final String event;
		private final int listId;

		LoggedDeviceEntry(Node node, String loggerName, String loggedDrf, String event, int listId)
		{
			this.node = node;
			this.loggerName = loggerName;
			this.loggedDrf = loggedDrf;
			this.event = event;
			this.listId = listId;
		}

		LoggedDeviceEntry(Node node, String loggedDrf) throws AcnetStatusException
		{
			this(node, LoggerConfigCache.loggerName(node), loggedDrf, "", -1);
		}

		LoggedDeviceEntry(int node, String loggerName, String loggedDrf, String event, int listId) throws AcnetStatusException
		{
			this(Node.get(node), loggerName, loggedDrf, event, listId);
		}

		LoggedDeviceEntry(int node, String loggedDrf, String event, int listId) throws AcnetStatusException
		{
			this(Node.get(node), LoggerConfigCache.loggerName(node), loggedDrf, event, listId);
		}

		LoggedDeviceEntry(String loggerNodeName, WhatDaq whatDaq) throws AcnetStatusException
		{
			this(LoggerConfigCache.loggerNode(loggerNodeName), whatDaq.loggedName());
		}

		@Override
		public Node node()
		{
			return node;
		}

		@Override
		public String loggerName()
		{
			return loggerName;
		}

		@Override
		public String loggedDrf()
		{
			return loggedDrf;
		}

		@Override
		public String event()
		{
			return event;
		}

		@Override
		public int id()
		{
			return listId;
		}

		@Override
		public boolean clientLogged()
		{
			return event.toLowerCase().startsWith("client");
		}
	}

	private static class LoggerList
	{
		final int node;
		final int listId;

		LoggerList(int node, int listId)
		{
			this.node = node;
			this.listId = listId;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(node, listId);
		}

		@Override
		public boolean equals(Object o)
		{
			if (o == this)
				return true;

			if (o instanceof LoggerList) {
				final LoggerList l = (LoggerList) o;

				return l.node == node &&
						l.listId == listId;
			}

			return false;
		}
	}

	private static final boolean isAllLowerCase(String s)
	{
		for (int ii = 0; ii < s.length(); ii++) {
			if (Character.isUpperCase(s.charAt(ii)))
				return false;
		}

		return true;
	}

	private static LoggerConfigCache instance;

	private LoggerConfigCache()
	{
		instance = this;
		update(Level.CONFIG);
	}

	@Override
	public void run()
	{
		update(Level.FINER);
	}

    private static final void update(Level logLevel)
	{
		final long start = System.currentTimeMillis();
		final HashMap<Device, List<LoggedDeviceEntry>> newLoggerDeviceMap = new HashMap<>();
		final HashMap<String, Integer> newLoggerNameMap = new HashMap<>();
		final HashMap<Integer, String> newLoggerNodeMap = new HashMap<>();

		try {
			final Map<LoggerList, String[]> listEventMap = new HashMap<>();

		   	{
				final String sqlcmd = "SELECT logger_node, listid, logger_rate, data_events_1, data_events_2, data_events_3, data_events_4, data_events_5, data_events_6  FROM wangjy.lj_data_events_table WHERE logger_rate=1";
				final ResultSet rs = getDbServer("adbs").executeQuery(sqlcmd);

				while (rs.next()) {
					final int node = rs.getInt("logger_node");
					final int listId = rs.getInt("listid");
					final String e1 = rs.getString("data_events_1").trim();
					final String e2 = rs.getString("data_events_2").trim();
					final String e3 = rs.getString("data_events_3").trim();
					final String e4 = rs.getString("data_events_4").trim();
					final String e5 = rs.getString("data_events_5").trim();
					final String e6 = rs.getString("data_events_6").trim();

					listEventMap.put(new LoggerList(node, listId), new String[] { e1, e2, e3, e4, e5, e6 }); 
				}
		    }

		    {
				final String sqlcmd = "SELECT S.node, S.historic_ID, I.logger_rate, I.button_name, I.list_data_event, N.dnx_devname, S.node_name, N.dsp_index, I.listid " +
										"FROM wangjy.lj_specs_table S, wangjy.lj_dlg_table I, wangjy.lj_device_name_table N " +
										"WHERE (I.logger_rate = 1) AND (I.listid = N.listid) AND (I.logger_node = N.logger_node) AND (S.node = I.logger_node)";

				final ResultSet rs = getDbServer("adbs").executeQuery(sqlcmd);

				while (rs.next()) {
					final int node = rs.getInt("node");
					final int id = rs.getInt("historic_ID");
					final int rate = rs.getInt("logger_rate");
					final String button = rs.getString("button_name").toUpperCase();
					final String event = rs.getString("list_data_event").trim();
					final String loggedDrf = rs.getString("dnx_devname").trim();
					final String loggerName = rs.getString("node_name").trim();
					final int displayIndex = rs.getInt("dsp_index");
					final int listId = rs.getInt("listid");
					final String loggerNameUC = loggerName.toUpperCase();
					final String badEntryKey = loggerName + ":" + listId + ":" + displayIndex;

					newLoggerNameMap.put(loggerNameUC, node);
					newLoggerNodeMap.put(node, loggerNameUC);

					if (!loggedDrf.isEmpty() && !isAllLowerCase(loggedDrf)) {
						String deviceEvent = null;
						{
							final String[] events = listEventMap.get(new LoggerList(node, listId));

							if (events != null && button.equals("EVENT")) {
								try {
									deviceEvent = events[displayIndex % (events.length - 1)];
								} catch (Exception ignore) {}
							}
						}

						try {
							final AcnetRequest r = new AcnetRequest(new DPMRequest(loggedDrf));
							final Device key = new Device(r.getDevice(), r.getProperty().pi);

							List<LoggedDeviceEntry> loggedNames = newLoggerDeviceMap.get(key);

							if (loggedNames == null) {
								loggedNames = new ArrayList<>();
								newLoggerDeviceMap.put(key, loggedNames);
							}

							loggedNames.add(new LoggedDeviceEntry(node, loggerNameUC, loggedDrf, deviceEvent != null ? deviceEvent : event, listId));
							badEntries.remove(badEntryKey);

							if (logger.isLoggable(Level.FINEST)) {
								logger.log(Level.INFO, String.format("%-10s %-10s %-3d %-20s %-20s %s\n", loggerNameUC, Node.get(node), listId, loggedDrf, event, deviceEvent));
							}
						} catch (Exception e) {
							if (!badEntries.contains(badEntryKey) && !button.equals("Client")) {
								logger.log(logLevel, "Exception with logger entry - logger:'" + loggerName + "' list:" + listId + 
														" index:" + displayIndex + " - " + e.getMessage());
								badEntries.add(badEntryKey);
							}
						}
					}
				}
		    }

			synchronized (instance) {
				loggerNameMap = newLoggerNameMap;
				loggerNodeMap = newLoggerNodeMap;
				loggerDeviceMap = newLoggerDeviceMap;
				logger.log(logLevel, String.format("LoggerDeviceCache.update: read %d entries in %d loggers in %d ms",
							loggerDeviceMap.size(), loggerNameMap.size(), System.currentTimeMillis() - start));
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "LoggerDeviceCache.update: failed", e);
		}
    }

	static final void init()
	{
		final LoggerConfigCache cache = new LoggerConfigCache();
		DPMServer.sharedTimer().scheduleAtFixedRate(cache, UPDATE_RATE, UPDATE_RATE);	
		logger.log(Level.FINE, "LoggerConfigCache init");
	}

	private static final List<LoggedDeviceEntry> search(Device key)
	{
		synchronized (instance) {
			final List<LoggedDeviceEntry> l = loggerDeviceMap.get(key);
			return l == null ? new ArrayList<LoggedDeviceEntry>() : l;
		}
	}

	static final List<LoggedDeviceEntry> search(WhatDaq whatDaq)
	{
		return search(new Device(whatDaq.name(), whatDaq.pi()));		
	}

	public static final void refresh()
	{
		update(Level.CONFIG);
	}

	static final Node loggerNode(String s) throws AcnetStatusException
	{
		final Integer nodeValue = loggerNameMap.get(s.toUpperCase());
		final Node node = (nodeValue == null ? Node.get(s) : Node.get(nodeValue));

		if (node == null)
			throw new AcnetStatusException(ACNET_NO_NODE);

		return node;
	}

	static final String loggerName(Node node) throws AcnetStatusException
	{
		final String name = loggerNodeMap.get(node.value());

		if (name == null)
			throw new AcnetStatusException(ACNET_NO_NODE);

		return name;
	}

	static final String loggerName(int nodeValue) throws AcnetStatusException
	{
		final String name = loggerNodeMap.get(nodeValue);

		if (name == null)
			throw new AcnetStatusException(ACNET_NO_NODE);

		return name;
	}

	static private final Event toEvent(String eventString)
	{
		try {
			return Event.parse(eventString);
		} catch (Exception e) {
			return new NeverEvent();
		}
	}

	public static final List<LoggedDevice> bestLoggers(WhatDaq whatDaq, String loggerName) throws AcnetStatusException
	{
		if (loggerName != null) {
			final Node node = loggerNode(loggerName);
			final List<LoggedDeviceEntry> list = search(whatDaq);	
			final ArrayList<LoggedDevice> exactMatch = new ArrayList<>();
			final int listSize = list.size();

			for (final LoggedDeviceEntry d : list) {
				final Event logEv = toEvent(d.event());

				if (node.value() == d.node.value()) {
					if (logEv.equals(loggerEventKludge(whatDaq.event())))
						exactMatch.add(d);	
				}
			}

			if (exactMatch.isEmpty())
				throw new AcnetStatusException(LJ_NO_SUCH);

			return exactMatch;
		}

		final List<LoggedDevice> list = bestLoggers(whatDaq);

		if (list.isEmpty())
			throw new AcnetStatusException(ACNET_NO_NODE);

		return list;
	}

	private static final List<LoggedDevice> bestLoggers(WhatDaq whatDaq) throws AcnetStatusException
	{
		final List<LoggedDeviceEntry> list = search(whatDaq);	
		final ArrayList<LoggedDeviceEntry> fastest = new ArrayList<>();
		final ArrayList<LoggedDeviceEntry> exactMatch = new ArrayList<>();
		final ArrayList<LoggedDeviceEntry> clientMatch = new ArrayList<>();

		PeriodicEvent fastestEvent = null;

		for (final LoggedDeviceEntry d : list) {
			try {
				final WhatDaq loggerWhatDaq = WhatDaq.create(d.loggedDrf); 

				if (loggerWhatDaq.contains(whatDaq)) {
					final Event logEv = Event.parse(d.event());

					if (logEv instanceof PeriodicEvent) {
						final PeriodicEvent e = (PeriodicEvent) logEv;

						if (fastestEvent == null || e.getPeriod() < fastestEvent.getPeriod()) {
							fastestEvent = e;
							fastest.add(0, d);
						} else
							fastest.add(d);
					}

					if (d.clientLogged())
						clientMatch.add(d);
					else if (logEv.equals(loggerEventKludge(whatDaq.event())))
						exactMatch.add(d);
				}
			} catch (Exception e) {
				logger.log(Level.FINE, "Exception with logger entry - " + d.loggerName + " list:" + 
											d.listId + " drf: " + d.loggedDrf + " - " + e.getMessage());
			}
		}
		
		if (whatDaq.defaultEvent()) {
			List<LoggedDeviceEntry> bestList;

			if (exactMatch.isEmpty()) {
				if (fastest.isEmpty())
					bestList = list;
				else
					bestList = fastest;
			} else {
				for (LoggedDeviceEntry e : fastest) {
					if (!exactMatch.contains(e))
						exactMatch.add(e);
				}
				bestList = exactMatch;
			}

			// When event is not specified, add special loggers 
			// if we've failed to find any in the previous search.

			if (bestList.isEmpty()) {
				if (whatDaq.isStateDevice()) {
					addSpecialLogger(bestList, "State", whatDaq);
					addSpecialLogger(bestList, "EventV", whatDaq);
				}

				if (whatDaq.isSettableProperty())
					addSpecialLogger(bestList, "Sets", whatDaq);

				addSpecialLogger(bestList, "Backup", whatDaq);
				addSpecialLogger(bestList, "ArkIv", whatDaq);

				bestList.addAll(clientMatch);
			}

			return new ArrayList<LoggedDevice>(bestList);
		}

		return new ArrayList<LoggedDevice>(exactMatch);
	}

	private static final void addSpecialLogger(List<LoggedDeviceEntry> list, String name, WhatDaq whatDaq)
	{
		try {
			list.add(new LoggedDeviceEntry(name, whatDaq));
		} catch (Exception e) {
			logger.log(Level.WARNING, "'" + name + "' is not valid special logger name");
		}
	}

	// Kludge methods to force 15Hz to 66ms for logger retrieval

	static Event loggerEventKludge(Event event)
	{
		try {
			return Event.parse(loggerEventKludge(event.toString()));
		} catch (Exception ignore) { }

		return event;
	}

	static String loggerEventKludge(String eventString)
	{
		logger.log(Level.FINEST, () -> "CHECK EVENT '" + eventString + "'");

		if (eventString.trim().startsWith("p,67"))
			return eventString.replace('7', '6');

		return eventString;
	}

	static List<String> drfListForLogger(String loggerName)
	{
		final ArrayList<String> drfList = new ArrayList<>();

		for (List<LoggedDeviceEntry> list : loggerDeviceMap.values()) {
			for (LoggedDeviceEntry entry : list) {
				if (loggerName.equals(entry.loggerName))
					drfList.add(entry.loggedDrf + "@" + entry.event);
			}
		}

		return drfList;
	}

	static Set<String> loggerNames()
	{
		return loggerNameMap.keySet();
	}

	public static void main(String[] args) throws Exception
	{
		Node.init();
		DeviceCache.init();
		DPMServer.setLogLevel(Level.FINEST);

		final LoggerConfigCache cache = new LoggerConfigCache();

		// Show all logged types for arg

		if (args.length > 0) {
			DeviceCache.add(args[0]);
			final WhatDaq whatDaq = WhatDaq.create(args[0]);

			for (LoggedDevice d : search(whatDaq))
				logger.log(Level.INFO, String.format("%-20s %-10s %-10s %4d %s\n", d.loggedDrf(), d.loggerName(), d.node(), d.id(), d.event()));

			logger.log(Level.INFO, "Best loggers for " + args[0]);

			for (LoggedDevice d : bestLoggers(whatDaq))
				logger.log(Level.INFO, String.format("\tlogger:%s list:%d event:%s\n", d.loggerName(), d.id(), d.event()));
		}

		System.exit(0);
	}
}

class DrfListForLogger
{
	static void drfList(String loggerName)
	{
		final List<String> drfList = LoggerConfigCache.drfListForLogger(loggerName.toUpperCase());

		for (String drf : drfList) {
			logger.log(Level.INFO, drf);
		}
	}

	public static void main(String[] args)
	{
		Node.init();
		DeviceCache.init();
		LoggerConfigCache.init();
		DPMServer.setLogLevel(Level.FINEST);

		if (args.length > 0)
			drfList(args[0]);
		else {
			for (String loggerName : LoggerConfigCache.loggerNames()) {
				logger.log(Level.INFO, "[" + loggerName + "]");
				drfList(loggerName);
			}
		}

		System.exit(0);
	}
}
