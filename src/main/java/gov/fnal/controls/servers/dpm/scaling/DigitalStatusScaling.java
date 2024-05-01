// $Id: DigitalStatusScaling.java,v 1.4 2023/11/02 16:36:16 kingc Exp $
package gov.fnal.controls.servers.dpm.scaling;

import java.util.ArrayList;
import java.sql.ResultSet;

import gov.fnal.controls.servers.dpm.acnetlib.AcnetErrors;
import gov.fnal.controls.servers.dpm.acnetlib.AcnetStatusException;

import static gov.fnal.controls.db.DbServer.getDbServer;

class DigitalStatusScaling implements AcnetErrors
{
	class StatusBit
	{
		int number;
		String text0;
		String text1;
		String label;
		int mask;
	}

	final ArrayList<StatusBit> bits;

	DigitalStatusScaling(int di) throws AcnetStatusException
	{
		this.bits = new ArrayList<>();

		final String query = "SELECT DDS.bit_no, DS.short_name_0, " + "DS.short_name_1, DS.bit_description "
								+ "FROM " + "accdb.digital_status DS, " + "accdb.device_digital_status DDS "
								+ "WHERE DDS.di = " + di + " AND DDS.digital_status_id = DS.digital_status_id";

		ResultSet rs = null;

		try {
			rs = getDbServer("adbs").executeQuery(query);

			while (rs.next()) {
				final StatusBit bit = new StatusBit();

				bit.number = rs.getInt("bit_no");
				bit.text0 = rs.getString("short_name_0").trim();
				bit.text1 = rs.getString("short_name_1").trim();
				bit.label = rs.getString("bit_description").trim();
				bit.mask = (1 << bit.number);

				bits.add(bit);
			}
		} catch (Exception e) {
			throw new AcnetStatusException(DIO_NOSCALE, "Retrieval or reformating of extended status failed.", e);
		} finally {
			try {
				rs.close();
			} catch (Exception ignore) { }
		}
	}

	String[] scale(int data)
	{
		final ArrayList<String> state = new ArrayList<>();

		for (StatusBit bit : bits)
			state.add((data & bit.mask) == 0 ? bit.text0 : bit.text1);

		return state.toArray(new String[state.size()]);
	}

	String[] getLabels()
	{
		final String[] labels = new String[bits.size()];

		int ii = 0;
		for (StatusBit bit : bits)
			labels[ii++] = bit.label;

		return labels;
	}

	public static void main(String[] args) throws Exception
	{
		final DigitalStatusScaling s = new DigitalStatusScaling(253488);

		for (String label : s.getLabels())
			System.out.println(label);

		System.out.println();

		for (String str : s.scale(0xffffffff))
			System.out.println(str);

		System.out.println();

		for (String str : s.scale(0))
			System.out.println(str);
	}
}
