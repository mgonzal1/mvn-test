// $Id: TimeStamper.java,v 1.7 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.events.ClockEvent;
import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DataEventObserver;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimeZone;

class TimeStamper implements DataEventObserver
{
    private static ClockEvent superCycleEvent;
    private static ClockEvent timeStampEvent;

    /**
     * monitor 0x0f time stamp events
     */
    private static ClockEvent fifteenHertzEvent;

    /**
     * current 0x02 time stamp event
     */
    private static volatile long currentTS;

    //private static volatile long currentTSMillis;

    private static volatile long fifteenHertzMillis = 0;

    /**
     * previous 0x02 time stamp event
     */
    private static volatile long previousTS;

    //private static volatile long previousTSMillis;

    /**
     * current 0x00 super cycle event
     */
    private static volatile long currentSC;

    /**
     * previous 0x00 super cycle event
     */
    private static volatile long previousSC;

    /**
     * clocks are synchronized
     */
    private static boolean clocksAreSynched = true;

    /**
     * debugging
     */
    //private static boolean bugs = false;

    /**
     * data log clock errors when true
     */
    private static boolean logClockErrors = false;


    /**
     * super cycle error observers
     */
    //private static LinkedList<SuperCycleClockErrorObserver> superCycleClockErrorObservers = new LinkedList<SuperCycleClockErrorObserver>();

    private static int numNegSCWithin50 = 0;

    private static int numNegSCWithin100 = 0;

    private static int numNegSCWithin200 = 0;

    private static int numNegSCWithin400 = 0;

    private static int numNegSCGreater400 = 0;

    private static int numSCWithin50 = 0;

    private static int numSCWithin100 = 0;

    private static int numSCWithin200 = 0;

    private static int numSCWithin400 = 0;

    private static int numSCGreater400 = 0;
    

    private static int numTimeStamperReportStatistics = 0;

    private static int numDatalogs = 0;

    private static int numBaseTime02Fix = 0;

    private static int numMissingTS = 0;

    private static String lastMissingTS = null;

    private static int lastUCDDelta = 0;

    private static long errorMillis = 0;

    private static long negErrorMillis = 0;

    private static String negErrorString = null;

    private static long posErrorMillis = 0;

    private static String posErrorString = null;

    /**
     * The timeStamper constant.
     */
    public static final TimeStamper timeStamper = new TimeStamper();

    /**
     * Create a time stamper.
     */
    private TimeStamper()
	{
        currentTS = previousTS = currentSC = previousSC = System.currentTimeMillis();
        superCycleEvent = new ClockEvent(0);
        timeStampEvent = new ClockEvent(2);
        fifteenHertzEvent = new ClockEvent(15);
        superCycleEvent.addObserver(this);
        timeStampEvent.addObserver(this);
        fifteenHertzEvent.addObserver(this);
    }

    public static long getCurrentSuperCycle()
	{
        return currentSC;
    }

    private static void clocksAreSynched(boolean state)
	{
        clocksAreSynched = state;
    }

    private boolean areClocksSynched()
	{
        return clocksAreSynched;
    }

    static int secsTicksInSuperCycle(int ticks)
	{
        final long now = System.currentTimeMillis();

        synchronized (superCycleEvent) {
            long ticksNow = (now - currentSC) / 67;
            if (ticksNow > ticks)
                return (int) (currentSC / 1000);
            else
                return (int) (previousSC / 1000);
        }
    }

    private static long getFifteenHertzMillis()
	{
        return fifteenHertzMillis;
    }

    public static long getBaseTime02(long feMillis)
	{
        synchronized (timeStampEvent) {
            //long returnTime = currentTSMillis;
            long returnTime = currentTS;
            long currentTimeMillis = System.currentTimeMillis();
            long millis = currentTimeMillis - returnTime;
            while (millis > 5000) {
				//System.out.println("HERE 1");
                ++numBaseTime02Fix;
                returnTime += 5000;
                millis -= 5000;
            }
            errorMillis = currentTimeMillis - (returnTime + feMillis);
            if (errorMillis > 4000) {
				//System.out.println("HERE 2");
                returnTime += 5000;
                errorMillis -= 5000;
            }
            else if (errorMillis < -1000) {
				//System.out.println("HERE 3");
                returnTime -= 5000;
                errorMillis += 5000;
            }
            if (errorMillis > posErrorMillis) {
				//System.out.println("HERE 4");
                posErrorMillis = errorMillis;
            } else if (errorMillis < negErrorMillis) {
				//System.out.println("HERE 5");
                negErrorMillis = errorMillis;
            }
            return (returnTime * 1000000);
        }
    }

    static int getUCDTimeError()
	{
        return lastUCDDelta;
    }
    
    public static long getCurrentTS()
	{
		return currentTS;
	}
    public static long getPreviousTS()  { return previousTS; }

    public synchronized void update(DataEvent userEvent, DataEvent currentEvent) {
        final long now = System.currentTimeMillis();
        if (userEvent == (DataEvent) fifteenHertzEvent) {
            fifteenHertzMillis = currentEvent.createdTime();

        } else if (userEvent == (DataEvent) timeStampEvent) {
            synchronized (timeStampEvent) {
                previousTS = currentTS;
                if (clocksAreSynched)
                    currentTS = currentEvent.createdTime();
                else
                    currentTS = currentEvent.createdTime();
				if (currentTS - previousTS > 7500) {
                    ++numMissingTS;
                }
            }
        }

        else if (userEvent == (DataEvent) superCycleEvent) {
            synchronized (superCycleEvent) {
                previousSC = currentSC;
                if (clocksAreSynched)
                    currentSC = currentEvent.createdTime();
                else
                    currentSC = now;
            }
            int delta = (int) (now - currentSC);
            if (delta < 0) {
                if (delta >= -50)
                    ++numNegSCWithin50;
                else if (delta >= -100)
                    ++numNegSCWithin100;
                else if (delta >= -200)
                    ++numNegSCWithin200;
                else if (delta >= -400)
                    ++numNegSCWithin400;
                else
                    ++numNegSCGreater400;
            } else {
                if (delta < 50)
                    ++numSCWithin50;
                else if (delta < 100)
                    ++numSCWithin100;
                else if (delta < 200)
                    ++numSCWithin200;
                else if (delta < 400)
                    ++numSCWithin400;
                else
                    ++numSCGreater400;
            }
            lastUCDDelta = delta;
        }
    }
	
    private static DataEvent getFifteenHertzEvent()
    {
    	return fifteenHertzEvent;
    }

	public static void main(String[] args) throws Exception
	{
		while (true) {
			final long now = System.currentTimeMillis();
			final long t20 = getBaseTime02(5000);

    		System.out.println((new Date(t20 / 1000000)) + " ");
			Thread.sleep(1000);
		}
	}
}
