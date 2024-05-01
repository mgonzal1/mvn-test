// $Id: AbsoluteTimeEvent.java,v 1.4 2024/01/10 20:57:18 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ConcurrentModificationException;

import gov.fnal.controls.servers.dpm.DPMServer;


public class AbsoluteTimeEvent extends TimerTask implements DataEvent//, DataEventObserver
{
	public final long absoluteTime;

	//transient DataEvent wakeupEvent = null;

	//final long beforeAccuracy;// accuracy in microseconds before the time
	//final long afterAccuracy; // accuracy in microseconds after the time
	final String string;

	final List<DataEventObserver> observers;

	/**
     * An absolute time event.
     * 
	 * @param theTime the event time
	 */
	public AbsoluteTimeEvent(long absoluteTime)
	{
		//this(theTime, 0, 0);
		this.absoluteTime = absoluteTime;
		this.string = "a," + absoluteTime + ",0,0";
		this.observers = new LinkedList<>();
	}

	/**
     * An absolute time event.
     * 
	 * @param theTime the event time
	 * @param accuracy time accuracy
	 */
	//public AbsoluteTimeEvent(long theTime, long accuracy)
	//{
	//	this(theTime, accuracy, accuracy);
	//}

	/**
     * An absolute time event.
     * 
	 * @param theTime the event time
	 * @param before accuracy before time
	 * @param after accuracy after time
	 */
	//public AbsoluteTimeEvent(long absoluteTime, long before, long after)
	//{
		//this.absoluteTime = absoluteTime;
		//this.beforeAccuracy = before;
		//this.afterAccuracy = after;
		//this.string = "a," + absoluteTime + "," + before + "," + after;
	//}

	/**
	 * Add a DataEventObserver who is interested in receiving notification when
	 * this AbsoluteTimeEvent occurs.
	 * 
	 * @param observer
	 *            observer to add
	 */
	public synchronized void addObserver(DataEventObserver observer)
	{
		observers.add(observer);
		
		if (observers.size() == 1)
			DPMServer.sharedTimer().schedule(this, new Date(absoluteTime));

/*
		if (wakeupEvent == null) {
			long deltaTime = absoluteTime - System.currentTimeMillis();
			if (deltaTime < 0)
				wakeupEvent = new OnceImmediateEvent();
			else
				wakeupEvent = new DeltaTimeEvent(deltaTime, false);
		}
		synchronized (wakeupEvent) {
			if (observers == null)
				observers = new LinkedList<DataEventObserver>();
			observers.add(observer);
			if (observers.size() == 1)
				wakeupEvent.addObserver(this);
		}
		*/
	}

	@Override
	public void run()
	{
		updateObservers();
		observers.clear();
	}

	private synchronized void updateObservers()
	{
		final AbsoluteTimeEvent now = new AbsoluteTimeEvent(System.currentTimeMillis());

		for (DataEventObserver o : observers) {
			try {
				o.update(this, now);
			} catch (Exception ignore) { }
		}
	}

	/**
	 * Observed event has fired.
	 * 
	 * @param userEvent
	 *            the requesting event.
	 * @param currentEvent
	 *            the current event.
	 */
	//public synchronized void update(DataEvent userEvent, DataEvent currentEvent)
	//{
		//synchronized (wakeupEvent) {
			//Iterator<DataEventObserver> all = observers.iterator();
			//try {
				//for (DataEventObserver next = null; all.hasNext();) {
				//	next = all.next();
				//	next.update(this, new AbsoluteTimeEvent(System.currentTimeMillis()));
				//}
			//} catch (ConcurrentModificationException e) {
			//}
			//observers.clear();
			//wakeupEvent.deleteObserver(this);
		//}
	//}

	/**
	 * Removes a previously registered DataEventObserver who was interested in
	 * receiving notification when this AbsoluteTimeEvent occurs.
	 * 
	 * @param observer
	 *            observer to remove from notification
	 */
	public synchronized void deleteObserver(DataEventObserver observer)
	{
		//if (wakeupEvent != null) {
			//synchronized (wakeupEvent) {
				//if (observers != null) {
					observers.remove(observer);
					//if (observers.size() == 0)
					//	wakeupEvent.deleteObserver(this);
				//}
			//}
		//}
	}

	@Override
	public String toString()
	{
		return string;
	}

	/**
     * Returns the absolute time.
     * 
	 * @return the absolute time
	 */
	//public long getAbsoluteTime()
	//{
	//	return absoluteTime;
	//}

	/**
     * Returns the accuracy (in microseconds) before time.
     * 
	 * @return the accuracy before time
	 */
	//public long getBeforeAccuracy()
	//{
	//	return beforeAccuracy;
	//}

	/**
     * Returns the accuracy (in microseconds) after time.
     * 
	 * @return the accuracy after time
	 */
	//public long getAfterAccuracy()
	//{
	//	return afterAccuracy;
	//}
}

