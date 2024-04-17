// $Id: DataEventThread.java,v 1.2 2023/06/12 16:37:55 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

class DataEventThread implements Runnable
{
	//protected static long longestUpdateMillis = 0; // cleared by ClockEventDecoder
	//protected static String longestUpdate = null;
	private DataEventObserver observer;

	private DataEvent event;

	private DataEvent current;

	DataEventThread(DataEventObserver observer, DataEvent event, DataEvent current)
	{
		this.observer = observer;
		this.event = event;
		this.current = current;
	}

	public synchronized void run()
	{
		//long now = System.currentTimeMillis();

		observer.update(event, current);
		//if (System.currentTimeMillis() - now > longestUpdateMillis) {
		//	longestUpdateMillis = System.currentTimeMillis() - now;
		//	longestUpdate = "observer: " + observer + ", request: " + request + ", millis: " + longestUpdateMillis;
		//}
		//observer = null;
		//request = null;
		//reply = null;
	}

	/**
     * Gets DataEvent request.
     * 
	 * @return the DataEvent request
	 */
	//public DataEvent getRequest()
	//{
	//	return request;
	//}

	/**
     * Gets DataEvent reply.
     * 
	 * @return the DataEvent reply
	 */
	//public DataEvent getReply()
	//{
	//	return reply;
	//}

	/**
     * Adds a DataEvent observer.
     * 
	 * @return a DataEvent observer
	 */
	//public DataEventObserver getObserver()
	//{
	//	return observer;
	//}

	//public String toString()
	//{
	//	return "DataEventThread, request: " + request + ", reply: " + reply
	//			+ ", observer: " + observer;
	//}
}
