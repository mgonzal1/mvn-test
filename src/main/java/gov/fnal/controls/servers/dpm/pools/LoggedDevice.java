// $Id: LoggedDevice.java,v 1.2 2024/02/22 16:33:02 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import gov.fnal.controls.servers.dpm.acnetlib.Node;

public interface LoggedDevice
{
	public Node node();
	public String loggerName();
	public String loggedDrf();
	public String event();
	public int id();
}
