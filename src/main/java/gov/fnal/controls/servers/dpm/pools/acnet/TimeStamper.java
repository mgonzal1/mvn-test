// $Id: TimeStamper.java,v 1.11 2024/11/19 22:34:44 kingc Exp $
package gov.fnal.controls.servers.dpm.pools.acnet;

class TimeStamper
{
    static volatile long currentTS = System.currentTimeMillis();

    public static long getBaseTime02(long feMillis)
	{
            final long now = System.currentTimeMillis();
            long returnTime = currentTS;
            long millis = now - returnTime;

            while (millis > 5000) {
                returnTime += 5000;
                millis -= 5000;
            }

            long errorMillis = now - (returnTime + feMillis);

            if (errorMillis > 4000) {
                returnTime += 5000;
				currentTS = returnTime;
            } else if (errorMillis < -1000) {
                returnTime -= 5000;
				currentTS = returnTime;
            }

            return returnTime * 1000000;
    }
}
