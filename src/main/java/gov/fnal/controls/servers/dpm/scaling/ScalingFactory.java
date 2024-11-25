// $Id: ScalingFactory.java,v 1.7 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;

public class ScalingFactory
{
	static final NoScaling noScaling = new NoScaling();

	public static Scaling get(WhatDaq whatDaq)
	{
		final DeviceInfo dInfo = whatDaq.deviceInfo();

        switch (whatDaq.property()) {
         case STATUS:
		 	return DPMBasicStatusScaling.get(whatDaq);

         case CONTROL:
		 	return DPMBasicControlScaling.get(whatDaq);

         case READING:
			if (dInfo.reading != null && dInfo.reading.validScaling())
		 		return DPMReadSetScaling.get(whatDaq, dInfo.reading.scaling);
			break;

         case SETTING:
			if (dInfo.setting != null && dInfo.setting.validScaling())
		 		return DPMReadSetScaling.get(whatDaq, dInfo.setting.scaling);
			break;

         case ANALOG:
		 	if (dInfo.analogAlarm != null && dInfo.reading != null && dInfo.reading.scaling != null)
				return DPMAnalogAlarmScaling.get(whatDaq, dInfo.reading.scaling);

         case DIGITAL:
		 	return DPMDigitalAlarmScaling.get(whatDaq);
        }

		return noScaling; 
	}
}
