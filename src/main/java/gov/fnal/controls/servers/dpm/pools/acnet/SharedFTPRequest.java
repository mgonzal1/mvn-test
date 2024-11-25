// $Id: SharedFTPRequest.java,v 1.8 2024/09/27 18:26:16 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class SharedFTPRequest extends FTPRequest implements ReceiveData
{
	final static int QUEUE_LEN = 100;

	final ArrayList<FTPRequest> users;
	final long[] queuedMicroSecs = new long[QUEUE_LEN];
	final int[] queuedNanoSecs = new int[QUEUE_LEN];
	final double[] queuedValues = new double[QUEUE_LEN];

	int numQueued = 0;

	SharedFTPRequest(WhatDaq sharedDevice, ClassCode classCode, FTPScope sharedScope)
	{
		super(sharedDevice, classCode, sharedScope, null);

		this.callback = this;
		this.users = new ArrayList<>();
	}
	
	synchronized void addUser(FTPRequest user)
	{
		users.add(user);
	}

	Collection<FTPRequest> users() 
	{ 
		return users;
	}

	@Override
	public synchronized void plotData(long timestamp, int error, int count, long[] ms, int[] ns, double[] values)
	{
		for (FTPRequest req : users) {
			if (!req.delete && req.callback != null)
				req.callback.plotData(timestamp, error, count, ms, ns, values);
		}

/*
		for (int ii = 0; ii < ftpUsers.size(); ii++) {
			if (ii < ftpUsers.size()) {
				final FTPRequest userFTPRequest = ftpUsers.elementAt(ii);

				if (userFTPRequest.delete)
					continue;

				if (userFTPRequest.callback != null) {
					userFTPRequest.callback.plotData(timestamp, error, numberPoints, microSecs, nanoSecs, values);
				}
			}
		}
		*/
	}

	void deliverError(int error)
	{
		for (FTPRequest user : users) {
			if (!user.delete)
				user.callback.plotData(System.currentTimeMillis(), error, 0, null, null, null);
		}
	}

	@Override
	public String toString()
	{
		return "SharedFTPRequest: " + super.toString();
	}

	synchronized void sendPlotData()
	{
		if (numQueued != 0) {
			long[] microSecs = null;
			int[] nanoSecs = null;
			double[] values = null;

			int numToSend = 0;
			int error;

			try {
				if (numQueued == 0) {
					microSecs = null;
					nanoSecs = null;
					values = null;
				} else {
					microSecs = new long[numQueued];
					nanoSecs = new int[numQueued];
					values = new double[numQueued];

					for (int ii = 0; ii < numQueued; ii++) {
						microSecs[ii] = queuedMicroSecs[ii];
						nanoSecs[ii] = queuedNanoSecs[ii];
						values[ii] = queuedValues[ii];
					}
				}
				numToSend = numQueued;
				numQueued = 0;
			} catch (Exception e) {
				logger.log(Level.WARNING, "exception in sendPlotData()", e);
				numQueued = 0;
				return;
			}
			
			plotData(System.currentTimeMillis(), 0, numToSend, microSecs, nanoSecs, values);
		}
	}
}
