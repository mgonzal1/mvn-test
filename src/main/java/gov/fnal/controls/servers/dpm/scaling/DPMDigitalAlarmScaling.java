// $Id: DPMDigitalAlarmScaling.java,v 1.9 2024/01/24 21:15:41 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

public interface DPMDigitalAlarmScaling extends Scaling
{
	//default void scale(byte[] data, int offset) throws AcnetStatusException
	//{
	//	throw new AcnetStatusException(DIO_NOSCALE);
	//}
	default double scale(ByteBuffer data) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default String textValue() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default int nom() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default int mask() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default boolean enabled() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default boolean inAlarm() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default int triesNeeded() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default int triesNow() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default boolean abortCapable() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default boolean abortInhibited() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double ftd() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double flags() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	class NoScaling implements DPMDigitalAlarmScaling
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
	}

	public static DPMDigitalAlarmScaling get(WhatDaq whatDaq)
	{
		try {
			return new DPMDigitalAlarmScalingImpl(whatDaq);
		} catch (Exception ignore) { }

		return new NoScaling();
	}
}

class DPMDigitalAlarmScalingImpl extends DigitalAlarmScaling implements DPMDigitalAlarmScaling
{
    private final int ftd;
    private final int length;

	DPMDigitalAlarmScalingImpl(WhatDaq whatDaq) throws AcnetStatusException
	{
		super(whatDaq.dInfo);

		this.ftd = whatDaq.pInfo.ftd;
		this.length = whatDaq.getLength();
	}

/*
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
*/
/*
	@Override
	public double scale(ByteBuffer data) throws AcnetStatusException
	{
		return scale(data);
	}
	*/

	@Override
	public double scale(byte[] data, int offset) throws AcnetStatusException
	{
		return scale(data, offset);
	}

	@Override
	public String textValue()
	{
		return isEnabled() ? (super.inAlarm() ? "IN-ALARM" : "") : "BYPASSED";
	}

	@Override
	public int nom()
	{
       	return super.nom();
	} 

	@Override
	public int mask()
	{
        return super.mask();
	}

	@Override
	public boolean enabled()
	{
        return isEnabled();
	}

	@Override
	public boolean inAlarm()
	{
        return super.inAlarm();
	}

	@Override
	public int triesNeeded()
	{
        return super.triesNeeded();
	}

	@Override
	public int triesNow()
	{
        return super.triesNow();
	}

	@Override
	public boolean abortCapable()
	{
        return isAbortCapable();
	}

	@Override
	public boolean abortInhibited()
	{
        return super.abortInhibited();
	}

	@Override
	public double ftd()
	{
        return ftd;
	}

	@Override
	public double flags()
	{
        return status;
	}
}
