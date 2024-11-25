// $Id: DPMInfo.java,v 1.3 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.scope;

import java.util.logging.Level;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.awt.*;

import gov.fnal.controls.servers.dpm.acnetlib.*;
import gov.fnal.controls.service.proto.DPMScope;

class DPMInfo implements Comparable<DPMInfo>, AcnetReplyHandler, AcnetErrors
{
	static final DecimalFormat formatter = new DecimalFormat("#,###");
	static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yy HH:mm:ss");

	static final String[] columnNames = {
		"", 
		"Node", 
		"Version", 
		"Host", 
		"Started", 
		"Up Time", 
		"PID", 
		"%Sys Load", 
		"%Heap Free", 
		"Active Lists", 
		"Active Requests", 
		"Reply/Sec", 
		"Consolidation Hits", 
		"Total Lists", 
		"Total Requests", 
		"Max Reply/Sec", 
		"Log Level"
	};

	static final Class[] classTypes = { 
		String.class,	// Status
		String.class,  	// Node
		String.class, 	// Version 
		String.class,	// Host name 
		String.class, 	// Started date
		String.class, 	// Up time
		Integer.class, 	// PID
		Integer.class,	// Load % 
		Integer.class, 	// Heap free %
		Integer.class,	// Active lists
		Integer.class,	// Active requests
		String.class,	// Replies/second 
		String.class,	// Consolidation hits 
		Integer.class,	// Total lists
		String.class,	// Total requests 
		String.class,	// Max replies/second
		String.class	// Log level
	};

	static final int StatusColumn = 0;
	static final int NodeNameColumn = 1;
	static final int CodeVersionColumn = 2;
	static final int HostNameColumn = 3;
	static final int StartedColumn = 4;
	static final int UpTimeColumn = 5;
	static final int PidColumn = 6;
	static final int LoadPctColumn = 7;
	static final int HeapFreePctColumn = 8;
	static final int ActiveListCountColumn = 9;
	static final int ActiveRequestCountColumn = 10;
	static final int RepliesPerSecondColumn = 11;
	static final int ConsolidationHitsColumn = 12;
	static final int TotalListCountColumn = 13;
	static final int TotalRequestCountColumn = 14;
	static final int MaxRepliesPerSecondColumn = 15;
	static final int LogLevelColumn = 16;

	static enum CellColor { 
		Normal(Color.BLACK, Color.WHITE),
		NormalAlt(Color.BLACK, new Color(0.90f, 0.90f, 1.f)),
		Restarted1(Color.BLACK, Color.GREEN),
		Restarted24(Color.BLACK, Color.YELLOW),
		Interesting(Color.BLACK, Color.LIGHT_GRAY),
		Warning(Color.WHITE, Color.RED),
		ActiveRefresh(Color.MAGENTA, Color.MAGENTA),
		RestartScheduled(Color.RED, Color.RED),
		//TotalRow(Color.BLACK, new Color(0.80f, 0.80f, 0.9f));
		TotalRow(Color.BLACK, Color.LIGHT_GRAY);

		final Color foreground, background;
		
		CellColor(Color foreground, Color background)
		{
			this.foreground = foreground;
			this.background = background;
		}
	}

   	final static Font bold = new Font("Dialog", Font.BOLD, 12);
   	final static Font plain = new Font("Dialog", Font.PLAIN, 12);


	boolean down;
	long replyTime;
	String nodeName;
	String codeVersion;
	String hostName;
	int scopePort;
	long started;
	int loadPct;
	int pid;
	int heapFreePct;
	int activeListCount;
	int totalListCount;
	int requestCount;
	long totalRequestCount;
	int consolidationHits;
	int repliesPerSecond;
	int repliesPerSecondMax;
	String logLevel;
	boolean activeRefresh;
	boolean restartScheduled;
	boolean inDebugMode;
	boolean exceptionStack;
	boolean selected;

	DPMInfo()
	{
	}

	DPMInfo(DPMScope.Reply.ServiceDiscovery m)
	{
		//update(m);
		this.down = false;
		this.replyTime = System.currentTimeMillis();
		this.nodeName = m.nodeName;
		this.codeVersion = m.codeVersion;
		this.hostName = m.hostName;
		this.scopePort = m.scopePort;
		this.started = m.date; 
		this.pid = m.pid;
		this.loadPct = m.loadPct;
		this.heapFreePct = m.heapFreePct;
		this.activeListCount = m.activeListCount;
		this.totalListCount = m.totalListCount;
		this.consolidationHits = m.consolidationHits;
		this.requestCount = m.requestCount;
		this.totalRequestCount = m.totalRequestCount;
		this.repliesPerSecond = m.repliesPerSecond;
		this.repliesPerSecondMax = m.repliesPerSecondMax;
		this.logLevel = m.logLevel;
		this.activeRefresh = m.activeRefresh;
		this.inDebugMode = m.inDebugMode;
		this.exceptionStack = m.exceptionStack;

		try {
			this.restartScheduled = m.restartScheduled;
		} catch (Exception e) {
			this.restartScheduled = false;
		}
		this.selected = false;
	}

