// $Id: DPMProtocolHandlerTCP.java,v 1.7 2021/12/30 20:17:46 kingc Exp $

package gov.fnal.controls.servers.dpm.protocols.tcp;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.fnal.controls.servers.dpm.protocols.Protocol;
import gov.fnal.controls.servers.dpm.protocols.HandlerType;
import gov.fnal.controls.servers.dpm.protocols.DPMProtocolHandler;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

import gov.fnal.controls.servers.dpm.protocols.tcp.pc.DPMListPC;
import gov.fnal.controls.servers.dpm.protocols.tcp.json.DPMListJSON;

public class DPMProtocolHandlerTCP extends DPMProtocolHandler implements Runnable
{
	private final ServerSocketChannel serverChannel;
	private final Thread thread; 
	private final LinkedBlockingQueue<Runnable> taskQ;
	private final ThreadPoolExecutor threadPool;
	
	public DPMProtocolHandlerTCP(int port) throws IOException
	{
		this.serverChannel = ServerSocketChannel.open();
		this.serverChannel.bind(new InetSocketAddress(port));

		this.taskQ = new LinkedBlockingQueue<>(16);
		this.threadPool = new ThreadPoolExecutor(5, 10, 30, TimeUnit.SECONDS, this.taskQ);

		this.thread = new Thread(this);
		this.thread.setName("DPMProtocolHandlerTCP");
		this.thread.start();
	}

	@Override
	public void logStart()
	{
		logger.info("DPM TCP service now available on port " + serverChannel.socket().getLocalPort());
	}

    @Override
    public Protocol protocol()
    {                                                                                                                                                                      
        return Protocol.PC;
    }

    @Override
    public HandlerType type()
    {
        return HandlerType.Service;
    }

	@Override
	public void run()
	{
		while (true) {
			try {
				final SocketChannel channel = serverChannel.accept();

				logger.fine("TCP connection from " + channel);

				Runnable task = () -> {
					try {
						final RemoteClientTCP client = new RemoteClientTCP(channel);
						final Protocol protocol = client.protocol();
						
						switch (protocol) {
						 case PC:
							new DPMListPC(DPMProtocolHandlerTCP.this, client);
						 	break;

						 case JSON:
							new DPMListJSON(DPMProtocolHandlerTCP.this, client);
						 	break;
						}
					} catch (Exception e) {
						try {
							channel.close();
						} catch (Exception ignore) { }
					}
				};
				
				try {
					threadPool.execute(task);
				} catch (RejectedExecutionException e) {
					channel.close();
				}
			} catch (IOException e) {
				logger.log(Level.FINE, "exception in TCP handler", e); 
			}
		}
	}
}

