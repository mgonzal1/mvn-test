// $Id: DataLoggerClientLoggingDataEvent.java,v 1.2 2023/06/12 16:37:55 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

public class DataLoggerClientLoggingDataEvent implements DataEvent
{
	@Override
	public boolean isRepetitive()
	{
		return true;
	}
}

