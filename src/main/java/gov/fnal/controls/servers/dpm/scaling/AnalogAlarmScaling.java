// $Id: AnalogAlarmScaling.java,v 1.9 2024/01/05 21:31:06 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import java.nio.ByteBuffer;

//import gov.fnal.controls.service.proto.Lookup_v2;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.drf3.Property;

class AnalogAlarmScaling extends ReadSetScaling
{
	static final int[] scaleLengths = { 1, 2, 4 };

	int status = 0;
	int minRaw = 0;
	int maxRaw = 0;
	double minScaled = 0;
	double maxScaled = 0;
	int triesNeeded = 0;
	int triesNow = 0;

	//AnalogAlarmScaling(int di, int pi, Lookup_v2.ReadSetScaling scaling)
	AnalogAlarmScaling(int di, int pi, DeviceInfo.ReadSetScaling scaling)
	{
		super(scaling);
	}

	@Override
	public double scale(ByteBuffer data) throws AcnetStatusException
	{
		try {
			status = data.getShort() & 0xffff;

			final int scaleLength = scaleLengths[(status & 0x0060) >> 5];
			final byte[] buf = new byte[4];

			data.get(buf);
			minRaw = raw(buf, 0, scaleLength);
			minScaled = super.scale(buf, 0, scaleLength);

			data.get(buf);
			maxRaw = raw(buf, 0, scaleLength);
			maxScaled = super.scale(buf, 0, scaleLength);

			if (isNomTol()) {
				final byte[] zero = new byte[4];
				maxScaled = maxScaled - super.scale(zero, 0, scaleLength);
			}

			triesNow = data.get() & 0xff;
			triesNeeded = data.get() & 0xff;

			return status;
		} catch (Exception e) {
			throw new AcnetStatusException(DIO_SCALEFAIL, e.toString());
		}
	}

	@Override
	public double scale(byte[] data, int offset) throws AcnetStatusException 
	{
		return scale(ByteBuffer.wrap(data, offset, 20));	
	}

	boolean isNomTol()
	{
		return (status & 0x0300) == 0;
	}

	boolean isNomPctTol()
	{
		return (status & 0x0300) == 0x0100;
	}

	boolean isMinMax()
	{
		return (status & 0x0300) == 0x0200;
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

	int minRaw()
	{
		return minRaw;
	}

	int maxRaw()
	{
		return maxRaw;
	}

	double minScaled()
	{
		return minScaled;
	}

	double maxScaled()
	{
		return maxScaled;
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
