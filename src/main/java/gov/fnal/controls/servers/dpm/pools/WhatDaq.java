// $Id: WhatDaq.java,v 1.30 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import java.util.Objects;
import java.util.EnumSet;
import java.util.Arrays;
import java.text.ParseException;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.acnetlib.NodeType;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.drf3.Range;
import gov.fnal.controls.servers.dpm.drf3.Event;
import gov.fnal.controls.servers.dpm.drf3.Property;
import gov.fnal.controls.servers.dpm.drf3.Field;
import gov.fnal.controls.servers.dpm.drf3.PeriodicEvent;
import gov.fnal.controls.servers.dpm.drf3.DefaultEvent;
import gov.fnal.controls.servers.dpm.drf3.AcnetRequest;
import gov.fnal.controls.servers.dpm.drf3.NeverEvent;
import gov.fnal.controls.servers.dpm.drf3.EventFormatException;
import gov.fnal.controls.servers.dpm.drf3.RequestFormatException;
import gov.fnal.controls.servers.dpm.drf3.DeviceFormatException;
import gov.fnal.controls.servers.dpm.drf3.FieldFormatException;
import gov.fnal.controls.servers.dpm.drf3.PropertyFormatException;

import gov.fnal.controls.servers.dpm.Job;
import gov.fnal.controls.servers.dpm.DPMList;
import gov.fnal.controls.servers.dpm.DPMRequest;
import gov.fnal.controls.servers.dpm.DPMProtocolReplier;
import gov.fnal.controls.servers.dpm.DataSource;
import gov.fnal.controls.servers.dpm.LiveDataSource;
import gov.fnal.controls.servers.dpm.DataReplier;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class DatabaseSource extends DataSource
{
	@Override
	public Job createJob(DPMList list)
	{
		return null;
	}

	@Override
	public DataType dataType(WhatDaq whatDaq)
	{
		return DataType.Scaled;
	}

	@Override
	public String toString()
	{
		return "Database";
	}
}

class AcnetSource extends LiveDataSource
{
	@Override
	public String toString()
	{
		return "ACNET";
	}
}

class EpicsSource extends LiveDataSource
{
	@Override
	public DataType dataType(WhatDaq whatDaq)
	{
		return DataType.Primary;
	}

	@Override
	public String toString()
	{
		return "EPICS";
	}
}

class LiveAcnetSource extends DataSource
{
	@Override
	public Job createJob(DPMList list)
	{
		return null;
	}

	@Override
	public DataType dataType(WhatDaq whatDaq)
	{
		return DataType.Scaled;
	}

	@Override
	public String toString()
	{
		return "Database";
	}
}

public class WhatDaq implements AcnetErrors, ReceiveData, Comparable<WhatDaq>
{
	public enum Option { FLOW_CONTROL }

	final static int SSDN_SIZE = 8;

	final EnumSet<Option> options;

	final DPMList list;
	final long refId;

	DPMRequest request;
	DeviceInfo dInfo;

	DeviceInfo.PropertyInfo pInfo;
	Property property;
	Field field;
	boolean defaultEvent;
	Event event;

	final String alarmListName;
	final String daqName;
	final String loggedName;
	final int dipi;

	int error;
	int length;
	int offset;
	int maxLength;
	int dataTypeId;
	int rawOffset;
	int numElements;
	int arrayElement;
	byte setting[];
	long protection;
	int ftd;
	volatile ReceiveData receiveData;
	PoolUser user;
	byte[] ssdn = new byte[SSDN_SIZE];
	boolean delete;
	int defaultLength;
	int poolMask;
	Node node;

	public static WhatDaq create(DPMList list, DPMRequest req) throws AcnetStatusException
	{
		try {
			return new WhatDaq(list, req, new AcnetRequest(req));
		} catch (DeviceNotFoundException e) {
			throw e;
		} catch (AcnetStatusException e) {
			throw e;
		} catch (DeviceFormatException e) {
			throw new AcnetStatusException(DPM_BAD_REQUEST, e);
		} catch (PropertyFormatException e) {
			throw new AcnetStatusException(DPM_BAD_REQUEST, e);
		} catch (FieldFormatException e) {
			throw new AcnetStatusException(DPM_BAD_REQUEST, e);
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception in create()", e);
			throw new AcnetStatusException(DPM_INTERNAL_ERROR, e);
		}
	}

