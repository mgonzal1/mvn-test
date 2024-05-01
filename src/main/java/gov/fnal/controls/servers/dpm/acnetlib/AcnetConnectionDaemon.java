// $Id: AcnetConnectionDaemon.java,v 1.2 2024/04/01 15:30:49 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.HashMap;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.net.URL;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.Delayed;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

abstract class AcnetConnectionDaemon extends AcnetConnection implements Delayed
{
	static final ReentrantLock sendCommandLock = new ReentrantLock();

	final static class ConnectionMonitor extends Thread
	{
		final DelayQueue<AcnetConnectionDaemon> q = new DelayQueue<>();
		
		ConnectionMonitor()
		{
			setName("ACNET monitor");
			setDaemon(true);
			start();
		}

		final void register(AcnetConnectionDaemon c)
		{
			c.setDelay(5000);
			q.add(c);
		}

		@Override
		public void run()
		{
			AcnetInterface.logger.log(Level.INFO, "ACNET connection monitor thread start");

			while (true) {
				try {
					final AcnetConnectionDaemon c = q.take();
					
					try {
						if (!c.disposed()) {
							if (c.connected())
								c.ping();
							else {
								AcnetInterface.logger.log(Level.INFO, "ACNET " + c.connectedName() + " - reconnect");
								c.connect();
								if (c.receiving)
									c.startReceiving();
							}
							c.setDelay(10000);
							q.add(c);
						}
					} catch (AcnetStatusException e) {
						AcnetInterface.logger.log(Level.INFO, "ACNET " + c.connectedName() + " - " + e);
						c.disconnect();
						c.setDelay(2000);
						q.add(c);
					}
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	static void start()
	{
		connectionMonitor = new ConnectionMonitor();
	}

	static void stop()
	{
	}

	static ConnectionMonitor connectionMonitor;

    final int pid;
	long nextMonitorTime;
    final ByteBuffer cmdBuf;

    AcnetConnectionDaemon(String name, Node vNode)
	{
		super(name, vNode);

		this.pid = AcnetInterface.getPid();
		this.cmdBuf = ByteBuffer.allocateDirect(16);
    }

	AcnetConnectionDaemon(int name, int taskId, Node vNode)
	{
		super(Rad50.decode(name), vNode);

		this.pid = AcnetInterface.getPid();
		this.taskId = taskId;
		this.cmdBuf = null;
	}

	final synchronized boolean connected()
	{
		return taskId != -1;
	}

    final synchronized void connect8() throws AcnetStatusException 
	{
		cmdBuf.clear();
		cmdBuf.putInt(pid).putShort(localPort());
		
		final ByteBuffer ackBuf = sendCommand(1, 1, null);

		this.taskId = (int) (ackBuf.get() & 0xff);
		this.name = ackBuf.getInt();
    }

    final synchronized void connect16() throws AcnetStatusException 
	{
		cmdBuf.clear();
		cmdBuf.putInt(pid).putShort(localPort());
		
		final ByteBuffer ackBuf = sendCommand(16, 16, null);

		this.taskId = (int) ackBuf.getShort() & 0xffff;
		this.name = ackBuf.getInt();
    }

	@Override
	public synchronized String getName(int node) throws AcnetStatusException 
	{
		cmdBuf.clear();
		cmdBuf.putShort((short) node);
		return Rad50.decode(sendCommand(12, 5, null).getInt());
	}

	@Override
	public synchronized int getNode(String name) throws AcnetStatusException
	{
		cmdBuf.clear();
		cmdBuf.putInt(Rad50.encode(name));
		return sendCommand(11, 4, null).getShort() & 0xffff;
	}

	@Override
	public synchronized int getLocalNode() throws AcnetStatusException
	{
		cmdBuf.clear();
		return sendCommand(13, 4, null).getShort() & 0xffff;
	}

	@Override
    public synchronized void send(int node, String task, ByteBuffer buf) throws AcnetStatusException 
	{
		cmdBuf.clear();
		cmdBuf.putInt(Rad50.encode(task)).putShort((short) node);
		sendCommand(4, 0, buf);
    }

	@Override
    public synchronized AcnetRequestContext sendRequest(int node, String task, boolean multRpy, ByteBuffer dataBuf, int tmo, AcnetReplyHandler replyHandler) throws AcnetStatusException 
	{
		synchronized (requestsOut) {
			final int t = Rad50.encode(task);

			cmdBuf.clear();
			cmdBuf.putInt(t).putShort((short) node).putShort((short) (multRpy ? 1 : 0)).putInt(tmo == 0 ? Integer.MAX_VALUE : tmo);
			final int reqid = sendCommand(18, 2, dataBuf).getShort();
			final AcnetRequestContext ctxt = new AcnetRequestContext(this, task, node, new RequestId(reqid), multRpy, tmo, replyHandler);
			requestsOut.put(ctxt.requestId(), ctxt);

			return ctxt;
		}
    }

	final synchronized void stopHandlingAll() throws AcnetStatusException
	{
		this.requestHandler = this;
		this.messageHandler = this;
		stopReceiving();
	}

	InetAddress nodeAddress(int node) throws AcnetStatusException
	{
		final RPCint rpc = new RPCint();
		final ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		final byte subcode = (byte) (0x40 | (((node >> 8) & 0xff) - 9));

		buf.put((byte) 17).put(subcode).putShort((short) (node & 0xff)).flip();
		rpc.execute(node, "ACNET", buf, 100);
		buf.clear();

		try {
			return InetAddress.getByAddress(buf.putInt(rpc.intVal).array());
		} catch (UnknownHostException e) {
			throw new AcnetStatusException(ACNET_NO_NODE);
		}
	}

	final void dispose()
	{
		if (replyThread != null)
			replyThread.stop();

		disconnect();
		close();
	}

	static void safeThreadInterrupt(Thread thread)
	{
		try {
			sendCommandLock.lock();
			thread.interrupt();
		} finally {
			sendCommandLock.unlock();
		}
	}

    synchronized void disconnect() 
	{
		try {
			cmdBuf.clear();
			sendCommand(3, 0, null);
		} catch (Exception e) { }

		synchronized (requestsOut) {
			for (AcnetRequestContext ctxt : requestsOut.values())
				ctxt.sendEndMult();

			requestsOut.clear();
		}

		taskId = -1;
    }

    synchronized void ping() throws AcnetStatusException 
	{
		cmdBuf.clear();
		sendCommand(0, 0, null);
    }

	synchronized void startReceiving() throws AcnetStatusException
	{
		receiving = true;
		cmdBuf.clear();
		sendCommand(6, 0, null);
	}

	synchronized void stopReceiving() throws AcnetStatusException
	{
		receiving = false;
		cmdBuf.clear();
		sendCommand(20, 0, null);
	}

    synchronized void requestAck(ReplyId replyId) throws AcnetStatusException
	{
		cmdBuf.clear();
		cmdBuf.putShort((short) replyId.value());
		sendCommand(9, 0, null);
    }

    synchronized void sendCancel(AcnetRequestContext context) throws AcnetStatusException 
	{
		synchronized (requestsOut) {
			requestsOut.remove(context.requestId);
		}

		cmdBuf.clear();
		cmdBuf.putShort((short) context.requestId.id);
		sendCommand(8, 0, null);
    }

	void sendCancel(AcnetReply reply) throws AcnetStatusException
	{
		synchronized (requestsOut) {
			requestsOut.remove(reply.requestId);
		}

		cmdBuf.clear();
		cmdBuf.putShort((short) reply.requestId.id);
		sendCommand(8, 0, null);
	}

	final void sendCancelNoEx(AcnetReply reply)
	{
		try {
			sendCancel(reply);
		} catch (Exception ignore) { }
	}

	synchronized void ignoreRequest(AcnetRequest request) throws AcnetStatusException
	{
		if (request.isCancelled())
			throw new AcnetStatusException(ACNET_NO_SUCH);

		final ReplyId replyId = request.replyId();

		synchronized (requestsIn) {
			requestsIn.remove(replyId);
			request.cancel();
		}

		cmdBuf.clear();
		cmdBuf.putShort((short) replyId.value());
		sendCommand(19, 0, null);
	}

    synchronized void sendReply(AcnetRequest request, int flags, ByteBuffer dataBuf, int status) throws AcnetStatusException 
	{
		if (request.isCancelled())
			throw new AcnetStatusException(ACNET_NO_SUCH);

		final short rpyid = (short) request.replyId().value();

		if (!request.multipleReply() || flags == REPLY_ENDMULT) {
			synchronized (requestsIn) {
				requestsIn.remove(request.replyId());
				request.cancel();
			}
		}

		cmdBuf.clear();
		cmdBuf.putShort(rpyid).putShort((short) flags).putShort((short) status);
		sendCommand(7, 3, dataBuf);
    }

	final synchronized void setDelay(long delay)
	{
		nextMonitorTime = System.currentTimeMillis() + delay;
	}

	@Override
	final public synchronized long getDelay(TimeUnit unit)
	{
		return unit.convert(nextMonitorTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

/*
	@Override
	public void handle(AcnetRequest r) 
	{ 
		// Default handler needs to shutdown the request

		try {
			r.sendLastStatus(ACNET_ENDMULT); 
		} catch (Exception e) { }
	}

	@Override
	public void handle(AcnetCancel c) 
	{ 
	}
	*/

	@Override
	final public synchronized int compareTo(Delayed o)
	{
		if (this.nextMonitorTime < ((AcnetConnectionDaemon) o).nextMonitorTime)
			return -1;
		else if (this.nextMonitorTime > ((AcnetConnectionDaemon) o).nextMonitorTime) 
			return 1;

		return 0;
	}

	static boolean isLocalAddress(InetAddress address)
	{
    	// Check if the address is a valid special local or loop back

    	if (address.isAnyLocalAddress() || address.isLoopbackAddress())
        	return true;

    	// Check if the address is defined on any interface

    	try {
        	return NetworkInterface.getByInetAddress(address) != null;
    	} catch (Exception e) {
        	return false;
    	}
	}

/*
	public static AcnetConnection open()
	{
		return open("", "");
	}

	public static AcnetConnection open(String name)
	{
		return open(name, "");
	}

	public static AcnetConnection open(String name, String vNode)
	{
		return new AcnetConnectionUDP(name, vNode);
	}

	public static AcnetConnection open(InetAddress address)
	{
		return open(address, "", "");
	}
	
	public static AcnetConnection open(InetAddress address, String name)
	{
		return open(address, "", "");
	}
*/
/*
	public static AcnetConnection open(InetAddress address, String name, String vNode)
	{
		return open(new InetSocketAddress(address, ACNET_TCP_PORT), name, vNode);
	}
	*/

/*
	public static AcnetConnection openProxy() throws UnknownHostException
	{
		return openProxy("", "");
	}

	public static AcnetConnection openProxy(String name) throws UnknownHostException
	{
		return openProxy(name, "");
	}

	public static AcnetConnection openProxy(String name, String vNode) throws UnknownHostException
	{
		return open(new InetSocketAddress(ACSYS_PROXY_HOST, ACNET_TCP_PORT), name, vNode);
	}

	public static AcnetConnection openProxy(URL url, String name, String vNode)
	{
		return open(new InetSocketAddress(url.getHost(), url.getPort()), name, vNode);
	}
	*/

/*
	public static AcnetConnection open(InetSocketAddress address, String name, String vNode)
	{
		if (isLocalAddress(address.getAddress()))
			return open(name, vNode);

		return new AcnetConnectionTCP(address, name, vNode);
	}
	*/

	abstract short localPort();
    abstract ByteBuffer sendCommand(int cmd, int ack, ByteBuffer dataBuf) throws AcnetStatusException;
	abstract void connect() throws AcnetStatusException;
	abstract void close();
	abstract boolean disposed();
}
