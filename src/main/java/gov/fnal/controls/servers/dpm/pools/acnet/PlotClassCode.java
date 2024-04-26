// $Id: PlotClassCode.java,v 1.10 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReply;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReplyHandler;
import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.acnetlib.NodeFlags;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;


class PlotClassCode implements NodeFlags, AcnetErrors 
{
	private class Signaler {
		int signalCount = 0;
		boolean waiting = false;

		private long _wait(long timeout) 
		{
			long t = System.currentTimeMillis();

			waiting = true;

			try {
				wait(timeout);
			} catch (InterruptedException e) {}

			waiting = false;

			return System.currentTimeMillis() - t;
		}

		private void waitCheck()
		{
			if (waiting) {
				System.err.println("Signaler: Multiple waits on signal");
				Thread.dumpStack();
			}
		}

		/**
		 * Sends a signal to the object waiting for a signal.
		 */
		public synchronized void signal()
		{
			signalCount++;
			notify();
		}

		/**
		 * Waits for a signal. If there are signals pending, this method will return
		 * immediately.
		 */
		public synchronized void waitForSignal() 
		{
			waitCheck();

			while (signalCount == 0)
				_wait(0);

			signalCount--;
		}

		/**
		 * Waits for a signal with timeout. If there are signals pending, this
		 * method will return immediately.
		 * 
		 * @param timeout
		 *        in milliseconds
		 *
		 * @return boolean
		 *        true if signal was received with in the specified  timeout
		 */
		public synchronized boolean waitForSignal(long timeout) 
		{
			waitCheck();

			while (signalCount == 0 && timeout > 0)
				timeout -= _wait(timeout);

			if (signalCount > 0) {
				signalCount--;
				return true;
			} else
				return false;
		}

	}

	private static final AcnetConnection acnetConnection = AcnetInterface.open("SNAPLT");

	/**
	 * @serial lists of front ends then of WhatDaqs
	 */
	private List<List<WhatDaq>> sortByFE;

	/**
	 * @serial all the requests across all the front ends
	 */
	private List<WhatDaq> requests;

	/**
	 * @serial the plot class code replies ordered by request
	 */
	private ClassCode[] codes;

	/**
	 * Constructs a PlotClassCode to receive requests.
	 * 
	 */
	private PlotClassCode()
	{
		sortByFE = new LinkedList<List<WhatDaq>>();
		requests = new LinkedList<WhatDaq>();
		codes = null;
	}

	/**
	 * Clears the list resources of the ClassCode object.
	 * 
	 * @param andCodes
	 *            also clears the array of returns.
	 * 
	 */
	private void clearLists()
	{
		sortByFE = null;
		requests = null;
		codes = null;
	}

	private static AcnetConnection getSnapShotManagerConnection()
	{
		return acnetConnection;
	}

	/**
	 * Adds a request for ClassCode lookup.
	 * 
	 * @param device
	 *            the device to lookup.
	 * 
	 */
	private synchronized void addClassCodeRequest(WhatDaq whatDaq)
	{
		requests.add(whatDaq);
		Iterator<List<WhatDaq>> allFE = sortByFE.iterator();
		for (List<WhatDaq> oneFE = null; allFE.hasNext();) {
			oneFE = allFE.next();
			if (oneFE.size() >= 200)
				continue;
			WhatDaq wFE = oneFE.get(0); // always has to be at least
			// one
			if (wFE.node() == whatDaq.node()) {// && wFE.getNode() == device.getNode()) {
				oneFE.add(whatDaq);
				return;
			}
		}
		// add this whatDaq to a new front end and add the front end
		List<WhatDaq> anotherFE = new LinkedList<WhatDaq>();
		anotherFE.add(whatDaq);
		sortByFE.add(anotherFE);
	}

