// $Id: SharedFTPRequest.java,v 1.6 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.Enumeration;
import java.util.Vector;

import gov.fnal.controls.servers.dpm.events.DataEvent;
import gov.fnal.controls.servers.dpm.events.DataEventObserver;
import gov.fnal.controls.servers.dpm.events.DeltaTimeEvent;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;

class SharedFTPRequest extends FTPRequest implements ReceiveData, DataEventObserver
{
	final static int QUEUE_LEN = 100;

	private Vector<FTPRequest> ftpUsers;

	private long[] queuedMicroSecs = new long[QUEUE_LEN];
	private int[] queuedNanoSecs = new int[QUEUE_LEN];
	private double[] queuedValues = new double[QUEUE_LEN];
	//private int queuedError = 0;
	private int numQueued = 0;


	//private DataEvent sendPlotDataEvent = new DeltaTimeEvent(67 * FTPPool.FTP_RETURN_PERIOD, false);

	SharedFTPRequest(WhatDaq sharedDevice, ClassCode classCode, FTPScope sharedScope)
	{
		//super((WhatDaq) sharedDevice.clone(), (ClassCode) classCode, (FTPScope) sharedScope.clone(), null);
		super(sharedDevice, classCode, sharedScope, null);

		this.callback = this;
		this.ftpUsers = new Vector<FTPRequest>(5);
	}
	
	void addUser(FTPRequest user)
	{
		ftpUsers.add(user);
	}

	Vector<FTPRequest> getUsers() 
	{ 
		return ftpUsers;
	}

	@Override
	public void plotData(long timestamp, int error, int numberPoints, long[] microSecs, int[] nanoSecs, double[] values)
	{
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
	}

	void deliverError(int error)
	{
	/*
		Enumeration<FTPRequest> users;
		FTPRequest user;

		users = ftpUsers.elements();

		while (users.hasMoreElements()) {
			user = users.nextElement();

			if (user.delete)
				continue;

			user.callback.plotData(System.currentTimeMillis(), error, 0, null, null, null);
		}
		*/

		for (FTPRequest user : ftpUsers) {
			if (!user.delete)
				user.callback.plotData(System.currentTimeMillis(), error, 0, null, null, null);
		}
	}

	@Override
	public String toString()
	{
		return "SharedFTPRequest: " + super.toString();
	}

/*
	public void stopCollection()
	{
	}
	*/

	synchronized void sendPlotData()
	{
		long[] microSecs = null;
		int[] nanoSecs = null;
		double[] values = null;
		int numToSend = 0;
		int error;

		if (numQueued != 0) { // || queuedError != 0) {
			//synchronized (sendPlotDataEvent) {
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
					//error = queuedError;
					//queuedError = 0;
				} catch (Exception e) {
				/*
					StringBuffer complaint = new StringBuffer();
					complaint.append("SharedFTPRequest, " + e);
					complaint.append("\r\nnumQueued: " + numQueued);
					if (microSecs != null)
						complaint.append(", microSecs len: " + microSecs.length);
					System.out.println(complaint.toString());
				*/	e.printStackTrace();
					numQueued = 0;
					return;
				}

			//}
			
			plotData(System.currentTimeMillis(), 0, numToSend, microSecs, nanoSecs, values);
		}
	}

/*
	public void update(DataEvent userEvent, DataEvent currentEvent)
	{
		if (userEvent == sendPlotDataEvent) {
			sendPlotData();
			return;
		}
	}
	*/
}