	void update(DPMInfo dpmInfo)
	{
		down = dpmInfo.down;
		replyTime = dpmInfo.replyTime;
		nodeName = dpmInfo.nodeName;
		codeVersion = dpmInfo.codeVersion;
		hostName = dpmInfo.hostName;
		scopePort = dpmInfo.scopePort;
		started = dpmInfo.started; 
		pid = dpmInfo.pid;
		loadPct = dpmInfo.loadPct;
		heapFreePct = dpmInfo.heapFreePct;
		activeListCount = dpmInfo.activeListCount;
		totalListCount = dpmInfo.totalListCount;
		requestCount = dpmInfo.requestCount;
		totalRequestCount = dpmInfo.totalRequestCount;
		consolidationHits = dpmInfo.consolidationHits;
		repliesPerSecond = dpmInfo.repliesPerSecond;
		repliesPerSecondMax = dpmInfo.repliesPerSecondMax;
		logLevel = dpmInfo.logLevel;
		activeRefresh = dpmInfo.activeRefresh;
		inDebugMode = dpmInfo.inDebugMode;
		restartScheduled = dpmInfo.restartScheduled;
		exceptionStack = dpmInfo.exceptionStack;
	}

	CellColor color(int row, int col)
	{
			switch (col) {
			 case StatusColumn:
			 	{
					if (restartScheduled)
						return CellColor.RestartScheduled;
					else if (activeRefresh)
						return CellColor.ActiveRefresh;
					else if (exceptionStack)
						return CellColor.Warning;
				}
			 	break;

			 case NodeNameColumn:
			 case CodeVersionColumn:
			 case UpTimeColumn:
				{
					final long upTime = upTime();

					if (upTime < 0)
						return CellColor.Warning;
					else if (upTime <= (1 * 60 * 60 * 1000))
						return CellColor.Restarted1;
					else if (upTime <= (24 * 60 * 60 * 1000))
						return CellColor.Restarted24;
					else if (upTime <= (72 * 60 * 60 * 1000))
						return CellColor.Interesting;
				}
				break;	

			 case LoadPctColumn:
				if (loadPct >= 50)
					return CellColor.Warning;
			 	else if (loadPct >= 10)
					return CellColor.Interesting;
			 	break;

			 case HeapFreePctColumn:
			 	if (heapFreePct >= 0) {
					if (heapFreePct <= 5)
						return CellColor.Warning;
					else if (heapFreePct <= 10)
						return CellColor.Interesting;
				}
			 	break;

			 case ActiveListCountColumn:
				if (activeListCount >= 200)
					return CellColor.Warning;
			 	else if (activeListCount >= 100)
					return CellColor.Interesting;
			 	break;

			 case ActiveRequestCountColumn:
			 	break;

			 case RepliesPerSecondColumn:
			 	if (repliesPerSecond >= 20000)
					return CellColor.Warning;
				else if (repliesPerSecond >= 5000)
					return CellColor.Interesting;
			 	break;

			 case MaxRepliesPerSecondColumn:
			 	if (repliesPerSecondMax >= 20000)
					return CellColor.Warning;
				else if (repliesPerSecondMax >= 5000)
					return CellColor.Interesting;
			 	break;
			 
			 case LogLevelColumn:
				try {
					if (Level.parse(logLevel).intValue() < Level.INFO.intValue())
						return CellColor.Interesting;
				} catch (Exception ignore) {
					return CellColor.Warning;
				}
				break;
			}

			return (row & 1) > 0 ? CellColor.Normal : CellColor.NormalAlt;
	}

	static String duration(long duration)
	{
		if (duration < 1000)
			return duration + " ms";
		else if (duration < (60 * 1000))
			return (duration / 1000) + " s";
		else if (duration < (60 * 60 * 1000))
			return String.format("%.1f m", (duration / (60.0 * 1000.0)));
		else if (duration < (24 * 60 * 60 * 1000))
			return String.format("%.1f h", (duration / (60.0 * 60.0 * 1000.0)));
		else
			return String.format("%.1f d", (duration / (24.0 * 60.0 * 60.0 * 1000.0)));
	}

