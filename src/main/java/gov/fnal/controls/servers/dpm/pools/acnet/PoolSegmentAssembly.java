// $Id: PoolSegmentAssembly.java,v 1.11 2024/10/10 16:31:34 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.BitSet;
import java.util.ArrayList;
import java.nio.ByteBuffer;

import gov.fnal.controls.servers.dpm.pools.WhatDaq;
import gov.fnal.controls.servers.dpm.pools.ReceiveData;

class PoolSegmentAssembly
{
	class WhatDaqSegment extends WhatDaq
	{
		final int segmentId;

		WhatDaqSegment(WhatDaq whatDaq, int length, int offset, int segmentId, ReceiveData receiveData)
		{
			super(whatDaq);

			this.segmentId = segmentId;

			lengthOffset(length, offset);
			receiveData(receiveData);
		}
	}

	class ReceiveSegment implements ReceiveData
	{
		final int index;
		final WhatDaqSegment whatDaq;

		ReceiveSegment(int index, int length, int offset, int id)
		{
			this.index = index;
			//this.whatDaq = new WhatDaq(bigWhatDaq, length, offset, id, this);
			//this.whatDaq = WhatDaq.create(bigWhatDaq, length, offset, id, this);
			this.whatDaq = new WhatDaqSegment(bigWhatDaq, length, offset, id, this);
		}

		@Override
		public void receiveData(ByteBuffer segment, long timestamp, long cycle)
		{
			if (whatDaq.isMarkedForDelete())
				return;
			segment.get(data, index, whatDaq.length());

			segmentsReceived.set(whatDaq.segmentId);

			if (segmentsReceived.cardinality() == segments.size()) {
				segmentsReceived.clear();
				bigWhatDaq.getReceiveData().receiveData(data, 0, timestamp, cycle);
				bigWhatDaq.error(0);
			}
		}

		@Override
		public void receiveStatus(int status, long timestamp, long cycle)
		{
			receiveError(status);
		}

		@Override
		public void receiveData(byte[] segment, int offset, long timestamp, long cycle)
		{
			if (whatDaq.isMarkedForDelete())
				return;

			System.arraycopy(segment, offset, data, index, whatDaq.length());

			segmentsReceived.set(whatDaq.segmentId);

			if (segmentsReceived.cardinality() == segments.size()) {
				segmentsReceived.clear();
				bigWhatDaq.getReceiveData().receiveData(data, 0, timestamp, cycle);
				bigWhatDaq.error(0);
			}
		}
	}

	private final WhatDaq bigWhatDaq;
	private BitSet segmentsReceived;
	private byte data[];
	private final ArrayList<ReceiveSegment> segments;

	private PoolSegmentAssembly(WhatDaq bigWhatDaq, int segmentSize, boolean isRepetitive)
	{
		this.bigWhatDaq = bigWhatDaq;
		this.segments = new ArrayList<>();
		this.data = new byte[bigWhatDaq.length()];

		int index = 0;
		int id = 0;
		int offset = bigWhatDaq.offset();

		for (; id < (bigWhatDaq.length() / segmentSize); id++, offset += segmentSize, index += segmentSize) {
			this.segments.add(new ReceiveSegment(index, segmentSize, offset, id));
		}

		final int lastSegmentSize = bigWhatDaq.length() % segmentSize; 

		if (lastSegmentSize > 0)
			this.segments.add(new ReceiveSegment(index, lastSegmentSize, offset, id));

		this.segmentsReceived = new BitSet(this.segments.size());
	}

	public static void insert(WhatDaq whatDaq, DaqPool pool, int segmentSize, boolean isRepetitive)
	{
		final PoolSegmentAssembly p = new PoolSegmentAssembly(whatDaq, segmentSize, isRepetitive);

		for (ReceiveSegment r : p.segments)
			pool.insert(r.whatDaq);
	}

	@Override
	public String toString()
	{
		return "PoolSegmentAssembly: " + bigWhatDaq;
	}

	private final void receiveError(int error)
	{
		bigWhatDaq.error(error);

		if (!bigWhatDaq.isRepetitive()) {
			for (ReceiveSegment r : segments)
				r.whatDaq.setMarkedForDelete();

			bigWhatDaq.getReceiveData().receiveStatus(error, System.currentTimeMillis(), 0);
		}
	}
}
