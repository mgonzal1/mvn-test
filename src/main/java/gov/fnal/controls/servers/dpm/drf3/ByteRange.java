//  $Id: ByteRange.java,v 1.1 2023/10/04 19:38:05 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class ByteRange extends Range 
{
    private static final long MAX_UPPER_BOUND = 2147483648L;
    
    private final int offset, length;
    private transient String text;

    public ByteRange( int offset, int length ) {
        if (offset < 0) {
            throw new IllegalArgumentException( "Invalid offset: " + offset );
        }
        if (length != MAXIMUM && length < 0) {
            throw new IllegalArgumentException( "Invalid length: " + length );
        }
        if (length != MAXIMUM && (long)offset + (long)length > MAX_UPPER_BOUND) {
            throw new IllegalArgumentException( "Invalid upper bound: " + offset + "+" + length );
        }
        if (offset == 0 && length == MAXIMUM) {
            throw new IllegalArgumentException( "Invalid full range" );
        }
        this.offset = offset;
        this.length = length;
    }

    @Override
    public RangeType getType() 
	{
        return RangeType.BYTE;
    }

    @Override
    public int getStartIndex() 
	{
        return offset;
    }

    @Override
    public int getEndIndex() 
	{
        return (length == MAXIMUM) ? MAXIMUM : offset + length - 1;
    }

    @Override
    public int getLength() 
	{
        return length;
    }

    @Override
    public int hashCode() 
	{
        return offset ^ length;
    }

    @Override
    public boolean equals( Object obj ) 
	{
        return (obj instanceof ByteRange)
                && ((ByteRange)obj).offset == offset
                && ((ByteRange)obj).length == length;
    }

    @Override
    public String toString() 
	{
        if (text == null) {
            if (length == MAXIMUM) {
                text = "{" + Integer.toString( offset ) + ":}";
            } else if (length != 1) {
                text = "{" + Integer.toString( offset ) + ":" + Integer.toString( length ) + "}";
            } else {
                text = "{" + Integer.toString( offset ) + "}";
            }
        }
        return text;
    }
}
