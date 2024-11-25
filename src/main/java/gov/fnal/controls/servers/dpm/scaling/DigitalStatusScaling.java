// $Id: DigitalStatusScaling.java,v 1.5 2024/07/03 16:39:30 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import java.util.List;
import java.util.ArrayList;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;
import gov.fnal.controls.servers.dpm.pools.DeviceInfo;

class DigitalStatusScaling implements AcnetErrors
{
	final ArrayList<DeviceInfo.Status.BitTextAttribute> bits;
	final ArrayList<String> bitNames;

	DigitalStatusScaling(DeviceInfo dInfo)
	{
		if (dInfo.status == null) {
			this.bits = new ArrayList<>();
			this.bitNames = new ArrayList<>();
		} else {
			this.bits = new ArrayList<>(dInfo.status.bitText);
			this.bitNames = new ArrayList<>(dInfo.status.bitNames);
		}
	}

	private final String value(int data, DeviceInfo.Status.BitTextAttribute bit)
	{
		return (data & bit.mask) == 0 ? bit.text0 : bit.text1;
	}

	List<String> bitValues(int data)
	{
		final ArrayList<String> values = new ArrayList<>();

		for (DeviceInfo.Status.BitTextAttribute bit : bits) {
			if (bit.bitNo >= values.size()) {
				final int emptyCount = bit.bitNo - values.size();

				for (int ii = 0; ii < emptyCount; ii++)
					values.add("");

				values.add(value(data, bit));
			} else
				values.set(bit.bitNo, value(data, bit));
		}

		return values;
	}

	String[] scale(int data)
	{
		final ArrayList<String> state = new ArrayList<>();

		for (DeviceInfo.Status.BitTextAttribute bit : bits)
			state.add((data & bit.mask) == 0 ? bit.text0 : bit.text1);

		return state.toArray(new String[state.size()]);
	}

	String[] getLabels()
	{
		final String[] labels = new String[bits.size()];

		int ii = 0;

		for (DeviceInfo.Status.BitTextAttribute bit : bits)
			labels[ii++] = bit.label;

		return labels;
	}
}