	private void processClassCodeRequest()
	{
		synchronized (sortByFE) {
			codes = new ClassCode[requests.size()];
			int numFE = sortByFE.size();
			LookupClassCode[] fes = new LookupClassCode[numFE];
			// start lookup
			for (int ii = 0; ii < numFE; ii++) {
				fes[ii] = new LookupClassCode(sortByFE.get(ii));
				if (ii > 3) {
					try {
						Thread.sleep(50L);
					} catch (Exception e) { }
				}
			}
			// wait for lookup to finish
			for (int ii = 0; ii < numFE; ii++) {
				fes[ii].classCodeSignal.waitForSignal(); // wait until reply
				// or timeout
			}
			List<WhatDaq> oneFE;
			for (int ii = 0; ii < numFE; ii++) {
				oneFE = fes[ii].devices;
				int numDevices = oneFE.size();
				for (int jj = 0; jj < numDevices; jj++) {
					WhatDaq device = oneFE.get(jj);
					int index = requests.indexOf(device);
					codes[index] = fes[ii].replies[jj];
				}
			}
		}
	}

	private ClassCode getClassCode(WhatDaq device)
	{
		int index = requests.indexOf(device);
		if (index == -1) {
			return null;
		}
		return codes[index];
	}

	static List<ClassCode> getPlotClassCode(List<WhatDaq> whatDaqs)
	{
		List<ClassCode> codes = new LinkedList<ClassCode>();
		if (whatDaqs == null || whatDaqs.isEmpty())
			return codes;
		int numRetries = 0;
		PlotClassCode plotClassCode = new PlotClassCode();
		Iterator<WhatDaq> requests = whatDaqs.iterator();
		for (WhatDaq next = null; requests.hasNext();) {
			next = (WhatDaq) requests.next();
			plotClassCode.addClassCodeRequest(next);
		}
		plotClassCode.processClassCodeRequest();

		ClassCode classCode;
		requests = whatDaqs.iterator();
		List<WhatDaq> retryList = new LinkedList<>();
		for (WhatDaq next = null; requests.hasNext();) {
			next = (WhatDaq) requests.next();
			classCode = plotClassCode.getClassCode(next);
			if (classCode.error() != 0 && whatDaqs.size() > 4) {
				int errorCode = classCode.error();
				if (errorCode == FTP_INVREQ
						|| errorCode == FTP_INVNUMDEV
						|| errorCode == ACNET_IVM
						|| errorCode == ACNET_UTIME) {
					++numRetries;
					System.out.println("PlotClassCode, retrying "
							+ whatDaqs.size() + " including " + next);
					codes.clear();
					retryList.clear();
					requests = whatDaqs.iterator();
					while (requests.hasNext()) {
						retryList.add(requests.next());
						if (retryList.size() == 4) {
							codes.addAll(getPlotClassCode(retryList));
							retryList.clear();
						}
					}
					if (retryList.size() > 0)
						codes.addAll(getPlotClassCode(retryList));
					plotClassCode.clearLists();
					plotClassCode = null;
					return codes;
				}
			}
			codes.add(classCode);
		}
		plotClassCode.clearLists();
		plotClassCode = null;

		return codes;
	}

	private class LookupClassCode implements AcnetReplyHandler
	{
		private List<WhatDaq> devices;

		/**
		 * the class codes
		 */
		private ClassCode[] replies;

		/**
		 * reply to class code request
		 */
		//private AcnetMessage classCodeReply = null;

		/**
		 * error status
		 */
		private int classCodeErrorStatus = 0;

		/**
		 * object used for waiting
		 */
		private Signaler classCodeSignal;

		/**
		 * ACNET request
		 */
		//private DaemonAcnetSendRequest classCodeRequest = null;

		LookupClassCode(List<WhatDaq> byFE)
		{
			devices = byFE;
			replies = new ClassCode[devices.size()];
			//int byfeCount = byFE.size();
			int msgSize = 4 + byFE.size() * (12);

			//AcnetMessage classCodeMsg = new AcnetMessage(msgSize);
			final ByteBuffer buf = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);

