// $Id: LoggerRetrievalEvent.java,v 1.5 2024/01/10 20:57:18 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

public class LoggerRetrievalEvent extends DeltaTimeEvent
{
	/**
	 * @serial minimum time betwen returned points
	 */
	protected int deltaSeconds; // minimum time betwen returned points

	/**
	 * @serial number of points to skip betwen returned points
	 */
	protected int skipCount; // number of points to skip betwen returned
								// points

	/**
	 * @serial align dates to today when true
	 */
	protected boolean alignDates; // align dates to today when true

	/**
	 * @serial initial skip offset
	 */
	protected int skipOffset; // initial skip offset

	/**
	 * @serial list id
	 */
	protected int listId; // list id

	/**
	 * @serial data event
	 */
	protected String eventString; // data event

	protected final boolean useNewProtocol = true;

	public LoggerRetrievalEvent(long t1, long t2, boolean alignDates, int deltaSeconds, 
								int skipCount, int skipOffset, int listId, String eventString)
	{
		super(t1, t2);
		this.deltaSeconds = deltaSeconds;
		this.alignDates = alignDates;
		this.skipCount = skipCount;
		this.skipOffset = skipOffset;
		this.listId = listId;
		this.eventString = eventString; //eventString15HzHack(eventString);
	}

	/**
	 * Time interval for collecting data from a data logger.
	 * 
	 * @param t1
	 *            earliest logger time.
	 * @param t2
	 *            latest logger time.
	 * @param alignDates
	 *            when true, dates are aligned to today.
	 * @param deltaSeconds
	 *            when non-zero, the minimum time in seconds between returned
	 *            points.
	 * @param skipCount
	 *            when non-zero, the number of points to skip between returned
	 *            points.
	 */
	//public LoggerRetrievalEvent(long t1, long t2, boolean alignDates,
	//		int deltaSeconds, int skipCount)
	//{
	//	this(t1, t2, false, deltaSeconds, skipCount, 0, -1, null);
	//}

	/**
	 * Time interval for collecting data from a data logger.
	 * 
	 * @param t1
	 *            earliest logger time
	 * @param t2
	 *            latest logger time
	 */
	public LoggerRetrievalEvent(long t1, long t2)
	{
		this(t1, t2, false, 0, 0, 0, -1, null);
	}

	/**
	 * Get the minimim time in seconds between returned points.
	 * 
	 * @return the minimim time in seconds between returned points
	 */
	public int getDeltaSeconds()
	{
		return deltaSeconds;
	}

	/**
	 * Get the number of points to skip between returned points.
	 * 
	 * @return the number of points to skip between returned points
	 */
	public int getSkipCount()
	{
		return skipCount;
	}

	/**
	 * Get the initial skip offset.
	 * 
	 * @return the initial skip offset
	 */
	public int getSkipOffset()
	{
		return skipOffset;
	}

	/**
	 * Get the list id.
	 * 
	 * @return the list id
	 */
	public int getListId() {
		return listId;
	}

	/**
	 * Get the event string.
	 * 
	 * @return the event string
	 */
	public String getEventString()
	{
		return eventString;
	}

	/**
	 * Inquire if to use new protocol.
	 * 
	 * @return the boolean
	 */
	public boolean useNewProtocol()
	{
		return useNewProtocol;
	}

	/**
	 * Inquire if the dates are to be aligned to today.
	 * 
	 * @return true if the dates are to be aligned to today
	 */
	public boolean isAlignDates()
	{
		return alignDates;
	}

	/**
	 * Describe this event.
	 * 
	 * @return a description of this event
	 */
	@Override
	public String toString()
	{
		return super.toString() + ", align: " + alignDates + ", minDeltaSecs: "
				+ deltaSeconds + ", skip: " + skipCount + ", offset: "
				+ skipOffset + ", listId: " + listId + ", event: "
				+ eventString;
	}

} // end LoggerRetrievalEvent
