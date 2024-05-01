// $Id: Model.java,v 1.34 2024/03/05 17:49:05 kingc Exp $
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

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

abstract class Model implements AcnetErrors, Comparable<Model>
{
	boolean restartable()
	{
		return true;
	}

	public static void main(String[] args) throws Exception
	{
		HashSet<Model> s = new HashSet<>();

		s.add(LiveDataModel.instance);
		s.add(new SavefileModel(300));
		s.add(new RedirectModel("*", "MIRROR"));

		logger.info("contains: " + s.contains(LiveDataModel.instance));
		logger.info("contains: " + s.contains(new SavefileModel(300)));
		logger.info("contains: " + s.contains(new RedirectModel("XMLRPC", "DCE46")));
		logger.info("contains: " + s.contains(new RedirectModel("XMLRPC", "DCE46")));
		logger.info("contains: " + s.contains(new SavefileModel(301)));
	}

	@Override
	public int compareTo(Model model)
	{
		return toString().compareTo(model.toString());
	}
}

abstract class JobModel extends Model 
{
	abstract Job createJob(DPMList list) throws AcnetStatusException;

	static JobModel parse(String model)
	{
		try {
			if (model == null || model.isEmpty())
				return LiveDataModel.instance;
			else if (model.equals("LIVEDATA"))
				return LiveDataModel.instance;
			else if (model.equals("FTP"))
				return LiveDataModel.instance;
			else {
				String[] s = model.split(":");
				String name = s[0].trim().toUpperCase();

				if (name.equals("SRFILE"))
					return new SavefileModel(Integer.parseInt(s[1]));
				else if (name.equals("LOGGERSINGLE"))
					//return new LoggerSingleModel(Integer.parseInt(s[1]), Long.parseLong(s[2]) * 1000,
					//								Integer.parseInt(s[3]) * 1000);
					return new LoggerSingleModel(s[1], Long.parseLong(s[2]) * 1000, Integer.parseInt(s[3]) * 1000);
				else if (name.equals("LOGGER")) {
					//final AcnetNodeInfo nodeInfo = (s.length > 3 ? LoggerConfigCache.loggerNode(s[3]) : null);

					//return new LoggerModel(Long.parseLong(s[1]), Long.parseLong(s[2]), nodeInfo); 
					return new LoggerModel(Long.parseLong(s[1]), Long.parseLong(s[2]), s.length > 3 ? s[3] : null); 
				} else if (name.equals("LOGGERDURATION")) {
					//final AcnetNodeInfo nodeInfo = (s.length > 2 ? LoggerConfigCache.loggerNode(s[2]) : null);

					//return new LoggerDurationModel(Long.parseLong(s[1]), nodeInfo); 
					return new LoggerDurationModel(Long.parseLong(s[1]), s.length > 2 ? s[2] : null); 
				} else if (name.equals("SDAFILE"))
					return new SDAModel(Integer.parseInt(s[1]), Integer.parseInt(s[2]), 
											Integer.parseInt(s[3]), Integer.parseInt(s[4]));
				else if (name.equals("REDIR")) {
					final String[] nodes = s[1].split("->");

					return new RedirectModel(nodes[0].trim(), nodes[1].trim());
				} else if (DPMServer.debug() && name.equals("SPEEDTEST")) {
					return new SpeedTestModel(Integer.parseInt(s[1]), Integer.parseInt(s[2]));
				}
			}
		} catch (AcnetStatusException e) {
			logger.log(Level.FINE, "exception parsing model", e);
			return new AcnetStatusModel(e.status);
		} catch (Exception e) {
			logger.log(Level.FINE, "exception parsing model", e);
			return new AcnetStatusModel(DPM_BAD_DATASOURCE_FORMAT);
		}

		return new AcnetStatusModel(DPM_INVALID_DATASOURCE);
	}
}

class AcnetStatusModel extends JobModel
{
	private final int status;

	AcnetStatusModel(int status)
	{
		this.status = status;
	}

	@Override
	Job createJob(DPMList list)
	{
		return new AcnetStatusJob(list, status);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof AcnetStatusModel)
			return ((AcnetStatusModel) o).status == status;

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

class DefaultModel extends Model
{
	public static final DefaultModel instance = new DefaultModel();

	private DefaultModel() { }