	public static WhatDaq create(DPMRequest request) throws IllegalArgumentException, AcnetStatusException, DeviceNotFoundException
	{
		return create(null, request);
	}

	public static WhatDaq create(String drf) throws IllegalArgumentException, AcnetStatusException, DeviceNotFoundException
	{
		return create(null, new DPMRequest(drf, 0));
	}

	protected WhatDaq(DPMList list, DPMRequest request, AcnetRequest acnetReq) throws AcnetStatusException, DeviceNotFoundException
	{
	
		this.list = list;
		this.request = request;
		this.refId = request.refId();
		this.options = EnumSet.noneOf(Option.class);
		this.defaultEvent = (request.getEvent() instanceof DefaultEvent);
		this.property = acnetReq.getProperty();
		this.dInfo = DeviceCache.get(acnetReq.getDevice());
		this.pInfo = propInfo();
		this.field = acnetReq.getField();

		if (defaultEvent) {
			try {
				this.event = Event.parse(pInfo.defEvent);
			} catch (EventFormatException e) {
				throw new AcnetStatusException(DPM_BAD_EVENT);
			}
		} else
			this.event = request.getEvent();

		this.dipi = dipi(dInfo.di, property.pi);
		this.node = Node.get(pInfo.node);

		this.error = 0;
		this.defaultLength = pInfo.atomicSize;
		this.maxLength = pInfo.size;
		this.protection = 0;
		System.arraycopy(pInfo.ssdn, 0, this.ssdn, 0, pInfo.ssdn.length);
		this.ftd = pInfo.ftd;
		this.poolMask = 0;
		this.receiveData = null; 
		this.alarmListName = "";

		if (acnetReq.fromDatabase())
			request.dataSource(new DatabaseSource());
		else if (request.isForLiveData()) {
		 	if (pInfo.controlSystem == DeviceInfo.ControlSystemType.Epics && node.type() == NodeType.EPICS)
				request.dataSource(new EpicsSource());
			else
				request.dataSource(new AcnetSource());
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
			this.daqName = this.dInfo.name + "@" + this.event;

		this.loggedName = acnetReq.toLoggedString();
	}

	protected WhatDaq(DPMList list, DPMRequest request)
	{
		this.list = list;
		this.refId = request.refId();
		this.request = request;
		this.options = EnumSet.noneOf(Option.class);
		this.property = Property.UNKNOWN;
		this.dipi = 0;
		this.dInfo = new DeviceInfo(request.getDevice());
		this.field = Field.getDefaultField(property);
		this.pInfo = new DeviceInfo.PropertyInfo();

		if (request.getEvent() instanceof DefaultEvent) {
			this.defaultEvent = true;
			this.event = new PeriodicEvent(true);
		} else {
			this.defaultEvent = false;
			this.event = request.getEvent();
		}

		this.defaultEvent = (event instanceof DefaultEvent);
		this.alarmListName = "";
		this.daqName = request.getDevice();
		this.loggedName = request.getDevice();
	}

	protected WhatDaq(WhatDaq whatDaq)
	{
		this.list = whatDaq.list;
		this.refId = whatDaq.refId;
		this.request = whatDaq.request;
		this.options = whatDaq.options;
		this.property = whatDaq.property;

		this.dInfo = whatDaq.dInfo;
		this.field = whatDaq.field;
		this.pInfo = whatDaq.pInfo;
		this.node = whatDaq.node;

		this.dipi = whatDaq.dipi;
		this.event = whatDaq.event;

		this.error = 0;
		this.length = whatDaq.length;
		this.offset = whatDaq.offset;
		this.defaultLength = whatDaq.defaultLength;
		this.maxLength = whatDaq.maxLength;
		this.protection = whatDaq.protection;
		this.ssdn = whatDaq.ssdn;
		this.ftd = whatDaq.ftd;
		this.poolMask = 0;
		this.receiveData = this; 
		this.user = whatDaq.user;
		this.defaultEvent = whatDaq.defaultEvent;
		this.alarmListName = whatDaq.alarmListName;
		this.rawOffset = whatDaq.rawOffset;
		this.daqName = whatDaq.daqName;
		this.loggedName = whatDaq.loggedName;
	}

	public final DPMList list()
	{
		return list;
	}

	public final long refId()
	{
		return refId;
	}
	
	public final DeviceInfo deviceInfo()
	{
		return dInfo;
	}

	public final DeviceInfo.PropertyInfo propertyInfo()
	{
		return pInfo;
	}

	public static int dipi(int di, int pi)
	{
		return (di & 0xffffff) | (pi << 24);
	}

	public final String name()
	{
		return dInfo.name;
	}

	public final String daqName()
	{
		return daqName;
	}

	public final String longName()
	{
		return dInfo.longName;
	}

	public final String loggedName()
	{
		return loggedName;
	}

	public final String description()
	{
		return dInfo.description;
	}

	public final String alarmListName()
	{
		return alarmListName;
	}

	public final int di()
	{
		return dInfo.di;
	}

	public final int pi()
	{
		return property.pi;
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

	public final Property property()
	{
		return property;
	}

	public final Field field()
	{
		return field;
	}

	public final int offset()
	{
		return offset;
	}

	public final byte[] ssdn()
	{
		return ssdn;
	}

	public final byte[] setting()
	{
		return setting;
	}

	public final boolean defaultEvent()
	{
		return defaultEvent;
	}

	public final double minimum()
	{
		switch (property) {
		 case READING:
		 	return dInfo.reading.prop.minimum;

		 case SETTING:
		 	return dInfo.setting.prop.minimum;
		}

		throw new RuntimeException("exception in maximum()");
	}

	public final double maximum()
	{
		switch (property) {
		 case READING:
		 	return dInfo.reading.prop.maximum;

		 case SETTING:
		 	return dInfo.setting.prop.maximum;
		}

		throw new RuntimeException("exception in maximum()");
	}

	public final void setOption(Option opt)
	{
		options.add(opt);
	}

	public final void clearOption(Option opt)
	{
		options.remove(opt);
	}

	public final boolean getOption(Option opt)
	{
		return options.contains(opt);
	}

	public final boolean isSettableProperty()
	{
		switch (property) {
			case SETTING:
			case CONTROL:
		 		return true;

		 	default:
		 		return false;
		}
	}

	public final boolean isStateDevice()
	{
		return dInfo.name.charAt(0) == 'V';		
	}

	public final String foreignName()
	{
		return this.pInfo.foreignName;
	}

	public final String foreignType()
	{
		return pInfo.controlSystem.toString();
	}

	public final boolean hasEvent()
	{
		return !(event instanceof NeverEvent);		
	}

	public final boolean isRepetitive()
	{
		return event.isRepetitive();
	}

	public final void setReceiveData(ReceiveData receiveData)
	{
		this.receiveData = (receiveData == null ? this : receiveData);
	}

	final public void receiveData(ReceiveData receiveData)
	{
		this.receiveData = (receiveData == null ? this : receiveData);
	}

	public void setUser(PoolUser user)
	{
		this.user = user;
	}

    private final DeviceInfo.PropertyInfo propInfo() throws AcnetStatusException
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
				return new DeviceInfo.PropertyInfo(property.pi, propToNode(), 128, 1);

			case INDEX:
				return new DeviceInfo.PropertyInfo(property.pi, propToNode(), 4, 4);

			case LONG_NAME:
				return new DeviceInfo.PropertyInfo(property.pi, propToNode(), 128, 1);

			case ALARM_LIST_NAME:
				return new DeviceInfo.PropertyInfo(property.pi, propToNode(), 128, 1);
        }

        throw new AcnetStatusException(DBM_NOPROP);
    }

