// $Id: PoolUser.java,v 1.3 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;
 
public interface PoolUser
{
    default public void cancel()
	{ 
		synchronized (this) {
			notify();
		}
	}

    default public void complete()
	{
		synchronized (this) {
			notify();
		}
	}

	public static PoolUser create()
	{
		return new PoolUser() { };
	}
}