	Object value(int col)
	{
		try {
			switch (col) {
				case StatusColumn:
					return exceptionStack ? "E" : (inDebugMode ? "D" : " ");
				case NodeNameColumn:
					return nodeName;
				case CodeVersionColumn:
					return codeVersion;
				case HostNameColumn:
					return hostName + ":" + scopePort;
				case StartedColumn:
					return dateFormat.format(started); 
				case UpTimeColumn:
					{
						if (down)
							return "DOWN";

						return duration(System.currentTimeMillis() - started);
					}
				case PidColumn:
					return pid;
				case LoadPctColumn:
					return loadPct;
				case HeapFreePctColumn:
					return heapFreePct < 0 ? "--" : heapFreePct;
				case ActiveListCountColumn:
					return formatter.format(activeListCount);
				case TotalListCountColumn:
					return formatter.format(totalListCount);
				case ActiveRequestCountColumn:
					return formatter.format(requestCount);
				case TotalRequestCountColumn:
					return formatter.format(totalRequestCount);
				case ConsolidationHitsColumn:
					return formatter.format(consolidationHits);
				case RepliesPerSecondColumn:
					return formatter.format(repliesPerSecond);
				case MaxRepliesPerSecondColumn:
					return formatter.format(repliesPerSecondMax);
				case LogLevelColumn:
					return logLevel;
			}
		} catch (Exception ignore) {
			ignore.printStackTrace();
		}

		return "-";
	}

	Component component(int row, int col, Component component)
	{
		//final CellColor color = CellColor.get(dpmInfoModel.get(row), row, col);

		final CellColor color = color(row, col);

		component.setForeground(color.foreground);
		component.setBackground(color.background);

		switch (col) {
			case StatusColumn:
			case ActiveListCountColumn:
			case ActiveRequestCountColumn:
			case RepliesPerSecondColumn:
			case ConsolidationHitsColumn:
				component.setFont(bold);
				break;

			default:
				component.setFont(plain);
				break;
		}

		return component;
	}

	long upTime()
	{
		return down ? -1 : System.currentTimeMillis() - started;
	}

	void sendRefreshMessage(AcnetConnection c, String refreshName)
	{
		try {
			final DPMScope.Request.Refresh m = new DPMScope.Request.Refresh();
			final ByteBuffer buf = ByteBuffer.allocate(64 * 1024);

			m.name = refreshName;

			buf.clear();
			m.marshal(buf).flip();
			final AcnetRequestContext context = c.requestSingle(nodeName, "SCOPE", buf, 5000, this); 
		} catch (AcnetStatusException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handle(AcnetReply r)
	{
	}

	@Override
	public int compareTo(DPMInfo obj)
	{
		if (obj instanceof DPMInfoTotal)
			return -1;

		return nodeName.compareTo(obj.nodeName);
	}
}

class DPMInfoTotal extends DPMInfo
{
	long activeListCount;
	long totalListCount;
	long requestCount;
	long totalRequestCount;
	long totalConsolidationHits;
	long repliesPerSecond;
	long repliesPerSecondMax;
	
	DPMInfoTotal()
	{
		this.nodeName = null;
	}

	@Override
	CellColor color(int row, int col)
	{
		return CellColor.TotalRow;
	}

	@Override
	Component component(int row, int col, Component component)
	{
		final CellColor color = color(row, col);

		component.setForeground(color.foreground);
		component.setBackground(color.background);
		//component.setFont(selected ? bold : plain);
		switch (col) {
			case ActiveListCountColumn:
			case ActiveRequestCountColumn:
			case RepliesPerSecondColumn:
				component.setFont(bold);
				break;

			default:
				component.setFont(selected ? bold : plain);
				break;
		}

		return component;
	}

	Object value(int col)
	{
		try {
			switch (col) {
			 	case NodeNameColumn:
					return "TOTALS";
				case ActiveListCountColumn:
					return formatter.format(activeListCount);
				case TotalListCountColumn:
					return formatter.format(totalListCount);
				case ActiveRequestCountColumn:
					return formatter.format(requestCount);
				case TotalRequestCountColumn:
					return formatter.format(totalRequestCount);
				case ConsolidationHitsColumn:
					return formatter.format(totalConsolidationHits);
				case RepliesPerSecondColumn:
					return formatter.format(repliesPerSecond);
				case MaxRepliesPerSecondColumn:
					return formatter.format(repliesPerSecondMax);
			}
		} catch (Exception ignore) {
			ignore.printStackTrace();
		}

		return "";
	}
}

