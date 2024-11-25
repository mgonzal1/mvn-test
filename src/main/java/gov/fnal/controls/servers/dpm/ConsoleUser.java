// $Id: ConsoleUser.java,v 1.2 2024/09/26 14:18:48 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.Set;

public interface ConsoleUser
{
	int id();
	String name();
	int permissions();
	int classes();
	Set<String> roles();
	Set<String> allowedDevices(String role);
	Set<String> allowedDevices();
}

