// $Id: DeltaTimeEvent.java,v 1.5 2024/01/25 23:14:01 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

import java.util.Iterator;
import java.util.LinkedList;

public class DeltaTimeEvent implements DataEvent
{
	static final long endOfTime = Long.MAX_VALUE;
	//static final LinkedList<DeltaTimeThread> threads = new LinkedList<>();

	final long t1;
	final long t2;
	final long millis;
	final boolean immediate;
	final String string;

	boolean repetitive;

	//DeltaTimeThread thread;

	public DeltaTimeEvent(long t1, long t2, long millis)
	{
		this.t1 = t1;
		this.t2 = t2;
		this.millis = millis;
		this.immediate = false;
		this.repetitive = millis > 0;
		this.string = "d," + t1 + "," + t2 + (this.repetitive ? "," + millis : "");
	}

	public DeltaTimeEvent(long t1, long t2)
	{
		this(t1, t2, 0);
	}

	//public DeltaTimeEvent(long t1, long millis)
	//{
	//	this(t1, endOfTime, millis);
	//}

	public DeltaTimeEvent(long m)
	{
		this(m, false);
	}

	public DeltaTimeEvent(long millis, boolean immediate)
	{
		this.t1 = System.currentTimeMillis() + (immediate ? 0 : millis);
		this.t2 = endOfTime;
		this.millis = millis;
		this.immediate = immediate;
		this.repetitive = true;
		this.string = "p," + millis + "," + immediate;
	}

	public boolean immediate()
	{
		return immediate;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof DeltaTimeEvent)
			return o.toString().equalsIgnoreCase(string);
			
		return false;
	}

	@Override
	public long defaultTimeout()
	{
		final long timeout;

		if (!repetitive)
			timeout = 5000;
		else
			timeout = (long) ((millis * 3.0) + (millis / 2.0));

		return timeout > 15000 ? 15000 : timeout;
	}

	@Override
	public void setRepetitive(boolean repetitive)
	{
		this.repetitive = repetitive;
	}

	@Override
	public int ftd() 
	{
		if (repetitive) {
			final long ftd = 60 * millis / 1000;

			return (int) (ftd < 4 ? 4 : ftd);
		}

		return 0;
	}

	@Override
	public boolean isRepetitive()
	{
		return repetitive;
	}

	@Override
	public synchronized void addObserver(DataEventObserver observer)
	{
		//thread = new DeltaTimeThread(observer, this);

		//synchronized (threads) {
			//threads.add(thread);
		//}
		//thread.start();
	}

	@Override
	public synchronized void deleteObserver(DataEventObserver observer)
	{
		//synchronized (threads) {
		//	final Iterator<DeltaTimeThread> iter = threads.iterator();
		//	DeltaTimeThread thread;

		//	while (iter.hasNext()) {
		//		thread = iter.next();
		//		if (thread.observer == observer && thread.event == this) {
		//			synchronized (thread) {
		//				thread.observer = null;
		//			}
		//			if (thread != Thread.currentThread()) {
		//				//AcnetInterface.safeThreadInterrupt(thread);
		//			}
		//			iter.remove();
		//		}
		//	}
		//}
	}

	@Override
	public String toString()
	{
		return string;
	}

	//protected void removeThread(DeltaTimeThread thread)
	//{
		//synchronized (threads) {
		//	threads.remove(thread);
		//}
	//}

	public synchronized long getStartTime()
	{
		return t1;
	}

	public synchronized long getStopTime()
	{
		return t2;
	}

	public long getRepeatRate()
	{
		return millis;
	}
}
