// $Id: DeviceCache.java,v 1.8 2024/04/11 20:21:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import gov.fnal.controls.service.proto.Dbnews;

import gov.fnal.controls.servers.dpm.DPMRequest;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetMessage;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetMessageHandler;

import gov.fnal.controls.db.DbServer;
import static gov.fnal.controls.db.DbServer.getDbServer;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class DIPI implements Comparable<DIPI>
{
	final int di;
	final int pi;
	final Integer dipi;

	DIPI(int di, int pi)
	{
		this.di = di;
		this.pi = pi;
		this.dipi = new Integer(di + (pi << 24));
	}

	@Override
	public String toString()
	{
		return String.format("(%d,%d,0x%08x)", di, pi, dipi);
	}

	@Override
	public boolean equals(Object o)
	{
		return dipi.equals(((DIPI) o).dipi);
	}

	@Override
	public int hashCode()
	{
		return dipi.hashCode();
	}

	@Override
	public int compareTo(DIPI o)
	{
		return dipi.compareTo(o.dipi);	
	}
}

class DISet
{
	final Set<Integer> diSet;

	DISet()
	{
		this.diSet = new HashSet<>();
	}

	void add(String name)
	{
		final DeviceCache.Holder h = DeviceCache.nameMap.get(name);

		if (h != null)
			diSet.add(h.di);
	}

	void add(int di)
	{
		diSet.add(di);
	}

	int size()
	{
		return diSet.size();
	}

	@Override
	public String toString()
	{
		if (diSet.size() == 0)
			return "(0)";
		else {
			final StringBuilder buf = new StringBuilder("(");

			for (Integer di : diSet) {
				if (buf.length() > 1)
					buf.append(',');
				buf.append(di);
			}

			buf.append(')');
			return buf.toString();
		}
	}
}

class DBMaps
{
	final String diSet;
	final DbServer dbServer;

	final Map<Integer, List<String>> family;
	final Map<DIPI, List<DeviceInfo.ReadSetScaling.Common.EnumString>> enumString;
	final Map<DIPI, String> foreignDevice;
	final Map<Integer, List<DeviceInfo.Control.Attribute>> control; 
	final Map<Integer, List<DeviceInfo.Status.Attribute>> status;
	final Map<Integer, List<DeviceInfo.Status.BitTextAttribute>> statusBitText;
	final Map<Integer, String> alarmListName;

	DBMaps(DbServer dbServer, String diSet) throws SQLException
	{
		this.diSet = diSet;
		this.dbServer = dbServer;
		this.family = family();
		this.enumString = enumString();
		this.foreignDevice = foreignDevice();
		this.control = control();
		this.status = status();
		this.statusBitText = statusBitText();
		this.alarmListName = alarmListName();
	}

	String diSet(String preFix)
	{
		if (diSet != null && diSet.length() > 0)
			return preFix + " " + diSet;

		return "";
	}

	private Map<DIPI, List<DeviceInfo.ReadSetScaling.Common.EnumString>> enumString() throws SQLException
	{
		final Map<DIPI, List<DeviceInfo.ReadSetScaling.Common.EnumString>> map = new HashMap<>();
		final String query = "SELECT di,pi,value,short_name,long_name " +
								"FROM accdb.read_set_enum_sets S " +
								"JOIN accdb.read_set_enum_values V ON S.enum_set_id=V.enum_set_id " +
								diSet("WHERE di IN");
								//(diSet != null ? ("WHERE di IN " + diSet) : "");

		final ResultSet rs = dbServer.executeQuery(query);

		while (rs.next()) {
			final DIPI dipi = new DIPI(rs.getInt("di"), rs.getInt("pi"));
			final DeviceInfo.ReadSetScaling.Common.EnumString e = new DeviceInfo.ReadSetScaling.Common.EnumString(rs);
	
			List<DeviceInfo.ReadSetScaling.Common.EnumString> list = map.get(dipi);

			if (list == null) {
				list = new ArrayList<>();
				map.put(dipi, list);	
			}
			list.add(e);
		}
		rs.close();

		return map;
	}

