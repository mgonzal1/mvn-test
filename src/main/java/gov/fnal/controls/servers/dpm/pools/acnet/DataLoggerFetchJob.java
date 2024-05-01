// $Id: DataLoggerFetchJob.java,v 1.12 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.TimerTask;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetRequestContext;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReply;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetReplyHandler;

import gov.fnal.controls.servers.dpm.TimeNow;
import gov.fnal.controls.servers.dpm.pools.PoolUser;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;
import gov.fnal.controls.servers.dpm.pools.LoggerRequest;
import gov.fnal.controls.servers.dpm.pools.LoggerEvent;

public class DataLoggerFetchJob extends TimerTask implements AcnetReplyHandler, AcnetErrors, TimeNow
{
	private static final AcnetConnection acnetConnection = AcnetInterface.open("LOGGET").setQueueReplies();

	final static short RETURN_TYPECODE = 17;
	final static int FETCH_POINT_COUNT = 487;
	final static int NAME_LENGTH = 32;
	final static int EVENT_LENGTH = 252;
	final static int FETCH_MSG_LENGTH = 18 + EVENT_LENGTH + 28 + NAME_LENGTH;

	final Node node;
	final PoolUser user;
	final LoggerEvent event;
	final ReceiveData callback;
	final String loggedName;
	final ByteBuffer buf;

	private AcnetRequestContext requestContext;
	private volatile boolean cancelled;
	private int consecutiveErrorCount;

	private int t1Seed;
	private final int t2Seed;