	private final int propToNode() throws AcnetStatusException
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

	public final String units()
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

	public final int rawInputLength()
	{
		if (property == Property.READING) {
			if (dInfo.reading != null)
				return dInfo.reading.scaling.primary.inputLen;
		} else if (dInfo.setting != null)
			return dInfo.setting.scaling.primary.inputLen;

		return 0;
	}

	final void clearSetting()
	{
		setting = null;
	}

    final public void setSetting(byte data[]) throws AcnetStatusException
	{
        if (data.length == length) {
			setting = data;
		} else
			throw new AcnetStatusException(DPM_BAD_LENGTH);
    }

	final private int end()
	{
		return offset + length;
	}

	final public double getEventFrequency()
	{
		if (event instanceof PeriodicEvent)
			return 1.0 / (((PeriodicEvent) event).getPeriodValue().getTimeMillis()) * 1000.0;

		return 0;
	}

	final public boolean contains(WhatDaq whatDaq)
	{
		return dInfo.di == whatDaq.dInfo.di && property.pi == whatDaq.property.pi && 
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
		return Objects.hash(dInfo.di, property.pi, length, offset);
	}

	@Override
	public int compareTo(WhatDaq whatDaq)
	{
		if (dInfo.di == whatDaq.dInfo.di) {
			if (property.pi == whatDaq.property.pi) {
				if (length == whatDaq.length) {
					if (offset == whatDaq.offset)
						return 0;

					return offset > whatDaq.offset ? 1 : -1;
				}
				return length > whatDaq.length ? 1 : -1;
			}
			return property.pi > whatDaq.property.pi ? 1 : -1;
		}
		return dInfo.di > whatDaq.dInfo.di ? 1 : -1;
	}

