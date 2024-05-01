// $Id: ReadSetScaling.java,v 1.7 2024/01/05 21:26:06 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

class ReadSetScaling implements Scaling
{
	final DeviceInfo.ReadSetScaling scaling;
	final Primary primary;
	final Common common;

	ReadSetScaling(DeviceInfo.ReadSetScaling scaling)// throws AcnetStatusException
	{
		this.scaling = scaling;
		this.primary = new Primary(scaling);
		this.common = new Common(scaling);
	}

	@Override
	public double scale(byte[] data, int offset) throws AcnetStatusException
	{
		return common.scale(primary.scale(raw(data, offset, scaling.primary.inputLen)));
	}

	@Override
	public byte[] unscale(double data, int length) throws AcnetStatusException
	{
		return raw(primary.unscale(common.unscale(data)), length);
	}

	@Override
	public byte[] unscale(String value, int length) throws AcnetStatusException
	{
		throw new AcnetStatusException(DIO_SCALEFAIL);
	}

	public double rawToCommon(int data) throws AcnetStatusException
	{
		return common.scale(primary.scale(data));
	}

	double scale(byte[] data, int offset, int length) throws AcnetStatusException
	{
		return rawToCommon(raw(data, offset, length));
	}
}
