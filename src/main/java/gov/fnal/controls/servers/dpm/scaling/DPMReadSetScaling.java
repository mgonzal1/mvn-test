// $Id: DPMReadSetScaling.java,v 1.15 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;

import gov.fnal.controls.servers.dpm.drf3.Property;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

public interface DPMReadSetScaling extends Scaling
{
	static final NoScaling noScaling = new NoScaling();

	default double rawToCommon(ByteBuffer data) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double rawToCommon(byte[] data, int offset) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double primaryToCommon(double value) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double[] rawToCommon(byte[] data, int offset, double[] values) throws AcnetStatusException
	{
		for (int ii = 0; ii < values.length; ii++, offset += L())
			values[ii] = rawToCommon(data, offset);

		return values;
	}


	default double[] rawToCommon(ByteBuffer data, double[] values) throws AcnetStatusException
	{
		for (int ii = 0; ii < values.length; ii++)
			values[ii] = rawToCommon(data);

		return values;
	}

	default double rawToPrimary(ByteBuffer data) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double rawToPrimary(byte[] data, int offset) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double rawToCommon(int data) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double[] rawToPrimary(ByteBuffer data, double[] values) throws AcnetStatusException
	{
		for (int ii = 0; ii < values.length; ii++)
			values[ii] = rawToPrimary(data);

		return values;
	}

	default double[] rawToPrimary(byte[] data, int offset, double[] values) throws AcnetStatusException
	{
		for (int ii = 0; ii < values.length; ii++, offset += L())
			values[ii] = rawToPrimary(data, offset);

		return values;
	}

	default byte[] primaryToRaw(double value) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default void primaryToRaw(double value, byte[] data, int offset) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default byte[] primaryToRaw(double[] values) throws AcnetStatusException
	{
		final byte[] data = new byte[L() * values.length];
		int offset = 0;

		for (final double value : values) {
			primaryToRaw(value, data, offset);
			offset += L();
		}

		return data;
	}

	default byte[] commonToRaw(double value) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default void commonToRaw(double value, byte[] data, int offset) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default byte[] commonToRaw(double[] values) throws AcnetStatusException
	{
		final byte[] data = new byte[L() * values.length];
		int offset = 0;

		for (final double value : values) {
			commonToRaw(value, data, offset);
			offset += L();
		}

		return data;
	}

	default double commonToPrimary(double value) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double[] commonToPrimary(double[] values, double[] newValues) throws AcnetStatusException
	{
		for (int ii = 0; ii < values.length; ii++)
			newValues[ii] = commonToPrimary(values[ii]);

		return newValues;
	}

	default byte[] stringToRaw(String value) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

    default void stringToRaw(String s, byte[] data, int offset) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

    default String rawToString(ByteBuffer data, int length) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default String rawToString(byte[] data, int offset, int length) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}
	
	default String[] rawToString(byte[] data, int offset, int length, String[] values) throws AcnetStatusException
	{
		for (int ii = 0; ii < values.length; ii++, offset += length)
			values[ii] = rawToString(data, offset, length);

		return values;
	}

	default String[] rawToString(ByteBuffer data, int length, String[] values) throws AcnetStatusException
	{
		for (int ii = 0; ii < values.length; ii++)
			values[ii] = rawToString(data, length);

		return values;
	}

	class NoScaling implements DPMReadSetScaling
	{
		@Override
		public double scale(byte[] data, int offset) throws AcnetStatusException
		{
			throw new AcnetStatusException(DIO_NOSCALE);
		}

		@Override
		public byte[] unscale(String data, int length) throws AcnetStatusException
		{
			throw new AcnetStatusException(DIO_NOSCALE);
		}

		@Override
		public byte[] unscale(double data, int length) throws AcnetStatusException
		{
			throw new AcnetStatusException(DIO_NOSCALE);
		}

		@Override
		public int L()
		{
			return 0;
		}
	}

	public static DPMReadSetScaling get(WhatDaq whatDaq)
	{
		final DeviceInfo dInfo = whatDaq.deviceInfo();
		final DeviceInfo.ReadSetScaling scaling;

		if (whatDaq.property() == Property.READING && dInfo.reading != null)
			return (DPMReadSetScaling) get(whatDaq, dInfo.reading.scaling); 
		else if (whatDaq.property() == Property.SETTING && dInfo.setting != null)
			return (DPMReadSetScaling) get(whatDaq, dInfo.setting.scaling); 

		return noScaling;
	}

	public static Scaling get(WhatDaq whatDaq, DeviceInfo.ReadSetScaling scaling)
	{
		final int len = whatDaq.rawInputLength();

		if (len == 4)
			return new DPMReadSetScalingImpl.L4(whatDaq, scaling);
		else if (len == 2)
			return new DPMReadSetScalingImpl.L2(whatDaq, scaling);
		else if (len == 1)
			return new DPMReadSetScalingImpl.L1(whatDaq, scaling);

		return noScaling;
	}

	int L();	
}

