// $Id: AcnetConnectionTCP.java,v 1.4 2024/03/06 15:38:07 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.Iterator;
import java.util.logging.Level;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

class AcnetConnectionTCP extends AcnetConnectionDaemon
{
	enum ReadState { PacketLength, PacketData };

    final static int ACNET_TCP_PORT = 6802;
	final static String ACSYS_PROXY_HOST = "acsys-proxy.fnal.gov";

	private static final short TCP_CLIENT_PING = 0;
	private static final short ACNETD_COMMAND = 1;
	private static final short ACNETD_ACK = 2;
	private static final short ACNETD_DATA = 3;

	private final class TCPBuffer extends Buffer
	{
		TCPBuffer(ByteBuffer buf)
		{
			super(buf);
		}

		@Override
		public void free() { }
	}

	private final static class SocketThread extends Thread {
		final Selector sel;
		final ConcurrentLinkedQueue<AcnetConnectionTCP> regQ;

		SocketThread()
		{
			try {
				sel = Selector.open();
				regQ = new ConcurrentLinkedQueue<AcnetConnectionTCP>();
				setName("AcnetConnectionTCP.SocketThread");
				start();
			} catch (Exception e) {
				AcnetInterface.logger.log(Level.SEVERE, "unable to create ACNET data thread", e);
				throw new RuntimeException("unable to create ACNET data thread");
			}
		}

		@Override
		public void run()
		{
			AcnetInterface.logger.info("Socket thread start");

			try {
				while (true) {
					if (sel.select() > 0) {
						Iterator<SelectionKey> ii = sel.selectedKeys().iterator();

						while (ii.hasNext()) {
							SelectionKey key = ii.next();

							if (key.isReadable()) {
								AcnetConnectionTCP c = (AcnetConnectionTCP) key.attachment();

								try {
									c.read();
								} catch (Exception e) {
									key.cancel();
									key.channel().close();

									AcnetInterface.logger.log(Level.FINER, "ACNET handler exception for connection '" + c.connectedName() + "'", e);
								}
							}

							ii.remove();
						}	
					}

					if (!regQ.isEmpty()) {
						AcnetConnectionTCP c;

						while ((c = regQ.poll()) != null) {
							c.channel.configureBlocking(false);
							c.channel.register(sel, SelectionKey.OP_READ, c);
						}
					}
				}
			} catch (IOException e) {
				AcnetInterface.logger.log(Level.SEVERE, "socket thread exception", e);
				throw new RuntimeException("socket thread exception", e);
			}
		}

		void register(AcnetConnectionTCP c)
		{
			regQ.add(c);
			sel.wakeup();
		}
	}

	private static SocketThread socketThread = new SocketThread();

	private final InetSocketAddress address;
	private final ByteBuffer tcpBuf = ByteBuffer.allocateDirect(16);
    private final ByteBuffer hdrBuf = ByteBuffer.allocateDirect(16);
    private final ByteBuffer nulBuf = ByteBuffer.allocate(0);
    private final ByteBuffer[] cmdBufs = { tcpBuf, hdrBuf, cmdBuf, nulBuf };
	private final BlockingQueue<ByteBuffer> ackQueue = new LinkedBlockingQueue<>(1);
	private final DataHandler dataHandler;
	private final ByteBuffer lenBuf = ByteBuffer.allocateDirect(4);

    private SocketChannel channel = null;
	private ByteBuffer readBuf;

	private ReadState readState = ReadState.PacketLength;

	//AcnetConnectionTCP(InetSocketAddress address)
	//{
	//	this(address, "", "");
    //}

	//AcnetConnectionTCP(InetSocketAddress address, String name)
	//{
	//	this(address, name, "");
	//}
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

    AcnetConnectionTCP(InetSocketAddress address, String name, Node vNode)
	{
		super(name, vNode);

		this.address = address;
		this.dataHandler = new DataHandler();

		try {
			connect();
		} catch (AcnetStatusException e) {
			AcnetInterface.logger.log(Level.SEVERE, "connect exception", e);
		}

		connectionMonitor.register(this);
    }

	AcnetConnectionTCP(String host, String name, Node vNode)
	{
		this(new InetSocketAddress(host, ACNET_TCP_PORT), name, vNode);
	}

	@Override
	boolean inDataHandler()
	{
		return false;
	}

	@Override
	final short localPort()
	{
		return 0;
	}

	@Override
    final synchronized void connect() throws AcnetStatusException
	{
		openChannel();

		socketThread.register(this);
		connect8();
		dataHandler.setName();
    }

	@Override
	final boolean disposed()
	{
		return channel == null;
	}

	private class DataHandler extends Thread
	{
		private final LinkedBlockingQueue<ByteBuffer> queue = new LinkedBlockingQueue<>();

