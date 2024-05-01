// $Id: ClientReport.java,v 1.8 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Objects;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.logging.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetInterface;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetConnection;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class ClientReport extends TimerTask
{
	static final byte TC_REPORT = 101;
	static final int ERROR_SETTING_BIT = 0x40000000;

	static AcnetConnection acnetConnection;
	static int localNode;

	HashMap<ErrorHashKey, ErrorHashValue> errorMap;

	final String taskName;

	final long setDate;

	static {
		acnetConnection = AcnetInterface.open("ERRRPT");
		try {
			localNode = acnetConnection.getLocalNode();
		} catch (Exception e) {
			localNode = 0;
		}
	}

	private class ErrorHashKey
	{
		int dipi, error;
		boolean isSetting;

		ErrorHashKey(int dipi, int error, boolean isSetting)
		{
			this.dipi = dipi;
			this.error = error;
			this.isSetting = isSetting;
		}

		@Override
		public boolean equals(Object o)
		{
			if (o instanceof ErrorHashKey) {
				final ErrorHashKey k = (ErrorHashKey) o;

				return dipi == k.dipi && isSetting == k.isSetting && error == k.error;
			}

			return false;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(dipi, isSetting, error);
		}
	}

	private class ErrorHashValue
	{
		int errorCount = 1;
	}

 	ClientReport(String taskName)
	{
		this.taskName = taskName;
		this.setDate = System.currentTimeMillis();

		this.errorMap = new HashMap<>();

		AcnetPoolImpl.sharedTimer.scheduleAtFixedRate(this, 0, 2 * 60 * 1000);
	}

	void reportError(int dipi, int error, boolean isSetting)
	{
		final ErrorHashKey key = new ErrorHashKey(dipi, error, isSetting);

		final int mapSize;

		synchronized (errorMap) {
			final ErrorHashValue val = errorMap.get(key);

			if (val == null)
				errorMap.put(key, new ErrorHashValue());
			else
				val.errorCount++;

			mapSize = errorMap.size();
		}

		if (mapSize > 500) {
			try {
				sendReport();
			} catch (Exception e) {
			}
		}
	}

	//void close() throws AcnetStatusException
	//{
		//sendTimer.deleteObserver(this);
//		cancel();
//		sendReport();
//	}

	synchronized private void sendReport() throws AcnetStatusException
	{
		if (errorMap.size() > 0) {
			logger.log(Level.FINER, taskName + " sending " + errorMap.size() + " reports");

			final ByteBuffer buf = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);

			buf.put(TC_REPORT);
			buf.putInt(taskName.length());
			buf.put(taskName.getBytes());
			buf.putInt(localNode & 0xff);
			buf.putInt(localNode >> 8);
			buf.putInt(0);
			buf.putInt(0);
			buf.putLong(setDate);
			buf.putLong(System.currentTimeMillis());
			buf.putInt(errorMap.size());

			for (Map.Entry<ErrorHashKey, ErrorHashValue> entry : errorMap.entrySet()) {
				final ErrorHashKey key = entry.getKey();
				final ErrorHashValue val = entry.getValue();

				buf.putInt(key.dipi | (key.isSetting ? ERROR_SETTING_BIT : 0));
				buf.putInt(key.error);
				buf.putInt(val.errorCount);
			}

			errorMap.clear();

			buf.flip();
			acnetConnection.send("ERRORZ", "ERRSVR", buf);
		}
	}

	@Override
	public void run()
	{
		try {
			sendReport();
		} catch (Exception e) {
			logger.log(Level.FINE, "error report send", e);
		}
	}
}
