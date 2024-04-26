// $Id: SettingData.java,v 1.4 2024/03/27 21:07:38 kingc Exp $
package gov.fnal.controls.servers.dpm;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;

public abstract class SettingData
{
	final long refId;

	public static interface Handler
	{
		public void handle(WhatDaq whatDaq, byte[] data) throws AcnetStatusException;
		public void handle(WhatDaq whatDaq, double data) throws AcnetStatusException;
		public void handle(WhatDaq whatDaq, double[] data) throws AcnetStatusException;
		public void handle(WhatDaq whatDaq, String data) throws AcnetStatusException;
		public void handle(WhatDaq whatDaq, String[] data) throws AcnetStatusException;
	}

	abstract public void deliverTo(WhatDaq whatDaq, Handler handler) throws AcnetStatusException;

	private SettingData(long refId)
	{
		this.refId = refId;
	}

	private static class ByteArray extends SettingData
	{
		final byte[] data;

		ByteArray(byte[] data, long refId)
		{
			super(refId);
			this.data = data;
		}

		@Override
		public void deliverTo(WhatDaq whatDaq, Handler handler) throws AcnetStatusException
		{
			handler.handle(whatDaq, data);
		}
	}

	private static class Double extends SettingData
	{
		final double data;

		Double(double data, long refId)
		{
			super(refId);
			this.data = data;
		}

		@Override
		public void deliverTo(WhatDaq whatDaq, Handler handler) throws AcnetStatusException
		{
			handler.handle(whatDaq, data);
		}
	}

	private static class DoubleArray extends SettingData
	{
		final double[] data;

		DoubleArray(double[] data, long refId)
		{
			super(refId);
			this.data = data;
		}

		@Override
		public void deliverTo(WhatDaq whatDaq, Handler handler) throws AcnetStatusException
		{
			handler.handle(whatDaq, data);
		}
	}

	private static class Text extends SettingData
	{
		final String data;

		Text(String data, long refId)
		{
			super(refId);
			this.data = data;
		}

		@Override
		public void deliverTo(WhatDaq whatDaq, Handler handler) throws AcnetStatusException
		{
			handler.handle(whatDaq, data);
		}
	}

	private static class TextArray extends SettingData
	{
		final String[] data;

		TextArray(String[] data, long refId)
		{
			super(refId);
			this.data = data;
		}

		@Override
		public void deliverTo(WhatDaq whatDaq, Handler handler) throws AcnetStatusException
		{
			handler.handle(whatDaq, data);
		}
	}

	public static SettingData create(byte[] data, long refId)
	{
		return new ByteArray(data, refId);
	}

	public static SettingData create(double data, long refId)
	{
		return new Double(data, refId);
	}

	public static SettingData create(double[] data, long refId)
	{
		return new DoubleArray(data, refId);
	}

	public static SettingData create(String[] data, long refId)
	{
		return new TextArray(data, refId);
	}

	public static SettingData create(String data, long refId)
	{
		return new Text(data, refId);
	}
}

