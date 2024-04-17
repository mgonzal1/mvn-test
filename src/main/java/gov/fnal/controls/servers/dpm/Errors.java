// $Id: Errors.java,v 1.1 2022/11/01 20:44:35 kingc Exp $
package gov.fnal.controls.servers.dpm;

import gov.fnal.controls.db.CachedResultSet;
import gov.fnal.controls.db.DbServer;

import java.util.HashMap;

import static gov.fnal.controls.db.DbServer.getDbServer;

public class Errors implements DbServer.Constants
{
	private static HashMap<Integer, String> values = new HashMap<>();
	private static HashMap<String, Integer> names = new HashMap<>();

	static void init()
	{
		final String sqlQuery = "select facility_number+(error_code*256) AS \"value\", full_error_text::text from hendricks.acnet_errors";

		try {
			final CachedResultSet rs = getDbServer("adbs").executeQuery(sqlQuery);

			while (rs.next()) {
				values.put(rs.getInt("value"), rs.getString("full_error_text"));
				names.put(rs.getString("full_error_text"), rs.getInt("value"));
			}
		} catch (Exception e) {
		}
	}

	public static String name(int value)
	{
		final String name = values.get(value);

		return name == null ? numericName(value) : name; 
	}

	public static String numericName(int value)
	{
		return (value & 0xff)  + " " + (value >> 8);
	}

	public static String numericName(String name)
	{
		return numericName(value(name));
	}

	public static String hex(int value)
	{
		return String.format("%04x", value & 0xffff);
	}

	public static String hex(String name)
	{
		return hex(value(name));
	}

	public static int value(String name)
	{
		final Integer value = names.get(name);

		return value == null ? -1 : value;
	}

//	private static String internalErrorCodeToVerboseText(int acnetErrorCode)
//			throws SQLException// , AcnetException
//	{

//		String acnetVerboseText;

//		acnetVerboseText = verboseErrorText.get(new Integer(
//				acnetErrorCode));

//		if (acnetVerboseText == null) // need to read text from the database
//		{
			//FacilityError facilityError = errorCodeToFacilityError(acnetErrorCode);
			//String sqlQuery = "select segment,error_text::text from hendricks.acnet_error_text where facility_number = "
			//		+ facilityError.facility
			//		+ " and error_code = "
			//		+ facilityError.error + " order by segment";
			//try {
			//	CachedResultSet results = getDbServer("adbs").executeQuery(sqlQuery);
			//	StringBuffer textBuffer = new StringBuffer(5 * 255);
//
//				for (int val = 0; val < results.size(); val++) {
//					textBuffer.append(results.get(val, 2));
//				}
//				acnetVerboseText = textBuffer.toString();
//
//			} catch (SQLException sqlExc) {
//				System.out.println("SQL error: " + sqlExc);
//				throw sqlExc;
//			}
//			if ((acnetVerboseText == null) || (acnetVerboseText.length() == 0)) // no
//																				// match
//																				// in
//																				// the
//																				// database
//			{
//				acnetVerboseText = errorCodeToNumericText(acnetErrorCode)
//						+ " : no help available for this error code";
//				verboseErrorText.put(new Integer(acnetErrorCode),
//						acnetVerboseText);
//				unmappedVerboseErrorText.add(new Integer(acnetErrorCode));
//			}
//		}

//		return (acnetVerboseText);

//	}

	public static void main(String args[]) throws Exception
	{
		init();

		for (String name : names.keySet())
			System.out.printf("%30s %8d %4s    [%s]\n", name, value(name), hex(name), numericName(name));
	}
}
