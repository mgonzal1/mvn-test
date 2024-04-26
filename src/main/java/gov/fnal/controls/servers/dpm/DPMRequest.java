// $Id: DPMRequest.java,v 1.7 2024/03/05 17:46:51 kingc Exp $
package gov.fnal.controls.servers.dpm;
 
import java.util.Objects;

import gov.fnal.controls.servers.dpm.drf3.DiscreteRequest;
import gov.fnal.controls.servers.dpm.drf3.ImmediateEvent;
		
public final class DPMRequest extends DiscreteRequest implements Comparable<DPMRequest>
{
	private final Model model;

	public DPMRequest(String request) throws IllegalArgumentException
	{
		super(request);

		this.model = this.modelStr.isEmpty() ? DefaultModel.instance :
						JobModel.parse(this.modelStr); 
	}

	final public boolean isSingleReply()
	{
		return event instanceof ImmediateEvent;
	}

	final public Model model()
	{
		return model;
	}

	@Override
	final public boolean equals(Object obj)
	{
		if (obj == this)
			return true;

		if (obj instanceof DPMRequest)
			return super.equals(obj) && model.equals(((DPMRequest) obj).model);
		
		return false;
	}

	@Override
	public int hashCode()
	{
	    return Objects.hash(deviceUC, fields[0], fields[1], range, event, model);
	}

	@Override
	public int compareTo(DPMRequest req)
	{
		int c;

		if ((c = deviceUC.compareTo(req.deviceUC)) == 0) {
			if ((c = fields[0].compareTo(req.fields[0])) == 0) {
				if ((c = fields[1].compareTo(req.fields[1])) == 0) {
					if ((c = range.compareTo(req.range)) == 0) {
						if ((c = event.compareTo(req.event)) == 0)
							c = model.compareTo(req.model);
					}
				}
			}
		}

		return c;
	}

	public static void main(String[] args)
	{
		try {
			final DPMRequest r1 = new DPMRequest(args[0]);
			final DPMRequest r2 = new DPMRequest(args[1]);

			System.out.println("'" + r1 + "' == '" + r2 + "' (" + (r1.equals(r2)) + ")"); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
