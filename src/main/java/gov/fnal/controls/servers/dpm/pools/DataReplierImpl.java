// $Id: DataReplierImpl.java,v 1.2 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import java.util.Arrays;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.concurrent.TimeUnit;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.drf3.Field;
import gov.fnal.controls.servers.dpm.drf3.Property;

import gov.fnal.controls.servers.dpm.TimeNow;
import gov.fnal.controls.servers.dpm.ListId;
import gov.fnal.controls.servers.dpm.DataReplier;
import gov.fnal.controls.servers.dpm.DPMProtocolReplier;

import gov.fnal.controls.servers.dpm.scaling.Scaling;
import gov.fnal.controls.servers.dpm.scaling.DPMReadSetScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMAnalogAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMDigitalAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMBasicStatusScaling;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class DataReplierImpl implements DataReplier, AcnetErrors, TimeNow
{
	final WhatDaq whatDaq;
	final DPMProtocolReplier protocolReplier;

	DataReplierImpl(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
	{
		this.whatDaq = whatDaq;
		this.protocolReplier = protocolReplier;
	}

    public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendStatus(DIO_NOSCALE);
	}
	
    public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendStatus(DIO_NOSCALE);
	}

    public void sendReply(double[] values, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendStatus(DIO_NOSCALE);
	}

    public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendStatus(DIO_NOSCALE);
	}

	public void sendReply(int value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendStatus(DIO_NOSCALE);
	}

    public void sendReply(String value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendStatus(DIO_NOSCALE);
	}

    public void sendReply(String value, int index, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendStatus(DIO_NOSCALE);
	}

	public void sendReply(double[] values, long[] micros, long seqNo) throws InterruptedException, IOException, AcnetStatusException
	{
		protocolReplier.sendStatus(DIO_NOSCALE);
	}

	static class Status extends DataReplierImpl
	{
		final int status;

		Status(WhatDaq whatDaq, DPMProtocolReplier protocolReplier, int status)
		{
			super(whatDaq, protocolReplier);
			this.status = status;
		}

		@Override
		public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
		{
			protocolReplier.sendReply(whatDaq.refId(), status, timestamp, 0);
		}

		@Override
		public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
		{
			protocolReplier.sendReply(whatDaq.refId(), status, timestamp, 0);
		}

		@Override
		public void sendReply(double[] values, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
		{
			protocolReplier.sendReply(whatDaq.refId(), status, timestamp, 0);
		}

		@Override
		public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
		{
			protocolReplier.sendReply(whatDaq.refId(), status, timestamp, 0);
		}

		@Override
		public void sendReply(int value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
		{
			protocolReplier.sendReply(whatDaq.refId(), status, timestamp, 0);
		}

		@Override
		public void sendReply(String value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
		{
			protocolReplier.sendReply(whatDaq.refId(), status, timestamp, 0);
		}

		@Override
		public void sendReply(String value, int index, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
		{
			protocolReplier.sendReply(whatDaq.refId(), status, timestamp, 0);
		}

		@Override
		public void sendReply(double[] values, long[] micros, long seqNo) throws InterruptedException, IOException, AcnetStatusException
		{
			protocolReplier.sendReply(whatDaq.refId(), status, now(), 0);
		}
	}

	static class PassThru extends DataReplierImpl
	{
		PassThru(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
		{
			super(whatDaq, protocolReplier);
		}

		@Override
		public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
		{
			protocolReplier.sendReply(whatDaq, data, timestamp, cycle);
		}

		@Override
		public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
		{
			protocolReplier.sendReply(whatDaq, Arrays.copyOfRange(data, offset, offset + whatDaq.length()), timestamp, cycle);
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
		public void sendReply(int value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
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

	static class RAW implements AcnetErrors
	{
		static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
		{
			switch (whatDaq.property()) {
				case READING:
				case SETTING:
					return ReadSet.get(whatDaq, protocolReplier);

				case ANALOG:
					return AnalogAlarm.get(whatDaq, protocolReplier);

				case DIGITAL:
					return DigitalAlarm.get(whatDaq, protocolReplier);

				case STATUS:
					return BasicStatus.get(whatDaq, protocolReplier);

				case CONTROL:
					return new Status(whatDaq, protocolReplier, 0);			
			}
			
			return new Status(whatDaq, protocolReplier, DIO_NOSCALE);
		}

		static class ReadSet implements AcnetErrors
		{
			static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
			{
				switch (whatDaq.field()) {
					case RAW:
						return new PassThru(whatDaq, protocolReplier);

					case PRIMARY:
						return Primary.get(whatDaq, protocolReplier);

					case SCALED:
						return Scaled.get(whatDaq, protocolReplier);
				}

				return new Status(whatDaq, protocolReplier, DIO_NOSCALE);
			}

			static class Primary
			{
				static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					if (whatDaq.isArray())
						return new Array(whatDaq, protocolReplier);
					else
						return new Single(whatDaq, protocolReplier);
				}

				static class Single extends DataReplierImpl
				{
					final DPMReadSetScaling scaling;

					Single(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
					{
						super(whatDaq, protocolReplier);
						this.scaling = DPMReadSetScaling.get(whatDaq);
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
				}

				static class Array extends DataReplierImpl
				{
					final double values[];
					final DPMReadSetScaling scaling;

					Array(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
					{
						super(whatDaq, protocolReplier);
						this.values = new double[whatDaq.getNumberElements()];
						this.scaling = DPMReadSetScaling.get(whatDaq);
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
				}
			}

			static class Scaled
			{
				static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					if (whatDaq.isArray())
						return Array.get(whatDaq, protocolReplier);
					else
						return Single.get(whatDaq, protocolReplier);
				}

				static class Single
				{
					static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
					{
						if (Scaling.usesStrings(whatDaq.deviceInfo(), whatDaq.property())) 
							return new Text(whatDaq, protocolReplier);
						else
							return new Number(whatDaq, protocolReplier);
					}

					static class Number extends DataReplierImpl
					{
						final DPMReadSetScaling scaling;

						Number(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
						{
							super(whatDaq, protocolReplier);
							this.scaling = DPMReadSetScaling.get(whatDaq);
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
					} 

					static class Text extends DataReplierImpl
					{
						final DPMReadSetScaling scaling;

						Text(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
						{
							super(whatDaq, protocolReplier);
							this.scaling = DPMReadSetScaling.get(whatDaq);
						}

						@Override
						public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
						{
							protocolReplier.sendReply(whatDaq, scaling.rawToString(data, whatDaq.length()), timestamp, cycle);
						}

						@Override
						public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
						{
							protocolReplier.sendReply(whatDaq, scaling.rawToString(data, offset, whatDaq.length()), timestamp, cycle);
						}
					}
				}

				static class Array
				{
					static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
					{
						if (Scaling.usesStrings(whatDaq.deviceInfo(), whatDaq.property())) 
							return new Text(whatDaq, protocolReplier);
						else
							return new Number(whatDaq, protocolReplier);
					}

					static class Number extends DataReplierImpl
					{
						final double values[];
						final DPMReadSetScaling scaling;

						Number(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
						{
							super(whatDaq, protocolReplier);
							this.values = new double[whatDaq.getNumberElements()];
							this.scaling = DPMReadSetScaling.get(whatDaq);
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
					}

					static class Text extends DataReplierImpl
					{
						final String values[];
						final DPMReadSetScaling scaling;

						Text(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
						{
							super(whatDaq, protocolReplier);
							this.values = new String[whatDaq.getNumberElements()];
							this.scaling = DPMReadSetScaling.get(whatDaq);
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
				}
			}
		}

		static class AnalogAlarm
		{
			static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
			{
				switch (whatDaq.field()) {
					case RAW:
						return new PassThru(whatDaq, protocolReplier);

					default:
						return new Replier(whatDaq, protocolReplier);
				}
			}

			static class Replier extends DataReplierImpl
			{
				final DPMAnalogAlarmScaling scaling;

				Replier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					super(whatDaq, protocolReplier);
					this.scaling = DPMAnalogAlarmScaling.get(whatDaq);
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

				final void sendReply(long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					switch (whatDaq.field()) {
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
		}

		static class DigitalAlarm
		{
			static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
			{
				switch (whatDaq.field()) {
					case RAW:
						return new PassThru(whatDaq, protocolReplier);

					default:
						return new Replier(whatDaq, protocolReplier);
				}
			}

			static class Replier extends DataReplierImpl
			{
				final DPMDigitalAlarmScaling scaling;

				Replier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					super(whatDaq, protocolReplier);

					this.scaling = DPMDigitalAlarmScaling.get(whatDaq);
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

				final void sendReply(long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					switch (whatDaq.field()) {
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
		}

		static class BasicStatus
		{
			static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
			{
				switch (whatDaq.field()) {
					case RAW:
						return new Raw(whatDaq, protocolReplier);

					default:
						return new Replier(whatDaq, protocolReplier);
				}
			}

			static class Raw extends DataReplierImpl
			{
				final DPMBasicStatusScaling scaling;

				Raw(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					super(whatDaq, protocolReplier);
					this.scaling = DPMBasicStatusScaling.get(whatDaq);
				}

				@Override
				public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					protocolReplier.sendReply(whatDaq, scaling.unscale(value, whatDaq.length()), timestamp, cycle);
				}

				@Override
				public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					protocolReplier.sendReply(whatDaq, data, timestamp, cycle);
				}
			}

			static class Replier extends DataReplierImpl
			{
				final DPMBasicStatusScaling scaling;

				Replier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					super(whatDaq, protocolReplier);
					this.scaling = DPMBasicStatusScaling.get(whatDaq);
				}

				@Override
				public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					scaling.scale(whatDaq, value);
					reply(timestamp, cycle);
				}

				@Override
				public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					scaling.scale(whatDaq, data, offset);
					reply(timestamp, cycle);
				}

				@Override
				public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					scaling.scale(whatDaq, data);
					reply(timestamp, cycle);
				}

				private final void reply(long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					switch (whatDaq.field()) {
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

					 case BIT_NAMES:
						protocolReplier.sendReply(whatDaq, scaling.bitNames(), timestamp, cycle);
						break;

					 case BIT_VALUES:
						protocolReplier.sendReply(whatDaq, scaling.bitValues(), timestamp, cycle);
						break;

					 case EXTENDED_TEXT:
						protocolReplier.sendReply(whatDaq, scaling.extendedText(), timestamp, cycle);
						break;
					}
				}
			}
		}
	}

	static class PRIMARY implements AcnetErrors
	{
		static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
		{
			switch (whatDaq.property()) {
				case READING:
				case SETTING:
					return ReadSet.get(whatDaq, protocolReplier);

				case STATUS:
					return BasicStatus.get(whatDaq, protocolReplier);	
			}

			return new Status(whatDaq, protocolReplier, DIO_NOSCALE);
		}

		static class ReadSet implements AcnetErrors
		{
			static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
			{
				switch (whatDaq.field()) {
					case RAW:
						return new Raw(whatDaq, protocolReplier);

					case PRIMARY:
						return new PassThru(whatDaq, protocolReplier);

					case SCALED:
						return new Scaled(whatDaq, protocolReplier);
				}

				return new Status(whatDaq, protocolReplier, DIO_NOSCALE);
			}

			static class Raw extends DataReplierImpl
			{
				final DPMReadSetScaling scaling;

				Raw(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					super(whatDaq, protocolReplier);
					this.scaling = DPMReadSetScaling.get(whatDaq);
				}

				@Override
				public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					protocolReplier.sendReply(whatDaq, scaling.primaryToRaw(value), timestamp, cycle);
				}

				@Override
				public void sendReply(double[] values, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					protocolReplier.sendReply(whatDaq, scaling.primaryToRaw(values), timestamp, cycle);
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
			}

			static class Scaled extends DataReplierImpl
			{
				final DPMReadSetScaling scaling;

				Scaled(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					super(whatDaq, protocolReplier);

					this.scaling = DPMReadSetScaling.get(whatDaq);
				}

				@Override
				public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					protocolReplier.sendReply(whatDaq, scaling.primaryToCommon(value), timestamp, cycle);
				}
			}
		}

		static class BasicStatus
		{
			static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
			{
				switch (whatDaq.field()) {
					case RAW:
						return new Raw(whatDaq, protocolReplier);

					default:
						return new Replier(whatDaq, protocolReplier);
				}
			}

			static class Raw extends DataReplierImpl
			{
				final DPMBasicStatusScaling scaling;

				Raw(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					super(whatDaq, protocolReplier);
					this.scaling = DPMBasicStatusScaling.get(whatDaq);
				}

				@Override
				public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					protocolReplier.sendReply(whatDaq, scaling.unscale(value, whatDaq.length()), timestamp, cycle);
				}

				@Override
				public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					protocolReplier.sendReply(whatDaq, data, timestamp, cycle);
				}
			}

			static class Replier extends DataReplierImpl
			{
				final DPMBasicStatusScaling scaling;

				Replier(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					super(whatDaq, protocolReplier);
					this.scaling = DPMBasicStatusScaling.get(whatDaq);
				}

				@Override
				public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					scaling.scale(whatDaq, value);
					reply(timestamp, cycle);
				}

				@Override
				public void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					scaling.scale(whatDaq, data, offset);
					reply(timestamp, cycle);
				}

				@Override
				public void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					scaling.scale(whatDaq, data);
					reply(timestamp, cycle);
				}

				private final void reply(long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					switch (whatDaq.field()) {
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

					 case BIT_NAMES:
						protocolReplier.sendReply(whatDaq, scaling.bitNames(), timestamp, cycle);
						break;

					 case BIT_VALUES:
						protocolReplier.sendReply(whatDaq, scaling.bitValues(), timestamp, cycle);
						break;

					 case EXTENDED_TEXT:
						protocolReplier.sendReply(whatDaq, scaling.extendedText(), timestamp, cycle);
						break;
					}
				}
			}
		}
	}

	static class SCALED implements AcnetErrors
	{
		static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
		{
			switch (whatDaq.property()) {
				case READING:
				case SETTING:
					return ReadSet.get(whatDaq, protocolReplier);

				 case DESCRIPTION:
				 case LONG_NAME:
				 case ALARM_LIST_NAME:
				 case INDEX:
				 case UNKNOWN:
					return new PassThru(whatDaq, protocolReplier);
			}

			return new Status(whatDaq, protocolReplier, DIO_NOSCALE);
		}

		static class ReadSet
		{
			static DataReplier get(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
			{
				switch (whatDaq.field()) {
					case RAW:
						return new Raw(whatDaq, protocolReplier);

					case PRIMARY:
						return new Primary(whatDaq, protocolReplier);
				}

				return new PassThru(whatDaq, protocolReplier);
			}

			static class Raw extends DataReplierImpl
			{
				final DPMReadSetScaling scaling;

				Raw(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					super(whatDaq, protocolReplier);

					this.scaling = DPMReadSetScaling.get(whatDaq);
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
			}

			static class Primary extends DataReplierImpl
			{
				final DPMReadSetScaling scaling;

				Primary(WhatDaq whatDaq, DPMProtocolReplier protocolReplier)
				{
					super(whatDaq, protocolReplier);

					this.scaling = DPMReadSetScaling.get(whatDaq);
				}

				@Override
				public void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					protocolReplier.sendReply(whatDaq, scaling.commonToPrimary(value), timestamp, cycle);
				}

				@Override
				public void sendReply(double[] values, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException
				{
					final double[] primaryValues = new double[values.length];

					protocolReplier.sendReply(whatDaq, scaling.commonToPrimary(values, primaryValues), timestamp, cycle);
				}
			}
		}
	}
}
