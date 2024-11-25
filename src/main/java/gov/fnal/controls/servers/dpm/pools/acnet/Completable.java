// $Id: Completable.java,v 1.2 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

public interface Completable
{
	void completed(int status);
}
