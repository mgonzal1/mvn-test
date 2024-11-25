//  $Id: TimeFreqUnit.java,v 1.2 2024/09/23 18:56:04 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public enum TimeFreqUnit
{
    SECONDS     ( true,  "S", 1000000 ), 
    MILLISECONDS( true,  "M", 1000 ), 
    MICROSECONDS( true,  "U", 1 ), 
    HERTZ       ( false, "H", 1 ), 
    KILOHERTZ   ( false, "K", 1000 );
    
    public static TimeFreqUnit parse(String str) throws RequestFormatException
	{
        if (str == null)
            throw new NullPointerException();
        
        for (TimeFreqUnit u : TimeFreqUnit.values()) {
            if (u.text.equalsIgnoreCase(str)) {
                return u;
            }
        }
        throw new RequestFormatException("Invalid time or frequency unit: " + str);
    }

    private final boolean timeMode;
    private final String text;
    private final long factor;
    
    private TimeFreqUnit(boolean timeMode, String text, long factor)
	{
        this.timeMode = timeMode;
        this.text = text;
        this.factor = factor;
    }
    
    public boolean isTime()
	{
        return timeMode;
    }
    
    public long getFactor()
	{
        return factor;
    }
    
    @Override
    public String toString()
	{
        return text;
    }
}
