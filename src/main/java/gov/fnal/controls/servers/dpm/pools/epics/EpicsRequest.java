// $Id: EpicsRequest.java,v 1.3 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.epics;

import java.util.BitSet;
import java.util.Objects;

import gov.fnal.controls.servers.dpm.DPMRequest;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import org.epics.pva.data.PVAStructure;
import org.epics.pva.client.PVAChannel;


public class EpicsRequest 
{
	final String channel;
	final String request;
	final EpicsListener listener;
	final Object newValue;
	final boolean isSingleReply;

	private boolean isAllUpperCase(String s)
	{
		for (int ii = 0; ii < s.length(); ii++)
			if (!Character.isUpperCase(s.charAt(ii)))
				return false;

		return true;
	}

	public EpicsRequest(String request, EpicsListener listener) throws AcnetStatusException
	{
		this(new DPMRequest(request), listener, null);
	}

	public EpicsRequest(String request, EpicsListener listener, Object newValue) throws AcnetStatusException
	{
		this(new DPMRequest(request), listener, newValue);
	}

	public EpicsRequest(DPMRequest request, EpicsListener listener)
	{
		this(request, listener, null); 
	}
	
	public EpicsRequest(DPMRequest request, EpicsListener listener, Object newValue)
	{
		final String[] fields = request.getFields();

		if (!fields[0].isEmpty()) {
			if (fields[0].length() <= 4 && isAllUpperCase(fields[0])) {
				this.channel = request.getDevice() + "." + fields[0];
				this.request = fields[1];
			} else {
				this.channel = request.getDevice();

				if (fields[1].isEmpty())
					this.request = fields[0];
				else
					this.request = fields[0] + "." + fields[1];
			}
		} else {
			this.channel = request.getDevice();
			this.request = "";
		}
		this.listener = listener;
		this.newValue = newValue;
		this.isSingleReply = request.isSingleReply();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(channel, request, listener);
	}

	@Override
	public String toString()
	{
		return channel + "(" + request + ")";
	}
}

