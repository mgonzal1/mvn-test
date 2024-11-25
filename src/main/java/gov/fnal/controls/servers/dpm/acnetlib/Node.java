// $Id: Node.java,v 1.7 2024/11/21 22:02:19 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.BitSet;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.sql.ResultSet;

import static gov.fnal.controls.db.DbServer.getDbServer;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class Node implements Comparable<Node>, NodeFlags, AcnetErrors
{
	final static HashMap<Integer, Node> byValue = new HashMap<>();
	final static HashMap<String, Node> byName = new HashMap<>();

	final int value;
	final String name;
	final InetSocketAddress address; 
	final BitSet flags;
	final String software;
	final NodeType type;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-8s %1$tF %1$tT %1$tL] %5$s %6$s%n");
	}

	Node()
	{
		this.value = 0;
		this.name = "";
		this.address = null;
		this.flags = null;
		this.software = "";
		this.type = NodeType.ACNET;
	}
	
	private Node(int value, String name, String host) 
	{
		this.value = value;
		this.name = name;
		this.address = new InetSocketAddress(host, 6801);
		this.flags = new BitSet();
		this.software = "";
		this.type = NodeType.ACNET;
	}

	private Node(ResultSet rs) throws AcnetStatusException
	{
		try {
			this.value = (rs.getInt("trunk") << 8) + rs.getInt("node");
			this.name = rs.getString("node_name").trim();
			this.address = new InetSocketAddress(rs.getString("ip_name").trim(), 6801);
			this.software = rs.getString("software").trim();
			
			final String status = rs.getString("status").trim();

			this.flags = new BitSet();
			this.flags.set(HARDWARE_CLOCK, rs.getBoolean("is_tclk_fe"));
			this.flags.set(EVENT_STRING_SUPPORT, rs.getBoolean("is_gets32_sets32"));
			this.flags.set(OPERATIONAL, status.equals("Operational"));
			this.flags.set(OBSOLETE, status.equals("Obsolete"));
			this.flags.set(OUT_OF_SERVICE, status.startsWith("Out of Service"));

			this.type = this.software.equals("EPICS IOC") ? NodeType.EPICS : NodeType.ACNET;
		} catch (Exception e) {
			throw new AcnetStatusException(SQL_USER0, "Node", e);
		}
	}

	@Override
	public int compareTo(Node node)
	{
		return name.compareTo(node.name);
	}

	public synchronized static List<Node> get(InetAddress addr)
	{
		final ArrayList<Node> nodes = new ArrayList<>();

		for (Node node : byName.values()) {
			final InetAddress tmp = node.address.getAddress();
			if (tmp != null && addr.equals(tmp))
				nodes.add(node);
		}

		return nodes;
	}

	public synchronized static Node get(int value) throws AcnetStatusException
	{
		final Node node = byValue.get(value);

		if (node != null)
			return node;

		throw new AcnetStatusException(ACNET_NO_NODE);
	}

	public synchronized static Node get(String name) throws AcnetStatusException
	{
		final Node node = byName.get(name.trim().toUpperCase());

		if (node != null)
			return node;

		throw new AcnetStatusException(ACNET_NO_NODE);
	}

	synchronized public static void init()
	{
		try {
			cacheNodes();
		} catch (Exception e) {
			throw new RuntimeException("Exception caching nodes" , e);
		}
	}

	final public InetSocketAddress address()
	{
		return address;
	}

	final public int value()
	{
		return value;
	}

	public String name()
	{
		return name;
	}

	final public boolean isValid()
	{
		if (address != null) {
			final InetAddress addr = address.getAddress();

			return addr instanceof Inet4Address && addr.getAddress() != null;
		}

		return false;
	}

	public boolean is(int flagNo)
	{
		return flags.get(flagNo);
	}

	public boolean has(int flagNo)
	{
		return flags.get(flagNo);
	}

	public synchronized final static void cacheNodes() throws AcnetStatusException
	{
		logger.log(Level.INFO, "Caching nodes");

		final String query = "SELECT N.trunk, N.node, I.node_name, N.ip_name, I.status, I.is_tclk_fe, I.is_gets32_sets32, I.software" +
							 " FROM accdb.acnet_node N, johnson.acnet_view_node_info I" +
							 " WHERE I.node_name = N.node_name";

		byValue.clear();
		byName.clear();

		ResultSet rs = null;

		try {
			rs = getDbServer("adbs").executeQuery(query);

			while (rs.next()) {
				final Node node = new Node(rs);

				byValue.put(node.value, node);
				byName.put(node.name, node);
			}
		} catch (Exception e) {
			throw new AcnetStatusException(SQL_USER0, "getNodes " + e, e);
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (Exception e) {
			}
		}

		final Node mcast = new Node(255, "MCAST", "239.128.4.1");

		byValue.put(mcast.value(), mcast);
		byName.put(mcast.name(), mcast);
	}

	public NodeType type()
	{
		return type;
	}

	public boolean isMCAST()
	{
		return value == 255;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Node)
			return ((Node) o).value == value;

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(value);
	}

	@Override
	public String toString()
	{
		return name;
	}

	public static void main(String[] args) throws AcnetStatusException
	{
		cacheNodes();

		for (java.util.Map.Entry<String, Node> entry : (new java.util.TreeMap<String, Node>(byName)).entrySet()) {
			final Node node = entry.getValue();
			logger.log(Level.INFO, String.format("%3d,%3d value:0x%04x name:%-10s oper:%-6s clock:%-6s %8s ip_name:%-50s software:%-15s type:%s\n", 
									node.value >> 8, node.value & 0xff, node.value, node.name, node.is(OPERATIONAL), 
									node.has(HARDWARE_CLOCK), node.has(EVENT_STRING_SUPPORT) ? "GETS32" : "RETDAT", 
									node.address, node.software, node.type));
		}

		System.exit(0);
	}
}
