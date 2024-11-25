//  $Id: Property.java,v 1.5 2024/09/23 18:56:04 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import java.util.Map;
import java.util.HashMap;

public enum Property {
	UNKNOWN(0),
    ANALOG(1),
    CONTROL(3),
    STATUS(4),
    DIGITAL(5),
    READING(12),
    SETTING(13),
    DESCRIPTION(15),
    BIT_STATUS(22),
    INDEX(23),
    LONG_NAME(24),
    ALARM_LIST_NAME(25);

	public final int pi;

	Property(int pi)
	{
		this.pi = pi;
	}
    
    private static final Map<String,Property> ALIASES = new HashMap<String,Property>() {{
        put( "READING", READING );
        put( "READ", READING );
        put( "PRREAD", READING );
        put( "SETTING", SETTING );
        put( "SET", SETTING );
        put( "PRSET", SETTING );
        put( "STATUS", STATUS );
        put( "BASIC_STATUS", STATUS );
        put( "STS", STATUS );
        put( "PRBSTS", STATUS );
        put( "CONTROL", CONTROL );
        put( "BASIC_CONTROL", CONTROL );
        put( "CTRL", CONTROL ); 
        put( "PRBCTL", CONTROL );
        put( "ANALOG", ANALOG );
        put( "ANALOG_ALARM", ANALOG );
        put( "AA", ANALOG );
        put( "PRANAB", ANALOG );
        put( "DIGITAL", DIGITAL );
        put( "DIGITAL_ALARM", DIGITAL );
        put( "DA", DIGITAL ); 
        put( "PRDABL", DIGITAL );
        put( "DESCRIPTION", DESCRIPTION );
        put( "DESC", DESCRIPTION );
        put( "PRDESC", DESCRIPTION );
        put( "INDEX", INDEX );
        put( "IDX", INDEX );
        put( "PRIDX", INDEX );
        put( "LONG_NAME", LONG_NAME );
        put( "LNGNAM", LONG_NAME );
        put( "PRLNAM", LONG_NAME );
        put( "ALARM_LIST_NAME", ALARM_LIST_NAME );
        put( "LSTNAM", ALARM_LIST_NAME );
        put( "PRALNM", ALARM_LIST_NAME );
    }};

    private static final Map<Character, Property> DEFAULT_PROPS = new HashMap<Character, Property>() {
        {
            put(':', READING);
            put('?', READING);
            put('_', SETTING);
            put('|', STATUS);
            put('&', CONTROL);
            put('@', ANALOG);
            put('$', DIGITAL);
            put('~', DESCRIPTION);
            put('^', INDEX);
            put('#', LONG_NAME);
            put('!', ALARM_LIST_NAME);
        }
    };

    /**
     * Converts the text string into a <code>Property</code> object.
     *
     * @param str a property string; not <code>null</code>.
     * @return A <code>Property</code> object.
     * @throws PropertyFormatException if the property format is invalid.
     */
    public static Property parse(String s) throws PropertyFormatException
	{
        Property prop = ALIASES.get(s.toUpperCase());

        if (prop == null)
            throw new PropertyFormatException( "Invalid property: \"" + s + "\"" );
        
        return prop;
    }
    
    public static boolean isProperty(String s)
	{
        return ALIASES.containsKey(s.toUpperCase());
    }

    public static Property getDefaultProperty(String device)
	{
        if (device.length() > 2) {
            Property prop = DEFAULT_PROPS.get(device.charAt(1));

            if (prop != null)
                return prop;
        }
        return READING;
    }
}
