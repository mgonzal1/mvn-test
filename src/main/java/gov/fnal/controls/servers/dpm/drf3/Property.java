//  $Id: Property.java,v 1.2 2023/06/12 16:42:21 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import java.util.HashMap;
import java.util.Map;

/**
 * <i>Property</i> attributes in <a href="http://www-bd.fnal.gov/controls/papers/drf2">DRF2</a>.
 * <p>
 * The name of each element in this enumeration corresponds to a canonical
 * property name.
 *
 */

public enum Property {
    ANALOG(false, 1),
    CONTROL(false, 3),
    STATUS(false, 4),
    DIGITAL(false, 5),
    READING(false, 12),
    SETTING(false, 13),
    DESCRIPTION(true, 15),
    BIT_STATUS(true, 22),
    INDEX(true, 23),
    LONG_NAME(true, 24),
    ALARM_LIST_NAME(true, 25);

	public final boolean fromDatabase;
	public final int indexValue;

	Property(boolean fromDatabase, int indexValue)
	{
		this.fromDatabase = fromDatabase;
		this.indexValue = indexValue;
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
