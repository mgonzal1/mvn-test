// $Id: DataEventObserver.java,v 1.2 2023/06/12 16:37:55 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

public interface DataEventObserver
{
	void update(DataEvent request, DataEvent reply);
}