	private Map<DIPI, String> foreignDevice() throws SQLException
	{
		final Map<DIPI, String> map = new HashMap<>();
		final String query = "SELECT di,pi,pv_name FROM accdb.foreign_device_mapping " + diSet("WHERE di IN");//"WHERE di IN " + diSet;
		final ResultSet rs = dbServer.executeQuery(query);

		while (rs.next()) {
			final DIPI dipi = new DIPI(rs.getInt("di"), rs.getInt("pi"));

			map.put(dipi, rs.getString("pv_name"));
		}
		rs.close();

		return map;
	}

	private Map<Integer, List<String>> family() throws SQLException
	{
		final Map<Integer, List<String>> map = new HashMap<>();
		final String query = "SELECT F.di,D.name FROM accdb.family F LEFT OUTER JOIN accdb.device D ON F.member_di=D.di " +
								diSet("WHERE F.di IN") + //"WHERE F.di IN " + diSet + 
								" ORDER BY F.di,seq";

								//System.out.println(query);

		final ResultSet rs = dbServer.executeQuery(query);

		while (rs.next()) {
			final int di = rs.getInt("di");

			List<String> list = map.get(di);

			if (list == null) {
				list = new ArrayList<>();
				map.put(di, list);
			}
			list.add(rs.getString("name"));
		}
		rs.close();

		return map;
	}

	private Map<Integer, List<DeviceInfo.Control.Attribute>> control() throws SQLException
	{
		final Map<Integer, List<DeviceInfo.Control.Attribute>> map = new HashMap<>();
		final String query = "SELECT C.di,C.value,C.order_number,C.short_name,C.long_name,L.length " +
								"FROM accdb.digital_control C " +
								"JOIN accdb.scaling_length L " +
								"ON L.di = C.di " +
								"WHERE L.pi=28 AND C.order_number<32 " +
								diSet("AND C.di IN") + //"AND C.di IN " + diSet +
								"ORDER BY C.di,C.order_number";

		final ResultSet rs = getDbServer("adbs").executeQuery(query);

		while (rs.next()) {
			final int di = rs.getInt("di");

			List<DeviceInfo.Control.Attribute> list = map.get(di);

			if (list == null) {
				list = new ArrayList<>();
				map.put(di, list);
			}
			list.add(new DeviceInfo.Control.Attribute(rs));
		}
		rs.close();

		return map;
	}

	private Map<Integer, List<DeviceInfo.Status.Attribute>> status() throws SQLException
	{
		final Map<Integer, List<DeviceInfo.Status.Attribute>> map = new HashMap<>();
		final String query = "SELECT S.di, S.mask_value, S.match_value, S.invert_flag, S.order_number, S.short_name, S.long_name, " +
								"S.true_string, S.true_color, S.true_char, S.false_string, S.false_color, S.false_char, L.length " +
								"FROM accdb.basic_status_scaling S " +
								"JOIN accdb.scaling_length L " +
								"ON L.di = S.di " +
								"WHERE L.pi = 33 AND S.order_number < 32 " +
								diSet("AND S.di IN") + //"AND S.di IN " + diSet +
								" ORDER BY S.di,S.order_number";

		final ResultSet rs = dbServer.executeQuery(query);
		
		while (rs.next()) {
			final int di = rs.getInt("di");

			List<DeviceInfo.Status.Attribute> list = map.get(di);

			if (list == null) {
				list = new ArrayList<>();
				map.put(di, list);
			}
			list.add(new DeviceInfo.Status.Attribute(rs));
		}
		rs.close();

		return map;
	}

