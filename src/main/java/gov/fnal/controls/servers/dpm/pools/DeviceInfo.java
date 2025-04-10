// $Id: DeviceInfo.java,v 1.14 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceInfo
{
	public enum DeviceState { Active, Deleted, Obsolete, Documented }
	public enum ControlSystemType { Acnet, Epics, Unknown }

	public static class PropertyInfo
	{
		public final int pi;
		public final int node;
		public final int ftd;
		public final byte[] ssdn;
		public final int size;
		public final int defSize;
		public final int atomicSize;
		public final String defEvent;
		public final boolean nonLinear; 
		public final String foreignName;
		public final ControlSystemType controlSystem;
		public final double minimum;
		public final double maximum;

		PropertyInfo()
		{
			this.pi = 0;
			this.node = 0;
			this.ftd = 0;
			this.ssdn = new byte[0];
			this.size = 0;
			this.defSize = 0;
			this.atomicSize = 0;
			this.defEvent = "p,1000";
			this.nonLinear = false;
			this.controlSystem = ControlSystemType.Unknown;
			this.foreignName = "";
			this.minimum = Double.MIN_VALUE;
			this.maximum = Double.MAX_VALUE;
		}

		PropertyInfo(DBMaps dbMaps, ResultSet rs) throws SQLException
		{
			this.pi = rs.getInt("pi");
			this.node = (rs.getInt("trunk") << 8) + rs.getInt("node");
			this.ftd = rs.getInt("ftd") & 0xffff;
			this.ssdn = rs.getBytes("ssdn");
			this.size = rs.getInt("size");
			this.defSize = rs.getInt("def_size");
			this.atomicSize = rs.getInt("atomic_size");
			this.defEvent = rs.getString("default_data_event");
			this.nonLinear = rs.getInt("addressing_mode") == 5;

			final ForeignDevice tmp = dbMaps.foreignDevice.get(new DIPI(rs.getInt("di"), this.pi));

			if (tmp == null) {
				this.foreignName = "";
				this.controlSystem = ControlSystemType.Acnet;
			} else {
				this.foreignName = tmp.name;

				switch (tmp.controlSystem) {
				 case 0:
					this.controlSystem = ControlSystemType.Acnet;
				 	break;

				 case 1:
					this.controlSystem = ControlSystemType.Epics;
				 	break;

				 default:
					this.controlSystem = ControlSystemType.Acnet;
				 	break;
				}
			}

			this.minimum = rs.getDouble("minimum");
			this.maximum = rs.getDouble("maximum");
		}

		PropertyInfo(int pi, int node, int size, int defSize)
		{
			this.pi = pi;
			this.node = node;
			this.ftd = 0;
			this.ssdn = new byte[0];
			this.size = size;
			this.defSize = defSize;
			this.atomicSize = defSize;
			this.defEvent = "i";
			this.nonLinear = false;
			this.foreignName = "";
			this.controlSystem = ControlSystemType.Unknown;
			this.minimum = Double.MIN_VALUE;
			this.maximum = Double.MAX_VALUE;
		}

		@Override public String toString()
		{
			return String.format("PropertyInfo(%d, size:%d def:%d atomic:%d defev:'%s' type'%s' foreign:%s)", 
									pi, size, defSize, atomicSize, defEvent, controlSystem, foreignName);
		}
	}

	public static class ReadSetScaling
	{
		public static class Primary
		{
			public final int index;
			public final int inputLen;
			public final String units;

			Primary(ResultSet rs) throws SQLException
			{
				this.index = rs.getInt("primary_index");
				this.inputLen = rs.getInt("scaling_length");
				this.units = rs.getString("primary_text");
			}

			@Override
			public String toString()
			{
				return String.format("Primary(%d,%d,%s)", index, inputLen, units);
			}
		}

		public static class Common
		{
			public static class EnumString
			{
				public final int value;
				public final String shortText;
				public final String longText;

				EnumString(ResultSet rs) throws SQLException
				{
					this.value = rs.getInt("value");
					this.shortText = rs.getString("short_name");
					this.longText = rs.getString("long_name");
				}
			}

			public final String units;
			public final int index;
			public final double[] constants;
			//public final EnumString[] enumStrings;
			public final Map<Object, EnumString> enums;

			Common(DBMaps dbMaps, ResultSet rs) throws SQLException
			{
				this.index = rs.getInt("common_index");
				this.units = rs.getString("common_text");
				this.constants = new double[rs.getInt("num_constants")];//new ArrayList<Double>();
				this.enums = new HashMap<>();

				//for (int ii = 1; ii <= rs.getInt("num_constants"); ii++)
				for (int ii = 0; ii < this.constants.length; ii++)
					this.constants[ii] = rs.getDouble("const" + (ii + 1));

				final DIPI dipi = new DIPI(rs.getInt("di"), rs.getInt("pi"));

				List<EnumString> list = dbMaps.enumString.get(dipi);

				if (list != null) {
					for (EnumString es : list) {
						enums.put(es.value, es);
						enums.put(es.shortText, es);
						enums.put(es.longText, es);
					}
				}
			}

			@Override
			public String toString()
			{
				return String.format("Common(%d,%s,%s,%s)", index, Arrays.toString(constants), units, enums);
			}
		}

		public enum DisplayFormat { Normal, Scientific }
		public enum DisplayLength { Short, Long }

		public final boolean controlledSetting;
		public final boolean motorController;
		public final DisplayFormat displayFormat;
		public final DisplayLength displayLength;

		public final Primary primary;
		public final Common common;

		ReadSetScaling(DBMaps dbMaps, ResultSet rs) throws SQLException
		{
			this.controlledSetting = rs.getBoolean("is_contr_setting");
			this.motorController = rs.getBoolean("is_step_motor");
			this.displayFormat = rs.getInt("display_format") == 1 ? DisplayFormat.Normal : DisplayFormat.Scientific;
			this.displayLength = rs.getInt("display_length") == 6 ? DisplayLength.Short : DisplayLength.Long;
			this.primary = new Primary(rs);
			this.common = new Common(dbMaps, rs);
		}
	}

	public static class Reading
	{
		public final PropertyInfo prop;
		public final ReadSetScaling scaling;

		Reading(DBMaps dbMaps, ResultSet rs) throws SQLException
		{
			this.prop = new PropertyInfo(dbMaps, rs);
			this.scaling = new ReadSetScaling(dbMaps, rs);
		}

		public boolean validScaling()
		{
			return scaling.primary.inputLen > 0;
		}

		@Override
		public String toString()
		{
			return prop + " " + (validScaling() ? String.format("Scaling(%s, %s)", scaling.primary, scaling.common) : "Scaling(None)");
		}
	}

	public static class Setting
	{
		public final PropertyInfo prop;
		public final ReadSetScaling scaling;

		Setting(DBMaps dbMaps, ResultSet rs) throws SQLException
		{
			this.prop = new PropertyInfo(dbMaps, rs);
			this.scaling = new ReadSetScaling(dbMaps, rs);
		}

		public boolean validScaling()
		{
			return scaling.primary.inputLen > 0;
		}

		@Override
		public String toString()
		{
			return prop + " " + (validScaling() ? String.format("Scaling(%s, %s)", scaling.primary, scaling.common) : "Scaling(None)");
		}
	}

	public static class AnalogAlarm
	{
		final public PropertyInfo prop;

		final public int flags;
		final public int minOrNom;
		final public int maxOrTol;
		final public int triesNeeded;
		final public int globalEvent;
		final public int subfunction;
		final public byte[] feData;
		final public int segment;
		final public String text;

		AnalogAlarm(DBMaps dbMaps, ResultSet rs) throws SQLException
		{
			this.prop = new PropertyInfo(dbMaps, rs);
			this.flags = rs.getInt("status");
			this.minOrNom = rs.getInt("min_or_nom");
			this.maxOrTol = rs.getInt("max_or_tol");
			this.triesNeeded = rs.getInt("tries_needed");
			this.globalEvent = rs.getInt("clock_event_no");
			this.subfunction = rs.getInt("subfunction_code");
			this.feData = rs.getBytes("specific_data");
			this.segment = rs.getInt("segment");
			this.text = rs.getString("analog_text");
		}

		@Override
		public String toString()
		{
			return prop + " " + String.format("(minNom:%d maxTol:%d)", minOrNom, maxOrTol);
		}
	}

	public static class DigitalAlarm
	{
		public static class Text
		{
			final int condition;
			final int mask;
			final String text;

			Text(ResultSet rs) throws SQLException
			{
				this.condition = rs.getInt("condition");
				this.mask = rs.getInt("mask");
				this.text = rs.getString("digital_text");
			}
		}

		final public PropertyInfo prop;

		final public int flags;
		final public int nominal;
		final public int mask;
		final public int triesNeeded;
		final public int globalEvent;
		final public int subfunction;
		final public byte[] feData;
		final public int segment;
		final public List<Text> text;

		DigitalAlarm(DBMaps dbMaps, ResultSet rs) throws SQLException
		{
			this.prop = new PropertyInfo(dbMaps, rs);
			this.flags = rs.getInt("status");
			this.nominal = rs.getInt("min_or_nom");;
			this.mask = rs.getInt("max_or_tol");
			this.triesNeeded = rs.getInt("tries_needed");
			this.globalEvent = rs.getInt("clock_event_no");
			this.subfunction = rs.getInt("subfunction_code");
			this.feData = rs.getBytes("specific_data");
			this.segment = rs.getInt("segment");
			this.text = new ArrayList<>();
		}

		@Override
		public String toString()
		{
			return prop + " " + String.format("(nom:%d)", nominal);
		}
	}

	public static class Status
	{
		public static class BitTextAttribute
		{
			final public int bitNo;
			final public int mask;
			final public String text0;
			final public String text1;
			final public String label;

			BitTextAttribute(ResultSet rs) throws SQLException
			{
				this.bitNo = rs.getInt("bit_no");
				this.mask = (1 << this.bitNo);
				this.text0 = rs.getString("short_name_0");
				this.text1 = rs.getString("short_name_1");
				this.label = rs.getString("bit_description");
			}
		}

		public static class Attribute
		{
			public static class Display
			{
				final public String text;
				final public String character;
				final public int color;

				Display(String prefix, ResultSet rs) throws SQLException
				{
					this.text = rs.getString(prefix + "_string");
					this.character = rs.getString(prefix + "_char");
					this.color = rs.getInt(prefix + "_color");
				}
			}

			final public int order;
			final public boolean inverted;
			final public long mask;
			final public long match;
			final public String shortName;
			final public String longName;
			final public Display trueDisplay;
			final public Display falseDisplay;

			Attribute(ResultSet rs) throws SQLException
			{
				this.order = rs.getInt("order_number");
				this.inverted = rs.getBoolean("invert_flag");
				this.mask = rs.getInt("mask_value");
				this.match = rs.getInt("match_value");
				this.shortName = rs.getString("short_name");
				this.longName = rs.getString("long_name");
				this.trueDisplay = new Display("true", rs);
				this.falseDisplay = new Display("false", rs);
			}

			@Override
			public String toString()
			{
				return shortName; // + "[" + trueDisplay + "/" + falseDisplay + "]";
			}
		}

		public final PropertyInfo prop;
		
		public final int inputLen;
		public final List<Attribute> attributes;
		public final List<BitTextAttribute> bitText;
		public final List<String> bitNames;

		Status(DBMaps dbMaps, ResultSet rs) throws SQLException
		{
			final int di = rs.getInt("di");

			this.prop = new PropertyInfo(dbMaps, rs);
			this.inputLen = inputLen(rs.getInt("scaling_length"));
			this.attributes = dbMaps.status.containsKey(di) ? dbMaps.status.get(di) : new ArrayList<>();
			this.bitText = dbMaps.statusBitText.containsKey(di) ? dbMaps.statusBitText.get(di) : new ArrayList<>();
			this.bitNames = bitNames();
		}

		private List<String> bitNames()
		{
			final ArrayList<String> names = new ArrayList<>();

			if (bitText != null) {
				for (BitTextAttribute bit : bitText) {
					if (bit.bitNo >= names.size()) {
						final int emptyCount = bit.bitNo - names.size();
						for (int ii = 0; ii < emptyCount; ii++)
							names.add("");
						names.add(bit.label);
					} else
						names.set(bit.bitNo, bit.label);
				}
			}

			return names;
		}

		private int inputLen(int len)
		{
			return len == 0 ? this.prop.atomicSize : len;
		}

		@Override
		public String toString()
		{
			final StringBuilder buf = new StringBuilder();

			for (Attribute attribute : attributes) {
				buf.append('"');
				buf.append(attribute);
				buf.append("\" ");
			}

			return buf.toString();
		}
	}

	public static class Control
	{
		public static class Attribute
		{
			public final int order;
			public final int value;
			public final String shortName;
			public final String longName;

			Attribute(ResultSet rs) throws SQLException
			{
				this.order = rs.getInt("order_number");
				this.value = rs.getInt("value");
				this.shortName = rs.getString("short_name");
				this.longName = rs.getString("long_name");
			}

			@Override
			public String toString()
			{
				return "Control.Attribute(" + order + "," + value + "," + shortName + "," + longName + ")";
			}
		}

		public final PropertyInfo prop;
		public final Attribute attributes[];

		Control(DBMaps dbMaps, ResultSet rs) throws SQLException
		{
			this.prop = new PropertyInfo(dbMaps, rs);

			final List<Attribute> tmp = dbMaps.control.get(rs.getInt("di"));
			this.attributes = tmp == null ? new Attribute[0] : tmp.toArray(new Attribute[0]);
		}
	}

	final public int di;
	final public String name;
	final public String description;
	final public String longName;
	final public String longDescription;
	final public long protectionMask;
	final public DeviceState state;
	//final public ControlSystemType type;

	final public Reading reading;
	final public Setting setting;
	final public Control control;
	final public Status status;
	final public AnalogAlarm analogAlarm;
	final public DigitalAlarm digitalAlarm;
	final public List<String> family;

	DeviceInfo(String name)
	{
		this.di = 0;
		this.name = name;
		this.description = "";
		this.longName = name;
		this.longDescription = "";
		this.protectionMask = 0;
		this.state = DeviceState.Active;
		this.reading = null;
		this.setting = null;
		this.control = null;
		this.status = null;
		this.analogAlarm = null;
		this.digitalAlarm = null;
		this.family = null;
	}

	DeviceInfo(DBMaps dbMaps, ResultSet rs) throws SQLException
	{
		this.di = rs.getInt("di");
		this.name = rs.getString("name");
		this.description = rs.getString("description");
		this.longName = rs.getString("long_name");
		this.longDescription = rs.getString("long_description");
		this.protectionMask = rs.getLong("protection_mask") & 0xffffffff;
		
		final int flags = rs.getInt("flags");

		if ((flags & 0x80) != 0)
			this.state = DeviceState.Deleted;
		else if ((flags & 0x40) != 0)
			this.state = DeviceState.Obsolete;
		else if ((flags & 0x20) != 0)
			this.state = DeviceState.Documented;
		else
			this.state = DeviceState.Active;

		this.family = dbMaps.family.get(this.di);

		Reading reading = null;
		Setting setting = null;
		Control control = null;
		Status status = null;
		AnalogAlarm analogAlarm = null;
		DigitalAlarm digitalAlarm = null;

		while (true) {
			switch (rs.getInt("pi")) {
			 case 1:
				analogAlarm = new AnalogAlarm(dbMaps, rs);
				break;
			 
			 case 3:
				control = new Control(dbMaps, rs);
				break;

			 case 4:
				status = new Status(dbMaps, rs);
				break;

			 case 5:
				if (digitalAlarm == null)
					digitalAlarm = new DigitalAlarm(dbMaps, rs);
				digitalAlarm.text.add(new DigitalAlarm.Text(rs));	
				break;

			 case 12:
				reading = new Reading(dbMaps, rs);
				break;

			 case 13:
				setting = new Setting(dbMaps, rs);
				break;
			}

			if (!rs.next() || rs.getInt("di") != this.di) {
				this.reading = reading;
				this.setting = setting;
				this.control = control;
				this.status = status;
				this.analogAlarm = analogAlarm;
				this.digitalAlarm = digitalAlarm;
				break;
			}
		}
	}

	@Override
	public String toString()
	{
		final StringBuilder buf = new StringBuilder(String.format("DeviceInfo(%d,%s,%s, mask:0x%08x)", di, name, description, protectionMask));

		buf.append("\nReading: ").append(reading); 
		buf.append("\nSetting: ").append(setting); 
		buf.append("\nAnalogAlarm: ").append(analogAlarm);
		buf.append("\nDigitalAlarm: ").append(digitalAlarm);
		buf.append("\nBasicStatus: ").append(status);

		return buf.toString();
	}
}
