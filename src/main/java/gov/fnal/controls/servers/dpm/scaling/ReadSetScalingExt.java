// $Id: ReadSetScalingExt.java,v 1.5 2024/02/22 16:33:36 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.pools.DeviceInfo;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.pools.DeviceCache;
import gov.fnal.controls.servers.dpm.Errors;
import gov.fnal.controls.servers.dpm.events.ClockEvent;

class ReadSetScalingExt extends ReadSetScaling
{
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
					//return Lookup.getDeviceInfo(rawReading(data, length)).name;
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
				String ev_name = ClockEvent.clockEventNumberToName(rawReading(data, length));
				if (ev_name.compareTo("Undefined") == 0) {
					throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate Scaling Failed no such Clock Event");
				} 
				return Integer.toHexString(rawReading(data, length)&0xffff).toUpperCase();

			default:
				return Integer.toString(rawReading(data, length));
		}
	}

	@Override
	public byte[] unscale(String value, int length) throws AcnetStatusException
	{
		return unscale(value);
	}

	byte[] stringToRaw(String s) throws AcnetStatusException
	{
		return unscale(s);
	}

	void stringToRaw(String s, byte[] data, int offset) throws AcnetStatusException
	{
		final byte[] tmp = unscale(s);

		for (int ii = 0; ii < tmp.length; ii++)
			data[offset++] = tmp[ii];
	}

	private byte[] unscale(String str) throws AcnetStatusException 
	{
		String sherr = null, sherr1 = null, sherr2 = null;
		short sh = 0;
		int v = 0, il = 0, jl = 0;
		long vl = 0L;
		short[] trunk_node = { 0, 0 };
		byte[] b_y_t_e_s = { 0, 0 };
		char char_at_1 = 0;	// default for CNV_SHORT_HEX,CNV_LONG_HEX
		StringBuffer sbw = null;
		int il_start = 0, il_end = 0;
		int ip_address_byte_length = 4;

		if (scaling.common.index == 84) {
			b_y_t_e_s = unscale_2 ( str );
			return (b_y_t_e_s);
		}
		// check transforms if appropriate throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate UnScaling Failed")
		if (scaling.common.index != 60)
			//throw new AcnetStatusException(DIO_SCALEFAIL, "Alternate UnScaling Failed");
			throw new AcnetStatusException(DIO_SCALEFAIL);

		int c0 = (int) scaling.common.constants[0];

		switch (c0) {
		case 2: // CNV_SHORT_HEX
			try {
				if (str.length() > 1)
					char_at_1 = str.charAt(1);
				if ((char_at_1 != 'x') && (char_at_1 != 'X'))
					sh = (short) (Integer.decode("0x" + str) & 0xffff);// in order to decode negatives
				else
					sh = (short) (Integer.decode(str) & 0xffff);
			} catch (NumberFormatException ex) {
				System.out.println("NumberFormatException" + ex);
				ex.printStackTrace();
			}
			b_y_t_e_s = raw(sh, 2);
			break;

		case 3: // CNV_LONG_HEX
			try {
				if (str.length() > 1)
					char_at_1 = str.charAt(1);
				if ((char_at_1 != 'x') && (char_at_1 != 'X'))
					v = (int) (Long.decode("0x" + str) & 0xffffffff);// in order to decode negatives
				else
					v = (int) (Long.decode(str) & 0xffffffff);
			} catch (NumberFormatException ex) {
				System.out.println("NumberFormatException" + ex);
				ex.printStackTrace();
			}
			b_y_t_e_s = raw(v, 4);
			break;

		case 8: // CNV_CHAR
			b_y_t_e_s = new byte[str.length()];
			for (int ii = 0; ii < str.length(); ii++)
				b_y_t_e_s[il] = (byte) str.charAt(ii);
			break;

		case 12: // CNV_ENUMERATED
			//v = EnumeratedStrings.getEnumeratedSetting(di, pi, str);
			v = getEnumSetting(str);
			b_y_t_e_s = raw(v, 4);
			break;

		case 288: // CNV_BYTE_ENUMERATED
			//v = EnumeratedStrings.getEnumeratedSetting(di, pi, str);
			v = getEnumSetting(str);
			b_y_t_e_s = raw(v, 1);
			break;

		case 289: // CNV_SHORT_ENUMERATED
			//v = EnumeratedStrings.getEnumeratedSetting(di, pi, str);
			v = getEnumSetting(str);
			b_y_t_e_s = raw(v, 2);
			break;

		case 347: // CNV_IP_ADDRESS : "aaa.bbb.ccc.ddd"
			b_y_t_e_s = new byte[ip_address_byte_length];
			sbw = new StringBuffer(str);
			for (int ii = 0; ii < ip_address_byte_length; ii++) {
			    v = 0;
			    if (ii < 3) {
				il_end = sbw.indexOf(".", il_start);
//		System.out.println("il = "+il+" il_end = " + il_end);
				if (il_end < 0 )
				    throw new AcnetStatusException(DIO_SCALEFAIL, "Wrong format of IP-address");
				}
			    else 
				il_end = sbw.length();
			    for (jl = il_start; jl < il_end; jl++) {
				char_at_1 = sbw.charAt(jl);
//		System.out.println("char = "+char_at_1);
				if ( (char_at_1 > '9') || (char_at_1 < '0') )
				    throw new AcnetStatusException(DIO_SCALEFAIL, "Wrong format of IP-address");
				v = 10*v + char_at_1-'0';
				}
			    if ( v > 255 )
				    throw new AcnetStatusException(DIO_SCALEFAIL, "Wrong format of IP-address");
			    b_y_t_e_s[il] = (byte)v;
			    il_start = il_end+1;
			    }
			break;

		case 15: // CNV_NODE
			final Node node = Node.get(str);
			b_y_t_e_s = new byte[2];
			b_y_t_e_s[1] = (byte) (node.value() >> 8); //(trunk_node[0] & (short) 0x00FF);
			b_y_t_e_s[0] = (byte) node.value(); //(trunk_node[1] & (short) 0x00FF);
			break;

		case 17: // CNV_DEVICE
			try {
				//final Lookup.DeviceInfo dInfo = Lookup.getDeviceInfo(str);

				//b_y_t_e_s = raw(dInfo.di, 4);
				b_y_t_e_s = raw(DeviceCache.di(str), 4);
			} catch (Exception e) {
			}
			//DeviceNameIndex dni = new DeviceNameIndex(str);
			//v = dni.getDeviceIndex();
			//b_y_t_e_s = raw(v, 4);
			break;

		case 21: // CNV_ERROR
			v = Errors.value(str);
			b_y_t_e_s = new byte[2];
			b_y_t_e_s[1] = (byte) ((v & (int) 0xFF00) >> 8);
			b_y_t_e_s[0] = (byte) (v & (int) 0xFF);
			break;

		case 22: // CNV_SHORT_ERROR
			{
				b_y_t_e_s = new byte[2];
				sherr = str;
				int ii;
				for (ii = 0; ii < sherr.length(); ii++)
					if (sherr.charAt(ii) != ' ')
						break;

				if (il > 0)
					sherr1 = sherr.substring(il);
				else
					sherr1 = sherr;

				ii = sherr1.indexOf(' '); //        if (il== 0) exception
				sherr2 = sherr1.substring(0, ii);
				b_y_t_e_s[0] = (byte) Integer.decode(sherr2).intValue(); // facility
				for (jl = ii; jl < sherr1.length(); jl++)
					if (sherr1.charAt(jl) != ' ')
						break;
				sherr2 = sherr1.substring(jl, sherr1.length());
				b_y_t_e_s[1] = (byte) Integer.decode(sherr2).intValue(); // error number
			}
			break;

		case 29: // CNV_CLINKS
			// dd-mmm-yyyy hh:mm:ss GMT(?) to clinks
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
			try {
				vl = (((DateFormat) sdf).parse(str)).getTime() - (730 * 24
						* 3600 * 1000L);
			} catch (ParseException ex) {
				System.out.println("Date Format ParseException" + ex);
				ex.printStackTrace();
			}
			Calendar cal = Calendar.getInstance();
			vl += cal.get(Calendar.ZONE_OFFSET);
			v = (int) (vl / 1000L);
			
			b_y_t_e_s = raw(v, 4);
			break;

		case 280: // CNV_GMT_FMT_CLINKS
			// dd-mmm-yyyy hh:mm:ss GMT(?) to clinks
			SimpleDateFormat gsdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
			try {
				vl = (((DateFormat) gsdf).parse(str)).getTime();
			} catch (ParseException ex) {
				System.out.println("Date Format ParseException" + ex);
				ex.printStackTrace();
			}
			v = (int) (vl / 1000L);
			b_y_t_e_s = raw(v, 4);
			break;

		case 669: // CNV_CLOCK_EVENT_HEX
			try {
				if (str.length() > 1)
					char_at_1 = str.charAt(1);
				if ((char_at_1 != 'x') && (char_at_1 != 'X'))
					v = (int) (Long.decode("0x" + str) & 0xffffffff);
				else
					v = (int) (Long.decode(str) & 0xffffffff);
			} catch (NumberFormatException ex) {
				System.out.println("NumberFormatException" + ex);
				ex.printStackTrace();
			}
			String ev_name = ClockEvent.clockEventNumberToName( v );
			if(ev_name.compareTo("Undefined") == 0) {
				throw new AcnetStatusException(DIO_SCALEFAIL,
					"Unscaling Failed no such Clock Event");
			    } 
			v = swapABCDtoCDAB ( v);
			b_y_t_e_s = raw(v, 4);
			break;

		default:
			throw new AcnetStatusException(DIO_SCALEFAIL, "Setting a byte[] not yet implemented");
		}

		return b_y_t_e_s;
	}

	private byte[] unscale_2(String str) throws AcnetStatusException 
	{
		final double[] constants = scaling.common.constants;

		switch ((int) constants[0]) {
			case 472: // special conversion
				// dd-mmm-yyyy hh:mm:ss GMT(?) to clinks
				if (str.length() < 8 )
					throw new AcnetStatusException(DIO_SCALEFAIL,
						"Alternate unScaling Failed c0=472,length < 8");

				final StringBuilder buf = new StringBuilder(str);

				final double hours   = ((int) (buf.charAt(0)) - '0') * 10 + ((int)(buf.charAt(1))-'0');
				final double minutes = ((int) (buf.charAt(3)) - '0') * 10 + ((int)(buf.charAt(4))-'0');
				final double seconds = ((int) (buf.charAt(6)) - '0') * 10 + ((int)(buf.charAt(7))-'0');
				final double flt_hours = hours + minutes / 60.0 + seconds / 3600.0;		

				if (constants[3] == 0.0)
					throw new AcnetStatusException(DIO_SCALEFAIL);

				final int rawData = primary.unscale((flt_hours - constants[5]) * constants[4] / constants[3]);	/* convert to raw data */

				return raw(rawData, 2);
		}

		throw new AcnetStatusException(DIO_SCALEFAIL);
	}

	/**
	 * Return swapped raw data: 0xABCD -> 0xBADC
	 * 
	 * @return BADC.
	 */
	private int swapABCDtoBADC (Number val) 
	{
		int x  = val.intValue();
		int x1 = (x << 8) & 0xFF000000;		    // x1 = B000

		x1  |= ((x & 0xFF000000) >> 8) & 0x00FF0000;// x1 = BA00
		x1  |= ((x & 0x000000FF) << 8) & 0x0000FF00;// x1 = BAD0
		x1  |= ((x & 0x0000FF00) >> 8) & 0x000000FF;// x1 = BADC

		return x1;
	}

	/**
	 * Return swapped raw data: ABCD -> CDAB
	 * 
	 * @return CDAB.
	 */
	private int swapABCDtoCDAB (Number val) 
	{
		int x  = val.intValue();
		int x1 = ((x & 0xFFFF0000) >> 16) & 0x0000FFFF;// x1 = 00AB
		int x2 = ((x & 0x0000FFFF) << 16) & 0xFFFF0000;// x2 = CD00

		return x1 | x2;
	}
}
