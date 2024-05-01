// $Id: DataReplier.java,v 1.42 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.Arrays;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.concurrent.TimeUnit;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.drf3.Field;
import gov.fnal.controls.servers.dpm.drf3.Property;

import gov.fnal.controls.servers.dpm.ListId;
import gov.fnal.controls.servers.dpm.scaling.Scaling;
import gov.fnal.controls.servers.dpm.scaling.DPMReadSetScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMAnalogAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMDigitalAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMBasicStatusScaling;
import gov.fnal.controls.servers.dpm.DataReplier;
import gov.fnal.controls.servers.dpm.SettingStatus;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;


public interface DataReplier
{
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException;
	
    default public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
	}

    default public void sendReply(double[] values, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
	}

    default public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
	}

	default public void sendReply(int value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
	}

    default public void sendReply(String value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
	}

    default public void sendReply(String value, int index, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
	}

	default public void sendReply(double[] values, long[] micros, long seqNo) throws InterruptedException, IOException, AcnetStatusException
	{
	}

    public static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
    {
        final Field field = whatDaq.field;
        final Property property = whatDaq.property;
        //final boolean isArray = whatDaq.isArray();

        if (whatDaq.isSettingReady()) {
            return new StatusReplier(whatDaq, 0, protocolReplier);
        } else if (field == Field.RAW) {
            return new RawReplier(whatDaq, protocolReplier);
		} else if (Scaling.usesStrings(whatDaq.dInfo, whatDaq.property)) {
            return whatDaq.isArray() ? new ReadSetTextArrayReplier(whatDaq, protocolReplier) :
            							new ReadSetTextReplier(whatDaq, protocolReplier);
        } else {
            switch (property) {
             case READING:
             case SETTING:
                //final boolean isArray = whatDaq.isArray();
                //final boolean stringScaling = DPMReadSetScaling.usesStringScaling(whatDaq);

                switch (field) {
                 case SCALED:
                    //if (stringScaling) {
                       // return isArray ? new ReadSetTextArrayReplier(whatDaq, protocolReplier) :
                        //                    new ReadSetTextReplier(whatDaq, protocolReplier);
                    //} else {
                        return whatDaq.isArray() ? new ReadSetCommonArrayReplier(whatDaq, protocolReplier) :
                                            		new ReadSetCommonReplier(whatDaq, protocolReplier);
                    //}

                 case PRIMARY:
                    return whatDaq.isArray() ? new ReadSetPrimaryArrayReplier(whatDaq, protocolReplier) :
                                            	new ReadSetPrimaryReplier(whatDaq, protocolReplier);
                }

             case STATUS:
                return new BasicStatusReplier(whatDaq, protocolReplier);

             case ANALOG:
                return new AnalogAlarmReplier(whatDaq, protocolReplier);

             case DIGITAL:
                return new DigitalAlarmReplier(whatDaq, protocolReplier);

             case CONTROL:
                return new StatusReplier(whatDaq, 0, protocolReplier);

             case DESCRIPTION:
             case LONG_NAME:
             case ALARM_LIST_NAME:
                return new ReadSetTextReplier(whatDaq, protocolReplier);

             case INDEX:
                return new ReadSetCommonReplier(whatDaq, protocolReplier);
            }
        }
        
        return new StatusReplier(whatDaq, 0, protocolReplier);
	}
}

class StatusReplier implements DataReplier
{
	final WhatDaq whatDaq;
	final int status;
	final DPMProtocolReplier protocolReplier;

	StatusReplier(WhatDaq whatDaq, int status, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.status = status;
		this.protocolReplier = protocolReplier;
	}

	@Override
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, status, System.currentTimeMillis(), 0);
	}

	@Override
	public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, status, System.currentTimeMillis(), 0);
	}
}

class RawReplier implements DataReplier
{
	final WhatDaq whatDaq;
	final DPMReadSetScaling scaling;
	final DPMProtocolReplier protocolReplier;

