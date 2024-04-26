// $Id: ClockEvent.java,v 1.5 2024/03/06 20:02:20 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

import java.sql.ResultSet;

import gov.fnal.controls.db.DbServer;
import static gov.fnal.controls.db.DbServer.getDbServer;

public class ClockEvent implements DataEvent, DbServer.Constants
{
	public static final int MAX_NUMBER_EVENTS = 256;
	public static final int MAX_EVENT_NUMBER = 0xFF;
	public static final int MIN_EVENT_NUMBER = 0;
	public static final int SUPER_CYCLE_RESET_EVENT = 0x00;

	final int number;
	final boolean substitute;
	final int micros = 0;
	private int count = 0;
	long eventSum = 0;
	
	final boolean hardEvent;
	final long delay;
	final String string;

	private static boolean getNames = true;

	private static String[] names = new String[MAX_NUMBER_EVENTS]; 
	private static String[] descr = new String[MAX_NUMBER_EVENTS]; 
	private final static String invalidClockEvent = "invalid clock event";
	private ClockEvent scReset = null;

	public ClockEvent(int number, boolean hardEvent, long delay, boolean substitute)
	{
		final int tmp = number & 0xff;

		this.string = "e," + Integer.toHexString(number & 0xff) + "," + 
						(hardEvent ? (substitute ? "e," : "h,") : "s,") + delay;
		this.number = number;
		this.hardEvent = hardEvent;
		this.delay = delay;
		this.substitute = substitute;
	} 

	public ClockEvent(int number, boolean hardEvent, long delay)
	{
		this(number, hardEvent, delay, true);
	}

	public ClockEvent(int number, boolean hardEvent)
	{
		this(number, hardEvent, 0);
	}

	public ClockEvent(int number)
	{
		this(number, true, 0, false);
	}

	@Override
	public boolean isRepetitive()
	{
		return true;
	}

	@Override
	public int ftd()
	{
		return delay == 0 ? (number | DataEvent.ftdMask()) : 0;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof ClockEvent) {
			return o.toString().equalsIgnoreCase(string);
		}
			
		return false;
	}

	@Override
	public String toString()
	{
		return string;
	}

	@Override
	public void addObserver(DataEventObserver observer)
	{
	}

	public boolean canSubstitute()
	{
		return substitute;
	}

	@Override
	public void deleteObserver(DataEventObserver observer)
	{
	}

	public static String clockEventNumberToName(int clockEvent)
	{
		if (clockEvent < 0 || clockEvent >= MAX_NUMBER_EVENTS)
			return invalidClockEvent;
		if (getNames) {
			getNames();
			getNames = false;
		}
		return names[clockEvent];
	}
	
	public static String clockEventNumberToDescription(int clockEvent)
	{
		if (clockEvent < 0 || clockEvent > 255)
			return invalidClockEvent;
		if (getNames) {
			getNames();
			getNames = false;
		}
		return descr[clockEvent];
	}

	public static int clockEventNameToNumber(String name)
	{
		if (getNames) {
			getNames = false;
			getNames();
		}
		for (int ii = 0; ii < MAX_NUMBER_EVENTS; ii++) {
			if (names[ii].equalsIgnoreCase(name))
				return ii;
		}
		return -1;
	}

	public int getClockEventNumber()
	{
		return number;
	}

	public boolean isSoftClockEvent()
	{
		return !hardEvent;
	}

	private static void getNames()
	{
		final String query = "SELECT symbolic_name, long_text, event_number FROM hendricks.clock_events where event_type = 0";

		try {
			final ResultSet rs = getDbServer("adbs").executeQuery(query);

			int eventNo;
			for (eventNo = 0; eventNo < names.length; eventNo++) {
				names[eventNo] = "Undefined";
				descr[eventNo] = "Undefined Event Number";
			}
			while (rs.next()) {
				eventNo = rs.getInt(3);
				if (eventNo < names.length) {
					names[eventNo] = rs.getString(1);
					descr[eventNo] = rs.getString(2);
				}
			}
			rs.close();
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}

	public int micros()
	{
		return micros;
	}

	public int count()
	{
		return count;
	}

	public long eventSum()
	{
		return eventSum;
	}

	public long timeInSuperCycle()
	{
		if (scReset == null) {
			return -1;
		} else {
			return (1000 * (createdTime() - scReset.createdTime()) + (micros - scReset.micros()));
		}
	}
}
