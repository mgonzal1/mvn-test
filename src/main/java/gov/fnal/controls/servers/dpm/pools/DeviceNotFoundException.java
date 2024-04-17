// $Id: DeviceNotFoundException.java,v 1.1 2024/01/05 21:29:21 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

public class DeviceNotFoundException extends AcnetStatusException
{
	DeviceNotFoundException()
	{
		super(AcnetErrors.DBM_NOPROP);
	}
}
