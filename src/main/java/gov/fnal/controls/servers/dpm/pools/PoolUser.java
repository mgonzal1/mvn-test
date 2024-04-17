// $Id: PoolUser.java,v 1.2 2022/11/01 20:37:08 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;
 
public interface PoolUser
{
    default public void cancel()
	{ 
		synchronized (this) {
			notify();
		}
	}

    //public void cancel(int error)
//	{
//		synchronized (this) {
//			notify();
//		}
//	}

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

    //public void repetitiveComplete()
//	{
//		synchronized (this) {
//			notify();
//		}
//	}
}

