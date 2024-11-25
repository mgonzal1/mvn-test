// $Id: FTPRequest.java,v 1.14 2024/11/19 22:34:44 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;

import gov.fnal.controls.servers.dpm.drf3.Event;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

class FTPRequest implements ReceiveData
{
	final WhatDaq whatDaq;
	final ClassCode plotClass;
	final FTPScope scope;
	final Trigger trigger;
	final ReArm reArm;

	boolean delete;
	int error;
	double ftpRate = 0.0;
	double ftpDuration = 0.0;

	int maxPoints = 0;
	int nextPoint = 0;
	long[] ftpMicroSecs = null;
	double[] ftpValues = null;
	private int ftpError = 0;
	boolean completed = false;

	ReceiveData callback;

	FTPRequest(WhatDaq whatDaq, ClassCode classCode, FTPScope userScope, ReceiveData userCallback)
	{
		this(whatDaq, classCode, null, userScope, userCallback, null);
	}

	FTPRequest(WhatDaq whatDaq, ClassCode classCode, Trigger userTrigger, FTPScope userScope, ReceiveData callback, ReArm again)
	{
		this.whatDaq = whatDaq;
		this.plotClass = classCode;
		this.trigger = userTrigger;
		this.scope = userScope;
		this.reArm = again;
		this.delete = false;
		this.callback = callback;

		if (userScope == null)
			logger.log(Level.WARNING, this + " has null scope");
		else {
			ftpRate = userScope.getRate();
			ftpDuration = userScope.getDuration();

			if (ftpDuration != 0.0 && ftpDuration != Double.MAX_VALUE) {
				if ((ftpRate * ftpDuration) > 1000000) {
					logger.log(Level.WARNING, "FTPRequest, too many points to clip, rate: " + ftpRate + ", duration: " + ftpDuration);
				} else {
					maxPoints = (int) (ftpRate * ftpDuration);
					if (maxPoints == 0) {
						logger.log(Level.WARNING, "FTPRequest, no points, rate: " + ftpRate + ", duration: " + ftpDuration);
					} else {
						ftpMicroSecs = new long[maxPoints];
						ftpValues = new double[maxPoints];
						this.callback = this;
					}
				}
			}
		}
	}

	WhatDaq getDevice()
	{
		return whatDaq;
	}

	int getFTPClassCode()
	{
		return plotClass.ftp();
	}

	ClassCode getClassCode()
	{
		return plotClass;
	}

	Trigger getTrigger()
	{
		return trigger;
	}

	ReArm getReArm()
	{
		return reArm;
	}

	FTPScope getScope()
	{
		return scope;
	}

	int getError()
	{
		return error;
	}

	void setDelete()
	{
		delete = true;
	}

	boolean getDelete()
	{
		return delete;
	}

	private void lastCall(boolean forceError, int error)
	{
		if (!completed) {
			sendCallbackPlotData(true, forceError, error);
		}
	}

	private void sendCallbackPlotData(boolean lastCall, boolean forceError, int error)
	{
		if (!completed) {
			completed = true;

			if (nextPoint == 0 && forceError && ftpError == 0)
				ftpError = error;

			final long now = System.currentTimeMillis();
			
			callback.plotData(now, ftpError, nextPoint, ftpMicroSecs, null, ftpValues);

			if (lastCall)
				setDelete();
		}
	}

	@Override
	public void receiveData(ByteBuffer buf, long timestamp, long cycle)
	{
		Thread.dumpStack();
	}

	@Override
	public void plotData(long timestamp, int error, int numberPoints, long[] microSecs, int[] nanoSecs, double[] values)
	{
		if (completed)
			return;

		ftpError = error;

		if (numberPoints > 0) {
			if ((numberPoints + nextPoint) > maxPoints)
				numberPoints = maxPoints - nextPoint;
			for (int ii = 0; ii < numberPoints; ii++) {
				ftpMicroSecs[nextPoint] = microSecs[ii];
				ftpValues[nextPoint++] = values[ii];
			}
			if (nextPoint >= maxPoints) {
				sendCallbackPlotData(false, false, 0);
			}
		}
	}
}
