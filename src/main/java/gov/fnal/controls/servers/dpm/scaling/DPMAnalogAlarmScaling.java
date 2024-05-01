// $Id: DPMAnalogAlarmScaling.java,v 1.11 2024/01/05 21:31:06 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

//import gov.fnal.controls.service.proto.Lookup_v2;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;

public interface DPMAnalogAlarmScaling extends Scaling
{
	static final NoScaling noScaling = new NoScaling();

	default String textValue() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double min() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double max() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double rawMin() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double rawMax() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double nom() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double tol() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double rawNom() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double rawTol() throws AcnetStatusException
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

	default double ftd() throws AcnetStatusException
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

	default double flags() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	class NoScaling implements DPMAnalogAlarmScaling
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

	public static DPMAnalogAlarmScaling get(WhatDaq whatDaq)
	{
		//final Lookup_v2.ReadSetScaling scaling;
		final DeviceInfo.ReadSetScaling scaling;

		if (whatDaq.dInfo.analogAlarm != null && whatDaq.dInfo.reading != null)
			scaling = whatDaq.dInfo.reading.scaling;
		else
			return noScaling;

		return scaling == null ? noScaling : (DPMAnalogAlarmScaling) get(whatDaq, scaling);
	}

	//public static Scaling get(WhatDaq whatDaq, Lookup_v2.ReadSetScaling scaling)
	public static Scaling get(WhatDaq whatDaq, DeviceInfo.ReadSetScaling scaling)
	{
		return new DPMAnalogAlarmScalingImpl(whatDaq, scaling);
	}
}

class DPMAnalogAlarmScalingImpl extends AnalogAlarmScaling implements DPMAnalogAlarmScaling
{
    private final String alarmText;
    private final int ftd;
    private final int length;

	//DPMAnalogAlarmScalingImpl(WhatDaq whatDaq, Lookup_v2.ReadSetScaling scaling)// throws AcnetStatusException
	DPMAnalogAlarmScalingImpl(WhatDaq whatDaq, DeviceInfo.ReadSetScaling scaling)// throws AcnetStatusException
	{
		super(whatDaq.di(), whatDaq.pi(), scaling);

		this.alarmText = whatDaq.dInfo.analogAlarm.text;
		this.ftd = whatDaq.pInfo.ftd;
		this.length = whatDaq.getLength();
	}

	@Override
	public String textValue() throws AcnetStatusException
	{
		return (isEnabled()) ? (inAlarm() ? alarmText : "") : "BYPASSED";
	}

	@Override
	public double min() throws AcnetStatusException
	{
		return isMinMax() ? minScaled() : minScaled() - maxScaled();
	}

	@Override
	public double max() throws AcnetStatusException
	{
		return isMinMax() ? maxScaled() : minScaled() + maxScaled();
	}

	@Override
	public double rawMin() throws AcnetStatusException
	{
		return isMinMax() ? minRaw() : minRaw() - maxRaw();
	}

	@Override
	public double rawMax() throws AcnetStatusException
	{
		return isMinMax() ? maxRaw() : minRaw() + maxRaw();
	}

	@Override
	public double nom() throws AcnetStatusException
	{
		return isMinMax() ? (minScaled() + maxScaled()) / 2 : minScaled();
	}

	@Override
	public double tol() throws AcnetStatusException
	{
		return isMinMax() ? (maxScaled() - minScaled()) / 2 : maxScaled();
	}

	@Override
	public double rawNom() throws AcnetStatusException
	{
		return isMinMax() ? (minRaw() + maxRaw()) / 2 : minRaw();
	}

	@Override
	public double rawTol() throws AcnetStatusException
	{
		return isMinMax() ? (maxRaw() - minRaw()) / 2 : minRaw();
	}
				
	@Override
	public boolean enabled() throws AcnetStatusException
	{
		return super.isEnabled();
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
	public double ftd()
	{
		return ftd;
	}

	@Override
	public boolean abortCapable()
	{
		return super.isAbortCapable();
	}

	@Override
	public boolean abortInhibited()
	{
		return super.abortInhibited();
	}

	@Override
	public double flags()
	{
		return status;
	}
}