			//classCodeMsg.putNextShort((short) 1);
			//classCodeMsg.putNextShort((short) byFE.size());
			buf.putShort((short) 1);
			buf.putShort((short) byFE.size());

			WhatDaq whatDaq = null;
			for (int ii = 0; ii < byFE.size(); ii++) {
				whatDaq = devices.get(ii);
				//classCodeMsg.putNextInt(whatDaq.dipi());
				//classCodeMsg.put(whatDaq.ssdn());
				buf.putInt(whatDaq.dipi());
				buf.put(whatDaq.ssdn());
			}
			classCodeSignal = new Signaler();

				final Node node = whatDaq.node();
				classCodeErrorStatus = 0;

				if (node.is(OUT_OF_SERVICE) || node.is(OBSOLETE))
					classCodeErrorStatus = ACNET_NODE_DOWN;
				else {
					for (int ii = 0; ii < devices.size(); ii++) {
						replies[ii] = new ClassCode(ClassCode.DAE_FTP_CLASS_CODE, ClassCode.DAE_SNAP_CLASS_CODE, 0);
					}
					try {
						classCodeSignal.signal();
					} catch (Exception e) {
					}
					return;
				}

			buf.flip();

			if (classCodeErrorStatus == 0) {
				try {
					acnetConnection.requestSingle(whatDaq.node().value(), "FTPMAN", buf, 5000, this);
				} catch (AcnetStatusException e) {
					classCodeErrorStatus = e.status;
				}
				/*
				classCodeRequest = connection.acnetSendRequest(whatDaq.node().value(), "FTPMAN", classCodeMsg.message(),
																msgSize, 0, false, this, true, false, 5000);
				if (!classCodeRequest.transmit()) {
					classCodeErrorStatus = classCodeRequest.getSendRequestStatus();
					classCodeRequest = null;
				}
				*/
			}
		}

		public void handle(AcnetReply r)
		//{
		//}

		//public boolean replyReceive(DaemonAcnetHeader acnetHeader, byte[] acnetReply, int replyLength, long replyTime)
		{
			final ByteBuffer buf = r.data().order(ByteOrder.LITTLE_ENDIAN);

			//if ((classCodeErrorStatus = acnetHeader.getStatus()) != 0) {
			if ((classCodeErrorStatus = r.status()) != 0) {

			} else {
				//if (replyLength < ((devices.size() * 6) + 2)) {
				if (buf.remaining() < ((devices.size() * 6) + 2)) {
					classCodeErrorStatus = DAE_FTP_REPLY_LENGTH;
				} else {
					//classCodeReply = new AcnetMessage(acnetReply, replyLength);
					classCodeErrorStatus = buf.getShort();
				}
			}

			for (int ii = 0; ii < devices.size(); ii++) {
				short ftp, snap;
				int err;
				if (classCodeErrorStatus != 0) {
					err = classCodeErrorStatus;
					ftp = 0;
					snap = 0;
				} else {
					//err = classCodeReply.getNextShort();
					//ftp = classCodeReply.getNextShort();
					//snap = classCodeReply.getNextShort();
					err = buf.getShort();
					ftp = buf.getShort();
					snap = buf.getShort();
				}
				replies[ii] = new ClassCode(ftp, snap, err);
			}
			try {
				classCodeSignal.signal();
			} catch (Exception e) {
				System.out.println("signal error: " + e);
			}
			//classCodeRequest = null;
			//return true;
		}

		//public boolean replyTimeout(int consecutiveTimeouts, long msTimeout, DaemonAcnetSendRequest request)
		//{
		//	classCodeErrorStatus = ACNET_UTIME;
		//	for (int ii = 0; ii < devices.size(); ii++) {
		//		replies[ii] = new ClassCode(0, 0, classCodeErrorStatus);
		//	}
		//	try {
		//		classCodeSignal.signal(); // notify main thread
		//	} catch (Exception e) {
		//		System.out.println("signal error: " + e);
		//	}
			//classCodeRequest = null;
		//	return true; // do cancel
		//}
	}
}
