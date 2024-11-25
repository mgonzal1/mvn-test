//  $Id: StateEvent.java,v 1.2 2024/09/23 18:56:04 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StateEvent extends Event
{
    private static final int MAX_VALUE = 65535;
    
    private static final Pattern RE = Pattern.compile(
        "(?i)S,(\\S+),(\\d+),(\\w+),(=|!=|\\*|>|<|<=|>=)" 
    );
    
    static Event parseState(String str) throws EventFormatException
	{
        final Matcher m = RE.matcher(str);
        if (!m.matches())
            throw new EventFormatException("Invalid state event: \"" + str + "\"");
        
        try {
            return new StateEvent(
                m.group(1),                   // Device
                Integer.parseInt(m.group(2)), // Value
                TimeFreq.parse(m.group(3)),   // Delay
                StateExpr.parse(m.group(4))   // Expression
            );
        } catch (IllegalArgumentException ex) {
            throw new EventFormatException("Invalid event parameter", ex);
        }
    }

    private final String device, deviceUC;
    private final int value;
    private final TimeFreq delay;
    private final StateExpr expr;

    private transient String text;

    public StateEvent(String device, int value, TimeFreq delay, StateExpr expr)
	{
        if (device == null || delay == null || expr == null) {
            throw new NullPointerException();
        }
        if (value < 0 || value > MAX_VALUE) {
            throw new IllegalArgumentException( "Invalid value" );
        }
        this.device = DeviceParser.parse(device);
        this.deviceUC = device.toUpperCase();
        this.value = value;
        this.delay = delay;
        this.expr = expr;
    }

	public StateEvent(String device, int value, TimeFreq delay, int exprValue)
	{
		this(device, value, delay, StateExpr.fromValue(exprValue));
	}

    //@Deprecated
    //public String getSource()
	//{
     //   return device;
    //}
    
    public String getDevice()
	{
        return device;
    }

    public int getValue()
	{
        return value;
    }

    public long getDelay()
	{
        return delay.getTimeMillis();
    }
    
    public TimeFreq getDelayValue()
	{
        return delay;
    }

    public StateExpr getExpression()
	{
        return expr;
    }

	@Override
	public boolean isRepetitive()
	{
		return true;
	}

	@Override
	public long defaultTimeout()
	{
		return 0;
	}

    @Override
    public int hashCode()
	{
        return deviceUC.hashCode() ^ value ^ delay.hashCode() ^ expr.hashCode();
    }

    @Override
    public boolean equals( Object obj )
	{
        return (obj instanceof StateEvent)
            && ((StateEvent)obj).deviceUC.equals( deviceUC )
            && ((StateEvent)obj).value == value
            && ((StateEvent)obj).delay.equals( delay )
            && ((StateEvent)obj).expr == expr;
    }

    @Override
    public String toString()
	{
        if (text == null) {
            StringBuilder buf = new StringBuilder();
            buf.append( "S," );
            buf.append( device );
            buf.append( "," );
            buf.append( value );
            buf.append( "," );
            buf.append( delay );
            buf.append( "," );
            buf.append( expr );
            text = buf.toString();
        }
        return text;
    }
}
