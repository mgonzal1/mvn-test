// $Id: LoggedDevice.java,v 1.3 2024/06/15 19:56:44 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import gov.fnal.controls.servers.dpm.acnetlib.Node;

public interface LoggedDevice
{
	Node node();
	String loggerName();
	String loggedDrf();
	String event();
	int id();
	boolean clientLogged();
}
