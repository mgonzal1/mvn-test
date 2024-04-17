// $Id: DaqDefinitions.java,v 1.7 2024/02/22 16:32:14 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import gov.fnal.controls.servers.dpm.acnetlib.Node;
import gov.fnal.controls.servers.dpm.acnetlib.NodeFlags;

abstract class DaqDefinitions implements NodeFlags
{
	static final int MaxAcnetMessageSize = (8 * 1024) + 256 + 2;
	final int RequestOverhead;
	final int ReplyOverhead;
	final int DeviceOverhead;
	final int StatusOverhead;

	private static final DaqDefinitions daq16 = new Daq16();
	private static final DaqDefinitions daq32 = new Daq32();

	abstract int requestSize(int count);
	abstract DaqSendTransaction newSendTransaction(DaqRequestList reqList);

	DaqDefinitions(int v1, int v2, int v3, int v4)
	{
		//this.MaxAcnetMessageSize = // AcnetDefinitions.ACNET_MAX_MESSAGE_SIZE;
		this.RequestOverhead = v1;
		this.ReplyOverhead = v2;
		this.DeviceOverhead = v3;
		this.StatusOverhead = v4;
	}

	static DaqDefinitions get(Node node)
	{
		return node.has(EVENT_STRING_SUPPORT) ? daq32 : daq16;
	}

    int maxRequestCount()
	{
		return (MaxAcnetMessageSize - RequestOverhead) / DeviceOverhead;
	}

	static int MaxReplyDataLength()
	{	
		return MaxAcnetMessageSize - 34 - 20;
	}
}

class Daq16 extends DaqDefinitions
{
	Daq16()
	{
		super(6, 0, 16, 2);
	}

	int requestSize(int count)
	{
		return RequestOverhead + (count * DeviceOverhead);
	}

	DaqSendTransaction newSendTransaction(DaqRequestList reqList)
	{
		return new DaqSendTransaction16(reqList);
	}
}

class Daq32 extends DaqDefinitions
{
	final int MaxEventStringLength;

	Daq32()
	{
		super(18, 34, 20, 2);
		this.MaxEventStringLength = 54;
	}

	int requestSize(int count)
	{
		return RequestOverhead + MaxEventStringLength + (count * DeviceOverhead);
	}

	DaqSendTransaction newSendTransaction(DaqRequestList reqList)
	{
		return new DaqSendTransaction32(reqList);
	}
}
