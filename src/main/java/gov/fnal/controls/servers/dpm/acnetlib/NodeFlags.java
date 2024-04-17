// $Id: NodeFlags.java,v 1.1 2024/02/22 16:29:45 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

public interface NodeFlags
{
	public static int OBSOLETE = 0;
	public static int OUT_OF_SERVICE = 1;
	public static int OPERATIONAL = 2;
	public static int EVENT_STRING_SUPPORT = 3;
	public static int HARDWARE_CLOCK = 4;
}
