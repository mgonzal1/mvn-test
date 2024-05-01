// $Id: AcnetRequestContext.java,v 1.5 2024/03/27 21:09:26 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.Timer;
import java.util.TimerTask;
import java.nio.ByteBuffer;

final public class AcnetRequestContext implements AcnetConstants, AcnetErrors, AcnetReplyHandler
{
	static Timer timeoutTimer = null; //new Timer("Request timeout timer");

	//final AtomicReference<AcnetConnection> connection;
	volatile AcnetConnection connection;
	final String task;
	final int taskId;
	final Node node;
	final RequestId requestId;
	final boolean isMult;
	final long timeout;

	volatile AcnetReplyHandler replyHandler;
	volatile TimeoutTask timeoutTask;

	class TimeoutTask extends TimerTask
	{
		@Override
		public void run()
		{
			if (replyHandler != null)
				replyHandler.handle(new AcnetReply(ACNET_UTIME, requestId, node.value(), !isMult));
		}
	}

	public AcnetRequestContext()
	{
		//this.connection = new AtomicReference<>();
		this.connection = null;
		this.task = "";
		this.taskId = -1;
		this.node = null;
		this.requestId = new RequestId(0);
		this.isMult = false;
		this.timeout = -1;
		this.replyHandler = this;
		this.timeoutTask = null;
	}

	@Override
	public void handle(AcnetReply reply) { }

	synchronized void startTimeoutTimer()
	{
		if (timeoutTimer == null)
			timeoutTimer = new Timer("Request timeout timer");

		//if (timeoutTimer != null)
			//timeoutTimer.cancel();

		timeoutTask = new TimeoutTask();
		timeoutTimer.schedule(timeoutTask, timeout);
	}

	synchronized void stopTimeoutTimer()
	{
		if (timeoutTimer != null) {
			timeoutTask.cancel();
			timeoutTask = null;
		}
	}

    AcnetRequestContext(AcnetConnection connection, String task, int node, RequestId requestId, boolean isMult, long timeout, AcnetReplyHandler replyHandler) throws AcnetStatusException
	{
		//this.connection = new AtomicReference<>(connection);
		this.connection = connection;
		this.task = task;
		this.taskId = connection.taskId;
		this.node = Node.get(node);
		this.requestId = requestId;
		this.isMult = isMult;
		this.timeout = timeout;
		this.replyHandler = replyHandler;
		this.timeoutTask = null;
    }

	int taskId() throws AcnetStatusException
	{
		//final AcnetConnection cLocal = connection.get();
		final AcnetConnection c = connection;
		
		if (c != null)
			return c.taskId;

		throw new AcnetStatusException(ACNET_NO_SUCH);
	}

	void sendEndMult()
	{
		replyHandler.handle(new AcnetReply(ACNET_ENDMULT, requestId, node.value(), true));
	}
	
	void localCancel()
	{
		connection = null;
		//connection.set(null);
	}

	public void setReplyHandler(AcnetReplyHandler replyHandler)
	{
		this.replyHandler = replyHandler;
	}

	public RequestId requestId()
	{
		return requestId;
	}

	public int hashCode()
	{
		return requestId.hashCode();
	}

    public boolean isCancelled() 
	{
		return connection == null;
		//return connection.get() == null;
    }

    public void cancel() throws AcnetStatusException 
	{
		//final AcnetConnection cLocal = connection.getAndSet(null);
		final AcnetConnection c = connection;

		if (c != null)
			c.sendCancel(this);
    }

	public void cancelNoEx()
	{
		try {
			cancel();
		} catch (Exception ignore) { }
	}

	public String toString()
	{
		return "AcnetRequestContext:" + requestId + (isCancelled() ? " (cancelled)" : " (active)");
	}
}
