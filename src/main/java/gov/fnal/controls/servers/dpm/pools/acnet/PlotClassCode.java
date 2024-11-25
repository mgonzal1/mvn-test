// $Id: PlotClassCode.java,v 1.13 2024/11/19 22:34:44 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReply;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReplyHandler;
import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.acnetlib.NodeFlags;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.DeviceCache;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class PlotClassCode implements NodeFlags, AcnetReplyHandler, AcnetErrors
{
	static final AcnetConnection acnetConnection = AcnetInterface.open("PLTCLS");

	final ByteBuffer buf;
	ClassCode classCode;

	static ClassCode get(WhatDaq whatDaq)
	{
		return (new PlotClassCode())._get(whatDaq);
	}

	private PlotClassCode()
	{
		this.buf = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
	}

	private synchronized ClassCode _get(WhatDaq whatDaq)
	{
		final Node node = whatDaq.node();

		if (node.is(OUT_OF_SERVICE) || node.is(OBSOLETE))
			return new ClassCode(ACNET_NODE_DOWN);
		
		buf.putShort((short) 1);
		buf.putShort((short) 1);
		buf.putInt(whatDaq.dipi());
		buf.put(whatDaq.ssdn());
		buf.flip();

		try {
			acnetConnection.requestSingle(whatDaq.node().value(), "FTPMAN", buf, 5000, this);

			try {
				wait();
			} catch (InterruptedException ignore) {
				return new ClassCode(ACNET_UTIME);
			}

			return classCode;
		} catch (AcnetStatusException e) {
			return new ClassCode(e.status);
		}
	}

	@Override
	synchronized public void handle(AcnetReply r)
	{
		if (r.status() != 0)
			classCode = new ClassCode(r.status());
		else {
			final ByteBuffer buf = r.data().order(ByteOrder.LITTLE_ENDIAN);

			if (buf.remaining() >= 8) {
				final short status = buf.getShort();

				if (status != 0)
					classCode = new ClassCode(status);
				else {
					final short error = buf.getShort();
					final short ftp = buf.getShort();
					final short snap = buf.getShort();

					classCode = new ClassCode(ftp, snap, error);
				}
			} else
				classCode = new ClassCode(ACNET_IVM);
		}

		notify();
	}

	public static void main(String[] args) throws Exception
	{
		DeviceCache.init();
		DeviceCache.add(args[0]);

		logger.log(Level.INFO, get(WhatDaq.create(args[0])).toString());

		System.exit(0);
	}
}
