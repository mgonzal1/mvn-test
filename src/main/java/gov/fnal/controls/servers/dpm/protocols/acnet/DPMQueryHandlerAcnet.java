// $Id: DPMQueryHandlerAcnet.java,v 1.11 2023/11/02 16:36:15 kingc Exp $
package gov.fnal.controls.servers.dpm.protocols.acnet;

import java.util.Collection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequest;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import gov.fnal.controls.servers.dpm.DPMServer;
import gov.fnal.controls.servers.dpm.ListId;
import gov.fnal.controls.servers.dpm.DPMList;
import gov.fnal.controls.servers.dpm.JobInfo;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.protocols.Protocol;
import gov.fnal.controls.servers.dpm.protocols.HandlerType;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class DPMQueryHandlerAcnet extends DPMProtocolHandlerAcnet implements AcnetErrors
{
	private final ByteBuffer replyBuf;

	public DPMQueryHandlerAcnet(String name) throws AcnetStatusException
	{
		super(name);

		this.replyBuf = ByteBuffer.allocate(48 * 1024);
		handleRequests(this);
	}

	private short client(DPMList list)
	{
		if (list instanceof DPMListAcnet)
			return (short) ((DPMListAcnet) list).client();
		
		return 0;
	}

	private short server(DPMList list)
	{
		if (list instanceof DPMListAcnet)
			return (short) ((DPMListAcnet) list).server();
		
		return 0;
	}

	@Override
	public Protocol protocol()
	{
		return Protocol.RAW;
	}

	@Override
	public HandlerType type()
	{
		return HandlerType.Service;
	}

	@Override
	public void handle(AcnetRequest r)
	{
		try {
			ByteBuffer buf = r.data();

			buf.order(ByteOrder.LITTLE_ENDIAN);

			final short typeCode = buf.getShort();

			replyBuf.clear();
			replyBuf.order(ByteOrder.LITTLE_ENDIAN);

			switch (typeCode) {
				case 8:
					{
						final int dipi = buf.getInt();

						replyBuf.putShort((short) 0);
						replyBuf.putShort(typeCode);
						replyBuf.putInt(dipi);
						replyBuf.putInt(0);

						int count = 0;

						for (DPMList list : DPMServer.activeLists()) {
							for (JobInfo jInfo : list.jobs()) {
								final long lc = jInfo.devicePropertyCount(dipi);

								if (lc > 0) {
									replyBuf.putShort(client(list));
									replyBuf.putShort((short) lc);

									try {
										replyBuf.putShort(Short.parseShort(list.property("SLOTINDEX")));
									} catch (Exception e) {
										replyBuf.putShort((short) 0);
									}

									replyBuf.putShort(server(list));

									try {
										replyBuf.put(String.format("%6s", list.property("TASK")).getBytes(), 0, 6);
									} catch (Exception e) {
										replyBuf.put("DPMD  ".getBytes());
										logger.log(Level.WARNING, "exception building reply", e);
									}

									replyBuf.put((byte) 0);
									replyBuf.put((byte) 0);
									replyBuf.putInt(list.id().value());

									count++;
								}
							}
						}

						replyBuf.putInt(8, count).flip();
						r.sendReply(replyBuf, ACNET_OK); 
					} 
					break;

				case 9:
					{
						final int node = buf.getShort() & 0xffff;

						replyBuf.putShort((short) 0);
						replyBuf.putShort(typeCode);
						replyBuf.putShort((short) node);
						replyBuf.putInt(0);

						int count = 0;

						for (DPMList list : DPMServer.activeLists()) {
							for (JobInfo jInfo : list.jobs()) {
								long lc = jInfo.nodeCount(node);

								if (lc > 0) {
									replyBuf.putShort(client(list));
									replyBuf.putShort((short) lc);

									try {
										replyBuf.putShort(Short.parseShort(list.property("SLOTINDEX")));
									} catch (Exception e) {
										replyBuf.putShort((short) 0);
									}

									replyBuf.putShort(server(list));

									try {
										replyBuf.put(String.format("%6s", list.property("TASK")).getBytes(), 0, 6);
									} catch (Exception e) {
										replyBuf.put("DPMD  ".getBytes());
										logger.log(Level.WARNING, "exception building reply", e);
									}

									replyBuf.put((byte) 0);
									replyBuf.put((byte) 0);
									replyBuf.putInt(list.id().value());

									count++;
								}
							}
						}

						replyBuf.putInt(6, count).flip();
						r.sendReply(replyBuf, ACNET_OK); 
					}
					break;
			
				case 11:
					{
						final int listId = buf.getInt();

						replyBuf.putShort((short) 0);
						replyBuf.putShort(typeCode);
						replyBuf.putInt(listId);
						replyBuf.putInt(0);

						int count = 0;

						try {
							for (JobInfo jInfo : DPMServer.list(listId).jobs()) {
								Collection<WhatDaq> c = jInfo.whatDaqs();

								for (WhatDaq whatDaq : c) {
									replyBuf.putShort((short) whatDaq.getReceiveDataId());
									replyBuf.putInt(whatDaq.dipi());
									count++;
								}
							}
						} catch (AcnetStatusException ignore) { }

						replyBuf.putInt(8, count).flip();
						r.sendReply(replyBuf, ACNET_OK);
					}
					break;

				case 12:
					{
						final short id = buf.getShort();
						final int listId = buf.getInt();


						try {
							for (JobInfo jInfo : DPMServer.list(listId).jobs()) {
								WhatDaq whatDaq = jInfo.whatDaq(id);

								if (whatDaq != null) {
									replyBuf.putShort((short) ACNET_OK);
									replyBuf.putShort(typeCode);
									replyBuf.putShort(id);
									replyBuf.putInt(listId);
									replyBuf.putInt(whatDaq.dipi());
									replyBuf.put(whatDaq.getSSDN());
									replyBuf.putShort((short) whatDaq.getFTD());
									replyBuf.putShort((short) whatDaq.getLength());
									replyBuf.putShort((short) whatDaq.getOffset());
									replyBuf.putShort((short) whatDaq.node().value());
									replyBuf.putShort((short) whatDaq.getError());
									replyBuf.put((byte) 0);
									replyBuf.put((byte) 0);
									replyBuf.put((byte) 0);
									replyBuf.put((byte) 0);
								} else {
									replyBuf.putShort((short) DPM_INVRINX);
								}
							}
						} catch (AcnetStatusException e) {
							replyBuf.putShort((short) e.status);
						}

						replyBuf.flip();
						r.sendReply(replyBuf, ACNET_OK);
					}
					break;

				default:
					DPMServer.logger.warning("DPM unhandled typecode: " + typeCode);
					r.ignore();
					break;
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception handling dpm query request", e);
			try {
				r.sendStatus(ACNET_SYS);
			} catch (Exception ignore) { }
		}
	}
}

