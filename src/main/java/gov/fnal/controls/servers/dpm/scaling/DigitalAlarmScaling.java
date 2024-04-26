// $Id: DigitalAlarmScaling.java,v 1.7 2024/01/05 21:31:06 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
//import gov.fnal.controls.service.proto.Lookup_v2;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;

class DigitalAlarmScaling implements Scaling
{
	static final int[] scaleLengths = { 1, 2, 4 };

	int status = 0;
	int nom = 0;
	int mask = 0;
	int triesNeeded = 0;
	int triesNow = 0;

	//DigitalAlarmScaling(Lookup_v2.DeviceInfo dInfo) throws AcnetStatusException
	DigitalAlarmScaling(DeviceInfo dInfo) throws AcnetStatusException
	{
		if (dInfo.digitalAlarm == null)
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
	public double scale(byte[] data, int offset) throws AcnetStatusException 
	{
		return scale(ByteBuffer.wrap(data, offset, 20));	
	}

	@Override
	public double scale(ByteBuffer data) throws AcnetStatusException
	{
		try {
			status = data.getShort() & 0xffff;

			final int scaleLength = scaleLengths[(status & 0x0060) >> 5];
			final byte[] buf = new byte[4];

			data.get(buf);
			nom = rawNoSignExt(buf, 0, scaleLength);

			data.get(buf);
			mask = rawNoSignExt(buf, 0, scaleLength);

			triesNow = data.get() & 0xff;
			triesNeeded = data.get() & 0xff;

			return status;
		} catch (Exception e) {
			throw new AcnetStatusException(DIO_SCALEFAIL);
		}
	}

	boolean abortInhibited()
	{
		return (status & 0x0008) == 0x0008;
	}

	boolean isAbortCapable()
	{
		return (status & 0x0004) == 0x0004;
	}

	boolean inAlarm()
	{
		return (status & 0x0002) == 0x0002;
	}

	boolean isEnabled()
	{
		return (status & 0x0001) == 0x0001;
	}

	int mask()
	{
		return mask;
	}

	int nom()
	{
		return nom;
	}

	int triesNeeded()
	{
		return triesNeeded;
	}

	int triesNow() 
	{
		return triesNow;
	}
}