	public boolean isEqual(WhatDaq whatDaq)
	{
		return compareTo(whatDaq) == 0;
	}

	@Override
	public void receiveStatus(int status, long timestamp, long cycle)
	{
	}

	@Override
	public void receiveData(ByteBuffer data, long timestamp, long cycle)
	{
	}

	@Override
	public String toString()
	{
		return String.format("%-14s %-8s %-8d %-16s L:%-6d O:%-6d %02x%02x/%02x%02x/%02x%02x/%02x%02x %s",
								dInfo.name, node.name(), dInfo.di, property, length, offset,
								ssdn[1], ssdn[0], ssdn[3], ssdn[2], ssdn[5], ssdn[4], ssdn[7], ssdn[6],
								setting != null ? "SettingReady" : "");
	}

	public final boolean isArray()
	{
		return (property.pi == Property.READING.pi || property.pi == Property.SETTING.pi)
					&& length != defaultLength && numElements > 1 && rawOffset == defaultLength;
	}

	public final Node node()
	{
		return node;
	}

	public final int error()
	{
		return error;
	}

	public final void error(int error)
	{
		this.error = error;
	}

	public final int getMaxLength()
	{
		return maxLength;
	}

	public final int getDeviceTypeId()
	{
		return dataTypeId;
	}

	public final int getNumberElements()
	{
		return numElements;
	}

	public final int getArrayElement()
	{
		return arrayElement;
	}

	public final byte[] getSetting()
	{
		return setting;
	}

	public final boolean isSettingReady()
	{
		return setting != null;
	}

	public final long getProtection()
	{
		return protection;
	}

	public final int ftd()
	{
		return pInfo.ftd;
	}

	public final Event event()
	{
		return event;
	}

	public final boolean monitorChange()
	{
		if (event instanceof PeriodicEvent)
			return !((PeriodicEvent) event).continuous();

		return false;
	}

	public final void event(Event event)
	{
		this.event = event;
	}

	public final ReceiveData getReceiveData()
	{
		return receiveData;
	}

	public final PoolUser user()
	{
		return user;
	}

	public final void redirect(Node node)
	{
		this.node = node;
	}

	public final boolean isMarkedForDelete()
	{
		return delete;
	}

	public final void setMarkedForDelete()
	{
		receiveData = this;
		delete = true;
	}

	public final int getDefaultLength()
	{
		return defaultLength;
	}

	public final void setLengthOffset(int length, int offset)
	{
		this.length = length;
		this.offset = offset;

		if (offset != 0 && defaultLength != 0)
			arrayElement = offset / defaultLength;
	}

	public final void lengthOffset(int length, int offset)
	{
		this.length = length;
		this.offset = offset;

		if (offset != 0 && defaultLength != 0)
			arrayElement = offset / defaultLength;
	}

	public DataReplier dataReplier(DPMProtocolReplier protocolReplier)
	{
		switch (request.dataSource().dataType(this)) {
			case Raw:
				return DataReplierImpl.RAW.get(this, protocolReplier);

			case Primary:
				return DataReplierImpl.PRIMARY.get(this, protocolReplier);

			case Scaled:
				return DataReplierImpl.SCALED.get(this, protocolReplier);
		}

		return new DataReplierImpl.Status(this, protocolReplier, DIO_NOSCALE);
	}

}