	private Map<Integer, List<DeviceInfo.Status.BitTextAttribute>> statusBitText() throws SQLException
	{
		final Map<Integer, List<DeviceInfo.Status.BitTextAttribute>> map = new HashMap<>();
		final String query = "SELECT DDS.di,DDS.bit_no,DS.short_name_0,DS.short_name_1,DS.bit_description " +
								"FROM accdb.digital_status DS,accdb.device_digital_status DDS " +
								"WHERE DDS.digital_status_id=DS.digital_status_id " +
								diSet("AND DDS.di IN") + //"AND DDS.di IN " + diSet +
								" ORDER BY DDS.di,DDS.bit_no";

		final ResultSet rs = dbServer.executeQuery(query);

			while (rs.next()) {
				final int di = rs.getInt("di");

				List<DeviceInfo.Status.BitTextAttribute> list = map.get(di);

				if (list == null) {
					list = new ArrayList<>();
					map.put(di, list);
				}
				list.add(new DeviceInfo.Status.BitTextAttribute(rs));
			}
		
		rs.close();

		return map;
	}

	private Map<Integer, String> alarmListName() throws SQLException
	{
		final HashMap<Integer, String> map = new HashMap<>();
		final String query = "SELECT list_number,name FROM hendricks.alarm_list_info";

		final ResultSet rs = dbServer.executeQuery(query);

		while (rs.next()) 
			map.put(rs.getInt("list_number"), rs.getString("name"));

		return map;
	}
}

public class DeviceCache implements AcnetErrors, AcnetMessageHandler, Dbnews.Request.Receiver
{
	static final class Holder 
	{
		final int di;
		final String name;

		Holder(int di, String name)
		{
			this.di = di;
			this.name = name;
		}

		@Override
		public String toString()
		{
			return "'" + name + "'" + " " + di;
		}
	}

	static volatile Map<String, Holder> nameMap = new HashMap<>();
	static Map<String, DeviceInfo> deviceCache = new HashMap<>();

	public static void init()
	{
		try {
			AcnetInterface.open("DBNEWS").setQueueReplies().handleMessages(new DeviceCache());
			nameMap = getNameMap();
			//cacheAllDevices();
		} catch (Exception e) {
			throw new RuntimeException("exception during startup", e);
		}
	}

	@Override
    public void handle(AcnetMessage r)
	{
		try {
			Dbnews.Request.unmarshal(r.data()).deliverTo(this);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception in dbnews message", e);
		}
	}

	@Override
	public void handle(Dbnews.Request.Report m)
	{
		logger.log(Level.FINE, "DBNEWS: " + m.info.length + " reports");

		synchronized (deviceCache) {
			logger.log(Level.FINE, "DeviceCache: starts with " + deviceCache.size() + " entries");

			for (Dbnews.Request.Info dbEdit : m.info) {
				final String diString = "0:" + dbEdit.di;
				final Holder h = nameMap.get(diString);

				logger.log(Level.FINE, "DeviceCache: removing " + diString);

				deviceCache.remove(diString);
				if (h != null) {
					logger.log(Level.FINE, "DeviceCache: removing " + h.name);
					deviceCache.remove(h.name);
				}
			}

			logger.log(Level.FINE, "DeviceCache: ends with " + deviceCache.size() + " entries");
		}

		try {
			nameMap = getNameMap();
		} catch (SQLException e) {
		}
	}

	static Map<String, Holder> getNameMap() throws SQLException
	{
		final long start = System.currentTimeMillis();
		final Map<String, Holder> map = new HashMap<>();
		final String query = "SELECT name,di,flags FROM accdb.device";

		final ResultSet rs = getDbServer("adbs").executeQuery(query);

		while (rs.next()) {
			final Holder h = new Holder(rs.getInt("di"), rs.getString("name"));

			//System.out.printf("%s 0x%04x\n", h, (rs.getInt("flags") & 0xffff));

			if ((rs.getInt("flags") & 0x80) == 0) {
				map.put(h.name, h);
				map.put("0:" + h.di, h);
			}
		}

		logger.log(Level.CONFIG, "DeviceCache name map in " + (System.currentTimeMillis() - start) + "ms");

		return map;
	}

/*
	static Map<String, Holder> getNameMap(String diSet) throws SQLException
	{
		final Map<String, Holder> map = new HashMap<>();
		final String query = "SELECT name, di FROM accdb.device WHERE di IN " + diSet;

		final ResultSet rs = getDbServer("adbs").executeQuery(query);

		while (rs.next()) {
			final Holder h = new Holder(rs.getInt("di"), rs.getString("name"));

			map.put(h.name, h);
			map.put("0:" + h.di, h);
		}

		return map;
	}
	*/