		DataHandler()
		{
			start();
		}

		@Override
		public void run()
		{
			while (true) {
				try {
					final ByteBuffer buf = queue.take();

					//try {
						//handleAcnetPacket(new TCPBuffer(buf));
					//} catch (Exception e) {
					//	AcnetInterface.logger.log(Level.FINE, "exception", e);
					//} finally {
						//BufferCache.release(buf);
					//}
				} catch (Exception ignore) { }
			}
		}

		void setName()
		{
			setName("AcnetConnectionTCP.DataHandler-" + connectedName());
		}

		void add(ByteBuffer buf) throws InterruptedException
		{
			queue.offer(buf);
		}
	}

    private final void read() throws IOException 
	{
		if (readState == ReadState.PacketLength) {
			if (lenBuf.remaining() > 0) {
				final int count = channel.read(lenBuf);

				if (count == -1)
					throw new IOException("closed channel");

				if (lenBuf.remaining() == 0) {
					lenBuf.flip();

					final int len = lenBuf.getInt();

					if (len < 0 || len > 65535)
						throw new IOException("bad length value " + len);

					//readBuf = BufferCache.alloc();
					readBuf = ByteBuffer.allocate(len);
					lenBuf.clear();
					channel.read(readBuf);
					readState = ReadState.PacketData;
				}
			}
		} 
		
		if (readState == ReadState.PacketData) {
			channel.read(readBuf);

			if (readBuf != null && readBuf.remaining() == 0) {
				readBuf.flip();

				switch (readBuf.getShort()) {
				 case TCP_CLIENT_PING:
					break;

				 case ACNETD_ACK:
					try {
						ackQueue.put(readBuf);
					} catch (InterruptedException e) {
					}
					break;
				
				 case ACNETD_DATA:
					readBuf.order(ByteOrder.LITTLE_ENDIAN);
					try {
						dataHandler.add(readBuf);
					} catch (InterruptedException e) {
						AcnetInterface.logger.log(Level.FINE, "interrupted", e);
					}
					//BufferCache.release(readBuf);
					readBuf = null;
					break;
				}

				lenBuf.clear();
				readState = ReadState.PacketLength;
			}
		}
    }

	@Override
    final synchronized ByteBuffer sendCommand(int cmd, int ack, ByteBuffer dataBuf) throws AcnetStatusException 
	{
		if (!disposed()) {
			hdrBuf.clear();
			hdrBuf.putShort((short) cmd).putInt(name).putInt(Rad50.encode(vNode.name())).flip();

			cmdBuf.flip();

			if (dataBuf == null)
				dataBuf = nulBuf;

			cmdBufs[3] = dataBuf;

			ByteBuffer ackBuf = null;
			final int pktLen = hdrBuf.remaining() + cmdBuf.remaining() + dataBuf.remaining() + 2;
			tcpBuf.clear();
			tcpBuf.putInt(pktLen).putShort(ACNETD_COMMAND).flip();

			try {
				boolean wasInterrupted;

				sendCommandLock.lock();
				wasInterrupted = Thread.currentThread().interrupted();

				// Send the command

				channel.write(cmdBufs);

				ackBuf = ackQueue.poll(5, TimeUnit.SECONDS);
				
				if (wasInterrupted)
					Thread.currentThread().interrupt();
			} catch (IOException e) {
				throw new AcnetUnavailableException();	
			} catch (InterruptedException e) {
				throw new AcnetUnavailableException();	
			} finally {
				sendCommandLock.unlock();
			}

			if (ackBuf != null && ackBuf.remaining() >= 4) {
				short rAck = ackBuf.getShort();
				short status = ackBuf.getShort();

				if (status != ACNET_SUCCESS)
					throw new AcnetStatusException(status);

				if (rAck != ack)
					throw new RuntimeException("AcnetConnectionTCP: command/acknowledge mismatch");

				return ackBuf;
			} else
				throw new AcnetUnavailableException();
		} else
			throw new AcnetStatusException(ACNET_NOT_CONNECTED);
    }

	private final synchronized void openChannel()
	{
		try {
			channel = SocketChannel.open(address);

			channel.configureBlocking(true);
			channel.write(ByteBuffer.wrap("RAW\r\n\r\n".getBytes()));
			channel.setOption(StandardSocketOptions.SO_SNDBUF, 2048 * 1024);
			channel.setOption(StandardSocketOptions.SO_RCVBUF, 2048 * 1024);
			channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
		} catch (IOException e) {
			throw new RuntimeException("AcnetConnectionTCP: unable to open socket channel", e);
		}
	}

	@Override
	final synchronized void close()
	{
		try {
			channel.close();
		} catch (Exception e) { }

		channel = null;
	}
}
