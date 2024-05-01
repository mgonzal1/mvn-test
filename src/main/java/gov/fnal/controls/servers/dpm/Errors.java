// $Id: Errors.java,v 1.2 2024/03/19 22:10:41 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.HashMap;

import gov.fnal.controls.db.CachedResultSet;
import static gov.fnal.controls.db.DbServer.getDbServer;

public class Errors
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

	public static void main(String args[]) throws Exception
	{
		init();

		for (String name : names.keySet())
			System.out.printf("%30s %8d %4s    [%s]\n", name, value(name), hex(name), numericName(name));
	}
}
