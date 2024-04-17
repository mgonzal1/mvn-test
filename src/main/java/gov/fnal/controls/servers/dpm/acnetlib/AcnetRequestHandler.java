// $Id: AcnetRequestHandler.java,v 1.1 2023/11/02 16:35:17 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

public interface AcnetRequestHandler 
{
	void handle(AcnetRequest r);
    void handle(AcnetCancel c);
}