abstract class DPMReadSetScalingImpl extends ReadSetScalingExt implements DPMReadSetScaling
{
	abstract byte[] raw(int value);
	abstract void raw(int value, byte[] data, int offset);
	abstract int raw(final byte[] data, int offset);
	abstract int raw(ByteBuffer data);

	private DPMReadSetScalingImpl(WhatDaq whatDaq, DeviceInfo.ReadSetScaling scaling)
	{
		super(scaling);
	}

	@Override
	public double rawToCommon(ByteBuffer data) throws AcnetStatusException
	{
		return common.scale(primary.scale(raw(data)));
	}

	@Override
	public double rawToCommon(byte[] data, int offset) throws AcnetStatusException
	{
		return common.scale(primary.scale(raw(data, offset)));
	}

	@Override
	public double rawToPrimary(ByteBuffer data) throws AcnetStatusException
	{
		return primary.scale(raw(data));
	}

	@Override
	public double rawToPrimary(byte[] data, int offset) throws AcnetStatusException
	{
		return primary.unscale(raw(data, offset));
	}

	@Override
	public double primaryToCommon(double value) throws AcnetStatusException
	{
		return common.scale(value);	
	}

	@Override
	public byte[] primaryToRaw(double value) throws AcnetStatusException
	{
		return raw(primary.unscale(value));
	}

	@Override
	public void primaryToRaw(double value, byte[] data, int offset) throws AcnetStatusException
	{
		raw(primary.unscale(value), data, offset);
	}

	@Override
	public byte[] commonToRaw(double value) throws AcnetStatusException
	{
		return raw(primary.unscale(common.unscale(value)));
	}

	@Override
	public void commonToRaw(double value, byte[] data, int offset) throws AcnetStatusException
	{
		raw(primary.unscale(common.unscale(value)), data, offset);
	}

	@Override
	public double commonToPrimary(double value) throws AcnetStatusException
	{
		return common.unscale(value);
	}

	@Override
	public byte[] stringToRaw(String value) throws AcnetStatusException
	{
		return super.stringToRaw(value);
	}

	@Override
    public void stringToRaw(String s, byte[] data, int offset) throws AcnetStatusException
	{
		super.stringToRaw(s, data, offset);
	}

	@Override
    public String rawToString(ByteBuffer data, int length) throws AcnetStatusException
	{
		return super.rawToString(data, length);
	}

	@Override
    public String rawToString(byte[] data, int offset, int length) throws AcnetStatusException
	{
		return super.rawToString(data, offset, length);
	}

	static class L1 extends DPMReadSetScalingImpl
	{
		L1(WhatDaq whatDaq, DeviceInfo.ReadSetScaling scaling)
		{
			super(whatDaq, scaling);
		}

		@Override
		final public int L()
		{
			return 1;
		}

		@Override
		int raw(ByteBuffer data)
		{
			return data.get();
		}

		@Override
		int raw(final byte[] data, int offset)
		{
			return data[offset];
		}

		@Override
		byte[] raw(int value)
		{
			return new byte[] { (byte) value }; 
		}

		@Override
		void raw(int value, byte[] data, int offset)
		{
			data[offset] = (byte) value;
		}
	}

	static class L2 extends DPMReadSetScalingImpl
	{
		L2(WhatDaq whatDaq, DeviceInfo.ReadSetScaling scaling)// throws AcnetStatusException
		{
			super(whatDaq, scaling);
		}

		@Override
		final public int L()
		{
			return 2;
		}

		@Override
		int raw(ByteBuffer data)
		{
			return data.getShort() & 0xffff;
		}

		@Override
		int raw(final byte[] data, int offset)
		{
            return (data[offset++] & 0xff) | (data[offset] << 8);
		}

		@Override
		byte[] raw(int value)
		{
			return new byte[] { (byte) value, (byte) (value >> 8) }; 
		}

		@Override
		void raw(int value, byte[] data, int offset)
		{
			data[offset] = (byte) value;
			data[offset + 1] = (byte) (value >> 8);
		}
	}

	static class L4 extends DPMReadSetScalingImpl
	{
		L4(WhatDaq whatDaq, DeviceInfo.ReadSetScaling scaling)
		{
			super(whatDaq, scaling);
		}

		@Override
		final public int L()
		{
			return 4;
		}

		@Override
		int raw(ByteBuffer data)
		{
			return data.getInt();
		}

		@Override
		int raw(final byte[] data, int offset)
		{
            return (data[offset++] & 0xff) |
                    ((data[offset++] & 0xff) << 8) |
                    ((data[offset++] & 0xff) << 16) |
                    ((data[offset] & 0xff) << 24);
		}

		@Override
		byte[] raw(int value)
		{
			return new byte[] { (byte) value, (byte) (value >> 8),
								(byte) (value >> 16), (byte) (value >> 24) };
		}

		@Override
		void raw(int value, byte[] data, int offset)
		{
			data[offset++] = (byte) value;
			data[offset++] = (byte) (value >> 8);
			data[offset++] = (byte) (value >> 16);
			data[offset] = (byte) (value >> 24);
		}
	}
}