	RawReplier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.scaling = DPMReadSetScaling.get(whatDaq);
		this.protocolReplier = protocolReplier;
	}

	@Override
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, data, timestamp, cycle);
	}

	@Override
    public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, Arrays.copyOfRange(data, offset, offset + whatDaq.getLength()), timestamp, cycle);
	}

	@Override
    public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.commonToRaw(value), timestamp, cycle);
	}

	@Override
    public void sendReply(double[] values, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.commonToRaw(values), timestamp, cycle);
	}

	@Override
    public void sendReply(String value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.stringToRaw(value), timestamp, cycle);
	}

	@Override
    public void sendReply(String value, int index, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.stringToRaw(value), timestamp, cycle);
	}

	@Override
	public void sendReply(double[] values, long[] micros, long seqNo) throws InterruptedException, IOException, AcnetStatusException
	{
	}
}

class ReadSetPrimaryReplier implements DataReplier
{
	final WhatDaq whatDaq;
	final DPMReadSetScaling scaling;
	final DPMProtocolReplier protocolReplier;

	ReadSetPrimaryReplier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.scaling = DPMReadSetScaling.get(whatDaq);
		this.protocolReplier = protocolReplier;
	}

	@Override
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToPrimary(data), timestamp, cycle);
	}

	@Override
    public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToPrimary(data, offset), timestamp, cycle);
	}

	@Override
    public void sendReply(double[] values, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final double[] primaryValues = new double[values.length];

		protocolReplier.sendReply(whatDaq, scaling.commonToPrimary(values, primaryValues), timestamp, cycle);
	}

	@Override
    public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.commonToPrimary(value), timestamp, cycle);
	}
}

class ReadSetCommonReplier implements DataReplier
{
	final WhatDaq whatDaq;
	final DPMReadSetScaling scaling;
	final DPMProtocolReplier protocolReplier;

	ReadSetCommonReplier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.scaling = DPMReadSetScaling.get(whatDaq);
		this.protocolReplier = protocolReplier;
	}

	@Override
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToCommon(data), timestamp, cycle);
	}

	@Override
    public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToCommon(data, offset), timestamp, cycle);
	}

	@Override
    public void sendReply(double[] values, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, values, timestamp, cycle);
	}

	@Override
    public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, value, timestamp, cycle);
	}

	@Override
    public void sendReply(String value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, value, timestamp, cycle);
	}

	@Override
    public void sendReply(String value, int index, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, value, timestamp, cycle);
	}

	@Override
	public void sendReply(double[] values, long[] micros, long seqNo) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, values, micros, seqNo);
	}
}

class ReadSetCommonArrayReplier implements DataReplier
{
	private final WhatDaq whatDaq;
	private final double values[];
	private final DPMReadSetScaling scaling;
	private final DPMProtocolReplier protocolReplier;

	ReadSetCommonArrayReplier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.values = new double[whatDaq.getNumberElements()];
		this.scaling = DPMReadSetScaling.get(whatDaq);
		this.protocolReplier = protocolReplier;
	}

	@Override
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToCommon(data, values), timestamp, cycle);
	}

	@Override
	public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToCommon(data, offset, values), timestamp, cycle);
	}

	@Override
	public void sendReply(double[] values, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, values, timestamp, cycle);
	}
}

class ReadSetPrimaryArrayReplier implements DataReplier
{
	private final WhatDaq whatDaq;
	private final double values[];
	private final DPMReadSetScaling scaling;
	private final DPMProtocolReplier protocolReplier;

	ReadSetPrimaryArrayReplier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.values = new double[whatDaq.getNumberElements()];
		this.scaling = DPMReadSetScaling.get(whatDaq);
		this.protocolReplier = protocolReplier;
	}

	@Override
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToPrimary(data, values), timestamp, cycle);
	}

	@Override
	public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToPrimary(data, offset, values), timestamp, cycle);
	}

	@Override
	public void sendReply(double[] values, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		final double[] primaryValues = new double[values.length];

		protocolReplier.sendReply(whatDaq, scaling.commonToPrimary(values, primaryValues), timestamp, cycle);
	}
}

class ReadSetTextReplier implements DataReplier
{
	final WhatDaq whatDaq;
	final DPMReadSetScaling scaling;
	final DPMProtocolReplier protocolReplier;

	ReadSetTextReplier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.scaling = DPMReadSetScaling.get(whatDaq);
		this.protocolReplier = protocolReplier;

		logger.log(Level.FINER, getClass().toString());
	}

	@Override
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToString(data, whatDaq.getLength()), timestamp, cycle);
	}

	@Override
	public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToString(data, offset, whatDaq.getLength()), timestamp, cycle);
	}

	@Override
	public void sendReply(String value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, value, timestamp, cycle);
	}

	@Override
	public void sendReply(String value, int index, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, value, timestamp, cycle);
	}

	@Override
	public void sendReply(double[] values, long[] micros, long seqNo) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, values, micros, seqNo);
	}
}

