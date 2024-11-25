// $Id: DPMRequest.java,v 1.10 2024/11/19 22:34:43 kingc Exp $
package gov.fnal.controls.servers.dpm;
 
import java.util.Objects;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.drf3.Event;
import gov.fnal.controls.servers.dpm.drf3.DataRequest;
import gov.fnal.controls.servers.dpm.drf3.ImmediateEvent;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;
		
public final class DPMRequest extends DataRequest implements Comparable<DPMRequest>
{
	final long refId;
	DataSource dataSource;

	public DPMRequest(String request, long refId, Event event) throws IllegalArgumentException
	{
		super(request, event);

		this.refId = refId;
		this.dataSource = super.dataSource.isEmpty() ? DefaultDataSource.instance :
														DataSource.parse(super.dataSource); 
	}

	public DPMRequest(String request, long refId) throws IllegalArgumentException
	{
		this(request, refId, null);
	}

	public DPMRequest(String request) throws IllegalArgumentException
	{
		this(request, 0, null);
	}

	final public boolean isSingleReply()
	{
		return event instanceof ImmediateEvent;
	}

	final public long refId()
	{
		return refId;
	}

	final public DataSource dataSource()
	{
		return dataSource;
	}

	final public void dataSource(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	final public boolean isForLiveData()
	{
		return dataSource == DefaultDataSource.instance || dataSource == LiveDataSource.instance;
	}

	@Override
	final public boolean equals(Object obj)
	{
		if (obj == this)
			return true;

		if (obj instanceof DPMRequest)
			return super.equals(obj) && dataSource.equals(((DPMRequest) obj).dataSource);
		
		return false;
	}

	@Override
	public int hashCode()
	{
	    return Objects.hash(device, fields[0], fields[1], range, event, dataSource);
	}

	@Override
	public int compareTo(DPMRequest req)
	{
		int c;

		if ((c = device.compareTo(req.device)) == 0) {
			if ((c = fields[0].compareTo(req.fields[0])) == 0) {
				if ((c = fields[1].compareTo(req.fields[1])) == 0) {
					if ((c = range.compareTo(req.range)) == 0) {
						if ((c = event.compareTo(req.event)) == 0)
							c = dataSource.compareTo(req.dataSource);
					}
				}
			}
		}

		return c;
	}

	public static void main(String[] args)
	{
		try {
			final DPMRequest r1 = new DPMRequest(args[0], 0);
			final DPMRequest r2 = new DPMRequest(args[1], 1);

			logger.log(Level.INFO, "'" + r1 + "' == '" + r2 + "' (" + (r1.equals(r2)) + ")"); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
