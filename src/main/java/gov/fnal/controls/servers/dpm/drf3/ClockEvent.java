//  $Id: ClockEvent.java,v 1.1 2023/10/04 19:13:42 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClockEvent extends Event
{    
    private static final ClockType DEFAULT_TYPE = ClockType.EITHER;
    private static final TimeFreq DEFAULT_DELAY = new TimeFreq( 0 );
    
    private static final int MAX_EVT_NUM = 0xFFFF;
    
    private static final Pattern RE = Pattern.compile(
        "(?i)E,([0-9A-F]+)(?:,([HSE])(?:,(\\w+))?)?"
    );
    
    static ClockEvent parseClock(String str) throws EventFormatException {
        Matcher m = RE.matcher(str);
        if (!m.matches()) {
            throw new EventFormatException( "Invalid clock event: \"" + str + "\"" );
        }
        try {
            int evt = Integer.parseInt( m.group( 1 ), 16 );
            if (m.group( 2 ) != null) {
                ClockType type = ClockType.parse( m.group( 2 ));
                if (m.group( 3 ) != null) {
                    TimeFreq delay = TimeFreq.parse( m.group( 3 ));
                    return new ClockEvent( evt, type, delay );
                } else {
                    return new ClockEvent( evt, type );
                }
            } else {
                return new ClockEvent( evt );
            }
        } catch (IllegalArgumentException ex) {
            throw new EventFormatException( ex );
        }
    }

    private final int evtNum;
    private final TimeFreq delay;
    private final ClockType type;

    private transient String text;

    public ClockEvent( int evtNum ) {
        this( evtNum, DEFAULT_TYPE );
    }

    public ClockEvent( int evtNum, ClockType type ) {
        this( evtNum, type, DEFAULT_DELAY );
    }

    public ClockEvent( int evtNum, ClockType type, TimeFreq delay ) {
        if (type == null || delay == null) {
            throw new NullPointerException();
        }
        if (evtNum < 0 || evtNum > MAX_EVT_NUM) {
            throw new IllegalArgumentException( "Invalid event number" );
        }
        this.evtNum = evtNum;
        this.type = type;
        this.delay = delay;
    }

    public int getEventNumber() {
        return evtNum;
    }

    public ClockType getClockType() {
        return type;
    }

    @Deprecated
    public long getDelay() {
        return delay.getTimeMillis();
    }
    
    public TimeFreq getDelayValue() {
        return delay;
    }

    @Override
    public int hashCode() {
        return evtNum ^ type.hashCode() ^ delay.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        return (obj instanceof ClockEvent)
            && ((ClockEvent)obj).evtNum == evtNum
            && ((ClockEvent)obj).type == type
            && ((ClockEvent)obj).delay.equals( delay );
    }

    @Override
    public String toString() {
        if (text == null) {
            StringBuilder buf = new StringBuilder();
            buf.append( "E," );
            buf.append( Integer.toHexString( evtNum ).toUpperCase());
            buf.append( "," );
            buf.append( type );
            buf.append( "," );
            buf.append( delay );
            text = buf.toString();
        }
        return text;
    }

}