	public DataLoggerFetchJob(PoolUser user, String loggerName, LoggerRequest request, LoggerEvent event) throws AcnetStatusException
	{
		this.user = user;
		this.node = LoggerConfigCache.loggerNode(loggerName);
		this.callback = request.callback;
		this.event = event;
		//this.t1Seed = (int) (event.getStartTime() / 1000);
		//this.t2Seed = (int) ((event.getStartTime() % 1000) * 1000000);
		this.t1Seed = (int) (event.t1 / 1000);
		this.t2Seed = (int) ((event.t1 % 1000) * 1000000);
		this.loggedName = request.loggedName;
		this.requestContext = new AcnetRequestContext();
		this.buf = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);
		this.cancelled = false;
		this.consecutiveErrorCount = 0;
	}

	public DataLoggerFetchJob start() throws AcnetStatusException
	{
		if (!cancelled)
			fetch();

		return this;
	}

	public void stop()
	{
		cancel(0);
	}

	private void putTime(final long t, ByteBuffer buf)
	{
		//long t = d.getTime();

		// Seconds

		buf.putInt((int) (t / 1000));

		// Nanoseconds

		buf.putInt((int) ((t % 1000) * 1000000));
	}

	private void fill(byte b, int count, ByteBuffer buf)
	{
		for (int ii = 0; ii < count; ii++)
			buf.put(b);
	}

	private void putString(final String s, final int len, ByteBuffer buf) throws AcnetStatusException
	{
		if (s == null)
			fill((byte) 0, len, buf);
		else {
			try {
				final byte[] sb = s.getBytes("ASCII");

				if (sb.length > len)
					throw new AcnetStatusException(LJ_INVLEN);

				buf.put(sb, 0, sb.length);
				fill((byte) ' ', len - sb.length, buf);
			} catch (Exception e) {
				throw new AcnetStatusException(LJ_INVARG);
			}
		}
	}

	private void fetch() throws AcnetStatusException
	{
		buf.clear();
		buf.putShort(RETURN_TYPECODE);
		buf.putInt(FETCH_POINT_COUNT);

		//buf.putInt(event.getDeltaSeconds());
		//buf.putInt(event.getSkipCount());
		//buf.putInt(event.getSkipOffset());
		buf.putInt(0);
		buf.putInt(0);
		buf.putInt(0);

		//putString(event.getEventString(), EVENT_LENGTH, buf);
		putString(event.event, EVENT_LENGTH, buf);

		//buf.putInt(event.getListId());
		buf.putInt(-1);

		//putTime(event.getStartTime(), buf);
		//putTime(event.getStopTime(), buf);
		putTime(event.t1, buf);
		putTime(event.t2, buf);

		buf.putInt(t1Seed);
		buf.putInt(t2Seed);
		putString(loggedName, NAME_LENGTH, buf);
		buf.flip();

		requestContext = acnetConnection.requestSingle(node.value(), "LMBRJK", buf, 20000, this);
	}

	@Override
	public void handle(AcnetReply r)
	{
		if (!cancelled) {
			try {
				final ByteBuffer buf = r.data().order(ByteOrder.LITTLE_ENDIAN);
				int status = r.status();

				if (status == 0)
					status = buf.getShort();

				if (status != 0) {
					consecutiveErrorCount++;
					handleStatus(status);
				} else {
					final int pointCount = buf.getInt();

					consecutiveErrorCount = 0;

					if (pointCount == 0)
						sendStatus(0);
						//callback.plotData(now(), status, 0, null, null, null);
					else {
						final byte[] name = new byte[NAME_LENGTH];

						buf.get(name);

						t1Seed = buf.getInt();

						buf.getInt();
						
						final long micros[] = new long[pointCount];
						final int nanos[] = new int[pointCount];
						final double values[] = new double[pointCount];

						for (int ii = 0; ii < pointCount; ii++) {
							micros[ii] = buf.getLong() * 1000;
							nanos[ii] = 0;
							values[ii] = Double.longBitsToDouble(buf.getLong());
						}
				
						callback.plotData(now(), 0, pointCount, micros, nanos, values);

						if (pointCount != FETCH_POINT_COUNT)
							cancel(0);
							//callback.plotData(now(), 0, 0, new long[0], new int[0], new double[0]);
						else
							fetch();
					}
				}
			} catch (AcnetStatusException e) {
				sendStatus(e.status);
			} catch (Exception e) {
				sendStatus(LJ_COMM);
			}
		}
	}

	private void cancel(int status)
	{
		cancelled = true;
		cancel();
		sendStatus(status);
		user.complete();
	}

	@Override
	public void run()
	{
		try {
			fetch();
		} catch (AcnetStatusException e) {
			cancel(e.status);
		}
	}

	private void sendStatus(int status)
	{
		callback.plotData(now(), status, 0, new long[0], new int[0], new double[0]);
	}

	private void handleStatus(int status)
	{
		//boolean willContinue = false;

		if (consecutiveErrorCount <= 5) {
			//willContinue = true;

			switch (status) {
				case ACNET_PEND:
				case ACNET_UTIME:
				case ACNET_REQTMO:
				case ACNET_REQPACK:
				case ACNET_REPLY_TIMEOUT:
					//if (++lastFetchComplaintCount > 20) {
						//willContinue = false;
					//}

					//ck.dataLoggerFetchTrouble(plotRequest, status);

					//if (!cancelled) {
						try {
							fetch();
						} catch (AcnetStatusException e) {
							cancel(e.status);
							//	ck.dataLoggerFetchTrouble(plotRequest, e.status);
							//willContinue = false;
						}
					//}

					//if (willContinue)
					//	return;

				case ACNET_BUSY:
				case ACNET_QUEFULL:
					AcnetPoolImpl.sharedTimer.schedule(this, 5000);
					break;
				//if (++lastFetchComplaintCount > 20) {
				//	willContinue = false;
				//}

				//ck.dataLoggerFetchTrouble(plotRequest, status);

				//if (willContinue)
				//	return;

				//case ACNET_NO_TASK:
			//	willContinue = false;
			//	status = LJ_COMM;
			//	break;

			}
		} else
			cancel(status);

		//if (!willContinue) {
			//plotRequest.troubleCallback.dataLoggerFetchTrouble(plotRequest, status);
			cancelled = true;
		//}

	}
}
