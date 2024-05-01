// $Id: BasicStatusScaling.java,v 1.8 2024/01/05 21:31:06 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import java.util.Arrays;

//import gov.fnal.controls.service.proto.Lookup_v2;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

class BasicStatusScaling implements Scaling
{
	static final String labels[] = { "Pwr ", "Sts ", "Ctrl ", "Pol ", "Ref " };
	static class AttributeStateInfo 
	{
		String text  = "****";
		String ch  = " ";
		int color = 0;
	}

	static class Attribute 
	{
		long mask = 0;
		long matchValue = 1;
		boolean invert = false;
		String shortName = "???";
		String longName = "???";
		
		final AttributeStateInfo match;
		final AttributeStateInfo nomatch; 

		Attribute()
		{
			match = new AttributeStateInfo();
			nomatch = new AttributeStateInfo();
		}
	}

	static final Attribute Undefined = new Attribute();

	static final int ON = 0; 
	static final int READY = 1; 
	static final int REMOTE = 2; 
	static final int POSITIVE = 3; 
	static final int RAMP = 4; 

	static final int STANDARD_ATTR_COUNT = 5;

	static boolean classBugs = false;
	private static final int[] inputMasks = { 0, 0x000000ff, 0x0000ffff, 0, 0xffffffff };
	
	final String name;
	final int di;
	final int length;
	final Attribute[] attributes = new Attribute[32];

	//BasicStatusScaling(Lookup_v2.DeviceInfo dInfo) throws AcnetStatusException
	BasicStatusScaling(DeviceInfo dInfo) throws AcnetStatusException
	{
		Arrays.fill(attributes, Undefined);

		if (dInfo.status != null) {
			this.name = dInfo.name;
			this.di = dInfo.di;
			this.length = dInfo.status.dataLen;

			//for (Lookup_v2.StatusAttribute sAttr : dInfo.status.attributes) {
			for (DeviceInfo.Status.Attribute sAttr : dInfo.status.attributes) {
				BasicStatusScaling.Attribute attr = new BasicStatusScaling.Attribute();
				
				attr.mask = sAttr.mask;
				attr.matchValue = sAttr.match;
				attr.invert = sAttr.inverted;
				attr.shortName = sAttr.shortName;
				attr.longName = sAttr.longName;

				attr.match.ch = sAttr.trueDisplay.character;
				attr.match.color = sAttr.trueDisplay.color; 
				attr.match.text = sAttr.trueDisplay.text;

				attr.nomatch.ch = sAttr.trueDisplay.character;
				attr.nomatch.color = sAttr.trueDisplay.color; 
				attr.nomatch.text = sAttr.trueDisplay.text;

				this.attributes[sAttr.order] = attr;

				//if (sAttr.order > STANDARD_ATTR_COUNT)
				//	sAttr.order = STANDARD_ATTR_COUNT;
			}
		} else
			throw new AcnetStatusException(DIO_NOSCALE);
	}

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

	private boolean getState(int data, int attrNo)
	{
		Attribute attr = attributes[attrNo];

		data &= inputMasks[length];	
		if (attr.invert)
			data = ~data;

		return (data & attr.mask) == attr.matchValue;
	}

	private String getStateChar(int data, int attrNo)
	{
		return getState(data, attrNo) ? attributes[attrNo].match.ch :
						attributes[attrNo].nomatch.ch;
	}

	String getStatusString(int data)
	{
		final StringBuffer buf = new StringBuffer(256);

		for (int ii = 0; ii < STANDARD_ATTR_COUNT; ii++)
			buf.append(getStateChar(data, ii));

		return buf.toString();
	}

	int scale(byte[] data, int offset, int length) throws AcnetStatusException
	{
		return rawNoSignExt(data, offset, length);
	}

	String[] getLabels()
	{
		return labels;
	}

	boolean[] getDefined()
	{
		boolean[] defined = new boolean[STANDARD_ATTR_COUNT];

		for (int ii = 0; ii < defined.length; ii++)
			defined[ii] = (attributes[ii] != Undefined);

		return defined;
	}

	String shortName(int attrNo)
	{
		return attributes[attrNo].shortName;
	}

	String longName(int attrNo)
	{
		return attributes[attrNo].longName;
	}

	int attributeCount()
	{
		return STANDARD_ATTR_COUNT;
	}

	boolean isOn(int data)
	{
		return getState(data, ON);
	}

	boolean isReady(int data)
	{
		return getState(data, READY);
	}

	boolean isRemote(int data)
	{
		return getState(data, REMOTE);
	}

	boolean isPositive(int data)
	{	
		return getState(data, POSITIVE);
	}

	boolean isRamp(int data)
	{
		return getState(data, RAMP);
	}
}