	static Map<String, DeviceInfo> getDeviceInfoMap(DBMaps dbMaps) throws SQLException
	{
		final Map<String, DeviceInfo> map = new HashMap<>();

		final String query = "SELECT D.name, D.di, D.flags, D.description, COALESCE(D.protection_mask,0) protection_mask, D.alarm_list_id, " +
								"P.pi, P.ssdn, P.size, P.atomic_size, P.def_size, P.trunk, P.node, P.ftd, P.addressing_mode, P.default_data_event, " +
								"S.di, S.primary_index, S.common_index, S.primary_text, S.common_text, S.minimum, S.maximum, S.display_format, " +
								"S.display_length, S.scaling_length, S.scaling_offset, S.num_constants, S.const1, S.const2, S.const3, S.const4, " +
								"S.const5, S.const6, S.const7, S.const8, S.const9, S.const10, F.is_step_motor, F.is_contr_setting, " +
								"ALM.status, ALM.min_or_nom, ALM.max_or_tol, ALM.tries_needed, ALM.clock_event_no, ALM.subfunction_code, ALM.specific_data, " +
								"ALM.segment, TA.text AS analog_text, DDA.condition, DDA.mask, DDA.condition, DDA.mask, TD.text AS digital_text, " +
								"AUX.control_system_type, AUX.long_name, AUX.long_description " +
								"FROM accdb.device D " +
								"LEFT OUTER JOIN accdb.property P ON D.di = P.di " +
								"LEFT OUTER JOIN accdb.device_analog_alarm DAA ON D.di = DAA.di " +
								"LEFT OUTER JOIN accdb.alarm_text TA ON DAA.alarm_text_id = TA.alarm_text_id " +
								"LEFT OUTER JOIN accdb.device_digital_alarm DDA ON D.di = DDA.di " +
								"LEFT OUTER JOIN accdb.alarm_text TD ON DDA.alarm_text_id = TD.alarm_text_id " +
								"LEFT OUTER JOIN accdb.alarm_block ALM ON (D.di = ALM.di and P.pi = ALM.pi) " +
								"LEFT OUTER JOIN accdb.device_scaling S ON (D.di = S.di and P.pi = S.pi) " +
								"LEFT OUTER JOIN accdb.device_flags F ON (D.di = F.di and P.pi = F.pi) " +
								"LEFT OUTER JOIN accdb.device_aux AUX ON (D.di = AUX.di) " +
								dbMaps.diSet("WHERE D.di IN") + //"WHERE D.di IN " + dbMaps.diSet +
								" ORDER BY D.di";

		final ResultSet rs = getDbServer("adbs").executeQuery(query);

		if (rs.next()) {
			while (!rs.isAfterLast()) {
				final String name = rs.getString("name");
				final DeviceInfo d = new DeviceInfo(dbMaps, rs);

				map.put(name, d);
				map.put("0:" + d.di, d);
			}
		}
		rs.close();

		return map;

	}


/*
	static void add(Collection<String> nameSet) throws SQLException
	{
		ArrayList<String> cacheSet = new ArrayList<>();

		for (String name : nameSet)
			if (!deviceCache.containsKey(name))
				cacheSet.add(name);

		if (cacheSet.size() > 0) {
			StringBuilder buf = new StringBuilder("(0");

			for (String name : cacheSet) {
				final Integer di = nameMap.get(name);

				if (di != null)
					buf.append("," + di);
			}
			buf.append(")");

			final String diSet = buf.toString();

			familyMap = getFamilyMap(diSet);
			enumStringMap = getEnumStringMap(diSet);
			foreignDeviceMap = getForeignDeviceMap(diSet);
			controlMap = getControlMap(diSet);
			statusMap = getStatusMap(diSet);
			statusBitTextMap = getStatusBitTextMap(diSet);

			Map<String, DeviceInfo> map = getDeviceInfoMap(diSet);

			System.out.println("map: " + map);

			deviceCache.putAll(map);
		}
	}
*/

