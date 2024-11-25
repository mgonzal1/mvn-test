// $Id: ExternalTrigger.java,v 1.9 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.drf3.Event;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class ExternalTrigger implements Trigger, AcnetErrors
{
	final WhatDaq whatDaq;
	final int armMask;
	final boolean armExternalSource;
	final int armSourceModifier;
	final int armDelay;
	final int armOffset;
	final int armValue;

	ExternalTrigger(String triggerString) throws AcnetStatusException
	{
		try {
			final int modifierIndex = triggerString.indexOf("x,mod=");

			if (modifierIndex != -1) {
				this.armSourceModifier = Integer.parseInt(triggerString.substring(modifierIndex + 1));
				this.whatDaq = null;
				this.armMask = 0;
				this.armExternalSource = true;
				this.armDelay = 0;
				this.armOffset = 0;
				this.armValue = 0;
			} else {
				this.armSourceModifier = 0;
				this.armExternalSource = false;
				final StringTokenizer tokens = new StringTokenizer(triggerString.substring(5), ",=", false); // skip trig=

				tokens.nextToken(); // skip d,
				final String triggerDeviceName = tokens.nextToken();

				//this.whatDaq = new WhatDaq(null, triggerDeviceName);
				this.whatDaq = WhatDaq.create(triggerDeviceName);
				this.armOffset = whatDaq.offset();

				String token = tokens.nextToken(); // look for ,mask=

				if (token.startsWith("mask")) {
					this.armMask = Integer.parseInt(tokens.nextToken(), 16);
					token = tokens.nextToken(); // look for ,val=
				} else
					this.armMask = 0;

				if (token.startsWith("val")) {
					this.armValue = Integer.parseInt(tokens.nextToken(), 16);
					token = tokens.nextToken(); // look for ,dly=
				} else
					this.armValue = 0;

				if (token.startsWith("dly"))
					this.armDelay = Integer.parseInt(tokens.nextToken()) * 1000;
				else
					this.armDelay = 0;
			}
		} catch (Exception e) {
			logger.log(Level.FINE, "Bad trigger event '" + triggerString + "'", e); 
			throw new AcnetStatusException(DPM_BAD_EVENT, "Bad trigger event '" + triggerString + "'");
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ExternalTrigger) {
			final ExternalTrigger t = (ExternalTrigger) obj;

			if (t.armExternalSource || armExternalSource) {
				if (t.armExternalSource != armExternalSource)
					return false;

				return t.armSourceModifier != armSourceModifier;
			}

			if (t.whatDaq.di() != whatDaq.di() || t.whatDaq.pi() != whatDaq.pi())
				return false;

			return t.armMask == armMask && t.armExternalSource == armExternalSource
						&& t.armSourceModifier == armSourceModifier && t.armDelay == armDelay
						&& t.armOffset == armOffset && t.armValue == armValue;
		}

		return false;
	}

	int getArmMask()
	{
		return armMask;
	}

	boolean isArmExternalSource()
	{
		return armExternalSource;
	}

	WhatDaq getArmDevice()
	{
		return whatDaq;
	}

	int getArmSourceModifier()
	{
		return armSourceModifier;
	}

	int getArmOffset()
	{
		return armOffset;
	}

	int getArmValue()
	{
		return armValue;
	}

	@Override
	public List<Event> getArmingEvents()
	{
		return null;
	}

	@Override
	public String toString()
	{
		if (armExternalSource)
			return "x,mod=" + armSourceModifier;

		return "";
	}
}
