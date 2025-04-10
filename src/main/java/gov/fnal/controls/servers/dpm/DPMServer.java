// $Id: DPMServer.java,v 1.95 2024/12/04 19:21:41 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.MemoryMXBean;

import gov.fnal.controls.servers.dpm.pools.AcceleratorPool;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;

public class DPMServer implements AcnetErrors
{
	static class ServerHandler extends ConsoleHandler
	{
		static final int warningValue = Level.WARNING.intValue();

		ServerHandler()
		{
			super();
		}

		@Override
		public void publish(LogRecord record)
		{
			final String name = record.getLoggerName();
			final Level level = record.getLevel();

			if (name.isEmpty()) {
				super.publish(record);

				if (level.intValue() >= warningValue) {
					Scope.exceptionStack();

					if (debug) {
						System.out.println("Exception stack status set: " + level + " " + record.getMessage());
					}
				}
			}
		}
	}

	public static final Logger logger;

	static final Level initLogLevel;
	static final int pid;
	static final long started;
	static final OperatingSystemMXBean os;
	static final double cpuCount;
	static final Timer timer;
	static final String localHostAddress;
	static final String localHostName;
	static final MemoryUsageDisplay memoryUsageDisplay;
	static final String codeVersion;
	static final ServerSocketChannel scopeChannel;
	static final boolean debug;

	static volatile RestartTask restartTask = null;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-8s %1$tF %1$tT %1$tL] %5$s %6$s%n");

		final Logger root = Logger.getLogger("");

		//logger = root; //Logger.getLogger(DPMServer.class.getName());
		//logger.setUseParentHandlers(false);

		for (Handler h : root.getHandlers())
			root.removeHandler(h);

		root.addHandler(new ServerHandler());
		logger = root;
		//logger.addHandler(new ConsoleHandler());

		codeVersion = System.getProperty("dae.version", System.getProperty("version", "--"));
		initLogLevel = Level.parse(System.getProperty("initLogLevel", "CONFIG"));

		pid = getPid();

		try {
			localHostAddress = InetAddress.getLocalHost().getHostAddress();
			localHostName = InetAddress.getLocalHost().getHostName();
			scopeChannel = ServerSocketChannel.open();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		debug = Boolean.parseBoolean(System.getProperty("debug", "false"));
		started = System.currentTimeMillis();
		os = ManagementFactory.getOperatingSystemMXBean();
		cpuCount = os.getAvailableProcessors();
		timer = new Timer("Shared timer", false);
		memoryUsageDisplay = new MemoryUsageDisplay();
	}

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

				//Scope.close();
				scopeChannel.close();
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
			//final int freePct = 100 - ((int) (((double) heap.getUsed() / (double) heap.getCommitted()) * 100));
			final int freePct = 100 - ((int) (((double) heap.getUsed() / (double) heap.getMax()) * 100));

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

	static String codeVersion()
	{
		return codeVersion;
	}

	static void scheduleRestart()
	{
		if (restartTask == null)
			restartTask = new RestartTask();
	}

	public static boolean restartScheduled()
	{
		return restartTask != null;
	}

	public static boolean debug()
	{
		return debug;
	}

	static Level getLogLevel()
	{
		return logger.getLevel();
	}

	public static void setLogLevel(Level level)
	{
		logger.log(Level.INFO, "Logging level set to " + level);

		logger.setLevel(level);

		for (Handler handler : logger.getHandlers())
			handler.setLevel(level);
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
		return started;
	}

	public static String localHostName()
	{
		return localHostName;
	}

	public static String localHostAddress()
	{
		return localHostAddress;
	}

	public static int pid()
	{
		return pid;
	}

	public static int heapFreePercentage()
	{
		return memoryUsageDisplay.lastValues[0];
	}

	public static Timer sharedTimer()
	{
		return timer;
	}

	public static int loadPercentage()
	{
		final double sysLoad = os.getSystemLoadAverage();
		final int load = (int) Math.round((sysLoad / cpuCount) * 100.0);

		return (load > 100) ? 100 : load;
	}

	private static void logCodeVersion() throws IOException
	{
		final String root = System.getProperty("engines.files.root");

		if (root != null) {
			final FileWriter file = new FileWriter(root + "/files/dpm/" + AcnetInterface.getVirtualNode());

			file.write(codeVersion);
			file.close();
		}
	}

	synchronized public static void main(String[] args) throws Exception
	{
		setLogLevel(initLogLevel);

		logger.log(Level.CONFIG, "DPM(" + pid + ") VERSION:'" + codeVersion +
				"' CLASSPATH:'" + System.getProperty("java.class.path") + "'");

		Runtime.getRuntime().addShutdownHook(new ShutdownHook());

		try {
			logCodeVersion();
		} catch (Exception ignore) { }

		AcceleratorPool.init();
		Errors.init();
		ConsoleUserManager.init();
		DPMCredentials.init();
		Scope.init();
		DPMProtocolHandler.init();

		try {
			setLogLevel(Level.parse(System.getProperty("loglevel")));
		} catch (Exception e) {
			final Level logLevel = debug ? Level.FINE : Level.INFO;
			setLogLevel(logLevel);
		}

		final InetSocketAddress address = new InetSocketAddress(Scope.TCP_SERVER_PORT);

		scopeChannel.bind(address);
		Thread.currentThread().setName("Scope accept");

		while (true) {
			try {
				new Scope.HandlerThread(scopeChannel.accept());
			} catch (Exception ignore) {
			}
		}

		//scopeChannel.close();

		//DPMServer.class.wait();
	}
}
