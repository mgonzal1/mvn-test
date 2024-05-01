// $Id: AcnetConnectionUDP.java,v 1.6 2024/04/01 15:30:49 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

class WeakAcnetConnection extends AcnetConnectionUDP
{
	WeakAcnetConnection()
	{
		super();
	}

	final synchronized void addNode(String name, int node, byte[] addr) throws AcnetStatusException
	{
		cmdBuf.clear();
		cmdBuf.put(addr);
		cmdBuf.putInt(0);
		cmdBuf.putShort((short) node);
		cmdBuf.putInt(Rad50.encode(name));
		sendCommand(10, 0, null);
	}

	final synchronized void addNode(Node node) throws AcnetStatusException
	{
		addNode(node.name, node.value, node.address.getAddress().getAddress());
	}

	final synchronized void lastNode() throws AcnetStatusException
	{
		addNode("", 0, new byte[] { 0, 0, 0, 0 });
	}
}

class AcnetConnectionUDP extends AcnetConnectionDaemon
{
	final static class DataThread extends Thread
	{
		final Selector sel;
		final ConcurrentLinkedQueue<AcnetConnectionUDP> regQ;

		DataThread()
		{
			try {
				sel = Selector.open();
				regQ = new ConcurrentLinkedQueue<AcnetConnectionUDP>();
				setName("ACNET data");
				start();
			} catch (Exception e) {
				throw new RuntimeException("unable to create ACNET data thread");
			}
		}

		@Override
		public void run()
		{
			AcnetInterface.logger.info("ACNET data thread start");

			try {
				while (true) {
					if (sel.select() > 0) {
						Iterator<SelectionKey> ii = sel.selectedKeys().iterator();

						while (ii.hasNext()) {
							SelectionKey key = ii.next();

							if (key.isReadable()) {
								final AcnetConnectionUDP c = (AcnetConnectionUDP) key.attachment();

								try {
									c.inHandler = true;
									//c.read(buf);
									c.read(BufferCache.alloc());
								} catch (AcnetStatusException e) {
									AcnetInterface.logger.log(Level.FINE, "ACNET handler exception ", e);
								} catch (IllegalStateException e) {
									AcnetInterface.logger.log(Level.SEVERE, "ACNET handler failure", e);
								} catch (Exception e) {
									AcnetInterface.logger.log(Level.WARNING, "AcnetConnection.UDP callback exception for connection '" + c.connectedName(), e);
								} finally {
									c.inHandler = false;
								}
							}

							ii.remove();
						}	
					}

					if (!regQ.isEmpty()) {
						AcnetConnectionUDP c;

						while ((c = regQ.poll()) != null) {
							c.dataChan.configureBlocking(false);
							c.dataChan.register(sel, SelectionKey.OP_READ, c);
						}
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("data thread exception", e);
			}
		}

		void register(AcnetConnectionUDP c)
		{
			regQ.add(c);
			sel.wakeup();
		}
	}

	private static DataThread dataThread = new DataThread();

    private DatagramChannel cmdChan = null, dataChan = null;
	private Selector cmdSel;
    private final ByteBuffer hdrBuf = ByteBuffer.allocateDirect(16);
	private final ByteBuffer nulBuf = ByteBuffer.allocate(0);
    private final ByteBuffer[] cmdBufs = { hdrBuf, cmdBuf, nulBuf };
    private final ByteBuffer ackBuf = ByteBuffer.allocateDirect(16);

	private boolean inHandler = false;

    AcnetConnectionUDP(String name, Node vNode)
	{
		super(name, vNode);

		openChannels();

		try {
			connect();
		} catch (AcnetStatusException ignore) { }

		dataThread.register(this);
		connectionMonitor.register(this);
    }

	AcnetConnectionUDP()
	{
		super("", new Node());

		openChannels();
	}

	@Override
	boolean inDataHandler()
	{
		return inHandler;
	}

	@Override
	final synchronized void connect() throws AcnetStatusException
	{
		connect8();
	}

	@Override
	final boolean disposed()
	{
		return cmdChan == null;
	}

    private final void read(Buffer buf) throws IOException, AcnetStatusException 
	{
		buf.receive(dataChan);
		AcnetPacket.create(buf).handle(this);
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

			cmdBufs[2] = dataBuf;

			ackBuf.clear();

			try {
				boolean wasInterrupted;

				sendCommandLock.lock();
				wasInterrupted = Thread.currentThread().interrupted();

				// Send the command

				cmdChan.write(cmdBufs);
				if (cmdSel.select(5000) > 0) {
					Iterator<SelectionKey> ii = cmdSel.selectedKeys().iterator();
					ii.next();
					ii.remove();
					cmdChan.receive(ackBuf);
				}
				
				if (wasInterrupted)
					Thread.currentThread().interrupt();
			} catch (IOException e) {
				AcnetInterface.logger.log(Level.FINE, "AcnetConnection.sendCommand exception: ", e);
				throw new AcnetUnavailableException();	
			} finally {
				sendCommandLock.unlock();
			}

			ackBuf.flip();

			if (ackBuf.remaining() >= 4) {
				short rAck = ackBuf.getShort();
				short status = ackBuf.getShort();

				if (status != ACNET_SUCCESS)
					throw new AcnetStatusException(status, "sending command " + cmd);

				if (rAck != ack)
					throw new RuntimeException("AcnetConnection: command/acknowledge mismatch");
			} else
				throw new AcnetUnavailableException();
		} else
			throw new AcnetStatusException(ACNET_NOT_CONNECTED);

		return ackBuf;
    }

	final synchronized void openChannels()
	{
		try {
			InetAddress loopback = InetAddress.getLoopbackAddress();

			cmdSel = Selector.open();
			cmdChan = DatagramChannel.open();
			cmdChan.connect(new InetSocketAddress(loopback, 6802));
			cmdChan.configureBlocking(false);
			cmdChan.setOption(StandardSocketOptions.SO_SNDBUF, 256 * 1024);
			cmdChan.register(cmdSel, SelectionKey.OP_READ);

			dataChan = DatagramChannel.open();
			dataChan.connect(new InetSocketAddress(loopback, 6802));
			dataChan.configureBlocking(false);
			dataChan.setOption(StandardSocketOptions.SO_RCVBUF, 2048 * 1024);
		} catch (IOException e) {
			AcnetInterface.logger.log(Level.SEVERE, "Unable to open ACNET sockets", e);
			throw new IllegalStateException("Unable to open ACNET sockets", e);
		}
	}

	@Override
	short localPort()
	{
		try {
			return (short) (((InetSocketAddress) dataChan.getLocalAddress()).getPort() & 0xffff);
		} catch (Exception e) {
			AcnetInterface.logger.log(Level.SEVERE, "unable to get local port", e);
		}

		return 0;
	}

	@Override
	final synchronized void close()
	{
		try {
			dataChan.close();
		} catch (Exception e) { }
		dataChan = null;

		try {
			cmdChan.close();
		} catch (Exception e) { }
		cmdChan = null;
	}
}
