// $Id: DeltaTimeThread.java,v 1.4 2024/01/10 20:57:18 kingc Exp $
//package gov.fnal.controls.servers.dpm.events;

/**
 * This class provides for the delta time event thread.
 * @author Cahill
 */
 /*
public class DeltaTimeThread extends Thread
{
    protected DataEventObserver observer;

	protected DeltaTimeEvent event;

	protected final long start;

	protected final long end;

	DeltaTimeThread(DataEventObserver observer, DeltaTimeEvent event)
	{
		this.observer = observer;
		this.event = event;

		setName("DeltaTimeThread_" + observer.toString() + "_" + event.getRepeatRate());

		start = event.getStartTime();
		end = event.getStopTime();
	}

	public void run()
	{
		long now = System.currentTimeMillis();
		long sleepTime = 0;
		long adjustedStart = start;
		long repeatRate = event.getRepeatRate();

		if (adjustedStart < now && repeatRate < 365 * 24 * 60 * 60 * 1000L) {	// watch out for cpu bound 0x7fff ffff
			while (adjustedStart < now) 
				adjustedStart += repeatRate;
		}

		if (!event.immediate && System.currentTimeMillis() < adjustedStart) {
			try {
				if (event.getRepeatRate() == 0) {
					//++numStartNotImmediateZero;
					//System.out
					//		.println("DeltaTimeThread, "
					//				+ Thread.currentThread().getName()
					//				+ " repeat rate is zero, forcing to 100 milliseconds: "
					//				+ request +  ", count: " + count + 
					//				", immed: " + request.immediate + ", observer: " + observer);
					
					sleepTime = 100;
				} else {
					now = System.currentTimeMillis();
					sleepTime = adjustedStart - now;
				}
				if (sleepTime < 10L) {
					//++numStartNotImmediateSleepLess10;
				//	if (classBugs) System.out.println("DeltaTimeThread, sleepTime1: " + sleepTime +
				//			", now: " + System.currentTimeMillis() + ", adjustedStart: " + adjustedStart +
				//			", repeatRate: " + request.getRepeatRate() + ",count: " + count +
				//			", immed: " + request.immediate + ", observer: " + observer +
				//			", setting to 10");
					sleepTime = 10;
				}
				//if (doPrintFirstSleep) {
				//	System.out.println("firstSleep before do: " + sleepTime);
				//	doPrintFirstSleep = false;
				//}
				sleep(sleepTime);
				//if (classBugs) System.out.println("DeltaTimeEvent, firstSleep, sleepTime: " + sleepTime + 
				//		", adjustedStart - now: " + (adjustedStart - now) + ", rate: " + request.getRepeatRate() +
				//		", now: " + now + ", after: " + System.currentTimeMillis() +

			} catch (InterruptedException ex) {
				// ex.printStackTrace();
			}
		}

		long base = System.currentTimeMillis();

		do {
			now = System.currentTimeMillis();
			//if (observer == null) {
			//	now = end; // get out since observer is null
			//} else {
				try {
					//if (tm != null) {
					//	DataEventObserver tmObserver = observer;
					//	DataEvent tmRequest = request;
					//	tm.yieldCheck("DeltaTimeThread before update, count: " + count + ", sleepTime: " + sleepTime, tmObserver, tmRequest);
					//			now), request.getStopTime(), request
					//			.getRepeatRate()));
					//	tm.yieldCheck("DeltaTimeThread after update, count: " + count + ", sleepTime: " + sleepTime, tmObserver, tmRequest);
					//}
					//else 
					
					//if (observer != null) 
						observer.update(event, new DeltaTimeEvent(now, event.getStopTime(), event.getRepeatRate()));

					//++count;
				} catch (Exception e) {
					//++numObserverUpdateException;
					//System.out.println("observer.update exception " + e);
					//System.out.println("observer: " + observer + ", request: " + request);
					//e.printStackTrace();
					//if (++observerExceptionCount == 10  || request == null || observer == null) {
					//	if (request != null) {
					//		++numObserverUpdateExceptionDeleteObserver;
					//		request.deleteObserver(observer);
					//	}
					//	System.out
					//			.println("DeltaTimeThread, deleted observer on excessive exceptions, count: " + observerExceptionCount);
					//}
				}

				if (!event.isRepetitive()) 
					break;

				sleepTime = base + event.getRepeatRate() - System.currentTimeMillis();
				base += event.getRepeatRate();

				if (sleepTime < 10L) {
					//++numAdjustSleepLess10;
					//if (classBugs) System.out.println("DeltaTimeThread, sleepTime2: " + sleepTime +
					//		", repeatRate: " + request.getRepeatRate() +
					//		", observer: " + observer + ",count: " + count +
					//		", immed: " + request.immediate + ", setting to 10");
					sleepTime = 10;
				}
				//if (doPrintFirstSleep) {
				//	System.out.println("firstSleep do: " + sleepTime);
				//	doPrintFirstSleep = false;
				//}
				if (sleepTime > 0) {
					try {
						sleep(sleepTime);
					} catch (InterruptedException ex) {
					}
				}
			//}
		} while (now < end);

		//++numRunEnd;
		event.removeThread(this);
	}
}
*/
