// $Id: AcnetConnection.java,v 1.4 2024/04/01 15:30:49 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingQueue;

abstract public class AcnetConnection implements AcnetErrors, AcnetConstants, AcnetMessageHandler, AcnetRequestHandler
{
    final HashMap<RequestId, AcnetRequestContext> requestsOut = new HashMap<>();
	final HashMap<ReplyId, AcnetRequest> requestsIn = new HashMap<>();
	final StatSet stats = new StatSet();

    int name;
    int taskId;
	Node vNode;
	boolean receiving = false;
	AcnetMessageHandler messageHandler = this;
	AcnetRequestHandler requestHandler = this;
	volatile ReplyThread replyThread = null;

    AcnetConnection(String name, Node vNode)
	{
		this.name = Rad50.encode(name);
		this.taskId = -1;
		this.vNode = vNode;
    }

    AcnetConnection(int name, int taskId, Node vNode)
	{
		this.name = name;
		this.taskId = taskId;
		this.vNode = vNode;
    }

	abstract public String getName(int node) throws AcnetStatusException;
	abstract public int getNode(String name) throws AcnetStatusException;
	abstract public int getLocalNode() throws AcnetStatusException;
    abstract public void send(int node, String task, ByteBuffer buf) throws AcnetStatusException;
    abstract public AcnetRequestContext sendRequest(int node, String task, boolean multRpy, ByteBuffer dataBuf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException;

    final public String connectedName() 
	{
		return Rad50.decode(name);
    }

	final public AcnetConnection setQueueReplies()
	{
		if (replyThread == null)
			replyThread = new ReplyThread();

		return this;
	}

	final public String getLocalNodeName() throws AcnetStatusException
	{
		return getName(getLocalNode());
	}

	final public void send(String node, String task, ByteBuffer buf) throws AcnetStatusException
	{
		send(getNode(node), task, buf);
	}

    final public AcnetRequestContext requestSingle(int node, String task, ByteBuffer buf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(node, task, false, buf, tmo, replyHandler);
    }

    final public AcnetRequestContext requestSingle(String node, String task, ByteBuffer buf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(getNode(node), task, false, buf, tmo, replyHandler);
    }

    final public AcnetRequestContext requestMultiple(int node, String task, ByteBuffer buf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(node, task, true, buf, tmo, replyHandler);
    }

    final public AcnetRequestContext requestMultiple(String node, String task, ByteBuffer buf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(getNode(node), task, true, buf, tmo, replyHandler);
    }

    final public AcnetRequestContext requestMultiple(int node, String task, ByteBuffer buf, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(node, task, true, buf, 0, replyHandler);
    }

    final public AcnetRequestContext requestMultiple(String node, String task, ByteBuffer buf, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		return sendRequest(getNode(node), task, true, buf, 0, replyHandler);
    }

    final public void handleMessages(AcnetMessageHandler messageHandler) throws AcnetStatusException 
	{
		this.messageHandler = messageHandler;
		startReceiving();
    }

    final public synchronized void handleRequests(AcnetRequestHandler requestHandler) throws AcnetStatusException 
	{
		this.requestHandler = requestHandler;
		startReceiving();
    }

	final public InetAddress remoteTaskAddress(int node, int taskId) throws AcnetStatusException
	{
		//System.out.println("remoteTaskAddres: " + Node.get(node) + " " + taskId);

		final RPCint rpc = new RPCint();
		final ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);

		buf.put((byte) 19).put((byte) 0).putShort((short) taskId).flip();
		rpc.execute(node, "ACNET", buf, 1500);
		buf.clear();

		try {
			return InetAddress.getByAddress(buf.putInt(rpc.intVal).array());
		} catch (UnknownHostException e) {
			throw new AcnetStatusException(ACNET_NO_TASK);
		}
	}


