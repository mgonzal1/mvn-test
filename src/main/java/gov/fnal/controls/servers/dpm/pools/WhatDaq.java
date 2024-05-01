// $Id: WhatDaq.java,v 1.14 2024/03/05 17:25:21 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import java.util.Objects;
import java.util.EnumSet;
import java.util.Arrays;
import java.text.ParseException;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.NeverEvent;
import gov.fnal.controls.servers.dpm.events.DefaultDataEvent;
import gov.fnal.controls.servers.dpm.events.DataEventFactory;
import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;

import gov.fnal.controls.servers.dpm.drf3.Range;
import gov.fnal.controls.servers.dpm.drf3.DeviceFormatException;
import gov.fnal.controls.servers.dpm.drf3.TimeFreq;
import gov.fnal.controls.servers.dpm.drf3.TimeFreqUnit;
import gov.fnal.controls.servers.dpm.drf3.PeriodicEvent;
import gov.fnal.controls.servers.dpm.drf3.Event;

import gov.fnal.controls.servers.dpm.drf3.Field;
import gov.fnal.controls.servers.dpm.drf3.Property;
import gov.fnal.controls.servers.dpm.drf3.EventFactory;
import gov.fnal.controls.servers.dpm.drf3.AcnetRequest;
import gov.fnal.controls.servers.dpm.drf3.FieldFormatException;
import gov.fnal.controls.servers.dpm.drf3.PropertyFormatException;

