// $Id: ConsoleUser.java,v 1.1 2023/10/04 19:41:11 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.Set;

public interface ConsoleUser
{
	public int id();
	public String name();
	public int permissions();
	public int classes();
	public Set<String> roles();
	public Set<String> allowedDevices(String role);
	public Set<String> allowedDevices();
}