class ReadSetTextArrayReplier implements DataReplier
{
	private final WhatDaq whatDaq;
	private final String values[];
	private final DPMReadSetScaling scaling;
	private final DPMProtocolReplier protocolReplier;

	ReadSetTextArrayReplier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.values = new String[whatDaq.getNumberElements()];
		this.scaling = DPMReadSetScaling.get(whatDaq);
		this.protocolReplier = protocolReplier;

		logger.log(Level.FINER, getClass().toString());
	}

	@Override
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToString(data, whatDaq.getDefaultLength(), values), timestamp, cycle);
	}

	@Override
	public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, scaling.rawToString(data, offset, whatDaq.getDefaultLength(), values), timestamp, cycle);
	}
}

class BasicStatusReplier implements DataReplier, AcnetErrors
{
	final WhatDaq whatDaq;
	final DPMBasicStatusScaling scaling;
	final DPMProtocolReplier protocolReplier;

	BasicStatusReplier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.scaling = DPMBasicStatusScaling.get(whatDaq);
		this.protocolReplier = protocolReplier;
	}

	private final void reply(long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
        switch (whatDaq.field) {
		 case ALL:
		 	protocolReplier.sendReply(whatDaq, scaling, timestamp, cycle);
			break;

		 case TEXT:
		 	protocolReplier.sendReply(whatDaq, scaling.textValue(), timestamp, cycle);
			break;

         case ON:
            protocolReplier.sendReply(whatDaq, scaling.isOn(), timestamp, cycle);
            break;

         case READY:
            protocolReplier.sendReply(whatDaq, scaling.isReady(), timestamp, cycle);
            break;

         case REMOTE:
            protocolReplier.sendReply(whatDaq, scaling.isRemote(), timestamp, cycle);
            break;

         case POSITIVE:
            protocolReplier.sendReply(whatDaq, scaling.isPositive(), timestamp, cycle);
            break;

         case RAMP:
            protocolReplier.sendReply(whatDaq, scaling.isRamp(), timestamp, cycle);
            break;

         case BIT_VALUE:
            protocolReplier.sendReply(whatDaq, scaling.rawStatus(), timestamp, cycle);
			break;

		 case EXTENDED_TEXT:
		 	protocolReplier.sendReply(whatDaq, scaling.extendedText(), timestamp, cycle);
			break;
        }
	}

	@Override
	public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		scaling.scale(whatDaq, value);
		reply(timestamp, cycle);
	}

	@Override
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		scaling.scale(whatDaq, data);
		reply(timestamp, cycle);
	}

	@Override
    public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		scaling.scale(whatDaq, data, offset);
		reply(timestamp, cycle);
	}

	@Override
	public void sendReply(double[] values, long[] micros, long seqNo) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendReply(whatDaq, values, micros, seqNo);	
	}
}

class AnalogAlarmReplier implements DataReplier, AcnetErrors
{
	private final WhatDaq whatDaq;
	private final DPMAnalogAlarmScaling scaling;
	private final DPMProtocolReplier protocolReplier;

