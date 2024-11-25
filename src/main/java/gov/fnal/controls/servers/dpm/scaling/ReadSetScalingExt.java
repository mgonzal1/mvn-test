// $Id: ReadSetScalingExt.java,v 1.9 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import java.util.Date;
import java.util.HashMap;
//import java.text.DateFormat;
//import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.sql.ResultSet;

import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.pools.DeviceCache;
import gov.fnal.controls.servers.dpm.Errors;
import gov.fnal.controls.db.DbServer;
import static gov.fnal.controls.db.DbServer.getDbServer;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class ReadSetScalingExt extends ReadSetScaling
{
	static final HashMap<Integer, String> clockNumberToName = new HashMap<>();
	static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

	ReadSetScalingExt(DeviceInfo.ReadSetScaling scaling)
	{
		super(scaling);
	}

	String rawToString(byte[] data, int offset, int length) throws AcnetStatusException 
	{
		return rawToString(ByteBuffer.wrap(data, offset, length), length);
	}

	private final int rawReading(ByteBuffer data, int length) throws AcnetStatusException
	{
		final int raw = raw(data, length);

		switch ((int) scaling.common.constants[2]) {
			case 0:
				return raw;

			case 1: 
				return swapABCDtoBADC(raw);

			case 2: 
				return swapABCDtoCDAB(raw);

			case 3:
				return swapABCDtoDCBA(raw);

			default:
				throw new AcnetStatusException(DIO_SCALEFAIL);
		}
	}

	private String getEnumString(int value) throws AcnetStatusException
	{
		final DeviceInfo.ReadSetScaling.Common.EnumString es = scaling.common.enums.get(value);

		if (es != null)
			return es.shortText;

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	private int getEnumSetting(String value) throws AcnetStatusException
	{
		final DeviceInfo.ReadSetScaling.Common.EnumString es = scaling.common.enums.get(value);

		if (es != null)
			return es.value;

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	String rawToString(ByteBuffer data, int length) throws AcnetStatusException 
	{
		final double[] constants = scaling.common.constants;

		if (scaling.common.index == 84) {
			if ((int) constants[0] == 472 && length > 1 && constants[4] != 0.0) {
				final int seconds = (int) ((constants[3] * primary.scale(raw(data, length)) / constants[4]) + constants[5] * 3600 + 0.5);

				return String.format("%02:%02:%02", seconds / 3600, (seconds % 3600) / 60, seconds % 60); 
			}

			throw new AcnetStatusException(DIO_SCALEFAIL);
		}

		final int c1 = (int) constants[1];
        final int c2 = (int) constants[2];

		switch ((int) constants[0]) {
			case 2: // CNV_SHORT_HEX
				if (length < 2) //2 = short size in bytes
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=2,length < 2");
				return Integer.toHexString(rawReading(data, length) & 0xffff);

			case 3: // CNV_LONG_HEX
				if (length < 4) //4 = long size in bytes
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=3,length < 4");
				return Integer.toHexString(rawReading(data, length));

			case 8: // CNV_CHAR
				{
					if (((c1 > 0) && (length > c1)) || (c2 >= 2) || (c2 < 0))
						throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=8,length > c1 or bad c2");

					final byte[] raw = new byte[length];
					final StringBuilder buf = new StringBuilder();

					data.get(raw);

					if (c2 == 1) {
						for (int ii = 0; ii < length; ii += 2) {
							buf.append((char) raw[ii + 1]);
							buf.append((char) raw[ii]);
						}

						if (length % 2 == 1)
							buf.append((char) raw[length-1]);
					} else {
						for (int ii = 0; ii < length; ii++) {
							if (raw[ii] == 0)
								break;
							buf.append((char) raw[ii]);
						}
					}

					return buf.toString();
				}

			case 12: // CNV_ENUMERATED
				if (length < 4) //4 = long size in bytes for the value
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=12,length < 4");
				//return EnumeratedStrings.getEnumeratedString(di, pi, rawReading(data, length));
				return getEnumString(rawReading(data, length));

			case 288: // CNV_BYTE_ENUMERATED
				if (length != 1) //1 = size in bytes for the value
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=12,length != 1");
				return getEnumString(rawReading(data, length));
				//return EnumeratedStrings.getEnumeratedString(di, pi, rawReading(data, length));

			case 289: // CNV_SHORT_ENUMERATED
				if (length != 2) //2 = size in bytes for the value
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=12,length != 2");
				return getEnumString(rawReading(data, length));
				//return EnumeratedStrings.getEnumeratedString(di, pi, rawReading(data, length));

			case 347: // CNV_IP_ADDRESS
				{
					if (length < 4)	//4 = long size in bytes
						throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=347,length < 4");

					final StringBuilder buf = new StringBuilder();
					int raw = rawReading(data, length);

					for (int ii = 0; ii < 4; ii++) {
						buf.append(Integer.toString(raw & 0xff));
						if (ii < 3 )
							buf.append((char)'.');

						raw >>= 8; 
					}
					return buf.toString();
				}
				
			case 15: // CNV_NODE
				if ((length < 2) || (c2 >= 2) || (c2 < 0)) //2 = short size in bytes
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=15,length <2 or bad c2");

				final int trunk, node;
				final byte[] raw = new byte[length];

				data.get(raw);

				if (c2 == 1) {
					node = raw[1] & 0xff;
					trunk = raw[0] & 0xff;
				} else {
					node = raw[0] & 0xff;
					trunk = raw[1] & 0xff;
				}

				return Node.get(((trunk << 8) | node) & 0xffff).name();

			case 17: // CNV_DEVICE
				if (length < 4) //2 = short size in bytes
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=17,length < 4");

				try {
					return DeviceCache.name(rawReading(data, length));
				} catch (Exception ignore) { }

				return "";

			case 21: // CNV_ERROR
				if (length < 2) //2 = short size in bytes
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=21,length < 2");
				return  Errors.name(rawReading(data, length));
				
			case 22: // CNV_SHORT_ERROR
				if (length < 2) //2 = short size in bytes
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=22, length < 2");
				return Errors.numericName(rawReading(data, length));
				
			case 29: // CNV_CLINKS
				{
					if (length < 4) //2 = short size in bytes
						throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=29,length < 4");
					//convert to milliseconds since 01.01.1970
					final Calendar cal = Calendar.getInstance();
					final Date d = new Date((((rawReading(data, length) & 0xffffffff) + (730 * 24 * 3600)) * 1000) - cal.get(Calendar.ZONE_OFFSET));
					final String s = d.toString();
					// DOW Mon dd hh:mm:ss yyyy -> dd-mmm-yyyy hh:mm:ss
					return  s.substring(8, 10) + "-" + s.substring(4, 7) + "-" + s.substring(24, 28) + s.substring(10, 19);
				}
				
			case 280: // CNV_GMT_FMT_CLINKS
				{
					if (length < 4) //2 = short size in bytes
						throw new AcnetStatusException(DIO_SCALEFAIL);

					final String s = (new Date((rawReading(data, length) & 0xffffffff) * 1000)).toString();
					// DOW Mon dd hh:mm:ss yyyy -> dd-mmm-yyyy hh:mm:ss
					return  s.substring(8, 10) + "-" + s.substring(4, 7) + "-" + s.substring(24, 28) + s.substring(10, 19);
				}
				
			case 669: // CNV_CLOCK_EVENT_HEX
				if (length < 4) 	//4 = size in bytes
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed c0=3,length < 4");

				final String name = clockNumberToName.get(rawReading(data, length));
				if (name == null)
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed no such Clock Event");
				
				return Integer.toHexString(rawReading(data, length) & 0xffff).toUpperCase();

			default:
				return Integer.toString(rawReading(data, length));
		}
	}

	@Override
	public byte[] unscale(String value, int length) throws AcnetStatusException
	{
		return stringToRaw(value);
	}

	void stringToRaw(String s, byte[] data, int offset) throws AcnetStatusException
	{
		final byte[] tmp = stringToRaw(s);

		for (int ii = 0; ii < tmp.length; ii++)
			data[offset++] = tmp[ii];
	}

	byte[] stringToRaw(String str) throws AcnetStatusException 
	{
		if (scaling.common.index == 84)
			return unscale2(str);
		
		if (scaling.common.index != 60)
			throw new AcnetStatusException(DIO_SCALEFAIL);

		switch ((int) scaling.common.constants[0]) {
		case 2: // CNV_SHORT_HEX
			try {

				return raw(Integer.decode(str) & 0xffff, 2);
			} catch (Exception e) {
				throw new AcnetStatusException(DIO_SCALEFAIL);
			}

		case 3: // CNV_LONG_HEX
			try {
				return raw((int) (Long.decode(str) & 0xffffffff), 4);
			} catch (NumberFormatException e) {
				throw new AcnetStatusException(DIO_SCALEFAIL);
			}

		case 8: // CNV_CHAR
			{
				final byte[] buf = new byte[str.length()];

				for (int ii = 0; ii < str.length(); ii++)
					buf[ii] = (byte) str.charAt(ii);

				return buf;
			}

		case 12: // CNV_ENUMERATED
			return raw(getEnumSetting(str), 4);

		case 288: // CNV_BYTE_ENUMERATED
			return raw(getEnumSetting(str), 1);

		case 289: // CNV_SHORT_ENUMERATED
			return raw(getEnumSetting(str), 2);

		case 347: // CNV_IP_ADDRESS : "aaa.bbb.ccc.ddd"
			try {
				final byte buf[] = new byte[4];
				final String octs[] = str.split(".");

				for (int ii = 0; ii < buf.length; ii++)
					buf[ii] = Byte.decode(octs[ii]);

				return buf;
				
			} catch (Exception e) {
				throw new AcnetStatusException(DIO_SCALEFAIL);
			}

		case 15: // CNV_NODE
			{
				final Node node = Node.get(str);
				final byte[] buf = new byte[2];

				buf[1] = (byte) (node.value() >> 8);
				buf[0] = (byte) node.value();

				return buf;
			}

		case 17: // CNV_DEVICE
			try {
				return raw(DeviceCache.di(str), 4);
			} catch (Exception e) {
				throw new AcnetStatusException(DIO_SCALEFAIL);
			}

		case 21: // CNV_ERROR
			{
				final int v = Errors.value(str);
				final byte[] buf = new byte[2];
				buf[1] = (byte) (v >> 8);
				buf[0] = (byte) v;

				return buf;
			}

		case 22: // CNV_SHORT_ERROR
			try {
				final byte[] buf = new byte[2];
				final String split[] = str.split(" ");

				for (int ii = 0; ii < buf.length; ii++)
					buf[ii] = Byte.decode(split[ii]);

				return buf;
			} catch (Exception e) {
				throw new AcnetStatusException(DIO_SCALEFAIL);
			}

		case 29: // CNV_CLINKS
			// dd-mmm-yyyy hh:mm:ss GMT(?) to clinks
			{
				try {
					final long v = dateFormat.parse(str).getTime() - (730 * 24 * 3600 * 1000L) +
									Calendar.getInstance().get(Calendar.ZONE_OFFSET);
					return raw((int) (v / 1000), 4);
				} catch (Exception e) {
					throw new AcnetStatusException(DIO_SCALEFAIL);
				}
			}

		case 280: // CNV_GMT_FMT_CLINKS
			try {
				return raw((int) (dateFormat.parse(str).getTime() / 1000), 4);
			} catch (Exception e) {
				throw new AcnetStatusException(DIO_SCALEFAIL);
			}

		case 669: // CNV_CLOCK_EVENT_HEX
			{
				try {
					return raw(swapABCDtoCDAB((int) (Long.decode(str) & 0xffffffff)), 4);
				} catch (Exception e) {
					throw new AcnetStatusException(DIO_SCALEFAIL);
				}
			}

		default:
			throw new AcnetStatusException(DIO_SCALEFAIL);
		}
	}

	private byte[] unscale2(String s) throws AcnetStatusException 
	{
		final double[] constants = scaling.common.constants;

		switch ((int) constants[0]) {
			case 472: // special conversion
				// dd-mmm-yyyy hh:mm:ss GMT(?) to clinks
				if (s.length() < 8 || constants[3] == 0.0)
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alt unscaling failed c0=472");

				final double hours = ((int) (s.charAt(0)) - '0') * 10 + ((int) (s.charAt(1)) - '0');
				final double minutes = ((int) (s.charAt(3)) - '0') * 10 + ((int) (s.charAt(4)) - '0');
				final double seconds = ((int) (s.charAt(6)) - '0') * 10 + ((int) (s.charAt(7)) - '0');
				final double fhours = hours + minutes / 60.0 + seconds / 3600.0;		

				return raw(primary.unscale((fhours - constants[5]) * constants[4] / constants[3]), 2);
		}

		throw new AcnetStatusException(DIO_SCALEFAIL);
	}

	private static final int swapABCDtoBADC(int x) 
	{
		final int l = (x << 8);
		final int r = (x >>> 8);

		return (l & 0xff000000) | (r & 0x00ff0000) | (l & 0x0000ff00) | (r & 0x000000ff);
	}

	private static final int swapABCDtoCDAB(int x) 
	{
		return (x >>> 16) | (x << 16);
	}

	private static final int swapABCDtoDCBA(int x)
	{
		return (x >>> 24) | ((x >>> 8) & 0xff00) | ((x << 8) & 0xff0000) | (x << 24);
	}

	static void getClockEventInfo()
	{
		final String query = "SELECT symbolic_name, long_text, event_number FROM hendricks.clock_events where event_type = 0";

		try {
			final ResultSet rs = getDbServer("adbs").executeQuery(query);

			while (rs.next())
				clockNumberToName.put(rs.getInt(3), rs.getString(1));
			
			rs.close();
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception in getClockEventInfo()", e);
		}
	}
}
