// $Id: DPMServer.java,v 1.85 2024/01/09 15:59:34 kingc Exp $
package gov.fnal.controls.servers.dpm;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.AcceleratorPool;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DPMServer implements AcnetErrors
{
	public static final Logger logger;
	private static final Level initLogLevel;

	private static final int pid;
	private static final boolean debug;
	private static final long started;
	private static final OperatingSystemMXBean os;
	private static final double cpuCount;
	private static final Timer timer;
	private static final String localHostName;
	private static final MemoryUsageDisplay memoryUsageDisplay;

	private static volatile RestartTask restartTask = null;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-8s %1$tF %1$tT %1$tL] %5$s %6$s%n");

		logger = Logger.getLogger(DPMServer.class.getName());
		logger.setUseParentHandlers(false);
		logger.addHandler(new ConsoleHandler());
		initLogLevel = Level.parse(System.getProperty("initLogLevel", "CONFIG"));

		pid = getPid();
		try {
			localHostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}

		debug = Boolean.parseBoolean(System.getProperty("debug", "false"));
		started = System.currentTimeMillis();
		os = ManagementFactory.getOperatingSystemMXBean();
		cpuCount = os.getAvailableProcessors();
		timer = new Timer("DPMServerSharedTimer", false);
		memoryUsageDisplay = new MemoryUsageDisplay();
	}	

	//private static DPMServer server = null;

