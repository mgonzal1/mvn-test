// $Id: Completable.java,v 1.1 2022/11/01 20:35:46 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

/**
 * Interface for reporting data acquisition completion.
 * 
 * @author Kevin Cahill, Ed Dambik
 * @version 0.01<br>
 *          Class created: 19 Nov 1998
 */
public interface Completable {
	/**
	 * Returns completion status back to a client, specifically, an overall
	 * failure, mode change or general list completion notification.
     * @param status
	 * 
	 */
	void completed(int status);
}
