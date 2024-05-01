// $Id: DPMListTCP.java,v 1.10 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm.protocols.tcp;

import java.util.Set;
import java.util.Iterator;
import java.util.logging.Level;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.net.InetAddress;
import java.net.StandardSocketOptions;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.DPMList;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

abstract public class DPMListTCP extends DPMList implements Runnable
{
	final Thread thread;
	final RemoteClientTCP client;
	final SocketChannel channel;

	final Selector selector;
	final ByteBuffer ibuf = ByteBuffer.allocate(128 * 1024);
	final ByteBuffer obufs[] = { ByteBuffer.allocate(4), ByteBuffer.allocate(128 * 1024) };
	final SelectionKey selectKey;

	protected abstract void handleRequest(final ByteBuffer buf) throws IOException;
	protected abstract boolean getReply(final ByteBuffer buf) throws IOException;

	public DPMListTCP(DPMProtocolHandler owner, RemoteClientTCP client) throws IOException, AcnetStatusException
	{
		super(owner);

		this.client = client;
		this.channel = client.channel();

		this.selector = Selector.open();
		this.channel.configureBlocking(false);
		this.channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
		this.channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
		this.selectKey = this.channel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

		this.ibuf.clear().limit(4);
		this.obufs[0].clear().limit(0);
		this.obufs[1].clear().limit(0);

		this.thread = new Thread(this);
		this.thread.setName("DPMListTCP(" + client.hostName() + ")"); 
		this.thread.start();

		try {
			if (client.hasHeader("upgrade"))
				this.addToProperties("TYPE", client.getHeader("upgrade"));
			this.addToProperties("PROTO", client.protocol().toString());
		} catch (Exception ignore) { }
	}

	@Override
	public boolean allowPrivilegedSettings()
	{
		return false;
	}

	@Override
	public String toString()
	{
		return "DPMListTCP-" + id();
	}

	public InetAddress fromHostAddress()
	{
		return client.hostAddress();
	}

	@Override
	public String clientHostName()
	{
		return client.hostName();
	}

	@Override
	public boolean dispose(int status)
	{
		if (super.dispose(status)) {
			try {
				channel.close();
			} catch (Exception ignore) { }

			selector.wakeup();
			//owner.listDisposed(this);

			return true;
		}

		return false;
	}

	public void wakeup()
	{
		selectKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	   	selector.wakeup();
	}

	@Override
	public void run()
	{
		int readLen = -1;

		try {
			while (!disposed()) {
				final int count = selector.select();

				if (count > 0) {
					final Set<SelectionKey> keys = selector.selectedKeys();
					final Iterator<SelectionKey> iter = keys.iterator();

					while (iter.hasNext()) {
						final SelectionKey key = iter.next();

						if (key.isReadable()) {
							if (readLen == -1) {
								if (channel.read(ibuf) != -1) {
									if (ibuf.remaining() == 0) {
										ibuf.flip();
										readLen = ibuf.getInt();
										ibuf.clear().limit(readLen);
									}
								} else
									dispose(ACNET_CANCELLED);
							}

							if (readLen != -1) {
								if (channel.read(ibuf) != -1) {
									if (ibuf.remaining() == 0) {
										ibuf.flip();
										readLen = -1;

										handleRequest(ibuf);
										ibuf.clear().limit(4);
									}
								} else
									dispose(ACNET_CANCELLED);
							}
						} else if (key.isWritable()) {
							while (true) {
								if (obufs[1].remaining() == 0) {
									obufs[1].clear();
									if (getReply(obufs[1])) {
										obufs[1].flip();
										obufs[0].clear();
										obufs[0].putInt(obufs[1].remaining());
										obufs[0].flip();
									} else {
										obufs[1].flip();
										key.interestOps(SelectionKey.OP_READ);
										break;
									}
								}

								channel.write(obufs);

								if (obufs[1].remaining() > 0)
									break;
							}
						}

						iter.remove();
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception in select thread", e);
			dispose(ACNET_CANCELLED);
		}

		logger.fine(String.format("%s DPMListTCP thread exit - %s", id(), channel));
	}
}

