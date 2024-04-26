//  $Id: Field.java,v 1.1 2022/11/01 20:42:17 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import java.util.HashMap;
import java.util.Map;
import java.util.EnumMap;

import static gov.fnal.controls.servers.dpm.drf3.Property.*;

public enum Field {

    RAW,
    PRIMARY,
    SCALED,
    ALL,
    EXTENDED_TEXT,
    TEXT,
    ON,
    READY,
    REMOTE,
    POSITIVE,
    RAMP,
    MIN,
    MAX,
    NOM,
    TOL,
    RAW_MIN,
    RAW_MAX,
    RAW_NOM,
    RAW_TOL,
    ALARM_ENABLE,
    ALARM_STATUS,
    TRIES_NEEDED,
    TRIES_NOW,
    ALARM_FTD,
    ABORT,
    ABORT_INHIBIT,
    FLAGS,
    MASK,
	BIT_VALUE;
    
    private static final Map<String,Field> ALIASES = new HashMap<String,Field>() {{
        put( "RAW", RAW );
        put( "PRIMARY", PRIMARY );
        put( "VOLTS", PRIMARY );
        put( "SCALED", SCALED );
        put( "COMMON", SCALED );
        put( "ALL", ALL );
        put( "TEXT", TEXT );        
        put( "EXTENDED_TEXT", EXTENDED_TEXT );
        put( "ON", ON );
        put( "READY", READY );
        put( "REMOTE", REMOTE );
        put( "POSITIVE", POSITIVE );
        put( "RAMP", RAMP );
        put( "MIN", MIN );
        put( "MINIMUM", MIN );
        put( "MAX", MAX );
        put( "MAXIMUM", MAX );
        put( "NOM", NOM );
        put( "NOMINAL", NOM );
        put( "TOL", TOL );
        put( "TOLERANCE", TOL );
        put( "RAW_MIN", RAW_MIN );
        put( "RAWMIN", RAW_MIN );
        put( "RAW_MAX", RAW_MAX );
        put( "RAWMAX", RAW_MAX );
        put( "RAW_NOM", RAW_NOM );
        put( "RAWNOM", RAW_NOM );
        put( "RAW_TOL", RAW_TOL );
        put( "RAWTOL", RAW_TOL );
        put( "ALARM_ENABLE", ALARM_ENABLE );
        put( "ENABLE", ALARM_ENABLE );
        put( "ALARM_STATUS", ALARM_STATUS );
        put( "STATUS", ALARM_STATUS );
        put( "TRIES_NEEDED", TRIES_NEEDED );
        put( "TRIES_NOW", TRIES_NOW );
        put( "ALARM_FTD", ALARM_FTD );
        put( "FTD", ALARM_FTD );
        put( "ABORT", ABORT );
        put( "ABORT_INHIBIT", ABORT_INHIBIT );
        put( "FLAGS", FLAGS );
        put( "MASK", MASK );
		put( "BIT_VALUE", BIT_VALUE);
    }};
   
    private static final Map<Property, Field> DEFAULT_FIELDS = new EnumMap<Property, Field>(
            Property.class) {
        {
            put(READING, SCALED);
            put(SETTING, SCALED);
            put(STATUS, ALL);
            put(CONTROL, SCALED);
            put(ANALOG, ALL);
            put(DIGITAL, ALL);
            put(DESCRIPTION, SCALED);
            put(INDEX, SCALED);
            put(LONG_NAME, SCALED);
            put(ALARM_LIST_NAME, SCALED);
        }
    };

    static Field getDefaultField(Property prop)
	{
        return DEFAULT_FIELDS.get(prop);
    }

	String fieldText;

    /**
     * Converts the text string into a <code>Field</code> object.
     *
     * @param str a field string; not <code>null</code>.
     * @return A <code>Field</code> object.
     * @throws RequestFormatException if the field format is invalid.
     */
    public static Field parse(String str) { // throws FieldFormatException {
        if (str == null) {
            throw new NullPointerException();
        }
        Field field = ALIASES.get(str.toUpperCase());
        if (field == null) {
            throw new FieldFormatException( "Invalid field: \"" + str + "\"" );
        }
        return field;
    }
}
