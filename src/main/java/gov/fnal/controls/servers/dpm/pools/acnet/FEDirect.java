// $Id: FEDirect.java,v 1.4 2024/03/18 15:29:03 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import static java.lang.System.out;

import java.util.Date;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;


import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequestContext;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReply;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReplyHandler;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.Errors;
import gov.fnal.controls.servers.dpm.DPMRequest;
import gov.fnal.controls.servers.dpm.DataReplier;
import gov.fnal.controls.servers.dpm.DPMProtocolReplier;
import gov.fnal.controls.servers.dpm.pools.DeviceCache;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.scaling.DPMAnalogAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMDigitalAlarmScaling;
import gov.fnal.controls.servers.dpm.scaling.DPMBasicStatusScaling;
import gov.fnal.controls.servers.dpm.events.DataEvent;

public class FEDirect extends Thread implements AcnetReplyHandler, DPMProtocolReplier
{
	static final Logger logger;
	static final char[] hexDigits = { 
	  '0', '1', '2', '3', '4', '5', '6', '7',
	  '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' 
	};

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-8s %1$tF %1$tT %1$tL] %5$s %6$s%n");

		logger = Logger.getLogger(FEDirect.class.getName());
	}	

	final WhatDaq whatDaq;
	final DataReplier replier;
	final AcnetConnection connection = AcnetInterface.open("FEDIR");
	final boolean gets32;
	final byte[] dataBuf = new byte[64 * 1024];
	final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");

	volatile AcnetRequestContext requestContext;
	volatile int seqNo;
	volatile int replyCount = 0;

	public FEDirect(WhatDaq whatDaq, String protocol) throws AcnetStatusException
	{
		this.whatDaq = whatDaq;
		this.replier = DataReplier.get(whatDaq, this);
		this.gets32 = protocol.trim().equalsIgnoreCase("gets32");

		Runtime.getRuntime().addShutdownHook(this);
	}

	synchronized void printRepliesForever() throws InterruptedException
	{
		this.wait();
	}

	void print(final int indent, final String format, final Object... args)
	{
		if (indent > 0)
			out.printf("%" + indent + "s", "");

		out.printf(format, args);
	}

	void printBytes(final ByteBuffer buf, final int indent, final int BytesPerLine)
	{
		final byte[] tmp = new byte[buf.remaining()];
		
		buf.get(tmp);
		printBytes(tmp, indent, BytesPerLine);
	}

	void printBytes(final byte[] buf, final int indent, final int BytesPerLine)
	{
		final int dataLen = buf.length;

		for (int ii = 0; ii < dataLen; ii += BytesPerLine) {
			print(indent, "[");

			for (int jj = 0; jj < BytesPerLine; jj++) {
				final int di = ii + jj;

				if (di < dataLen) {
					out.print(hexDigits[(buf[di] >> 4) & 0x0f]);
					out.print(hexDigits[buf[di] & 0x0f]);
				} else
					out.print("  ");

				out.print(' ');
			}

			for (int jj = 0; jj < BytesPerLine; jj++) {
				final int di = ii + jj;
				if (di >= dataLen)
					out.print(' ');
				else if (buf[di] >= 32 && buf[di] <= 127)
					out.print((char) buf[di]);
				else
					out.print('.');
			}
			out.println("]");
		}
	}

	public int getInt(ByteBuffer buf)
	{
		return ((int) buf.get() & 0xff) |
				((int) (buf.get() & 0xff) << 8) |
				((int) (buf.get() & 0xff) << 16) |
				((int) (buf.get() & 0xff) << 24);
	}
	
	public long getLong(ByteBuffer buf)
	{
		return ((long) buf.get() & 0xff) |
				((long) (buf.get() & 0xff) << 8) |
				((long) (buf.get() & 0xff) << 16) |
				((long) (buf.get() & 0xff) << 24) |
				((long) (buf.get() & 0xff) << 32) |
				((long) (buf.get() & 0xff) << 40) |
				((long) (buf.get() & 0xff) << 48) |
				((long) (buf.get() & 0xff) << 56);
	}

	@Override
	public void handle(AcnetReply reply)
	{
		if (replyCount++ == 0) {
			out.println();

			try {
				logger.info("Replies from " + reply.serverTaskName() + "@" + reply.serverName());
			} catch (Exception ignore) { }
		}

		if (reply.status() == 0) {
			final ByteBuffer buf = reply.data();

			if (gets32) {
				final short globalStatus = buf.getShort();
				buf.getInt();
				seqNo = getInt(buf);

				if (globalStatus != 0)
					logger.info(String.format("global status : %04x %s", globalStatus & 0xffff, (dateString(System.currentTimeMillis()))));

				final long cycleTS = getLong(buf);
				final long collectionTS = getLong(buf);
				final long replyTS = getLong(buf);

				final short status = buf.getShort();
				buf.get(dataBuf, 0, whatDaq.getLength());

				try {
					if (status == 0)
						replier.sendReply(dataBuf, 0, collectionTS, cycleTS);
					else
						logger.info(String.format("status : %04x %s", status & 0xffff, (dateString(System.currentTimeMillis()))));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
			System.out.println("remaining: " + buf.remaining() + " len:" + whatDaq.length());
				final short status = buf.getShort();
				buf.get(dataBuf, 0, whatDaq.getLength());

				try {
					if (status == 0)
						replier.sendReply(dataBuf, 0, System.currentTimeMillis(), 0);
					else
						logger.info(String.format("status : %04x %s", reply.status() & 0xffff, (dateString(System.currentTimeMillis()))));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			logger.info(String.format("status : %04x %s", reply.status() & 0xffff, (dateString(System.currentTimeMillis()))));
		}
	}

	String dateString(long timestamp)
	{	
		return dateFormat.format(new Date(timestamp), new StringBuffer(), new FieldPosition(0)).toString();
	}

	@Override
	public void sendReply(WhatDaq whatDaq, byte[] data, long timestamp, long cycle) throws InterruptedException, IOException
	{
		logger.info(String.format("reading: %8d %s", seqNo, dateString(timestamp)));
		printBytes(data, 4, 8);
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double data, long timestamp, long cycle) throws IOException
	{
		logger.info(String.format("reading: %8d %10.2f %-8s %s cycle:%d", seqNo, data, whatDaq.getUnits(), dateString(timestamp), cycle));
	}

	@Override
	public void sendReply(WhatDaq whatDaq, double[] data, long timestamp, long cycle) throws InterruptedException, IOException
	{
		String dateStr = dateString(timestamp);

		for (int ii = 0; ii < data.length; ii++) {
			logger.info(String.format("reading: [%3d] %8d %10.2f %-8s %s cycle:%d", ii, seqNo, data[ii], whatDaq.getUnits(), dateStr, cycle));
			dateStr = "";
		}
	}

	@Override
	public void sendReply(WhatDaq whatDaq, String data, long timestamp, long cycle) throws InterruptedException, IOException
	{
		logger.info(String.format("reading: %8d %20s %-8s %s cycle:%d", seqNo, data, whatDaq.getUnits(), dateString(timestamp), cycle));
	}

	@Override
	public void sendReply(WhatDaq whatDaq, String[] data, long timestamp, long cycle) throws InterruptedException, IOException
	{
		String dateStr = dateString(timestamp);

		for (int ii = 0; ii < data.length; ii++) {
			logger.info(String.format("reading: [%3d] %8d %20s %-8s %s cycle:%d", ii, seqNo, data[ii], whatDaq.getUnits(), dateStr, cycle));
			dateStr = "";
		}
	}

	@Override
	public void sendReply(WhatDaq whatDaq, boolean data, long timestamp, long cycle) throws InterruptedException, IOException
	{
		logger.info(String.format("reading: %8d %10s %-8s %s cycle:%d", seqNo, data, whatDaq.getUnits(), dateString(timestamp), cycle));
	}

	@Override
	public void sendReply(WhatDaq whatDaq, DPMAnalogAlarmScaling data, long timestamp, long cycle) throws InterruptedException, IOException
	{
		logger.info(String.format("reading: %20s %s cycle:%d", data, dateString(timestamp), cycle));
	}

	@Override
	public void sendReply(WhatDaq whatDaq, DPMDigitalAlarmScaling data, long timestamp, long cycle) throws InterruptedException, IOException
	{
		logger.info(String.format("reading: %20s %s %d cycle:%d", data, dateString(timestamp), cycle));
	}

	@Override
	public void sendReply(WhatDaq whatDaq, DPMBasicStatusScaling data, long timestamp, long cycle) throws InterruptedException, IOException
	{
		logger.info(String.format("reading: %20s %s cycle:%d", data, dateString(timestamp), cycle));
	}

	FEDirect sendRequest() throws AcnetStatusException
	{
		final ByteBuffer buf = ByteBuffer.allocate(32 * 1024);
		final DataEvent event = whatDaq.getEvent();

		if (gets32) {
			logger.info("sending GETS32 request");

			buf.put((byte) 1);
			buf.put((byte) 1);
			buf.put((byte) 0);
			buf.put((byte) (whatDaq.node().value() >> 8));
			buf.put((byte) whatDaq.node().value());
			buf.put((byte) 0);
			buf.put((byte) 1);
			buf.put((byte) (event.isRepetitive() ? 1 : 0));

			buf.order(ByteOrder.LITTLE_ENDIAN);

			buf.putInt(34 + 2 + whatDaq.getLength());
			buf.putShort((short) 1);
			buf.putShort((short) whatDaq.getFTD());

			final byte[] eventBytes = event.toString().getBytes();

			if ((eventBytes.length & 1) > 0) {
				buf.putShort((short) (eventBytes.length + 1));
				buf.put(eventBytes);
				buf.put((byte) 0);
			} else {
				buf.putShort((short) eventBytes.length);
				buf.put(eventBytes);
			}

			buf.putInt(whatDaq.dipi());
			buf.put(whatDaq.getSSDN());
			buf.putInt(whatDaq.getLength());
			buf.putInt(whatDaq.getOffset());
			buf.flip();

			requestContext = connection.requestMultiple(whatDaq.node().value(), "GETS32", buf, 500000, this);
		} else {
			logger.info("sending RETDAT request");

			buf.order(ByteOrder.LITTLE_ENDIAN);

			buf.putShort((short) (2 + whatDaq.getLength()));
			buf.putShort((short) 1);
			buf.putShort((short) event.ftd());
			
			buf.putInt(whatDaq.dipi());
			buf.put(whatDaq.getSSDN());
			buf.putShort((short) whatDaq.getLength());
			buf.putShort((short) whatDaq.getOffset());
			buf.flip();

			requestContext = connection.requestMultiple(whatDaq.node().value(), "RETDAT", buf, 500000, this);
		}

		return this;
	}

	@Override
	public void run()
	{
		out.println();
		out.println("Shutdown: cancelling request - received " + replyCount + " replies");

		try {
			requestContext.cancel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		try {
			final DPMRequest request = new DPMRequest(args[1]);
			final ArrayList<DPMRequest> requestList = new ArrayList<>();

			requestList.add(request);

			DeviceCache.init();
			DeviceCache.add(requestList);
			final WhatDaq whatDaq = new WhatDaq(null, 0, request);

			(new FEDirect(whatDaq, args[0])).sendRequest().printRepliesForever();
		} catch (AcnetStatusException e) {
			System.out.println();
			System.out.println("AcnetStatusException: " + Errors.name(e.status));			
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println();
			System.out.println("Exception: " + e);			
			e.printStackTrace();
		}

		System.exit(0);
	}
}
