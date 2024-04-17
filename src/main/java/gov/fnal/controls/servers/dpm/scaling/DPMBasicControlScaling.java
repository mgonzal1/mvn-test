// $Id: DPMBasicControlScaling.java,v 1.5 2023/11/02 16:36:16 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

public interface DPMBasicControlScaling extends Scaling
{
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

	class NoScaling implements DPMBasicControlScaling
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

	public static DPMBasicControlScaling get(WhatDaq whatDaq)
	{
		try {
			return new DPMBasicControlScalingImpl(whatDaq);
		} catch (Exception ignore) {
		}

		return new NoScaling();
	}
}

class DPMBasicControlScalingImpl extends BasicControlScaling implements DPMBasicControlScaling
{
	//final WhatDaq whatDaq;
	final int di;
	final int pi;
	final int length;

	DPMBasicControlScalingImpl(WhatDaq whatDaq) throws AcnetStatusException
	{
		super(whatDaq.dInfo);

		this.di = whatDaq.getDeviceIndex();
		this.pi = whatDaq.getPropertyIndex();
		this.length = whatDaq.getLength();
	}

	@Override
	public void scale(WhatDaq whatDaq, byte[] data, int offset) throws AcnetStatusException
	{
	}

	@Override
	public void scale(WhatDaq whatDaq, double value) throws AcnetStatusException
	{
	}

	@Override
	public String textValue()
	{
		return "";
	}

	@Override
	public boolean hasOn()
	{
		//return defined[BasicStatusScaling.ON];
		return false;
	}

	@Override
	public boolean isOn() throws AcnetStatusException
	{
		//if (defined[BasicStatusScaling.ON])
			//return basicStatus.isOn();

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public boolean hasReady()
	{
		//return defined[BasicStatusScaling.READY];
		return false;
	}

	@Override
	public boolean isReady() throws AcnetStatusException
	{
		//if (defined[BasicStatusScaling.READY])
			//return basicStatus.isReady();

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public boolean hasRemote()
	{
		//return defined[BasicStatusScaling.REMOTE];
		return false;
		//throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public boolean isRemote() throws AcnetStatusException
	{
		//if (defined[BasicStatusScaling.REMOTE])
			//return basicStatus.isRemote();

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public boolean hasPositive()
	{
		//return defined[BasicStatusScaling.POSITIVE];
		return false;
	}

	@Override
	public boolean isPositive() throws AcnetStatusException
	{
		//if (defined[BasicStatusScaling.POSITIVE])
			//return basicStatus.isPositive();

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public boolean hasRamp()
	{
		//return defined[BasicStatusScaling.RAMP];
		return false;
	}

	@Override
	public boolean isRamp() throws AcnetStatusException
	{
		//if (defined[BasicStatusScaling.RAMP])
			//return basicStatus.isRamp();

		throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public double rawStatus()
	{
	 	//return basicStatus.getRawInt();
		return 0;
	}

	@Override
	public String[] extendedText() throws AcnetStatusException
	{
	/*
		if (digitalScaling == null)                                                                                                                                  
			digitalScaling = new DigitalStatusScaling(di, pi);

		if (digitalScaling != null) {
			final String[] bitName = digitalScaling.getLabels();
			final String[] bitState = (String[]) digitalScaling.scale(basicStatus.getRawInt());
			final String[] data = new String[bitName.length * 2];

			for (int ii = 0; ii < bitName.length; ii++) {
				data[2 * ii] = bitName[ii];
				data[2 * ii + 1] = bitState[ii];
			}

			return data;
		}
		*/

		throw new AcnetStatusException(DIO_NOSCALE);
	}
}
