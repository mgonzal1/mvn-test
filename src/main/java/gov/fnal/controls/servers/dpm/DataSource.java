// $Id: DataSource.java,v 1.2 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm;
 
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.io.IOException;
import org.ietf.jgss.GSSName;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;


public abstract class DataSource implements AcnetErrors, Comparable<DataSource>
{
	public enum DataType { Raw, Primary, Scaled }

	protected DataSource() { }

	abstract public Job createJob(DPMList list) throws AcnetStatusException;

	public DataType dataType(WhatDaq whatDaq)
	{
		return DataType.Raw;
	}

	public boolean restartable()
	{
		return true;
	}

	public int compareTo(DataSource dataSource)
	{
		return toString().compareTo(dataSource.toString());
	}

	static DataSource parse(String dataSource)
	{
		try {
			if (dataSource == null || dataSource.isEmpty())
				return LiveDataSource.instance;
			else if (dataSource.equals("LIVEDATA"))
				return LiveDataSource.instance;
			else if (dataSource.equals("FTP"))
				return LiveDataSource.instance;
			else {
				String[] s = dataSource.split(":");
				String name = s[0].trim().toUpperCase();

				if (name.equals("SRFILE"))
					return new SavefileDataSource(Integer.parseInt(s[1]));
				else if (name.equals("LOGGERSINGLE"))
					return new LoggerSingleDataSource(s[1], Long.parseLong(s[2]) * 1000, Integer.parseInt(s[3]) * 1000);
				else if (name.equals("LOGGER")) {
					return new LoggerDataSource(Long.parseLong(s[1]), Long.parseLong(s[2]), s.length > 3 ? s[3] : null); 
				} else if (name.equals("LOGGERDURATION")) {
					return new LoggerDurationDataSource(Long.parseLong(s[1]), s.length > 2 ? s[2] : null); 
				} else if (name.equals("SDAFILE"))
					return new SDADataSource(Integer.parseInt(s[1]), Integer.parseInt(s[2]), 
											Integer.parseInt(s[3]), Integer.parseInt(s[4]));
				else if (name.equals("REDIR")) {
					final String[] nodes = s[1].split("->");

					return new RedirectDataSource(nodes[0].trim(), nodes[1].trim());
				}

				if (DPMServer.debug()) {
					if (name.equals("SPEEDTEST")) {
						return new SpeedTestDataSource(Integer.parseInt(s[1]), Integer.parseInt(s[2]));
					} else if (name.equals("MIRROR")) {
						return new RedirectDataSource("*", "MIRROR");
					}
				}
			}
		} catch (AcnetStatusException e) {
			logger.log(Level.FINE, "exception parsing data source", e);
			return new AcnetStatusDataSource(e.status);
		} catch (Exception e) {
			logger.log(Level.FINE, "exception parsing data source", e);
			return new AcnetStatusDataSource(DPM_BAD_DATASOURCE_FORMAT);
		}

		return new AcnetStatusDataSource(DPM_INVALID_DATASOURCE);
	}
}

class DefaultDataSource extends DataSource
{
	static final DefaultDataSource instance = new DefaultDataSource();

	private DefaultDataSource() { }

	@Override
	public Job createJob(DPMList list)
	{
		throw new RuntimeException("Called createJob() with DefaultDataSource");
	}

	@Override
	public boolean equals(Object o)
	{
		return o == instance;
	}

	@Override
	public int hashCode()
	{
		return getClass().hashCode();
	}

	@Override
	public String toString()
	{
		return "Default";
	}
}

class AcnetStatusDataSource extends DataSource
{
	private final int status;

	AcnetStatusDataSource(int status)
	{
		this.status = status;
	}

	@Override
	public Job createJob(DPMList list)
	{
		return new AcnetStatusJob(list, status);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof AcnetStatusDataSource)
			return ((AcnetStatusDataSource) o).status == status;

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(status);
	}

	@Override
	public String toString()
	{
		return "Acnet Status " + status;
	}
}

class RedirectDataSource extends DataSource
{
	private final Node srcNode;
	private final Node dstNode;

	RedirectDataSource(String src, String dest) throws AcnetStatusException
	{
		Node tmp = null;

		try {
			tmp = parseNode(src);
		} catch (Exception ignore) { }

		this.srcNode = tmp;
		this.dstNode = parseNode(dest);
	}

	private Node parseNode(String name) throws AcnetStatusException
	{
		if (name.startsWith("#"))
			return Node.get(Integer.parseInt(name.substring(1)));
		else if (name.equals("*"))
			return null;
		
		return Node.get(name);
	}

	@Override
	public Job createJob(DPMList list)
	{
		return new RedirectAcceleratorJob(list, srcNode, dstNode);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof RedirectDataSource) {
			RedirectDataSource m = (RedirectDataSource) o;
			
			return m.srcNode.equals(srcNode) && 
					m.dstNode.equals(dstNode);
		}
		
		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(srcNode, dstNode);
	}

	@Override
	public String toString()
	{
		return "Redirect " + srcNode + "->" + dstNode;
	}
}

class SavefileDataSource extends DataSource
{
	private final int fileAlias;

	SavefileDataSource(int fileAlias)
	{
		this.fileAlias = fileAlias;
	}

