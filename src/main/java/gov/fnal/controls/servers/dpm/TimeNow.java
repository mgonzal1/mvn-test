// $Id: TimeNow.java,v 1.1 2022/04/01 19:45:59 kingc Exp $
package gov.fnal.controls.servers.dpm;

public interface TimeNow
{
	default long now()
	{
		return System.currentTimeMillis();
	}
}
