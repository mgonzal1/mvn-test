// $Id: AcnetUnavailableException.java,v 1.2 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

public class AcnetUnavailableException extends AcnetStatusException implements AcnetConstants, AcnetErrors {
    public AcnetUnavailableException() 
	{
		super(ACNET_SYS);
    }
}
