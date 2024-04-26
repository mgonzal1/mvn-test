// $Id: AcnetInterface.java,v 1.11 2024/04/22 20:10:52 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AcnetInterface implements AcnetConstants, AcnetErrors
{
	final static Logger logger = Logger.getLogger(AcnetInterface.class.getName());

	static final Node vNode;
	static final boolean usingDaemon;
	static final StatSet stats = new StatSet();

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-8s %1$tF %1$tT %1$tL] %5$s %6$s%n");

		Node.init();

		vNode = getVirtualNode();

		logger.log(Level.INFO, "Using vNode '" + vNode + "'");

		boolean bindFailed;

		try {
			final DatagramChannel channel = DatagramChannel.open().bind(new InetSocketAddress(ACNET_PORT));

			try {
				AcnetConnectionInternal.start(channel, vNode);
				logger.log(Level.INFO, "Using internal ACNET implementation");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			bindFailed = false;
		} catch (IOException e) {
			logger.log(Level.INFO, "Using the ACNET daemon");

			bindFailed = true;
			AcnetConnectionDaemon.start();
			//sendNodeTableEntriesToDaemon();
		}

		usingDaemon = bindFailed;
	}

	public static void sendNodeTableEntriesToDaemon()
	{
		try {
			int count = 0;
			final WeakAcnetConnection c = new WeakAcnetConnection();

			for (Node node : Node.byValue.values())
				if (node.isValid()) {
					c.addNode(node);
					count++;
				}

			c.lastNode();
			
			logger.log(Level.INFO, "Downloaded " + count + " node entries to the ACNET daemon");

			c.close();
		} catch (AcnetStatusException e) {
			throw new RuntimeException(e);
		}
	}

	public static Node getVirtualNode()
	{
		try {
			return Node.get(System.getProperty("vnode", ""));
		} catch (Exception e) {
			final Node[] nodes = nodesOnThisHost();

			if (nodes.length == 0)
				throw new RuntimeException("Unable to determine vnode");

			return nodes[0];
		}
	}

	synchronized static Node[] nodesOnThisHost()
	{
		final ArrayList<Node> nodes = new ArrayList<>();

		try {
			for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
				final NetworkInterface ni = e.nextElement();

				for (InterfaceAddress iAddr : ni.getInterfaceAddresses()) {
					final InetAddress addr = iAddr.getAddress();

					if (!addr.isLoopbackAddress())
						nodes.addAll(Node.get(addr));
				}
			}
		} catch (Exception ignore) { }

		return nodes.toArray(new Node[nodes.size()]);
	}

	final static int getPid()
	{
		try {
			return Integer.valueOf(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		} catch (Exception e) {
			return 0;
		}
	}

	public static final int allocatedBufferCount()
	{
		return BufferCache.allocatedCount;
	}

	public static final int freeBufferCount()
	{
		return BufferCache.freeList.size();
	}

	final public static void close()
	{
		if (usingDaemon)
			AcnetConnectionDaemon.stop();
		else 
			AcnetConnectionInternal.stop();
	}

	final synchronized public static AcnetConnection open(String name)
	{
		if (usingDaemon)
			return new AcnetConnectionUDP(name, vNode);
		else
			return AcnetConnectionInternal.open(name, vNode);
	}

	final synchronized public static AcnetConnection open()
	{
		if (usingDaemon)
			return new AcnetConnectionUDP("", vNode);
		else
			return AcnetConnectionInternal.open("", vNode);
	}

	final synchronized public static AcnetConnection open(String host, String name)
	{
		return new AcnetConnectionTCP(host, name, vNode);
	}

	final public static String hostName()
	{
		return vNode.address().getHostName();
	}

	final public static InetAddress localhost()
	{
		return vNode.address().getAddress();
	}

	final public static Node vNode()
	{
		return vNode;
	}

	final public static Node localNode() throws AcnetStatusException
	{
		return vNode;
	}

	synchronized public static void main(String[] args) throws Exception
	{
		System.exit(0);
	}
}
