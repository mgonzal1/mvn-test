// $Id: Scaling.java,v 1.9 2024/01/05 21:31:06 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
//import gov.fnal.controls.service.proto.Lookup_v2;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;

import gov.fnal.controls.servers.dpm.drf3.Property;

public interface Scaling extends AcnetErrors
{
	public double scale(byte[] data, int offset) throws AcnetStatusException;
	public byte[] unscale(double value, int length) throws AcnetStatusException;
	public byte[] unscale(String value, int length) throws AcnetStatusException;

	default public double scale(ByteBuffer buf) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default public byte[] unscale(double[] data, int length) throws AcnetStatusException
	{
		final byte[] raw = new byte[length * data.length];

		for (int ii = 0, offset = 0; ii < data.length; ii++, offset += length)
			System.arraycopy(unscale(data[ii], length), 0, raw, offset, length);

		return raw;
	}

	default public byte[] unscale(String[] data, int length) throws AcnetStatusException
	{
		final byte[] raw = new byte[length * data.length];

		for (int ii = 0, offset = 0; ii < data.length; ii++, offset += length)
			System.arraycopy(unscale(data[ii], length), 0, raw, offset, length);

		return raw;
	}

	default public int raw(final ByteBuffer data, int length) throws AcnetStatusException
	{
        if (length == 2)
			return data.getShort() & 0xffff;
        else if (length == 4)
			return data.getInt();
        else if (length == 1)
            return data.get() & 0xff;

        throw new AcnetStatusException(DIO_SCALEFAIL);
	}

    default public int raw(final byte[] data, int offset, int length) throws AcnetStatusException
    {
        if (length == 2)
            return (data[offset++] & 0xff) | 
                    (data[offset] << 8);
        else if (length == 4)
            return (data[offset++] & 0xff) |
                    ((data[offset++] & 0xff) << 8) |
                    ((data[offset++] & 0xff) << 16) |
                    ((data[offset] & 0xff) << 24);                                                                                                                             
        else if (length == 1)
            return data[offset] & 0xff;

        throw new AcnetStatusException(DIO_SCALEFAIL);
    }

    default public byte[] raw(int value, int length) throws AcnetStatusException
    {
        if (length <= 0 || length > 4)
            throw new AcnetStatusException(DIO_SCALEFAIL);

        final byte[] raw = new byte[length];

        for (int ii = 0; ii < length; ii++, value >>= 8)
            raw[ii] = (byte) (value & 0xff);

        return raw;
    }

	default public int rawNoSignExt(byte[] data, int offset, int length) throws AcnetStatusException
	{
		final int raw = raw(data, offset, length);

		if (length == 1)
			return raw & 0xff;
		else if (length == 2)
			return raw & 0xffff;

		return raw;
	}

	default public int rawNoSignExt(ByteBuffer data, int length) throws AcnetStatusException
	{
		if (length == 1)
			return data.get() & 0xff;
		else if (length == 2)
			return data.getShort() & 0xffff;
		else
			return data.getInt();
	}

	//public static boolean usesStrings(Lookup_v2.DeviceInfo dInfo, Property prop)
	public static boolean usesStrings(DeviceInfo dInfo, Property prop)
	{
		if (prop == Property.READING && dInfo.reading != null && dInfo.reading.scaling != null)
			return dInfo.reading.scaling.primary.index == 68;
		else if (prop == Property.SETTING && dInfo.setting != null && dInfo.setting.scaling != null)
			return dInfo.setting.scaling.primary.index == 68;

		return false;
	}
}

class NoScaling implements Scaling
{
	@Override
	public double scale(byte[] data, int offset) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public byte[] unscale(double value, int length) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public byte[] unscale(String value, int length) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}
}

