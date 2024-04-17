// $Id: ReceiveData.java,v 1.7 2024/03/05 17:28:02 kingc Exp $
package gov.fnal.controls.servers.dpm.pools;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public interface ReceiveData
{
	default public void receiveStatus(int status)
	{
		receiveStatus(status, System.currentTimeMillis(), 0);
	}

	void receiveData(ByteBuffer data, long timestamp, long cycle);

	default public void receiveData(byte[] data, int offset, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData() cycle for " + getClass());
	}

	default public void receiveStatus(int status, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveStatus() for " + getClass());
		Thread.dumpStack();
	}	

	default public void receiveData(double value, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData() for " + getClass());
	}

	default public void receiveData(double value[], long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData() for " + getClass());
	}

	default public void receiveData(String value, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveData() for " + getClass());
	}

	default public void receiveEnum(String value, int index, long timestamp, long cycle)
	{
		logger.log(Level.WARNING, "missing receiveEnum() for " + getClass());
	}

	default public void plotData(long timestamp, int error, int numberPoints, long[] microSecs, int[] nanoSecs, double[] values)
	{
		logger.log(Level.WARNING, "missing plotData() for " + getClass());
	}
}