/*
	static void serveForever(String host, String vNode) throws Exception
	{
		if (server == null) {
			server = new DPMServer(host, vNode);

			synchronized (server) {
				server.wait();
			}
		}
	}
*/
/*
	DPMServer() throws Exception
	{
		setLogLevel(Level.ALL);

		server = this;

		this.pid = getPid();
		this.debug = Boolean.parseBoolean(System.getProperty("debug", "false"));
		this.started = System.currentTimeMillis();
		this.os = ManagementFactory.getOperatingSystemMXBean();
		this.cpuCount = os.getAvailableProcessors();
		this.timer = new Timer("DPMServerSharedTimer", false);
		this.localHostName = InetAddress.getLocalHost().getHostName();
		this.memoryUsageDisplay = new MemoryUsageDisplay();
	}
*/

	//private DPMServer(String host, String vNode) throws Exception
	/*
	private static void init() throws Exception
	{
		setLogLevel(initLogLevel);

		//server = this;
		//this.pid = getPid();

		logger.config("DPM(" + pid + ") CLASSPATH: " + 
						System.getProperty("java.class.path"));

		//AcnetInterface.init(host, vNode);
		
		this.debug = Boolean.parseBoolean(System.getProperty("debug", "false"));
		this.started = System.currentTimeMillis();
		this.os = ManagementFactory.getOperatingSystemMXBean();
		this.cpuCount = os.getAvailableProcessors();
		this.timer = new Timer("DPMServerSharedTimer", false);
		this.localHostName = InetAddress.getLocalHost().getHostName();
		this.memoryUsageDisplay = new MemoryUsageDisplay();

		Runtime.getRuntime().addShutdownHook(new ShutdownHook());

		AcceleratorPool.init();
		//Lookup.init();
		Errors.init();
		//AcnetNodeInfo.init();
		//AdditionalDeviceInfo.init();
		ConsoleUserManager.init();
		DPMCredentials.init();
		//LoggerConfigCache.init();
		//DBNews.init();
		//States.init();
		Scope.init();
		DPMProtocolHandler.init();

		try {
			final Level logLevel = Level.parse(System.getProperty("loglevel"));
			setLogLevel(logLevel);
		} catch (Exception e) {
			final Level logLevel = debug ? Level.FINE : Level.INFO;
			logger.config("Logging level defaulted to " + logLevel);
			setLogLevel(logLevel);
		}
	}
	*/

	private static int getPid()
	{
		try {
			return Integer.valueOf(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		} catch (Exception e) { 
			return 0;
		}
	}

	private static class ShutdownHook extends Thread implements AcnetErrors
	{
		@Override
		public void run()
		{
			logger.log(Level.INFO, "DPM server shutdown");

			memoryUsageDisplay.cancel();

			try {
				for (DPMProtocolHandler h : DPMProtocolHandler.allHandlers())
					h.shutdown();

				Scope.close();
			} catch (Exception e) {
				logger.log(Level.WARNING, "shutdown", e);
			}

			AcnetInterface.close();
		}
	}

	private static class RestartTask extends TimerTask
	{
		RestartTask()
		{
			timer.scheduleAtFixedRate(this, 100, 5000);
		}

		@Override
		public void run()
		{
			logger.log(Level.FINE, "Checking for restart");

			for (DPMProtocolHandler h : DPMProtocolHandler.allHandlers()) {
				if (!h.restartable())
					return;	
			}

			logger.log(Level.INFO, "Restarting ...");
			System.exit(100);
		}
	}

	private static class MemoryUsageDisplay extends TimerTask
	{
		final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		volatile int[] lastValues = { -1, -1, -1 };

		MemoryUsageDisplay()
		{
			timer.scheduleAtFixedRate(this, 30000, 10000);
		}

		@Override
		public void run()
		{
			final MemoryUsage heap = memory.getHeapMemoryUsage();
			final int freePct = 100 - ((int) (((double) heap.getUsed() / (double) heap.getCommitted()) * 100));

			if (freePct != lastValues[0]) {
				lastValues[0] = freePct;
				logger.log(Level.FINE, freePct + "% Heap free ");
			}

			final int alloc = AcnetInterface.allocatedBufferCount();
			final int free = AcnetInterface.freeBufferCount();

			if (alloc != lastValues[1] || free != lastValues[2]) {
				lastValues[1] = alloc;
				lastValues[2] = free;
				logger.log(Level.FINE, "BufferCache allocated:" + alloc + " free:" + free);
			}
		}
	}

	static void scheduleRestart()
	{
		//if (server.restartTask == null)
			//server.restartTask = server.new RestartTask();
		if (restartTask == null)
			restartTask = new RestartTask();
	}

	public static boolean restartScheduled()
	{
		//return server.restartTask != null;
		return restartTask != null;
	}

	public static boolean debug()
	{
		//return server.debug;
		return debug;
	}

	static Level getLogLevel()
	{
		//return logger.getLevel();
		return logger.getLevel();
	}

  	public static void setLogLevel(Level level)
	{
		logger.setLevel(level);

		for (Handler handler : logger.getHandlers())
			handler.setLevel(level);

		logger.log(Level.INFO, "Logging level set to " + level);
	}

	public static Collection<DPMList> activeLists()
	{
		final ArrayList<DPMList> lists = new ArrayList<>(2048);

		for (DPMProtocolHandler h : DPMProtocolHandler.allHandlers())
			lists.addAll(h.activeLists());

		return lists;
	}

	public static Collection<DPMList> disposedLists()
	{
		final ArrayList<DPMList> lists = new ArrayList<>(2048);

		for (DPMProtocolHandler h : DPMProtocolHandler.allHandlers())
			lists.addAll(h.disposedLists());
		
		return lists;
	}

	public static DPMList list(int listId) throws AcnetStatusException
	{
		for (DPMProtocolHandler h : DPMProtocolHandler.allHandlers()) {
			try {
				return h.list(listId);
			} catch (Exception ignore) { }
		}

		throw new AcnetStatusException(DPM_NO_SUCH_LIST);
	}

	static long started()
	{
		//return server.started;
		return started;
	}

	public static String localHostName()
	{
		//return server.localHostName;
		return localHostName;
	}

	public static int pid()
	{
		//return server.pid;
		return pid;
	}

	public static int heapFreePercentage()
	{
		//return server.memoryUsageDisplay.lastValues[0];
		return memoryUsageDisplay.lastValues[0];
	}

	public static Timer sharedTimer()
	{
		//return server.timer;
		return timer;
	}

	public static int loadPercentage()
	{
		//final double sysLoad = server.os.getSystemLoadAverage();
		//final int load = (int) Math.round((sysLoad / server.cpuCount) * 100.0);
		final double sysLoad = os.getSystemLoadAverage();
		final int load = (int) Math.round((sysLoad / cpuCount) * 100.0);

		return (load > 100) ? 100 : load;
	}

	synchronized public static void main(String[] args) throws Exception
	{
		//init();
		setLogLevel(initLogLevel);

		//server = this;
		//this.pid = getPid();

		logger.log(Level.CONFIG, "DPM(" + pid + ") CLASSPATH: " + 
						System.getProperty("java.class.path"));

		//AcnetInterface.init(host, vNode);
		
		/*
		this.debug = Boolean.parseBoolean(System.getProperty("debug", "false"));
		this.started = System.currentTimeMillis();
		this.os = ManagementFactory.getOperatingSystemMXBean();
		this.cpuCount = os.getAvailableProcessors();
		this.timer = new Timer("DPMServerSharedTimer", false);
		this.localHostName = InetAddress.getLocalHost().getHostName();
		this.memoryUsageDisplay = new MemoryUsageDisplay();
		*/

		Runtime.getRuntime().addShutdownHook(new ShutdownHook());

		//gov.fnal.controls.servers.dpm.pools.Node.init();

		AcceleratorPool.init();
		//Lookup.init();
		
		Errors.init();
		//AcnetNodeInfo.init();
		//AdditionalDeviceInfo.init();
		ConsoleUserManager.init();
		DPMCredentials.init();
		//LoggerConfigCache.init();
		//DBNews.init();
		//States.init();
		Scope.init();
		DPMProtocolHandler.init();

		try {
			setLogLevel(Level.parse(System.getProperty("loglevel")));
		} catch (Exception e) {
			final Level logLevel = debug ? Level.FINE : Level.INFO;
			logger.log(Level.CONFIG, "Logging level defaulted to " + logLevel);
			setLogLevel(logLevel);
		}
		//if (server == null) {
			//server = new DPMServer(host, vNode);
			//server = new DPMServer();

			//synchronized (server) {
			//	server.wait();
			//}
		DPMServer.class.wait();
		//}
		//DPMServer.serveForever();
	}
}
