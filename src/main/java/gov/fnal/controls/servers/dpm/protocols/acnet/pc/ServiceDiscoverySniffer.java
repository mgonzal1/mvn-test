import gov.fnal.controls.service.proto.DPM;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetCancel;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequest;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequestHandler;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

public class ServiceDiscoverySniffer implements AcnetRequestHandler
{
	final AcnetConnection acnet;
	final long startTime;

	volatile int perSecondCount = 0;
	volatile long total = 0;
	
	ServiceDiscoverySniffer(String name) throws AcnetStatusException
	{
		this.acnet = AcnetInterface.open(name);
		this.startTime = System.currentTimeMillis();
		this.acnet.handleRequests(this);
	}

	boolean anyActivity()
	{
		return perSecondCount > 0;
	}	
	
	@Override
	public void handle(AcnetRequest request)
	{
		try {
			final DPM.Request.ServiceDiscovery m = (DPM.Request.ServiceDiscovery) DPM.Request.unmarshal(request.data());

			total++;
			perSecondCount++;
		} catch (Exception e) {
		} finally {
			try {
				request.ignore();
			} catch (Exception ignore) { }
		}
	}

	@Override
	public void handle(AcnetCancel c)
	{
	}

	public void print()
	{
		final long average = total / ((System.currentTimeMillis() - startTime) / 1000);

		System.out.println(acnet.connectedName().trim() + "," + average + "," + perSecondCount + "," + total);

		perSecondCount = 0;
	}

	public static void main(String[] args) throws InterruptedException, AcnetStatusException
	{
		final ServiceDiscoverySniffer dpmd = new ServiceDiscoverySniffer("DPMD");

		while (true) {
			Thread.sleep(1000);

			if (dpmd.anyActivity())
				dpmd.print();	
		}
	}
}
