// $Id: AcnetRequest.java,v 1.2 2023/10/04 19:38:05 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collection;
import java.util.Collections;
import static java.util.Arrays.asList;

import static gov.fnal.controls.servers.dpm.drf3.Property.*;
import static gov.fnal.controls.servers.dpm.drf3.Field.*;

public class AcnetRequest
{
    protected static final Pattern RE = Pattern.compile(
        "(?i)[A-Z0#][:?_|&@$~][A-Z0-9_:-]{1,62}"
    );
    
    protected static final Map<Property,Character> QUALIFIERS = Collections.unmodifiableMap( 
            new EnumMap<Property,Character>( Property.class ) {{
                put( READING,     ':' );
                put( SETTING,     '_' );
                put( STATUS,      '|' );
                put( CONTROL,     '&' );
                put( ANALOG,      '@' );
                put( DIGITAL,     '$' );
                put( DESCRIPTION, '~' );
        }});

    protected static final Map<Character, Property> DEFAULT_PROPS = Collections.unmodifiableMap(new HashMap<Character, Property>() {
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
    });

    protected static final Map<Property, Collection<Field>> PROP_FIELDS = Collections.unmodifiableMap(new EnumMap<Property, Collection<Field>>(
            Property.class) {
        {
            put(READING, asList(RAW, PRIMARY, SCALED));
            put(SETTING, asList(RAW, PRIMARY, SCALED));
            put(STATUS,
                    asList(RAW, ALL, TEXT, EXTENDED_TEXT, ON, READY, REMOTE,
                            POSITIVE, RAMP, BIT_VALUE));
            put(CONTROL, asList(RAW, SCALED));
            put(ANALOG,
                    asList(RAW, ALL, TEXT, MIN, MAX, NOM, TOL, RAW_MIN,
                            RAW_MAX, RAW_NOM, RAW_TOL, ALARM_ENABLE,
                            ALARM_STATUS, TRIES_NEEDED, TRIES_NOW, ALARM_FTD,
                            ABORT, ABORT_INHIBIT, FLAGS));
            put(DIGITAL,
                    asList(RAW, ALL, TEXT, NOM, MASK, ALARM_ENABLE,
                            ALARM_STATUS, TRIES_NEEDED, TRIES_NOW, ALARM_FTD,
                            ABORT, ABORT_INHIBIT, FLAGS));
            put(DESCRIPTION, asList(SCALED));
            put(INDEX, asList(SCALED));
            put(LONG_NAME, asList(SCALED));
            put(ALARM_LIST_NAME, asList(SCALED));
        }
    });

    /**
     * Validates the <i>device</i> attribute of a data request and converts it
     * to a canonical form.
     *
     * @param str a <i>device</i> attribute; not <code>null</code>.
     * @return A canonical form of the device.
     * @throws DeviceFormatException if the device format is incorrect.
     */
    protected String parse(String s) throws DeviceFormatException
	{
        final Matcher m = RE.matcher(s);
        if (!m.matches())
            throw new DeviceFormatException( "Invalid device: \"" + s + "\"" );
        
        final char[] cc = s.toCharArray();

		// Support the '#' char for device index (for now) but 
		// replace it with '0'

		if (cc[0] == '#')
			cc[0] = '0';

        cc[1] = ':';

        return new String(cc);
    }

    protected static Property getDefaultProperty(String device)
	{
        if (device.length() > 2) {
            Property prop = DEFAULT_PROPS.get(device.charAt(1));
            if (prop != null) {
                return prop;
            }
        }
        return READING;
    }
    
	protected final boolean hasRange;
	protected final boolean hasEvent;

    protected final String device;
    protected final Property property;
    protected final Range range;
    protected final Field field;
    protected final Event event;

    protected transient String text = null;