	@Override
	public Job createJob(DPMList list) throws AcnetStatusException
	{
		return new SavefileJob(list, fileAlias);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof SavefileDataSource)
			return ((SavefileDataSource) o).fileAlias == fileAlias;

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(fileAlias);
	}

	@Override
	public String toString()
	{
		return "Savefile " + fileAlias;
	}
}

class LoggerSingleDataSource extends DataSource
{
	private final String loggerNode;
	private final long timestamp;
	private final int accuracy;

	LoggerSingleDataSource(String loggerNode, long timestamp, int accuracy)
	{
		this.loggerNode = loggerNode;
		this.timestamp = timestamp;
		this.accuracy = accuracy;
	}

	@Override
	public Job createJob(DPMList list)
	{
		return new LoggerSingleJob(list, loggerNode, timestamp, accuracy);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof LoggerSingleDataSource) {
			LoggerSingleDataSource m = (LoggerSingleDataSource) o;

			return m.loggerNode == loggerNode &&
					m.timestamp == timestamp &&
					m.accuracy == accuracy;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(loggerNode, timestamp, accuracy);
	}

	@Override
	public String toString()
	{
		return String.format("Logger Single %tc +/- %d seconds", timestamp, accuracy);
	}
}

class LoggerDataSource extends DataSource
{
	private final long t1, t2;
	//private final AcnetNodeInfo nodeInfo;
	private final String logger;

	LoggerDataSource(long t1, long t2, String logger)
	{
		this.t1 = t1;
		this.t2 = t2;
		//this.nodeInfo = nodeInfo;
		this.logger = logger;
	}

	@Override
	public DataType dataType(WhatDaq whatDaq)
	{
		return DataType.Scaled;
	}

	@Override
	public boolean restartable()
	{
		return false;
	}

	@Override
	public Job createJob(DPMList list)
	{
		return new LoggerJob(list, t1, t2, logger);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof LoggerDataSource) {
			LoggerDataSource m = (LoggerDataSource) o;

			return m.t1 == t1 &
					m.t2 == t2;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(t1, t2);
	}

	@Override
	public String toString()
	{
		return String.format("Logger %tc -> %tc", t1, t2);
	}
}

class LoggerDurationDataSource extends DataSource
{
	private final long duration;
	private final String logger;

	LoggerDurationDataSource(long duration, String logger)
	{
		this.duration = duration;
		this.logger = logger;
	}

	@Override
	public Job createJob(DPMList list)
	{
		long now = System.currentTimeMillis();

		return new LoggerJob(list, now - duration, now, logger);
	}

	@Override
	public DataType dataType(WhatDaq whatDaq)
	{
		return DataType.Scaled;
	}

	@Override
	public boolean restartable()
	{
		return false;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof LoggerDurationDataSource) {
			LoggerDurationDataSource m = (LoggerDurationDataSource) o;

			return m.duration == duration;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(duration);
	}

	@Override
	public String toString()
	{
		return "Logger Duration " + duration + "ms";
	}
}

class SettingDataSource extends DataSource
{
	final List<SettingData> settings;
	final ConsoleUser user;

	SettingDataSource(List<SettingData> settings, ConsoleUser user)
	{
		this.settings = settings;
		this.user = user;
	}

	@Override
	public boolean restartable()
	{
		return false;
	}

	@Override
	public synchronized Job createJob(DPMList list)
	{
		return new SettingJob(settings, list, user);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof SettingDataSource;
	}

	@Override
	public int hashCode()
	{
		return getClass().hashCode();
	}

	@Override
	public String toString()
	{
		return "Setting";
	}
}

class SDADataSource extends DataSource
{
	private final int usage;
	private final int fileAlias;
	private final int sdaCase;
	private final int sdaSubcase;

	SDADataSource(int usage, int fileAlias, int sdaCase, int sdaSubcase)
	{
		this.usage = usage;
		this.fileAlias = fileAlias;
		this.sdaCase = sdaCase;
		this.sdaSubcase = sdaSubcase;
	}

	@Override
	public Job createJob(DPMList list) throws AcnetStatusException
	{
		return new SavefileJob(list, usage, fileAlias, sdaCase, sdaSubcase);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof SDADataSource) {
			SDADataSource m = (SDADataSource) o;

			return m.usage == usage &&
					m.fileAlias == fileAlias &&
					m.sdaCase == sdaCase &&
					m.sdaSubcase == sdaSubcase;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(usage, fileAlias, sdaCase, sdaSubcase);
	}

	@Override
	public String toString()
	{
		return "SDA " + fileAlias + " " + sdaCase + " " + sdaSubcase;
	}
}

class SpeedTestDataSource extends DataSource
{
	final int replyCount;
	final int arraySize;

	SpeedTestDataSource(int replyCount, int arraySize)
	{
		this.replyCount = replyCount;
		this.arraySize = arraySize;
	}

	@Override
	public Job createJob(DPMList list)
	{
		return new SpeedTestJob(list, replyCount, arraySize);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof SpeedTestDataSource) {
			SpeedTestDataSource m = (SpeedTestDataSource) o;

			return m.replyCount == replyCount &&
					m.arraySize == arraySize;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(replyCount, arraySize);
	}

	@Override
	public String toString()
	{
		return "SpeedTestDataSource with " + replyCount + " replies and " + arraySize + " array elements";
	}
}
