// $Id: DPMBasicStatusScaling.java,v 1.7 2023/11/02 16:36:16 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

public interface DPMBasicStatusScaling extends Scaling
{
	default void scale(WhatDaq whatDaq, ByteBuffer data) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default void scale(WhatDaq whatDaq, byte[] data, int offset) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default void scale(WhatDaq whatDaq, double value) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default String textValue() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default boolean hasOn()
	{
		return false;
	}

	default boolean isOn() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default boolean hasReady()
	{
		return false;
	}

	default boolean isReady() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default boolean hasRemote()
	{
		return false;
	}

	default boolean isRemote() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default boolean hasPositive()
	{
		return false;
	}

	default boolean isPositive() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default boolean hasRamp()
	{
		return false;
	}

	default boolean isRamp() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default double rawStatus() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}

	default String[] extendedText() throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_NOSCALE);
	}
	
	class NoScaling implements DPMBasicStatusScaling
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

	public static DPMBasicStatusScaling get(WhatDaq whatDaq)
	{
		try {
			return new DPMBasicStatusScalingImpl(whatDaq);
		} catch (Exception ignore) { }

		return new NoScaling();
	}
}

class DPMBasicStatusScalingImpl extends BasicStatusScaling implements DPMBasicStatusScaling
{
	//final WhatDaq whatDaq;
	final int di;
	final int pi;
	final int length;
	final boolean[] defined;

	int rawStatus;

	//BasicStatus basicStatus;
	DigitalStatusScaling digitalScaling;

	DPMBasicStatusScalingImpl(WhatDaq whatDaq) throws AcnetStatusException
	{
		super(whatDaq.dInfo);

		//this.whatDaq = whatDaq;
		this.di = whatDaq.getDeviceIndex();
		this.pi = whatDaq.getPropertyIndex();
		this.length = whatDaq.getLength();
		this.defined = getDefined();
		//this.basicStatus = null;
		this.digitalScaling = null;
	}

	@Override
	public void scale(WhatDaq whatDaq, ByteBuffer data) throws AcnetStatusException
	{
		rawStatus = rawNoSignExt(data, length);
	}

	@Override
	public void scale(WhatDaq whatDaq, byte[] data, int offset) throws AcnetStatusException
	{
    	//basicStatus = (BasicStatus) scale(data, offset, length);
    	rawStatus = scale(data, offset, length);
		//basicStatus.setWhatDaq(whatDaq);
	}

	@Override
	public void scale(WhatDaq whatDaq, double value) throws AcnetStatusException
	{
		rawStatus = (int) value;

    	//basicStatus = new BasicStatus((int) value);
		//basicStatus.setWhatDaq(whatDaq);
	}

	@Override
	public String textValue()
	{
		//return basicStatus.toString();
		return getStatusString(rawStatus);
	}

	@Override
	public boolean hasOn()
	{
		return defined[BasicStatusScaling.ON];
	}

	@Override
	public boolean isOn() throws AcnetStatusException
	{
		if (defined[BasicStatusScaling.ON])
			//return basicStatus.isOn();
			return isOn(rawStatus);

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public boolean hasReady()
	{
		return defined[BasicStatusScaling.READY];
	}

	@Override
	public boolean isReady() throws AcnetStatusException
	{
		if (defined[BasicStatusScaling.READY])
			//return basicStatus.isReady();
			return isReady(rawStatus);

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public boolean hasRemote()
	{
		return defined[BasicStatusScaling.REMOTE];
	}

	@Override
	public boolean isRemote() throws AcnetStatusException
	{
		if (defined[BasicStatusScaling.REMOTE])
			//return basicStatus.isRemote();
			return isRemote(rawStatus);

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public boolean hasPositive()
	{
		return defined[BasicStatusScaling.POSITIVE];
	}

	@Override
	public boolean isPositive() throws AcnetStatusException
	{
		if (defined[BasicStatusScaling.POSITIVE])
			//return basicStatus.isPositive();
			return isPositive(rawStatus);

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public boolean hasRamp()
	{
		return defined[BasicStatusScaling.RAMP];
	}

	@Override
	public boolean isRamp() throws AcnetStatusException
	{
		if (defined[BasicStatusScaling.RAMP])
			//return basicStatus.isRamp();
			return isRamp(rawStatus);

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public double rawStatus()
	{
	 	//return basicStatus.getRawInt();
	 	return rawStatus; 
	}

	@Override
	public String[] extendedText() throws AcnetStatusException
	{
		if (digitalScaling == null)                                                                                                                                  
			digitalScaling = new DigitalStatusScaling(di);

		//if (digitalScaling != null) {
			final String[] bitName = digitalScaling.getLabels();
			//final String[] bitState = digitalScaling.scale(basicStatus.getRawInt());
			final String[] bitState = digitalScaling.scale(rawStatus);
			final String[] data = new String[bitName.length * 2];

			for (int ii = 0; ii < bitName.length; ii++) {
				data[2 * ii] = bitName[ii];
				data[2 * ii + 1] = bitState[ii];
			}

			return data;
		//}

		//throw new AcnetStatusException(DIO_NOSCALE);
	}
}
