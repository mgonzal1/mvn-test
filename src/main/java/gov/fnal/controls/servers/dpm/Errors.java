// $Id: Errors.java,v 1.4 2024/11/19 22:34:43 kingc Exp $
package gov.fnal.controls.servers.dpm;

import java.util.HashMap;
import java.sql.ResultSet;
import java.util.logging.Logger;
import java.util.logging.Level;

import static gov.fnal.controls.db.DbServer.getDbServer;
import static gov.fnal.controls.servers.dpm.DPMServer.logger;

public class Errors
{
	private static HashMap<Integer, String> values = new HashMap<>();
	private static HashMap<String, Integer> names = new HashMap<>();

	public static void init()
	{
		final String sqlQuery = "select facility_number+(error_code*256) AS \"value\", full_error_text::text from hendricks.acnet_errors";

		ResultSet rs = null;

		try {
			rs = getDbServer("adbs").executeQuery(sqlQuery);

			while (rs.next()) {
				values.put(rs.getInt("value"), rs.getString("full_error_text"));
				names.put(rs.getString("full_error_text"), rs.getInt("value"));
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception in init()", e);
		} finally {
			try {
				rs.close();
			} catch (Exception ignore) { }
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
			logger.log(Level.INFO, String.format("%30s %8d %4s    [%s]\n", name, value(name), hex(name), numericName(name)));
	}
}