	public boolean equalsModel(Model model)
	{
		return model.equals(model);
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
		return "DefaultModel";
	}
}

class LiveDataModel extends JobModel
{
	public static final LiveDataModel instance = new LiveDataModel();

	private LiveDataModel() { }

	@Override
	Job createJob(DPMList list)
	{
		return new AcceleratorJob(list);
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
		return "LiveData";
	}
}

class RedirectModel extends JobModel
{
	private final Node srcNode;
	private final Node dstNode;

	RedirectModel(String src, String dest) throws AcnetStatusException
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
	Job createJob(DPMList list)
	{
		return new RedirectAcceleratorJob(list, srcNode, dstNode);
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof RedirectModel) {
			RedirectModel m = (RedirectModel) o;
			
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

class SavefileModel extends JobModel
{
	private final int fileAlias;

	SavefileModel(int fileAlias)
	{
		this.fileAlias = fileAlias;
	}

	@Override
	Job createJob(DPMList list) throws AcnetStatusException
	{
		return new SavefileJob(list, fileAlias);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof SavefileModel)
			return ((SavefileModel) o).fileAlias == fileAlias;

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

class LoggerSingleModel extends JobModel
{
	private final String loggerNode;
	private final long timestamp;
	private final int accuracy;

	LoggerSingleModel(String loggerNode, long timestamp, int accuracy)
	{
		this.loggerNode = loggerNode;
		this.timestamp = timestamp;
		this.accuracy = accuracy;
	}

	@Override
	Job createJob(DPMList list)
	{
		return new LoggerSingleJob(list, loggerNode, timestamp, accuracy);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof LoggerSingleModel) {
			LoggerSingleModel m = (LoggerSingleModel) o;

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

class LoggerModel extends JobModel
{
	private final long t1, t2;
	//private final AcnetNodeInfo nodeInfo;
	private final String logger;

	LoggerModel(long t1, long t2, String logger)
	{
		this.t1 = t1;
		this.t2 = t2;
		//this.nodeInfo = nodeInfo;
		this.logger = logger;
	}

	@Override
	boolean restartable()
	{
		return false;
	}

	@Override
	Job createJob(DPMList list)
	{
		return new LoggerJob(list, t1, t2, logger);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof LoggerModel) {
			LoggerModel m = (LoggerModel) o;

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

class LoggerDurationModel extends JobModel
{
	private final long duration;
	private final String logger;

	LoggerDurationModel(long duration, String logger)
	{
		this.duration = duration;
		this.logger = logger;
	}

	@Override
	Job createJob(DPMList list)
	{
		long now = System.currentTimeMillis();

		return new LoggerJob(list, now - duration, now, logger);
	}

	@Override
	boolean restartable()
	{
		return false;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof LoggerDurationModel) {
			LoggerDurationModel m = (LoggerDurationModel) o;

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

class SettingModel extends JobModel
{
	final List<SettingData> settings;
	final ConsoleUser user;

	SettingModel(List<SettingData> settings, ConsoleUser user)
	{
		this.settings = settings;
		this.user = user;
	}

	@Override
	boolean restartable()
	{
		return false;
	}

	@Override
	synchronized Job createJob(DPMList list)
	{
		return new SettingJob(settings, list, user);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof SettingModel;
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

class SDAModel extends JobModel
{
	private final int usage;
	private final int fileAlias;
	private final int sdaCase;
	private final int sdaSubcase;

	SDAModel(int usage, int fileAlias, int sdaCase, int sdaSubcase)
	{
		this.usage = usage;
		this.fileAlias = fileAlias;
		this.sdaCase = sdaCase;
		this.sdaSubcase = sdaSubcase;
	}

	@Override
	Job createJob(DPMList list) throws AcnetStatusException
	{
		return new SavefileJob(list, usage, fileAlias, sdaCase, sdaSubcase);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof SDAModel) {
			SDAModel m = (SDAModel) o;

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

class SpeedTestModel extends JobModel
{
	final int replyCount;
	final int arraySize;

	SpeedTestModel(int replyCount, int arraySize)
	{
		this.replyCount = replyCount;
		this.arraySize = arraySize;
	}

	@Override
	Job createJob(DPMList list)
	{
		return new SpeedTestJob(list, replyCount, arraySize);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof SpeedTestModel) {
			SpeedTestModel m = (SpeedTestModel) o;

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
		return "SpeedTestModel with " + replyCount + " replies and " + arraySize + " array elements";
	}
}
