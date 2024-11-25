// $Id: BasicControlScaling.java,v 1.10 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;

class BasicControlScaling implements Scaling
{
    public static final int RESET = 0; 
    public static final int ON = 1; 
    public static final int OFF = 2; 
    public static final int POS = 3; 
    public static final int NEG = 4;
    public static final int RAMP = 5;
    public static final int DC = 6; 
    public static final int LOCAL = 7; 
    public static final int REMOTE = 8; 
    public static final int TRIP = 9; 

	static final int MAX = 256;

	final int length;
	final int controlCount;
	final boolean defined[] = new boolean[MAX]; 
	final int order[] = new int[MAX];
	final String[] longText = new String[MAX];
	final String[] shortText = new String[MAX];
	final int data[] = new int[MAX];

	BasicControlScaling(DeviceInfo dInfo) throws AcnetStatusException
	{
		if (dInfo.control != null) {
			this.length = dInfo.control.prop.size;

			int count = 0;

			for (DeviceInfo.Control.Attribute attr : dInfo.control.attributes) {
				final int order = attr.order;

				this.defined[order] = true;		
				this.data[order] = attr.value;
				this.longText[order] = attr.longName;
				this.shortText[order] = attr.shortName.trim();
				this.order[count] = order;

				count++;
			}

			this.controlCount = count;
		} else
			throw new AcnetStatusException(DIO_NOSCALE);
	}

	@Override
	public double scale(byte[] data, int offset) throws AcnetStatusException
	{
		return rawNoSignExt(data, offset, length);
	}

	@Override
	public byte[] unscale(double value, int length) throws AcnetStatusException
	{
	    return raw(getControlData((int) value), length);
	}

	@Override
	public byte[] unscale(String value, int length) throws AcnetStatusException
	{
		for (int ii = 0; ii < controlCount; ii++) {
			final int orderNo = order[ii];

			if (value.equalsIgnoreCase(getShortText(orderNo)))
				return raw(getControlData(orderNo), length);
			else if (value.equalsIgnoreCase(getLongText(orderNo)))
				return raw(getControlData(orderNo), length);
		}
		
		throw new AcnetStatusException(DIO_SCALEFAIL, "Control operation not defined.");
	}

	int getControlData(int attr) throws AcnetStatusException
	{
		if (attr < defined.length && defined[attr])
			return data[attr];	

		throw new AcnetStatusException(DIO_SCALEFAIL, "Control operation not defined.");
	}

	String getLongText(int attr) throws AcnetStatusException
	{
		if (attr < defined.length && defined[attr])
			return longText[attr];
		
		throw new AcnetStatusException(DIO_SCALEFAIL, "Control operation not defined.");
	}

    String getShortText(int attr) throws AcnetStatusException
	{
        if (attr < defined.length && defined[attr])
        	return shortText[attr];
 
        throw new AcnetStatusException(DIO_SCALEFAIL, "Control operation not defined.");
    }
}