	public AcnetRequest(DiscreteRequest dReq) throws DeviceFormatException
	{
		this.device = parse(dReq.getDevice()).toUpperCase();

		String deviceAttr = dReq.getDevice();
		String propAttr = dReq.getFields()[0];
		String fieldAttr = dReq.getFields()[1];

		Property prop;
        if (propAttr.isEmpty()) {
            prop = getDefaultProperty(deviceAttr);
        } else if (dReq.hasRange() || !fieldAttr.isEmpty()
                		|| Property.isProperty(propAttr)) {
            prop = Property.parse(propAttr);
        } else {
            prop = getDefaultProperty(deviceAttr);
            fieldAttr = propAttr;
        }

		this.property = prop;
        this.range = dReq.getRange();

        this.field = fieldAttr.isEmpty() ? Field.getDefaultField(prop) : Field.parse(fieldAttr);
        this.event = dReq.getEvent(); 

		this.hasRange = dReq.hasRange();
		this.hasEvent = dReq.hasEvent();

		if (this.device.charAt(1) != ':' && this.property != getDefaultProperty(deviceAttr))
			throw new DeviceFormatException("Property mismatch");
		
		if (!PROP_FIELDS.get(this.property).contains(this.field))
			throw new DeviceFormatException("Illegal field '" + this.field + "'");
	}

    /**
     * Gets the <i>device</i> attribute of this request in a canonical form.
     * 
     * @return The <i>device</i> attribute, not <code>null</code>.
     */
    public String getDevice()
	{
        return device;
    }

    /**
     * Gets the <i>property</i> attribute of this request.
     * 
     * @return The <i>property</i> attribute, not <code>null</code>.
     */
    public Property getProperty()
	{
        return property;
    }

    /**
     * Gets the <i>range</i> attribute of this request.
     * <p>
     * In the case of a default range, which doesn't appear in the canonical
     * request string, this method returns that default value.
     * 
     * @return The <i>range</i> attribute, not <code>null</code>.
     */
    public Range getRange()
	{
        return range;
    }

    /**
     * Gets the <i>field</i> attribute of this request.
     * <p>
     * In the case of a default field, which doesn't appear in the canonical
     * request string, this method returns that default value. If the property
     * doesn't support fields, this method returns <code>null</code>.
     * 
     * @return The <i>field</i> attribute, or <code>null</code> if the property
     *         doesn't support fields.
     */
    public Field getField()
	{
        return field;
    }

    /**
     * Gets the <i>event</i> attribute of this request in a canonical form.
     * 
     * @return The <i>event</i> attribute, not <code>null</code>.
     */
    public Event getEvent()
	{
        return event;
    }

	@Override
	public int hashCode()
	{
		return Objects.hash(device, property, range, field, event);
	}

	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof AcnetRequest)
				&& ((AcnetRequest) obj).device.equals(device)
				&& ((AcnetRequest) obj).property == property
				&& ((AcnetRequest) obj).range.equals(range)
				&& (((AcnetRequest) obj).field != null ? ((AcnetRequest) obj).field
						.equals(field) : field == null)
				&& ((AcnetRequest) obj).event.equals(event);
	}

	/**
	 * Returns the canonical textual form of this request.
	 * <p>
	 * This method is a convenient way to call
	 * {@link DataRequestParser#format(DataRequest)}.
	 * 
	 * @return Canonical data request.
	 * @see DataRequestParser
	 */
	@Override
	public String toString()
	{
		if (text == null) {
			final StringBuilder buf = new StringBuilder();

			buf.append(device);
			buf.append('.');
			buf.append(property);
			if (hasRange) {
				buf.append(range);
			}
			if (field != getDefaultField(property)) {
				buf.append('.');
				buf.append(field);
			}
			if (hasEvent) {
				buf.append('@');
				buf.append(event);
			}
			text = buf.toString();
		}
		return text;
	}

	public String toLoggedString()
	{
        final StringBuilder buf = new StringBuilder(device);

 		//static final char[] ScaledArrayBracketChars = { '[', ']' };


        //int lastArrayIndex = firstArrayIndex + arraySize - 1;

        buf.setCharAt(1, QUALIFIERS.get(property));

		if (hasRange)
			buf.append(range);
/*
        else if (arraySize <= 1) {
            if (firstArrayIndex > 0) {
                buf.append(arrayBracketChars[OpeningBracket]);
                buf.append(Integer.toString(firstArrayIndex));
                buf.append(arrayBracketChars[ClosingBracket]);
            }
        } else {
            buf.append(arrayBracketChars[OpeningBracket]);
            buf.append(Integer.toString(firstArrayIndex));
            buf.append(ArrayIndexSeparatorChar);
            buf.append(Integer.toString(lastArrayIndex));
            buf.append(arrayBracketChars[ClosingBracket]);
        }
		*/

        return buf.toString();                                                                                                                                                        
	}
}
