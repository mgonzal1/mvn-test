//  $Id: Range.java,v 1.2 2024/03/05 17:19:45 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Range implements Comparable<Range>
{ 
    public static final int MAXIMUM = Integer.MIN_VALUE;

    private static final Pattern RE = Pattern.compile(
        "(\\[(\\d*)(?::(\\d*))?\\])|(\\{(\\d*)(?::(\\d*))?\\})"
    );
    
    /**
     * Converts the text string into a <code>Range</code> object.
     *
     * @param str a range string; not <code>null</code>.
     * @return A <code>Range</code> object.
     * @throws RequestFormatException if the range format is invalid.
     */
    public static Range parse( String str ) throws RangeFormatException
	{
        if (str == null) {
            throw new NullPointerException();
        }
        Matcher m = RE.matcher( str );
        if (!m.matches()) {
            throw new RangeFormatException( "Invalid range: "  + str );
        }
        try {
            if (m.group( 1 ) != null) {
                return createArrayRange( m.group( 2 ), m.group( 3 ));
            } else {
                return createByteRange( m.group( 5 ), m.group( 6 ));
            }
        } catch (IllegalArgumentException ex) {
            throw new RangeFormatException( ex );
        }
    }
    
    private static Range createArrayRange( String s1, String s2 ) throws RequestFormatException
	{
        if (s1.isEmpty()  && (s2 == null || s2.isEmpty())) {
            return new FullRange();
        }
        int i1 = s1.isEmpty() ? 0 : Integer.parseInt( s1 );
        int i2;
        if (s2 == null) {
            i2 = i1;
        } else {
            i2 = s2.isEmpty() ? Range.MAXIMUM : Integer.parseInt( s2 );
        }
        if (i1 == 0 && i2 == Range.MAXIMUM) {
            return new FullRange();
        } else {
            return new ArrayRange( i1, i2 );
        }
    }

    private static Range createByteRange( String s1, String s2 ) throws RequestFormatException
	{
        if (s1.isEmpty()  && (s2 == null || s2.isEmpty())) {
            return new FullRange();
        }
        int i1 = s1.isEmpty() ? 0 : Integer.parseInt( s1 );
        int i2;
        if (s2 == null) {
            i2 = 1;
        } else {
            i2 = s2.isEmpty() ? Range.MAXIMUM : Integer.parseInt( s2 );
        }
        if (i1 == 0 && i2 == Range.MAXIMUM) {
            return new FullRange();
        } else {
            return new ByteRange( i1, i2 );
        }
    }

    protected Range() {}

    /**
     * Gets the range's type: FULL, ARRAY, and BYTE.
     *
     * @return The range type.
     */
    public abstract RangeType getType();

    /**
     * Gets the start index.
     *
     * @return The start index.
     */
    public abstract int getStartIndex();

    /**
     * Gets the end index.
     *
     * @return The end index or <code>MAXIMUM</code>.
     */
    public abstract int getEndIndex();

    /**
     * Gets the offset (same as the start index).
     *
     * @return The offset.
     */
    public final int getOffset()
	{
        return getStartIndex();
    }

    /**
     * Gets the length.
     *
     * @return The length or <code>MAXIMUM</code>.
     */
    public abstract int getLength();

	@Override
	public int compareTo(Range r)
	{
		return toString().compareTo(r.toString());
	}
}
