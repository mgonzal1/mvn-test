// $Id: Trigger.java,v 1.6 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.List;

import gov.fnal.controls.servers.dpm.drf3.Event;

interface Trigger
{
    List<Event> getArmingEvents();
}
