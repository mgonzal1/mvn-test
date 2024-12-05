
import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;

import gov.fnal.controls.service.proto.DPM;
import gov.fnal.controls.service.dpm.DPMList;
import gov.fnal.controls.service.dpm.DPMDataHandler;

public class Example implements DPMDataHandler
{
	public void handle(DPM.Reply.DeviceInfo devInfo, DPM.Reply.Status s)
	{
		if (devInfo != null)
			System.out.printf("%d: status: %s = %04x @ %tc\n", s.ref_id, devInfo.name, s.status, s.timestamp);
		else
			System.out.printf("%d: status %04x @ %tc\n", s.ref_id, s.status, s.timestamp);
	}

	public void handle(DPM.Reply.DeviceInfo devInfo, DPM.Reply.Scalar s)
	{
		System.out.printf("%d: %-8s = %.4f %s @ %tc collection:%d cycle:%d (%g)\n", 
							s.ref_id, devInfo.name, s.data, devInfo.units, 
							s.timestamp, s.timestamp, s.cycle, s.data);
	}

	public void handle(DPM.Reply.DeviceInfo devInfo, DPM.Reply.AnalogAlarm a)
	{
		System.out.printf("%d: %-8s = %s\n", a.ref_id, devInfo.name, a);
	}

	public void handle(DPM.Reply.DeviceInfo devInfo, DPM.Reply.DigitalAlarm a)
	{
		System.out.printf("%d: %-8s = %s\n", a.ref_id, devInfo.name, a);
	}

	public void handle(DPM.Reply.DeviceInfo devInfo, DPM.Reply.Text t)
	{
		System.out.printf("%d: %-8s = '%s' @ %tc\n", t.ref_id, devInfo.name, t.data, t.timestamp);
	}

	public void serviceFound(DPM.Reply.ServiceDiscovery m)
	{
		System.out.println("DPM Service Found: " + m.serviceLocation);
	}

	public void serviceNotFound()
	{
		System.out.println("DPM Service Not Found");
	}

	public static void main(String[] args) throws Exception
	{
	    final String[] requests = { 
			"G:DPMTEST1@q,1H"
		};

		final DPMList list = DPMList.open(AcnetInterface.open(), "ADKDPM");

		for (int refId = 0; refId < requests.length; refId++)
			list.addRequest(refId, requests[refId]);

		list.start(new Example());
	}
}