	AnalogAlarmReplier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.scaling = DPMAnalogAlarmScaling.get(whatDaq);
		this.protocolReplier = protocolReplier;
	}

	@Override
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		scaling.scale(data);
		sendReply(timestamp, cycle);
	}

	@Override
    public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		scaling.scale(data, offset);
		sendReply(timestamp, cycle);
	}

	void sendReply(long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		switch (whatDaq.field) {
		 case ALL:
			protocolReplier.sendReply(whatDaq, scaling, timestamp, cycle);
			break;

		 case TEXT:
			protocolReplier.sendReply(whatDaq, scaling.textValue(), timestamp, cycle);	
			break;

         case MIN:
			protocolReplier.sendReply(whatDaq, scaling.min(), timestamp, cycle);	
			break;

         case MAX:
			protocolReplier.sendReply(whatDaq, scaling.max(), timestamp, cycle);	
			break;

         case RAW_MIN:
			protocolReplier.sendReply(whatDaq, scaling.rawMin(), timestamp, cycle);	
			break;

         case RAW_MAX:
			protocolReplier.sendReply(whatDaq, scaling.rawMax(), timestamp, cycle);	
			break;

         case NOM:
			protocolReplier.sendReply(whatDaq, scaling.nom(), timestamp, cycle);	
			break;

         case TOL:
			protocolReplier.sendReply(whatDaq, scaling.tol(), timestamp, cycle);	
			break;

         case RAW_NOM:
			protocolReplier.sendReply(whatDaq, scaling.rawNom(), timestamp, cycle);	
			break;

         case RAW_TOL:
			protocolReplier.sendReply(whatDaq, scaling.rawTol(), timestamp, cycle);	
			break;

		 case ALARM_ENABLE:
			protocolReplier.sendReply(whatDaq, scaling.enabled(), timestamp, cycle);	
			break;

         case ALARM_STATUS:
			protocolReplier.sendReply(whatDaq, scaling.inAlarm(), timestamp, cycle);	
			break;

         case TRIES_NEEDED:
			protocolReplier.sendReply(whatDaq, scaling.triesNeeded(), timestamp, cycle);	
			break;

         case TRIES_NOW:
			protocolReplier.sendReply(whatDaq, scaling.triesNow(), timestamp, cycle);	
			break;

         case ALARM_FTD:
			protocolReplier.sendReply(whatDaq, scaling.ftd(), timestamp, cycle);	
			break;

         case ABORT:
			protocolReplier.sendReply(whatDaq, scaling.abortCapable(), timestamp, cycle);	
			break;

         case ABORT_INHIBIT:
			protocolReplier.sendReply(whatDaq, scaling.abortInhibited(), timestamp, cycle);	
			break;

         case FLAGS:
			protocolReplier.sendReply(whatDaq, scaling.flags(), timestamp, cycle);	
			break;

		 default:
		 	throw new AcnetStatusException(DPM_BAD_REQUEST);
		}
	}
}

class DigitalAlarmReplier implements DataReplier, AcnetErrors
{
	private final WhatDaq whatDaq;
	private final DPMDigitalAlarmScaling scaling;
	private final DPMProtocolReplier protocolReplier;

	DigitalAlarmReplier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.scaling = DPMDigitalAlarmScaling.get(whatDaq);
		this.protocolReplier = protocolReplier;
	}

	@Override
    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		scaling.scale(data);
		sendReply(timestamp, cycle);
	}

	@Override
    public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		scaling.scale(data, offset);
		sendReply(timestamp, cycle);
	}

    private void sendReply(long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		//scaling.scale(data, offset);

		switch (whatDaq.field) {
		 case ALL:
			protocolReplier.sendReply(whatDaq, scaling, timestamp, cycle);
			break;

		 case TEXT:
			protocolReplier.sendReply(whatDaq, scaling.textValue(), timestamp, cycle);	
			break;
        
        case NOM:
			protocolReplier.sendReply(whatDaq, scaling.nom(), timestamp, cycle);	
			break;

        case MASK:
			protocolReplier.sendReply(whatDaq, scaling.mask(), timestamp, cycle);	
			break;

        case ALARM_ENABLE:
			protocolReplier.sendReply(whatDaq, scaling.enabled(), timestamp, cycle);	
			break;

        case ALARM_STATUS:
			protocolReplier.sendReply(whatDaq, scaling.inAlarm(), timestamp, cycle);	
			break;

        case TRIES_NEEDED:
			protocolReplier.sendReply(whatDaq, scaling.triesNeeded(), timestamp, cycle);	
			break;

        case TRIES_NOW:
			protocolReplier.sendReply(whatDaq, scaling.triesNow(), timestamp, cycle);	
			break;

        case ALARM_FTD:
			protocolReplier.sendReply(whatDaq, scaling.ftd(), timestamp, cycle);	
			break;

        case ABORT:
			protocolReplier.sendReply(whatDaq, scaling.abortCapable(), timestamp, cycle);	
			break;

        case ABORT_INHIBIT:
			protocolReplier.sendReply(whatDaq, scaling.abortInhibited(), timestamp, cycle);	
			break;

        case FLAGS:
			protocolReplier.sendReply(whatDaq, scaling.flags(), timestamp, cycle);	
			break;

		default:
			throw new AcnetStatusException(DPM_BAD_REQUEST);
        }
	}
}
