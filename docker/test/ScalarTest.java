import java.math.BigDecimal;
import java.math.MathContext;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.service.proto.DPM;
import gov.fnal.controls.service.dpm.DPMList;
import gov.fnal.controls.service.dpm.DPMDataHandler;

public class ScalarTest implements DPMDataHandler
{
	final MathContext mathCtx;
	final BigDecimal testVal;

	ScalarTest(String testVal)
	{
		this.mathCtx = new MathContext(testVal.indexOf('.') == -1 ? testVal.length() : testVal.length() - 1); 
		this.testVal = new BigDecimal(testVal, this.mathCtx);
	}

	public void handle(DPM.Reply.DeviceInfo devInfo, DPM.Reply.Status s)
	{
		System.exit(s.status);
	}

	public void handle(DPM.Reply.DeviceInfo devInfo, DPM.Reply.Scalar s)
	{
		System.exit(new BigDecimal(s.data, mathCtx).compareTo(testVal));
	}

	public static void main(String[] args) throws Exception
	{
		DPMList.open(AcnetInterface.open(), args[0]).addRequest(0, args[1]).start(new ScalarTest(args[2]));
	}
}