import gov.fnal.controls.servers.dpm.DPMList;
import gov.fnal.controls.servers.dpm.DPMRequest;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class WhatDaq implements AcnetErrors, ReceiveData, Comparable<WhatDaq>
{
	public final static int SSDN_SIZE = 8;
	public enum Option { FLOW_CONTROL }

	final EnumSet<Option> options;

	public final boolean defaultEvent;

	public final DPMList list;
	public final long refId;
	public final DeviceInfo dInfo;
	public final Field field;
	public final DeviceInfo.PropertyInfo pInfo;
	public final Property property;
	public final Event event;
	public final String alarmListName;
	public final String daqName;
	public final String loggedName;

	final protected int di;
	final protected int pi;
	final protected int dipi;
	final protected String name;
	protected int error;
	protected int length;
	protected int offset;
	protected int maxLength;
	protected int dataTypeId;
	protected int rawOffset;
	protected int numElements;
	protected int arrayElement;
	protected byte setting[];
	protected long protection;
	protected int ftd;
	protected DataEvent when;
	private volatile ReceiveData receiveData;
	protected int receiveDataId;
	protected PoolUser user;
	protected byte[] ssdn = new byte[SSDN_SIZE];
	protected boolean delete;
	protected int defaultLength;
	protected int poolMask;

	Node node;

	public static int dipi(int di, int pi)
	{
		return (di & 0xffffff) | (pi << 24);
	}

	public WhatDaq(DPMList list, String drf) throws IllegalArgumentException, AcnetStatusException, DeviceNotFoundException
	{
		this(list, 0, new DPMRequest(drf));
	}

	public WhatDaq(DPMList list, long refId, DPMRequest req, DataEvent event) throws AcnetStatusException, DeviceNotFoundException
	{
		this(list, refId, req);
		this.when = event;
	}

	public WhatDaq(WhatDaq whatDaq, int length, int offset, int receiveDataId, ReceiveData receiveData)
	{
		this(whatDaq);

		this.length = length;
		this.offset = offset;
		this.receiveDataId = receiveDataId;
		this.receiveData = receiveData;
	}

	public WhatDaq(WhatDaq whatDaq, int id)
	{
		this(whatDaq, whatDaq.length, whatDaq.offset, id, null);
	}

	public WhatDaq(DPMList list, long refId, DPMRequest req) throws AcnetStatusException, DeviceNotFoundException
	{
		final AcnetRequest acnetReq;

		try {
			acnetReq = new AcnetRequest(req);
		} catch (DeviceFormatException e) {
			throw new AcnetStatusException(DPM_BAD_REQUEST, e);
		} catch (PropertyFormatException e) {
			throw new AcnetStatusException(DPM_BAD_REQUEST, e);
		} catch (FieldFormatException e) {
			throw new AcnetStatusException(DPM_BAD_REQUEST, e);
		} catch (Exception e) {
			throw new AcnetStatusException(ACNET_NXE, e);
		}
	
		this.list = list;
		this.refId = refId;
		this.receiveDataId = 0;
		this.options = EnumSet.noneOf(Option.class);
		this.property = acnetReq.getProperty();
		this.pi = (this.property.indexValue & 0xff);

		this.field = acnetReq.getField();
		this.dInfo = DeviceCache.get(acnetReq.getDevice());
		this.pInfo = propInfo();

		this.di = dInfo.di;
		this.dipi = dipi(di, pi);
		this.name = dInfo.name;
		this.event = acnetReq.getEvent();

		this.node = Node.get(this.pInfo.node);

		this.error = 0;
		this.defaultLength = pInfo.atomicSize;
		this.maxLength = pInfo.size;
		this.protection = 0;
		System.arraycopy(pInfo.ssdn, 0, this.ssdn, 0, pInfo.ssdn.length);
		this.ftd = (pInfo.ftd & 0xffff);
		this.poolMask = 0;
		this.receiveData = null; 
		this.when = EventFactory.createEvent(this.event);
		this.defaultEvent = (this.when instanceof DefaultDataEvent);
		this.alarmListName = "";

		if (this.defaultEvent) {
			try {
				this.when = DataEventFactory.stringToEvent(pInfo.defEvent);
			} catch (ParseException e) {
				throw new AcnetStatusException(DPM_BAD_EVENT);
			}
		}

		// Setup length and offset

		final Range range = acnetReq.getRange();
		
		switch (range.getType()) {
		 	case ARRAY:
		 		offset = range.getStartIndex() * pInfo.atomicSize;
				length = range.getLength() * pInfo.atomicSize;
				break;

			case BYTE:
				offset = range.getOffset();
				length = range.getLength();
				break;

			case FULL:
				offset = 0;
				length = pInfo.size;
				break;

			default:
				throw new AcnetStatusException(DPM_BAD_REQUEST);
		}

		switch (this.property) {
			case ANALOG:
			case DIGITAL:
				if (field != Field.RAW) {
					length = pInfo.size;
					offset = 0;
				}
				break;
		}

		if (length == 0)
			length = pInfo.atomicSize;

		if (pInfo.nonLinear) {
			if (offset >= pInfo.size || length > pInfo.size)
				throw new AcnetStatusException(DPM_OUT_OF_BOUNDS);
		} else {
			if ((offset % pInfo.atomicSize != 0) || (length % pInfo.atomicSize) != 0)
				throw new AcnetStatusException(DPM_BAD_FRAMING);

			if (offset + length > pInfo.size)
				throw new AcnetStatusException(DPM_OUT_OF_BOUNDS);
		}

		this.rawOffset = pInfo.atomicSize;

		// Setup array info

       	if (offset != 0 && defaultLength != 0 && (length % pInfo.atomicSize) == 0)
	    	this.arrayElement = offset / defaultLength;
		else 
			this.arrayElement = 0;

		switch (this.property) {
			case READING:
		 	case SETTING:
				this.numElements = length / pInfo.atomicSize;
				break;
		}

		final String f = this.pInfo.foreignName;

		if (f != null && !f.isEmpty())
			this.daqName = f + "@" + this.event;
		else
			this.daqName = this.name + "@" + this.event;

		this.loggedName = acnetReq.toLoggedString();
	}

	public WhatDaq(WhatDaq whatDaq)
	{
		this.list = whatDaq.list;
		this.refId = whatDaq.refId;
		this.options = whatDaq.options;
		this.property = whatDaq.property;
		this.pi = whatDaq.pi;

		this.field = whatDaq.field;
		this.dInfo = whatDaq.dInfo;
		this.pInfo = whatDaq.pInfo;
		this.node = whatDaq.node;

		this.di = whatDaq.di;
		this.dipi = whatDaq.dipi;
		this.name = whatDaq.name;
		this.event = whatDaq.event;

		this.error = 0;
		this.defaultLength = whatDaq.defaultLength;
		this.maxLength = whatDaq.maxLength;
		this.protection = whatDaq.protection;
		this.ssdn = whatDaq.ssdn;
		this.ftd = whatDaq.ftd;
		this.poolMask = 0;
		this.receiveDataId = whatDaq.receiveDataId;
		this.receiveData = this; 
		this.when = whatDaq.when;
		this.defaultEvent = whatDaq.defaultEvent;
		this.alarmListName = whatDaq.alarmListName;
		this.rawOffset = whatDaq.rawOffset;
		this.daqName = whatDaq.daqName;
		this.loggedName = whatDaq.loggedName;
	}

	public final int di()
	{
		return di;
	}

	public final int pi()
	{
		return pi;
	}

	public final int dipi()
	{
		return dipi;
	}

	public final int length()
	{
		return length;
	}

	public final int defaultLength()
	{
		return defaultLength;
	}

	public final boolean lengthIsOdd()
	{
		return (length & 1) == 1;
	}

	public Property property()
	{
		return property;
	}

	public final int offset()
	{
		return offset;
	}

	public final byte[] ssdn()
	{
		return ssdn;
	}

	public byte[] setting()
	{
		return setting;
	}

	public void setOption(Option opt)
	{
		options.add(opt);
	}

	public void clearOption(Option opt)
	{
		options.remove(opt);
	}

	public boolean getOption(Option opt)
	{
		return options.contains(opt);
	}

	public boolean isSettableProperty()
	{
		switch (property) {
			case SETTING:
			case CONTROL:
		 		return true;

		 	default:
		 		return false;
		}
	}

	public boolean isStateDevice()
	{
		return name.charAt(0) == 'V';		
	}

	public boolean hasEvent()
	{
		return !(when instanceof NeverEvent);		
	}

	public boolean isRepetitive()
	{
		return when.isRepetitive();
	}

	public void setReceiveData(ReceiveData receiveData)
	{
		this.receiveData = (receiveData == null ? this : receiveData);
	}

	public void setUser(PoolUser user)
	{
		this.user = user;
	}

    private DeviceInfo.PropertyInfo propInfo() throws AcnetStatusException
    {
       	DeviceInfo.PropertyInfo pInfo = null;

        switch (property) {
            case STATUS:
                if (dInfo.status != null)
                    return dInfo.status.prop;
                break;

            case CONTROL:
                if (dInfo.control != null)
                    return dInfo.control.prop;
                break;

            case READING:
                if (dInfo.reading != null)
                    return dInfo.reading.prop;
                break;

            case SETTING:
                if (dInfo.setting != null)
                    return dInfo.setting.prop;
                break;

            case ANALOG:
                if (dInfo.analogAlarm != null)
                    return dInfo.analogAlarm.prop;
                break;

            case DIGITAL:
                if (dInfo.digitalAlarm != null)
                    return dInfo.digitalAlarm.prop;
                break;

			case DESCRIPTION:
				return new DeviceInfo.PropertyInfo(pi, propToNode(), 128, 1);

			case INDEX:
				return new DeviceInfo.PropertyInfo(pi, propToNode(), 4, 4);

			case LONG_NAME:
				return new DeviceInfo.PropertyInfo(pi, propToNode(), 128, 1);

			case ALARM_LIST_NAME:
				return new DeviceInfo.PropertyInfo(pi, propToNode(), 128, 1);
        }

        throw new AcnetStatusException(DBM_NOPROP);
    }

	private int propToNode() throws AcnetStatusException
	{
		if (dInfo.reading != null)
			return dInfo.reading.prop.node;

		if (dInfo.setting != null)
			return dInfo.setting.prop.node;

		if (dInfo.status != null)
			return dInfo.status.prop.node;

		if (dInfo.control != null)
			return dInfo.control.prop.node;

		if (dInfo.analogAlarm != null)
			return dInfo.analogAlarm.prop.node;

		if (dInfo.digitalAlarm != null)
			return dInfo.digitalAlarm.prop.node;

        throw new AcnetStatusException(DBM_NOPROP);
	}

	public String getUnits()
	{
		try {
			final DeviceInfo.ReadSetScaling scaling;

			switch (property) {
			 case READING:
			 	scaling = dInfo.reading.scaling;
				break;

			 case SETTING:
			 	scaling = dInfo.reading.scaling;
				break;

			 default:
			 	return null;
			}

			return field == Field.PRIMARY ? scaling.primary.units : scaling.common.units;
		} catch (Exception ignore) { }

		return null;
	}

	void clearSetting()
	{
		setting = null;
	}

    final public void setSetting(byte data[]) throws AcnetStatusException
	{
        if (data.length == length) {
			setting = data;
		} else
			throw new AcnetStatusException(DIO_INVLEN);
    }

	final private int end()
	{
		return offset + length;
	}

	final public double getEventFrequency()
	{
		if (when instanceof DeltaTimeEvent && when.isRepetitive())
			return 1.0 / (((DeltaTimeEvent) when).getRepeatRate()) * 1000.0;

		return 0;
	}

	final public boolean contains(WhatDaq whatDaq)
	{
		return di == whatDaq.di && pi == whatDaq.pi && 
				whatDaq.offset >= offset && whatDaq.end() <= end();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;

		if (obj instanceof WhatDaq)
			return compareTo((WhatDaq) obj) == 0;
	
		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(di, pi, length, offset);
	}

	@Override
	public int compareTo(WhatDaq whatDaq)
	{
		if (di == whatDaq.di) {
			if (pi == whatDaq.pi) {
				if (length == whatDaq.length) {
					if (offset == whatDaq.offset)
						return 0;

					return offset > whatDaq.offset ? 1 : -1;
				}
				return length > whatDaq.length ? 1 : -1;
			}
			return pi > whatDaq.pi ? 1 : -1;
		}
		return di > whatDaq.di ? 1 : -1;
	}

	public boolean isEqual(WhatDaq whatDaq)
	{
		return compareTo(whatDaq) == 0;
		/*
		if (di != whatDaq.di || pi != whatDaq.pi
				|| length != whatDaq.length
				|| offset != whatDaq.offset)
			return false;

		return true;
		*/
	}

	@Override
	public void receiveStatus(int status, long timestamp, long cycle)
	{
	}

	@Override
	public void receiveData(ByteBuffer data, long timestamp, long cycle)
	{
	}

	//public Object clone()
	//{
	//	return new WhatDaq(this);
	//}

	@Override
	public String toString()
	{
		return String.format("%-14s %-8s %-8d %-16s L:%-6d O:%-6d %02x%02x/%02x%02x/%02x%02x/%02x%02x %s",
								name, node.name(), di, property, length, offset,
								ssdn[1], ssdn[0], ssdn[3], ssdn[2], ssdn[5], ssdn[4], ssdn[7], ssdn[6],
								setting != null ? "SettingReady" : "");
	}

	public boolean isArray()
	{
		return (pi == Property.READING.indexValue || pi == Property.SETTING.indexValue)
					&& length != defaultLength && numElements > 1 && rawOffset == defaultLength;
	}

	public int getDeviceIndex()
	{
		return di;
	}

	public int getPropertyIndex()
	{
		return pi;
	}

	public String getDeviceName()
	{
		return name;
	}

	public final Node node()
	{
		return node;
	}

	public int getError()
	{
		return error;
	}

	public void setError(int error)
	{
		this.error = error;
	}

	public int getLength()
	{
		return length;
	}

	public int getOffset()
	{
		return offset;
	}

	public int getMaxLength()
	{
		return maxLength;
	}

	public int getDeviceTypeId()
	{
		return dataTypeId;
	}

	public int getRawOffset()
	{
		return rawOffset;
	}

	public int getNumberElements()
	{
		return numElements;
	}

	public int getArrayElement()
	{
		return arrayElement;
	}

	public byte[] getSetting()
	{
		return setting;
	}

	public boolean isSettingReady()
	{
		return setting != null;
	}

	public long getProtection()
	{
		return protection;
	}

	public int getFTD()
	{
		return ftd;
	}

	public DataEvent event()
	{
		return when;
	}

	public DataEvent getEvent()
	{
		return when;
	}

	public void setEvent(DataEvent event)
	{
		when = event;
	}

	public ReceiveData getReceiveData()
	{
		return receiveData;
	}

	public int getReceiveDataId()
	{
		return receiveDataId;
	}

	public int id()
	{
		return receiveDataId;
	}

	public PoolUser getUser()
	{
		return user;
	}

	public byte[] getSSDN()
	{
		return ssdn;
	}

	public void redirect(Node node)
	{
		this.node = node;
	}

	public boolean isMarkedForDelete()
	{
		return delete;
	}

	public void setMarkedForDelete()
	{
		receiveData = this;
		delete = true;
	}

	public int getDefaultLength()
	{
		return defaultLength;
	}

	public void setLengthOffset(int length, int offset)
	{
		this.length = length;
		this.offset = offset;

		if (offset != 0 && defaultLength != 0)
			arrayElement = offset / defaultLength;
	}
}
