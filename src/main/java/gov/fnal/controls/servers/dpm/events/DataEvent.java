// $Id: DataEvent.java,v 1.3 2023/09/26 20:52:04 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

public interface DataEvent
{
    public static int oneShotFtd()
	{
		return 0;
	}
	
    /**
     * The EVENT_FTD mask.
     * 
     */
    public static int ftdMask()
	{
		return 0x8000;
	}

	default public long defaultTimeout()
	{
		return 0;
	}

	/**
	 * adds an observer of this DataEvent
	 * 
	 * @param observer
	 *            observer to notify on the occurrence of this DataEvent
	 * @see #deleteObserver(DataEventObserver)
	 */
	default void addObserver(DataEventObserver observer)
	{
		throw new RuntimeException("addObserver() not implemented");
	}

	/**
	 * removes an observer of this DataEvent from future notifications
	 * 
	 * @param observer
	 *            observer to remove from future notifications of this DataEvent
	 * @see #addObserver(DataEventObserver)
	 */
	default void deleteObserver(DataEventObserver observer)
	{
		throw new RuntimeException("deleteObserver() not implemented");
	}

	/**
	 * returns the Frequency Time Descriptor (FTD)
	 * 
	 * @return the Frequency Time Descriptor (FTD)
	 */
	default int ftd()
	{
		return 0;
	}

	/**
     * Inquire if data event is repetitive. 
     * 
	 * @return true if data event is repetitive
	 */
	default boolean isRepetitive()
	{
		return false;
	}

	/**
	 * Sets data event to repetitive.
	 */
	default void setRepetitive(boolean repetitive)
	{
	}

	default long createdTime()
	{
		return 0;
	}

	/**
	 * returns delay in milliseconds
	 * 
	 * @return number of milliseconds to delay from this DataEvent before
	 *         notifying any observers
	 */
	default long getDelay()
	{
		return 0;
	}
}
