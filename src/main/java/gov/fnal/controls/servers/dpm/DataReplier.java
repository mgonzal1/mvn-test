// $Id: DataReplier.java,v 1.50 2024/11/19 22:34:43 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.io.IOException;
import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

public interface DataReplier
{
    void sendReply(ByteBuffer data, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException;
    void sendReply(byte[] data, int offset, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException;
    void sendReply(double[] values, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException;
    void sendReply(double value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException;
	void sendReply(int value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException;
    void sendReply(String value, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException;
    void sendReply(String value, int index, long timestamp, long cycle) throws InterruptedException, IOException, AcnetStatusException;
	void sendReply(double[] values, long[] micros, long seqNo) throws InterruptedException, IOException, AcnetStatusException;
}
