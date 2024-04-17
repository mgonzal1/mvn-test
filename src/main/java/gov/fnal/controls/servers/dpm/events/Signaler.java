// $Id: Signaler.java,v 1.1 2022/11/01 20:40:57 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

/**
 * This class provides a signaling mechanism for objects. 
 * There should only be one object waiting for a signal at any given time.
 */
public class Signaler {
	int signalCount = 0;
	boolean waiting = false;

	private long _wait(long timeout) 
	{
		long t = System.currentTimeMillis();

		waiting = true;

		try {
			wait(timeout);
		} catch (InterruptedException e) {}

		waiting = false;

		return System.currentTimeMillis() - t;
	}

	private void waitCheck()
	{
		if (waiting) {
			System.err.println("Signaler: Multiple waits on signal");
			Thread.dumpStack();
		}
	}

	/**
	 * Sends a signal to the object waiting for a signal.
	 */
	public synchronized void signal()
	{
		signalCount++;
		notify();
	}

	/**
	 * Waits for a signal. If there are signals pending, this method will return
	 * immediately.
	 */
	public synchronized void waitForSignal() 
	{
		waitCheck();

		while (signalCount == 0)
			_wait(0);

		signalCount--;
	}

	/**
	 * Waits for a signal with timeout. If there are signals pending, this
	 * method will return immediately.
	 * 
	 * @param timeout
	 *        in milliseconds
	 *
	 * @return boolean
	 *        true if signal was received with in the specified  timeout
	 */
	public synchronized boolean waitForSignal(long timeout) 
	{
		waitCheck();

		while (signalCount == 0 && timeout > 0)
			timeout -= _wait(timeout);

		if (signalCount > 0) {
			signalCount--;
			return true;
		} else
			return false;
	}

	/**
	 * Testing
	 */
	 /*
	static public void main(String[] args) throws InterruptedException, java.io.IOException
	{
		final Signaler s = new Signaler();

		(new Thread(new Runnable() {
			public void run() 
			{
				System.out.print("Waiting for signal: ");
				s.waitForSignal();
				System.out.println("Got the signal");
			}
		})).start();
		Thread.sleep(2000);
		s.signal();

		Thread.sleep(1000);
		(new Thread(new Runnable() {
			public void run() 
			{
				System.out.print("Waiting for signal");

				while (!s.waitForSignal(100))
					System.out.print(".");

				System.out.println(" Got the signal");
			}
		})).start();
		Thread.sleep(5300);
		s.signal();

		Thread.sleep(1000);
		s.signal();
		(new Thread(new Runnable() {
			public void run() 
			{
				System.out.print("Waiting for signal");

				while (!s.waitForSignal(500))
					System.out.print(".");

				System.out.println(" Got the signal");
			}
		})).start();

		Thread.sleep(1000);

		Thread t;

		t = new Thread(new Runnable() {
			public void run() 
			{
				System.out.println("Waiting for signal(1)");
				s.waitForSignal();
				System.out.println("Got the signal(1)");
			}
		});
		t.setDaemon(true);
		t.start();

		t = new Thread(new Runnable() {
			public void run() 
			{
				System.out.println("Waiting for signal(2)");
				s.waitForSignal();
				System.out.println("Got the signal(2)");
			}
		});
		t.setDaemon(true);
		t.start();

		Thread.sleep(1000);
		s.signal();

		Thread.sleep(3000);
	}
	*/
}
