// $Id: AcnetConnectionInternal.java,v 1.4 2024/03/06 15:40:03 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.HashMap;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class AcnetConnectionInternal extends AcnetConnection
{
	static class AcnetAuxHandler implements AcnetRequestHandler, AcnetErrors
	{
		final ByteBuffer buf = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);

		synchronized void putTime(long timeBase, ByteBuffer buf)
		{
			final long t = System.currentTimeMillis() - timeBase;
			
			buf.putShort((short) t);
			buf.putShort((short) (t >> 16));
			buf.putShort((short) (t >> 32));
		}

		void taskHandler(int subType, AcnetRequest r)
		{
			switch (subType) {
			 case 0:
				buf.putShort((short) connectionsByName.size());
				for (AcnetConnection c : connectionsById)
						buf.putInt(c.name);
				for (AcnetConnection c : connectionsById)
						buf.put((byte) c.taskId);
				break;

			 case 1:
			 	short count = 0;
				for (AcnetConnection c : connectionsById) {
					if (c.receiving)
						count++;	
				}
				buf.putShort(count);
				for (AcnetConnection c : connectionsById) {
					if (c.receiving)
						buf.putInt(c.name);
				}
				for (AcnetConnection c : connectionsById) {
					if (c.receiving)
						buf.put((byte) c.taskId);
				}
				break;

			 case 2:
				buf.putShort((short) connectionsByName.size());
				for (AcnetConnection c : connectionsById)
					buf.putInt(0);
				for (AcnetConnection c : connectionsById)
					buf.put((byte) c.taskId);
				break;
			 
			 case 3:
				break;
			}

			buf.flip();
			r.sendLastReplyNoEx(buf, 0);
		}

		@Override
		public void handle(AcnetRequest r)
		{
			try {
				final ByteBuffer data = r.data().order(ByteOrder.LITTLE_ENDIAN);

				final int type = data.get() & 0xff;
				final int subType = data.get() & 0xff;

				AcnetInterface.logger.log(Level.INFO, "AcnetAuxHandler: " + type + " subtype: " + subType);

				buf.clear();

				switch (type) {
				 case 0:
					buf.putShort((short) 0).flip();
					r.sendLastReply(buf, ACNET_SUCCESS);
					break;

				 case 1:
					{
						final AcnetConnection c = connectionsByName.get(data.getInt());

						if (c != null) {
							buf.putShort((short) c.taskId).flip();
							r.sendLastReply(buf, ACNET_SUCCESS);
						} else
							r.sendLastStatus(ACNET_NO_TASK);
					}
					break;

				 case 2:
					{
						if (subType < connectionsById.size()) {
							final AcnetConnection c = connectionsById.get(subType);

							if (c != null) {
								buf.putInt(c.name).flip();
								r.sendLastReply(buf, ACNET_SUCCESS);
							} else
								r.sendLastStatus(ACNET_NO_TASK);
						} else
							r.sendLastStatus(ACNET_LEVEL2);
					}
					break;
				 
				 case 3:
					buf.putShort((short) 0x0100);
					buf.putShort((short) 0x0100);
					buf.putShort((short) 0x0100).flip();
					r.sendLastReply(buf, ACNET_SUCCESS);
					break;

				 case 4:
					taskHandler(subType, r);
					break;

				 case 6:
				 	{
						putTime(nodeStatsTime, buf);
						buf.putLong(0);
						stats.putStats(buf).flip();
						r.sendLastReply(buf, ACNET_SUCCESS);

						if (subType != 0) {
							nodeStatsTime = System.currentTimeMillis();
							stats.clear();
						}
					}
				 	break;

				 case 7:
				 	{
						putTime(connectionStatsTime, buf);
						buf.putShort((short) (0x900 + connectionsById.size()));

						for (AcnetConnection c : connectionsById) {
							buf.putShort((short) c.taskId);
							buf.putInt(c.name);
							c.stats.putStats(buf);

							if ((subType & 1) > 0) {
								connectionStatsTime = System.currentTimeMillis();
								c.stats.clear();
							}
						}
						buf.flip();
						r.sendLastReply(buf, ACNET_SUCCESS);

					}
				 	break;

				 default:
					r.sendLastStatus(ACNET_LEVEL2);
					break;
				}
			} catch (Exception e) {
				r.sendLastStatusNoEx(ACNET_LEVEL2);
			}
		}

		@Override
		public void handle(AcnetCancel c)
		{
		}
	}

	static class IdBank implements AcnetErrors
	{
		final static Random random = new Random();

		final int bankBegin, bankEnd;
		final LinkedList<Integer> freeIds;

		IdBank(int size)
		{
			this.bankBegin = random.nextInt(0xffff) & 0xf000;
			this.bankEnd = this.bankBegin + size;
			this.freeIds = new LinkedList<Integer>();

			for (int ii = bankBegin; ii < bankEnd; ii++)
				freeIds.add(ii);
		}

		synchronized int alloc() throws AcnetStatusException
		{
			if (freeIds.size() == 0)
				throw new AcnetStatusException(ACNET_NOLCLMEM);
				
			return freeIds.removeFirst();
		}
		
		synchronized void free(int id)
		{
			if (id >= bankBegin && id < bankEnd)
				freeIds.addLast(id);
			else
				AcnetInterface.logger.log(Level.WARNING, "free wrong id " + id);
		}
	}

	static class ReadThread extends Thread
	{
		final DatagramChannel channel;

		ReadThread(DatagramChannel channel)
		{
			this.channel = channel;

			setName("ACNET read thread");
			start();
		}

		@Override
		public void run()
		{
			final ByteBuffer rcvBuf = ByteBuffer.allocate(64 * 1024);

			//try {
				while (true) {
					try {
						final SocketAddress from = channel.receive(rcvBuf);

						rcvBuf.flip();
						//System.out.println("RECEIVED " + rcvBuf.remaining() + " BYTES  from " + from);

						if (rcvBuf.remaining() >= 18) {
							final Buffer buf = BufferCache.alloc();
							final byte[] s = rcvBuf.array();
							final byte[] d = buf.array();

							for (int ii = 0; ii < buf.remaining(); ii += 2) {
								d[ii] = s[ii + 1];
								d[ii + 1] = s[ii];
							}
							buf.remaining(rcvBuf.remaining()).littleEndian();

							final AcnetPacket packet = AcnetPacket.create(buf);

							final int task = buf.remaining(rcvBuf.remaining()).littleEndian().peekInt(8);
							//System.out.println("RECEIVE REPLY FROM TASK: " + packet.serverTaskName() + " " + packet.clientTaskId());

							if (packet.isReply() && packet.clientTaskId() < connectionsById.size()) {
								final AcnetConnectionInternal connection = connectionsById.get(packet.clientTaskId());

								if (connection != null) {
									//System.out.println("REPLY - FOUND TASK: " + connection.connectedName() + " " + connection.taskId);

									// Don't handle a multicast reply with a status

									if (!packet.isMulticast() || packet.status() == 0) {
										connection.inHandler = true;
										packet.handle(connection);
										connection.inHandler = false;
									} else
										buf.free();
								} else
									buf.free();
							} else {
								final AcnetConnectionInternal connection = connectionsByName.get(packet.serverTask());

								if (connection != null && connection.receiving) {
									//System.out.println("FOUND TASK: " + packet.serverTaskName());
									if (!packet.isMulticast() || packet.status() == 0) {
										connection.inHandler = true;
										packet.handle(connection);
										connection.inHandler = false;
									} else
										buf.free();
								} else if (packet.isRequest() && !packet.isMulticast()) {
									writeThread.sendReply((AcnetRequest) packet, REPLY_ENDMULT, null, ACNET_NO_TASK);
									buf.free();
								} else
									buf.free();
							}

							//buf.free();
						}

						rcvBuf.clear();
					} catch (IOException e) {
						return;
					} catch (AcnetStatusException e) {
					}
				}
			//} catch (Exception e) {
			//	e.printStackTrace();
			//}
		}
	}

	static class WriteThread extends Thread implements AcnetErrors
	{
		final DatagramChannel channel;
		final private LinkedBlockingQueue<Buffer> queue;
		//final ByteBuffer buf;
		final Node vNode;

		WriteThread(DatagramChannel channel, Node vNode) throws AcnetStatusException
		{
			this.channel = channel;
			this.queue = new LinkedBlockingQueue<>();
			this.vNode = vNode;
			//this.buf = ByteBuffer.allocate(64 * 1024);

			setName("ACNET write thread");
			start();
		}

		@Override
		public void run()
		{
			while (true) {
				Buffer buf = null;

				try {
					while ((buf = queue.take()) != Buffer.nil) {
						//System.out.println("WRITE THREAD TAKE");
						//buf = queue.take();
						//System.out.println("WRITE TAKE RETURN");

						//System.out.println("SENDING " + buf.remaining() + " BYTES to " + buf.address);

						channel.send(buf.buf, buf.address);

						buf.address = null;
						buf.free();
					}

					return;
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (buf != null) {
					buf.address = null;
					buf.free();
				}
			}
		}

		void sendRequest(AcnetRequestContext context, ByteBuffer data) throws AcnetStatusException
		{
			//System.out.println("queue: " + queue.remainingCapacity());
			//context.data = data;

			final Buffer buf = BufferCache.alloc();

			//System.out.println("SENDING REQUEST mult:" + context.isMult + " " + context.node + " " + context.task + " " + context.taskId() + " " + context.node.address());

			buf.bigEndian().putShort(ACNET_FLG_REQ | (context.isMult ? ACNET_FLG_MLT : 0));
			buf.putShort(0);
			buf.littleEndian().putShort(context.node.value());
			buf.putShort(vNode.value());

			final int task = Rad50.encode(context.task);

			buf.bigEndian().putShort((short) task);
			buf.putShort((short) (task >> 16));
			buf.putShort(context.taskId());
			buf.putShort(context.requestId.value());
			buf.putShort(18 + data.remaining());

			final byte[] src = data.array();

			for (int ii = 0; ii < data.remaining(); ii += 2) {
				buf.put(src[ii + 1]).put(src[ii]);
			}

			stats.requestSent();

			queue.offer(buf.setAddressAndFlip(context.node.address()));
		}

		void sendMessage(int nodeValue, String taskName, ByteBuffer data) throws AcnetStatusException
		{
			final Node node = Node.get(nodeValue); 
			final Buffer buf = BufferCache.alloc();

			buf.bigEndian().putShort(ACNET_FLG_USM);
			buf.putShort(0);
			buf.littleEndian().putShort(node.value());
			buf.putShort(vNode.value());

			final int task = Rad50.encode(taskName);

			buf.bigEndian().putShort(task);
			buf.putShort((short) (task >> 16));
			buf.putShort(0);
			buf.putShort(0);
			buf.putShort(18 + data.remaining());

			final byte[] src = data.array();

			for (int ii = 0; ii < data.remaining(); ii += 2)
				buf.put(src[ii + 1]).put(src[ii]);

			stats.messageSent();

			queue.offer(buf.setAddressAndFlip(node.address()));
		}

    	void sendReply(AcnetRequest request, int flags, ByteBuffer data, int status) throws AcnetStatusException 
		{
			final Node node = Node.get(request.client); 
			//System.out.println("SENDING REPLY " + node + " " + request.serverTaskName() + " " + request.clientTaskId + " " + request.client + " " + node.address());
			final Buffer buf = BufferCache.alloc();
			final boolean lastReply = (flags & REPLY_ENDMULT) > 0;

			buf.bigEndian().putShort(ACNET_FLG_RPY | ((request.multipleReply() && !lastReply) ? ACNET_FLG_MLT : 0));

			//System.out.printf("isMult: %s status:0x%04x\n", request.multipleReply(), status);
			//System.out.printf("flags: 0x%04x\n", flags);

			if (lastReply) {
				if (request.multipleReply()) {
					if (status != 0)
						buf.putShort(status);
					else
						buf.putShort(ACNET_ENDMULT);
				} else
					buf.putShort(status);
			} else {
				//System.out.println("putSHort(STAUTUS)");
				buf.putShort(status);
			}

			buf.littleEndian().putShort(vNode.value());
			buf.littleEndian().putShort(request.client);

			final int task = request.serverTask;

			buf.bigEndian().putShort(task);
			buf.putShort(task >> 16);
			buf.putShort(request.clientTaskId);
			buf.putShort(request.id);

			if (data != null) {
				buf.putShort(18 + data.remaining());

				final byte[] src = data.array();

				for (int ii = 0; ii < data.remaining(); ii += 2) {
					//d[pos] = s[ii + 1];
					//d[pos + 1] = s[ii];
					//System.out.println("data[" + ii + "] " + ((char) src[ii]));
					//System.out.println("data[" + (ii + 1) + "] " + ((char) src[ii + 1]));
					buf.put(src[ii + 1]).put(src[ii]);
				}
			} else
				buf.putShort(18);

			stats.replySent();

			queue.offer(buf.setAddressAndFlip(node.address()));
		}

    	void sendCancel(AcnetRequestContext context) throws AcnetStatusException 
		{
			//System.out.println("SENDING CANCEL " + context.node + " " + context.task);
			final Buffer buf = BufferCache.alloc();

			buf.bigEndian().putShort(ACNET_FLG_CAN);
			buf.putShort(0);
			buf.littleEndian().putShort(context.node.value());
			buf.putShort(vNode.value());

			final int task = Rad50.encode(context.task);

			buf.bigEndian().putShort((short) task & 0xffff);
			buf.putShort((short) (task >> 16));
			buf.putShort(context.taskId);
			buf.putShort(context.requestId.id);
			buf.putShort(18);

			queue.offer(buf.setAddressAndFlip(context.node.address()));
		}

		void sendCancel(AcnetReply reply) throws AcnetStatusException
		{
			final Node node = Node.get(reply.server);
			final Buffer buf = BufferCache.alloc();
			//System.out.println("SENDING CANCEL " + node + " " + Rad50.decode(reply.serverTask));

			buf.bigEndian().putShort(ACNET_FLG_CAN);
			buf.putShort(0);
			buf.littleEndian().putShort(node.value());
			buf.putShort(vNode.value());

			int task = reply.serverTask;

			buf.bigEndian().putShort((short) task & 0xffff);
			buf.putShort((short) (task >> 16));
			buf.putShort(reply.clientTaskId);
			buf.putShort(reply.requestId.id);
			buf.putShort(18);

			queue.offer(buf.setAddressAndFlip(node.address()));
		}
	}

	static void start(DatagramChannel channel, Node vNode) throws IOException, AcnetStatusException
	{
		channel.configureBlocking(true);
		channel.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);

		for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
			final NetworkInterface ni = e.nextElement();

			//for (InterfaceAddress addr : ni.getInterfaceAddresses()) {
			//	System.out.println("HOSTNAME: " + addr.getAddress().getHostName());
			//}

			if (!ni.isLoopback())
				channel.join(InetAddress.getByName("239.128.4.1"), ni);
		}

		nodeStatsTime = connectionStatsTime = System.currentTimeMillis();

		readThread = new ReadThread(channel);
		writeThread = new WriteThread(channel, vNode);

		//for (int ii = 0; ii < connectionsById.size(); ii++)
		//	connectionsById[ii] = null;

		open("ACNET", vNode).handleRequests(new AcnetAuxHandler());

		//acnet.handleRequests(new AcnetAuxHandler());

		AcnetInterface.logger.log(Level.CONFIG, "Using internal ACNET");
	}

	static void stop()
	{
		for (AcnetConnection connection : connectionsByName.values())
			connection.disconnect();

		writeThread.queue.offer(Buffer.nil);

		try {
			writeThread.join();
			Thread.sleep(1500);
		} catch (Exception ignore) { }
	}

	static final IdBank reqIdBank = new IdBank(4096);
	static long nodeStatsTime, connectionStatsTime;
	static final StatSet stats = new StatSet(); 
	static ReadThread readThread;
	static WriteThread writeThread;
	static final HashMap<Integer, AcnetConnectionInternal> connectionsByName = new HashMap<>();
	static final ArrayList<AcnetConnectionInternal> connectionsById = new ArrayList<>();

	boolean inHandler;

	static synchronized AcnetConnection open(String name, Node vNode)
	{
		final int taskId = connectionsById.size();

		if (name == null || name.isEmpty())
			name = String.format("%%%05d", taskId);

		final int encodedName = Rad50.encode(name);

		AcnetConnectionInternal connection = connectionsByName.get(encodedName);

		if (connection == null) {
			connection = new AcnetConnectionInternal(encodedName, connectionsById.size(), vNode);
			connectionsByName.put(encodedName, connection);
			connectionsById.add(connection);
		}

		return connection;
	}

    AcnetConnectionInternal(int name, int taskId, Node vNode)
	{
		super(name, taskId, vNode);

		this.inHandler = false;
    }

	@Override
	final public synchronized String getName(int node) throws AcnetStatusException 
	{
		return Node.get(node).name();
	}

	@Override
	final public synchronized int getNode(String name) throws AcnetStatusException
	{
		return Node.get(name).value();
	}

	@Override
	final public synchronized int getLocalNode() throws AcnetStatusException
	{
		return vNode.value();
	}

	@Override
    final public synchronized void send(int node, String task, ByteBuffer buf) throws AcnetStatusException 
	{
		writeThread.sendMessage(node, task, buf);

		stats.messageSent();
    }

	@Override
    final public synchronized AcnetRequestContext sendRequest(int node, String task, boolean isMult, ByteBuffer data, int timeout, AcnetReplyHandler replyHandler) throws AcnetStatusException
	{
		synchronized (requestsOut) {
			final AcnetRequestContext context = new AcnetRequestContext(this, task, node, new RequestId(reqIdBank.alloc()), isMult, timeout, replyHandler);

			requestsOut.put(context.requestId(), context);
			writeThread.sendRequest(context, data);
			stats.requestSent();

			return context;
		}
    }

	@Override
    final synchronized void disconnect() 
	{
		synchronized (requestsOut) {
			for (AcnetRequestContext context : requestsOut.values()) {
				try {
					context.cancel();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			requestsOut.clear();

			for (AcnetRequest request : requestsIn.values()) {
				try {
					request.sendLastStatus(ACNET_DISCONNECTED);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			requestsIn.clear();
		}
    }

	@Override
	final synchronized void startReceiving() throws AcnetStatusException
	{
		receiving = true;
	}

	@Override
	final synchronized void stopReceiving() throws AcnetStatusException
	{
		receiving = false;
	}

	@Override
    final synchronized void sendCancel(AcnetRequestContext context) throws AcnetStatusException 
	{
		synchronized (requestsOut) {
			requestsOut.remove(context.requestId);
		}

		writeThread.sendCancel(context);
    }

	@Override
    final synchronized void sendCancel(AcnetReply reply) throws AcnetStatusException 
	{
		synchronized (requestsOut) {
			requestsOut.remove(reply.requestId);
		}

		writeThread.sendCancel(reply);
    }

	@Override
	final synchronized void ignoreRequest(AcnetRequest request) throws AcnetStatusException
	{
		if (request.isCancelled())
			throw new AcnetStatusException(ACNET_NO_SUCH);

		final ReplyId replyId = request.replyId();

		synchronized (requestsIn) {
			requestsIn.remove(replyId);
			request.cancel();
		}

	}

	@Override
    final synchronized void sendReply(AcnetRequest request, int flags, ByteBuffer dataBuf, int status) throws AcnetStatusException 
	{
		if (request.isCancelled())
			throw new AcnetStatusException(ACNET_NO_SUCH);

		if (!request.multipleReply() || flags == REPLY_ENDMULT) {
			synchronized (requestsIn) {
				requestsIn.remove(request.replyId());
				request.cancel();
			}
		}

		writeThread.sendReply(request, flags, dataBuf, status);		

		stats.replySent();
    }

	@Override
	boolean inDataHandler()
	{
		return inHandler;
	}
}
