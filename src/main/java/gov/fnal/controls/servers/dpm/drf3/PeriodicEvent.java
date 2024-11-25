//  $Id: PeriodicEvent.java,v 1.4 2024/09/23 18:56:04 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeriodicEvent extends Event
{
    private static final TimeFreq DEFAULT_TIMEFREQ = new TimeFreq(1000);
    private static final boolean DEFAULT_IMMEDIATE = true;
    
    private static final Pattern RE = Pattern.compile(
        "(?i)(P|Q)(?:,(\\w+)(?:,(F|FALSE|T|TRUE))?)?"
    );

    static PeriodicEvent parsePeriodic(String str) throws EventFormatException
	{
        final Matcher m = RE.matcher(str);

        if (!m.matches())
            throw new EventFormatException("Invalid periodic event: \"" + str + "\"");

        try {
            final char cc = m.group(1).charAt(0);
            final boolean cont = (cc == 'P' || cc == 'p');

            if (m.group(2) != null) {
                final TimeFreq period = TimeFreq.parse(m.group(2));

                if (m.group(3) != null) {
                    final char ic = m.group(3).charAt(0);
                    final boolean immd = (ic == 'T' || ic == 't');

                    return new PeriodicEvent(cont, period, immd);
                } else {
                    return new PeriodicEvent(cont, period);
                }
            } else
                return new PeriodicEvent(cont);
        } catch (IllegalArgumentException e) {
            throw new EventFormatException(e);
        }
    }
    
    private final TimeFreq period;
    private final boolean continuous, immediate;

    private transient String text;

    public PeriodicEvent(boolean continuous)
	{
        this(continuous, DEFAULT_TIMEFREQ);
    }
    
    public PeriodicEvent(boolean continuous, TimeFreq timeFreq)
	{
        this(continuous, timeFreq, DEFAULT_IMMEDIATE);
    }

    public PeriodicEvent(boolean continuous, TimeFreq period, boolean immediate)
	{
        if (period == null)
            throw new NullPointerException();
        
        this.continuous = continuous;
        this.period = period;
        this.immediate = immediate;
    }
    
    public boolean continuous()
	{
        return continuous;
    }

    public long getPeriod()
	{
        return period.getTimeMillis();
    }

	@Override
	public boolean isRepetitive()
	{
		return true;
	}

	@Override
	public long defaultTimeout()
	{
		final long millis = period.getTimeMillis();
		final long timeout = (long) ((millis * 3.0) + (millis / 2.0));

		//if (!repetitive)
			//timeout = 5000;
		//else
			//timeout = (long) ((millis * 3.0) + (millis / 2.0));

		return timeout > 15000 ? 15000 : timeout;
	}
    
    public TimeFreq getPeriodValue()
	{
        return period;
    }

    public boolean isImmediate()
	{
        return immediate;
    }

    @Override
    public int hashCode()
	{
        return period.hashCode() ^ (continuous ? 0x0000ffff : 0) ^ (immediate  ? 0xffff0000 : 0);
    }

    @Override
    public boolean equals(Object obj) 
	{
        return (obj instanceof PeriodicEvent) && ((PeriodicEvent)obj).period.equals(period) && ((PeriodicEvent) obj).immediate == immediate;
    }

    @Override
    public String toString()
	{
        if (text == null) {
            final StringBuilder buf = new StringBuilder();

            buf.append(continuous ? "P" : "Q");
            buf.append(",");
            buf.append(period);
            buf.append(",");
            buf.append(immediate ? "TRUE" : "FALSE");
            text = buf.toString();
        }
        return text;
    }
    
}