	public static void add(String device) throws SQLException
	{
		final ArrayList<DPMRequest> list = new ArrayList<>();

		list.add(new DPMRequest(device));
		add(list);
	}

	private static final String getDevice(DPMRequest request)
	{
		final String device = request.getDevice();

		if (device.charAt(1) != ':') {
			final char tmp[] = device.toCharArray();

			tmp[1] = ':';
			return new String(tmp);
		}

		return device;
	}

	public static void add(Collection<DPMRequest> requests) throws SQLException
	{
		final DbServer dbServer = getDbServer("adbs");
		//final HashSet<String> cacheSet = new HashSet<>();
		final DISet diSet = new DISet();

		for (DPMRequest request : requests) {
			final String device = getDevice(request);
			
			if (!deviceCache.containsKey(device))
				diSet.add(device);
		}

		//if (cacheSet.size() > 0) {
		if (diSet.size() > 0) {
		/*
			final StringBuilder buf = new StringBuilder("(");

			for (String name : cacheSet) {
				final Holder h = nameMap.get(name);

				if (h != null) {
					if (buf.length() != 1)
						buf.append(",");
					buf.append(h.di);
				}
			}
			buf.append(")");

			final String diSet = buf.toString();
		*/

			synchronized (deviceCache) {
				deviceCache.putAll(getDeviceInfoMap(new DBMaps(dbServer, diSet.toString())));
				logger.log(Level.FINE, "DeviceCache: " + deviceCache.size() + " entries");
			}
		}
	}

	public static void cacheAllDevices() throws SQLException
	{
		final DbServer dbServer = getDbServer("adbs");

		synchronized (deviceCache) {
			deviceCache.putAll(getDeviceInfoMap(new DBMaps(dbServer, null)));
			logger.log(Level.FINE, "DeviceCache: " + deviceCache.size() + " entries");
		}
	}

	public static String name(int di) throws DeviceNotFoundException
	{
		final Holder h = nameMap.get("0:" + di);

		if (h != null)
			return h.name;

		throw new DeviceNotFoundException();
	}

	public static int di(String name) throws DeviceNotFoundException
	{
		final Holder h = nameMap.get(name);

		if (h != null)
			return h.di;

		throw new DeviceNotFoundException();
	}

	public static DeviceInfo get(String name) throws DeviceNotFoundException
	{
		final DeviceInfo dInfo = deviceCache.get(name);

		if (dInfo != null)
			return dInfo;

		throw new DeviceNotFoundException();
	}

	public static void main(String[] args) throws Exception
	{
		//gov.fnal.controls.servers.dpm.DPMServer.setLogLevel(Level.CONFIG);
		nameMap = getNameMap();
		gov.fnal.controls.servers.dpm.DPMServer.setLogLevel(Level.FINE);
		//add(args[0]);
		//System.out.println("info " + get(args[0]));

		//for (final Holder holder : nameMap.values())
			//System.out.println(holder.name);

		cacheAllDevices();

/*
		final ArrayList<DPMRequest> list = new ArrayList<>();

		for (Holder holder : nameMap.values()) {
			System.out.println(holder.name);
			list.add(new DPMRequest(holder.name));

			if (list.size() > ) {
				add(list);
				list.clear();
			}
		}
		
		if (list.size() > 0)
			add(list);
*/
	}
}
