// $Id: Trigger.java,v 1.3 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.events.DataEvent;

import java.util.List;

interface Trigger
{
    //String getReconstructionString();
    List<DataEvent> getArmingEvents();
    //int getArmingDelay();
    //boolean isArmImmediately();
}
