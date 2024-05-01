// $Id: FTPPool.java,v 1.12 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.TimerTask;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.TimeNow;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequestContext;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReply;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReplyHandler;
import gov.fnal.controls.servers.dpm.acnetlib.Node;

import gov.fnal.controls.servers.dpm.scaling.DPMReadSetScaling;
import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.PoolUser;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class FTPPool extends TimerTask implements AcnetReplyHandler, DaqPoolUserRequests<FTPRequest>, AcnetErrors, TimeNow
{
	private static AcnetConnection acnetConnection;
	private static List<FTPPool> pools = new LinkedList<>();

	private List<SharedFTPRequest> userRequestList = new LinkedList<>();
	private List<FastScaling> activeRequestList;

	private int numActive = 0;

	final static int FTP_RETURN_PERIOD = 3;

	Node node;

	private FTPScope ftpScope;

	private ClassCode ftpCode;
	private int ftpClassCode;
	private boolean newProtocol;
	AcnetRequestContext context;
	private boolean setupInProgress;
	private boolean reissueRequests;
	private boolean modified;
	private short typecode;
	private int r50Name;
	private short returnPeriod;
	private short startReturnTime;
	private short stopReturnTime;

	final static int PLOT_PRIORITY_SDA = 3;
	final static int PLOT_PRIORITY_MCR = 2;
	final static int PLOT_PRIORITY_REMOTE_MCR = 1;
	final static int PLOT_PRIORITY_USER = 0;
	private short currentTime;

	// The protocol is centered around the ACNET task name of the requester.
	// So, each pool must have a unique task name.
	static {
		acnetConnection = AcnetInterface.open("FTPPLT");
	}

	FTPPool(FTPRequest ftpRequest)
	{
		this.ftpScope = ftpRequest.getScope();
		this.ftpCode = ftpRequest.getClassCode();
		this.ftpClassCode = ftpRequest.getFTPClassCode();
		this.context = new AcnetRequestContext();

		this.node = ftpRequest.getDevice().node();
		this.modified = false;

		if (ftpClassCode >= 11) {
			this.newProtocol = true;
			this.typecode = 6;
		} else {
			this.newProtocol = false;
			this.typecode = 2;
		}
		this.returnPeriod = 0;
		this.startReturnTime = 0;
		this.stopReturnTime = 0;
		this.currentTime = 0;
		this.reissueRequests = false;
		AcnetPoolImpl.sharedTimer.schedule(this, 30000);
	}

	synchronized static FTPPool get(FTPRequest req)
	{
		for (FTPPool pool : pools) {
			if (pool.services(req))
				return pool;
		}

		final FTPPool pool = new FTPPool(req);

		pools.add(pool);

		return pool;
	}

	@Override
	public void insert(FTPRequest ftpRequest)
	{
		synchronized (userRequestList) {
			Iterator<SharedFTPRequest> requests = userRequestList.iterator();
			for (SharedFTPRequest shared = null; requests.hasNext();) {
				shared = requests.next();
				if (shared.getDevice().isEqual(ftpRequest.getDevice())) {
					synchronized (shared) {
						shared.addUser(ftpRequest);
					}
					return;
				}
			}
			SharedFTPRequest shared = new SharedFTPRequest(ftpRequest.getDevice(), ftpRequest.getClassCode(), ftpRequest.getScope());
			shared.addUser(ftpRequest);
			userRequestList.add(shared);
			modified = true;
		}
	}

	@Override
	public void cancel(PoolUser user, int error)
	{
		//boolean anyDeletes = false;

		// mark all user requests with this Scheduler for delete
		synchronized (userRequestList) {
			Iterator<SharedFTPRequest> requests = userRequestList.iterator();

			for (SharedFTPRequest shared = null; requests.hasNext();) {
				shared = requests.next();
				synchronized (shared) {

					for (FTPRequest req : shared.getUsers()) {
						if (req.getDevice().getUser() == user) {
									req.callback.plotData(System.currentTimeMillis(), error, 0, null, null, null);
							req.setDelete();
		//					anyDeletes = true;
						}
					}
				}
			}
		}
	}

	@Override
	public synchronized boolean process(boolean forceRetransmission)
	{
		int sendCount = 0;
		int byteCount = 0;

		boolean anyActiveUsers = false;
		reissueRequests = false;
			Iterator<SharedFTPRequest> requests = userRequestList.iterator();

			while (requests.hasNext()) {
				final SharedFTPRequest shared = requests.next();
				boolean anySharedUsers = false;

				synchronized (shared) {
					final Iterator<FTPRequest> users = shared.getUsers().iterator();

					while (users.hasNext()) {
						final FTPRequest user = users.next();
						if (user.getDelete()) {
							users.remove();
						} else {
							anySharedUsers = true;
						}
					}
				}

				if (!anySharedUsers) {
					//shared.stopCollection();
					requests.remove();
					modified = true;
				} else {

					byteCount += (shared.getDevice().getDefaultLength() == 4 ? 4 : 2);
					anyActiveUsers = true;
					sendCount++;
				}
			}

			if (!anyActiveUsers) {
				context.cancelNoEx();
				return false;
			}

		if (!modified && !forceRetransmission)
			return false;

		startFTPPool(sendCount, byteCount);

		return true;
	}

	private boolean isTimeout(AcnetReply r)
	{
		final int s = r.status();

		return s == ACNET_REQTMO || s == ACNET_REPLY_TIMEOUT;
	}

	private void startFTPPool(int numDevices, int numDataBytes)
	{
		final ByteBuffer buf = ByteBuffer.allocate(32 * 1024).order(ByteOrder.LITTLE_ENDIAN);

		if (!newProtocol)
			typecode |= 0;

		buf.putShort(typecode);
		buf.putInt(0);

		if (newProtocol)
			buf.putShort((short) numDevices);
		else
			buf.putShort((short) (numDevices | (currentTime << 8)));
		
		returnPeriod = FTP_RETURN_PERIOD; // number 15 hz ticks between
											// returns
		buf.putShort(returnPeriod);

		int msgSize = (int) ((numDataBytes + (numDevices << 1)) * ftpScope.rate);
																					
		msgSize += (6 * numDevices); // status, index, numPoints
		msgSize += (msgSize >> 1); // 50% extra
		msgSize /= (returnPeriod << 1); // to words

		if (msgSize > (DaqDefinitions.MaxAcnetMessageSize >> 1))
			msgSize = (DaqDefinitions.MaxAcnetMessageSize >> 1);

		buf.putShort((short) msgSize);
		int referenceWord;

		if (ftpScope.onGroupEventCode)
			referenceWord = ftpScope.groupEventCode | (0x8000);
		else
			referenceWord = (ftpScope.clockEventNumber) & 0xff;

		buf.putShort((short) referenceWord);
		buf.putShort(startReturnTime);
		buf.putShort(stopReturnTime);

		if (newProtocol) {
			buf.putShort((short) 0);
			buf.putShort(currentTime);
			buf.putShort((short) 0);
			buf.putShort((short) 0);
			buf.putShort((short) 0);
			buf.putShort((short) 0);
			buf.putShort((short) 0);
		}

		// pack the packets
		// look for shared in userRequestList, add to shared if existing
		SharedFTPRequest shared;

		context.cancelNoEx();

			activeRequestList = buildActiveList();

			for (FastScaling scaling : activeRequestList) {
				shared = scaling.shared;
				buf.putInt(shared.getDevice().dipi());

				if (newProtocol)
					buf.putInt(shared.getDevice().getOffset());

				buf.put(shared.getDevice().getSSDN());
				short samplePeriod;
				samplePeriod = (short) (100000 / shared.getScope().rate);
				buf.putShort(samplePeriod);

				if (newProtocol)
					buf.putInt(0);
			}
		try {
			context = acnetConnection.requestMultiple(node.value(), "FTPMAN", buf, 1000 * 10, this);
		} catch (AcnetStatusException e) {
			deliverError(e.status);
		}
	}

	private boolean services(FTPRequest ftp)
	{
		return ftp.getDevice().node().equals(node) && ftp.getFTPClassCode() == ftpClassCode &&
				ftp.getScope().equals(ftpScope);
	}

	@Override
	public void handle(AcnetReply r)	
	{
		final ByteBuffer hdr = r.data().order(ByteOrder.LITTLE_ENDIAN);
		final int replyLength = hdr.remaining();

		if (isTimeout(r)) {
			deliverError(ACNET_UTIME);
			context.cancelNoEx();
			reissueRequests = true;
		}

		int minReplyLength = 4 + (numActive << 1);

		int error = r.status();

		if (error < 0 || replyLength < minReplyLength) {
			if (error != 0)
				deliverError(error);

			reissueRequests = true;
			return;
		}

		error = hdr.getShort();

		if (error < 0) {
			deliverError(error);
			return;
		}

		final short replyType = hdr.getShort();

		if (replyType == 2)
			minReplyLength += (4 + (numActive << 2));

		if (replyLength < minReplyLength) {
			reissueRequests = true;
			return;
		}

		if (replyType == 2) {
			hdr.getInt(); // unused
		}
		synchronized (activeRequestList) {
			int activeRequestListSize = activeRequestList.size();
			for (FastScaling scaling : activeRequestList) {
				final SharedFTPRequest shared = scaling.shared;

				error = hdr.getShort();
				if (error != 0) {
					reissueRequests = true;
					shared.plotData(System.currentTimeMillis(), error, 0, null, null, null);

					if (replyType == 1) {
						continue;
					} else {
						hdr.getShort(); // ignore index
						hdr.getShort(); // ignore number points
						continue;
					}
				}

				if (replyType == 1)
					continue;

				final int index = hdr.getShort() & 0xffff; // byte offset to next packet
				final int numPoints = hdr.getShort() & 0xffff;

				final ByteBuffer data = hdr.duplicate().order(ByteOrder.LITTLE_ENDIAN);

				try {
					data.position(index);
				} catch (Exception e) {
					reissueRequests = true;
					return;
				}

				final long[] microSecs = new long[numPoints];
				final int[] nanoSecs = new int[numPoints];
				final double[] values = new double[numPoints];

				int nextData = 0;
				int nextTS = 0;
				int lastTS = 0;
				int firstTS = 0;
				int previousTS = 0;
				long nanos, previous = 0, baseTime02 = 0;

				for (int ii = 0; ii < numPoints; ii++) {
					previousTS = nextTS;
					nextTS = data.getShort() & 0xffff;
					while (nextTS > 50000) {
						nextTS -= 50000;
					}
					nanos = nextTS * 100000L; // get nanoseconds since 0x02

					if (ii == 0) {
						baseTime02 = TimeStamper.getBaseTime02(nextTS / 10);
						previous = nanos; // base time handles first one
						firstTS = nextTS;
					}
					if ((previous - nanos) > 2500000000L) {// wrapped around zero within the packet
						baseTime02 += (5000000000L); // timestamps wrap every
														// 5 seconds
						if (baseTime02 / 1000000 - System.currentTimeMillis() > 1000) {
							baseTime02 -= (5000000000L);
						}
					}
					previous = nanos;
					lastTS = nextTS;
					nanos += baseTime02;
					microSecs[ii] = (nanos / 1000);
					nanoSecs[ii] = (short) (nanos % 1000);

					nextData = scaling.bigRawData ? data.getInt() : data.getShort();

					try {
						values[ii] = scaling.scaling.rawToCommon(nextData);
					} catch (Exception e) {
						values[ii] = 0.0;
					}
				}
				if (numPoints == 0) {
				} else {
					shared.plotData(System.currentTimeMillis(), error, numPoints, microSecs, nanoSecs, values);
				}
			}
		}
	}

	private void deliverError(int error)
	{
		synchronized (userRequestList) {
			for (SharedFTPRequest shared : userRequestList) {
				shared.deliverError(error);
			}
		}

		reissueRequests = true;
	}

	@Override
	public String toString()
	{
		return "FTPPool: node: " + node + " " + ftpScope + " class code = " + ftpClassCode;
	}

	private class FastScaling
	{
		private SharedFTPRequest shared;
		private DPMReadSetScaling scaling;
		private boolean bigRawData;
		private int pointSize = 4;

		private FastScaling(SharedFTPRequest shared, boolean bigRawData)
		{
			this.shared = shared;
			this.scaling = DPMReadSetScaling.get(shared.getDevice());
			this.bigRawData = bigRawData;
			if (bigRawData)
				pointSize += 2;
		}
	}

	private synchronized List<FastScaling> buildActiveList()
	{
		List<FastScaling> list = new LinkedList<>();

		for (SharedFTPRequest shared : userRequestList) {
			final boolean bigData = shared.getDevice().getDefaultLength() == 4;
			list.add(new FastScaling(shared, bigData));
		}

		return list;
	}

	@Override
	public void run()
	{
		logger.log(Level.FINER, "FTPPool reissue requests " + reissueRequests);
		process(reissueRequests);
		AcnetPoolImpl.sharedTimer.schedule(this, 30000);
	}
}