	final void handleReply(AcnetReply reply)
	{
		final RequestId requestId = reply.requestId();
		final AcnetRequestContext context;

		synchronized (requestsOut) {
			context = requestsOut.get(requestId);
		}

		if (context != null) {
			try {
				context.replyHandler.handle(reply);
			} catch (Exception e) {
				AcnetInterface.logger.log(Level.WARNING, "ACNET reply callback exception for connection '" + connectedName() + "'", e);
			}

			if (reply.last()) {
				synchronized (requestsOut) {
					requestsOut.remove(requestId);
				}
				context.localCancel();
			}
		} else
			sendCancelNoEx(reply);
	}

	abstract void startReceiving() throws AcnetStatusException;
	abstract void stopReceiving() throws AcnetStatusException;
	abstract void ignoreRequest(AcnetRequest request) throws AcnetStatusException;
    abstract void sendCancel(AcnetRequestContext context) throws AcnetStatusException;
	abstract void sendCancel(AcnetReply reply) throws AcnetStatusException;
    abstract void sendReply(AcnetRequest request, int flags, ByteBuffer dataBuf, int status) throws AcnetStatusException;
	abstract boolean inDataHandler();
    abstract void disconnect();

    void requestAck(ReplyId replyId) throws AcnetStatusException { }

	final void sendCancelNoEx(AcnetRequestContext context)
	{
		try {
			sendCancel(context);
		} catch (Exception ignore) { }
	}

	void sendCancelNoEx(AcnetReply reply)
	{
		try {
			sendCancel(reply);
		} catch (Exception ignore) { }
	}

	@Override
	public void handle(AcnetMessage m) { }

	@Override
	public void handle(AcnetRequest r) 
	{ 
		// Default handler needs to shutdown the request

		try {
			r.sendLastStatus(ACNET_ENDMULT); 
		} catch (Exception e) { }
	}

	@Override
	public void handle(AcnetCancel c) { }

	final class ReplyThread implements Runnable
	{
		final Thread thread;
		final LinkedBlockingQueue<AcnetReply> queue;

		ReplyThread()
		{
			this.queue = new LinkedBlockingQueue<>();
			this.thread = new Thread(this);
			this.thread.setName("ACNET " + connectedName() + " reply");
			this.thread.start();
		}

		void stop()
		{
			queue.clear();
			queue.offer(AcnetReply.nil);
		}

		@Override
		public void run()
		{
			AcnetInterface.logger.log(Level.INFO, "ACNET - " + connectedName() + " reply queue thread start");

			AcnetReply reply;

			try {
				while ((reply = queue.take()) != AcnetReply.nil) {
					handleReply(reply);
					reply.data = null;
					reply.buf.free();
				}

			} catch (Exception e) {
				AcnetInterface.logger.log(Level.WARNING, "ACNET reply queue thread exception", e);
			}

			AcnetInterface.logger.log(Level.INFO, "ACNET - " + connectedName() + " reply queue thread stop");
		}
	}

	abstract class RPC implements AcnetReplyHandler
	{
		int status = 0;

		abstract void rpcHandle(AcnetReply reply);
		
		@Override
        final public synchronized void handle(AcnetReply reply)
        {
			rpcHandle(reply);
			notify();
		}

		final synchronized void execute(int node, String task, ByteBuffer buf, int timeout) throws AcnetStatusException
		{
			if (inDataHandler())
				throw new IllegalStateException("ACNET RPC calls are invalid during ACNET handler execution.");

			sendRequest(node, task, false, buf, timeout, this);

			try {
				wait();
				if (status != 0)
					throw new AcnetStatusException(status);
			} catch (InterruptedException e) {
				throw new AcnetStatusException(ACNET_DISCONNECTED);
			}
		}
	}

	final class RPCint extends RPC
	{
		int intVal;

		@Override
		public void rpcHandle(AcnetReply reply)
		{
			if (reply.status == 0) {
				if (reply.data().remaining() == 4)
					intVal = reply.data().getInt();
				else
					status = ACNET_IVM;
			} else
				status = reply.status;
		}
	}
}
