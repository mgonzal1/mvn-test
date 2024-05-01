// $Id: MaxReplySizeOverride.java,v 1.3 2024/01/10 20:53:50 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.TimerTask;
import java.io.FileReader;
import java.io.BufferedReader;

import static gov.fnal.controls.servers.dpm.DPMServer.logger;

// Read a text file of maximum reply size overrides for front-ends.  This should
// just be a temporary fix until the large ACNET message code is removed from MOOC.

class MaxReplySizeOverride extends TimerTask
{
	private static final long updateRate = 10 * 60 * 1000;
	private static final HashMap<String, Integer> overrides = new HashMap<>();
	private static final HashSet<String> announcedOverride = new HashSet<>();

	static {
		AcnetPoolImpl.sharedTimer.scheduleAtFixedRate(new MaxReplySizeOverride(), updateRate, updateRate);
	}

	private MaxReplySizeOverride()
	{
		run();
	}

	@Override
	synchronized public void run()
	{
		try {
			final String fileName = "/export/engines/files/MaxReplySizeOverrides.txt";
			final BufferedReader in = new BufferedReader(new FileReader(fileName));
	
			overrides.clear();

			String line;
			while ((line = in.readLine().trim()) != null) {
				if (!line.startsWith("#")) {
					final String[] s = line.split(",");

					try {
						overrides.put(s[0].trim(), Integer.parseInt(s[1].trim()));	
					} catch (Exception ignore) {
					}
				}
			}

			in.close();
		} catch (Exception ignore) { }
	}

	synchronized static int get(String nodeName, int defaultValue)
	{
		final Integer tmp = overrides.get(nodeName);

		if (tmp == null)
			return defaultValue;
		else {
			if (!announcedOverride.contains(nodeName)) {
				announcedOverride.add(nodeName);
				logger.log(Level.INFO, "Using maximum reply size override of " 
											+ tmp.intValue() + " for " + nodeName);
			}

			return tmp.intValue();
		}
	}

	public static void main(String[] args) throws InterruptedException
	{
		final String[] nodes = { "XTBPM1", "NMLBP3", "MI3" };

		while (true) {
			for (String node : nodes)
				System.out.printf("%-8s = %d\n", node, get(node, 1234));
			Thread.sleep(2000);
		}
	}
}
