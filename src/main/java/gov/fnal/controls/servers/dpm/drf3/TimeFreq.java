//  $Id: TimeFreq.java,v 1.2 2024/09/23 18:56:04 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.fnal.controls.servers.dpm.drf3.TimeFreqUnit.*;

public class TimeFreq
{
    private static final TimeFreqUnit DEFAULT_UNIT = MILLISECONDS;
    private static final int MAX_VALUE = 2147483647;
    private static final Pattern RE = Pattern.compile(
        "(?i)(\\d+)([A-Z]+)?"
    );
    
    public static TimeFreq parse(String str) throws RequestFormatException
	{
        if (str == null)
            throw new NullPointerException();
        
        final Matcher m = RE.matcher(str);
        if (!m.matches())
            throw new RequestFormatException("Invalid time or frequency: \"" + str + "\"");
        
        try {
            final int value = Integer.parseInt(m.group(1));
            if (m.group(2) != null) {
                TimeFreqUnit unit = TimeFreqUnit.parse(m.group(2));
                return new TimeFreq(value, unit);
            } else {
                return new TimeFreq(value);
            }
        } catch (IllegalArgumentException ex) {
            throw new RequestFormatException(ex);
        }
    }
    
    private final long value;
    private final long timeMillis;
    private final TimeFreqUnit unit;
    
    private final String text;
    
    public TimeFreq(long value)
	{
        this(value, DEFAULT_UNIT);
    }

    public TimeFreq(long value, TimeFreqUnit unit)
	{
        if (unit == null)
            throw new NullPointerException();
        
        if (value < 0 || value > MAX_VALUE)
            throw new IllegalArgumentException("Invalid value: " + value);
        
        if (value == 0) {
            this.value = 0;
            this.unit = DEFAULT_UNIT;
            timeMillis = 0;
        } else if (unit.isTime()) {
            long microsec = value * unit.getFactor();

            if (microsec >= 1000000 & microsec % 1000000 == 0) {
                this.value = (int)(microsec / 1000000);
                this.unit = SECONDS;
            } else if (microsec >= 1000 & microsec % 1000 == 0) {
                this.value = (int)(microsec / 1000);
                this.unit = MILLISECONDS;
            } else {
                this.value = (int)microsec;
                this.unit = MICROSECONDS;
            }
            timeMillis = Math.round(microsec / 1000.0);
        } else {
            long hertz = value * unit.getFactor(); 
            if (hertz >= 1000 && hertz % 1000 == 0) {
                this.value = (int) (hertz / 1000);
                this.unit = KILOHERTZ;
            } else {
                this.value = (int) hertz;
                this.unit = HERTZ;
            }
            timeMillis = Math.round(1000.0 / hertz); 
        }

		this.text = Long.toString(timeMillis);
    }
    
    public long getValue()
	{
        return value;
    }
    
    public TimeFreqUnit getUnit()
	{
        return unit;
    }
    
    public long getTimeMillis()
	{
        return timeMillis;
    }
    
    @Override
    public int hashCode()
	{
        return Objects.hash(value, unit);
    }

    @Override
    public boolean equals(Object obj)
	{
        return (obj instanceof TimeFreq)
                && ((TimeFreq) obj).value == value
                && ((TimeFreq) obj).unit == unit;
    }

    @Override
    public String toString()
	{
        //if (text == null) {
            //if (unit == DEFAULT_UNIT)
                //text = Long.toString(value);
            //else
                //text = Long.toString(value) + unit;
        //}
        return text;
    }
}
