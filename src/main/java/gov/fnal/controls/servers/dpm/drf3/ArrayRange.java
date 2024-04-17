//  $Id: ArrayRange.java,v 1.1 2023/10/04 19:38:05 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class ArrayRange extends Range {
    
    private static final int MAX_INDEX = 32767;

    private final int startIndex, endIndex;
    private transient String text;

    public ArrayRange( int startIndex, int endIndex )
	{
        if (startIndex < 0 || startIndex > MAX_INDEX) {
            throw new IllegalArgumentException( "Invalid start index: " + startIndex );
        }
        if (endIndex != MAXIMUM && (endIndex < startIndex || endIndex > MAX_INDEX)) {
            throw new IllegalArgumentException( "Invalid end index: " + endIndex );
        }
        if (startIndex == 0 && endIndex == MAXIMUM) {
            throw new IllegalArgumentException( "Invalid full range" );
        }
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    public RangeType getType()
	{
        return RangeType.ARRAY;
    }

    @Override
    public int getStartIndex()
	{
        return startIndex;
    }

    @Override
    public int getEndIndex()
	{
        return endIndex;
    }

    @Override
    public int getLength()
	{
        return (endIndex == MAXIMUM) ? MAXIMUM : endIndex - startIndex + 1;
    }

    @Override
    public int hashCode()
	{
        return startIndex ^ endIndex;
    }

    @Override
    public boolean equals( Object obj )
	{
        return (obj instanceof ArrayRange)
                && ((ArrayRange)obj).startIndex == startIndex
                && ((ArrayRange)obj).endIndex == endIndex;
    }

    @Override
    public String toString()
	{
        if (text == null) {
            if (endIndex == MAXIMUM) {
                text = "[" + Integer.toString( startIndex ) + ":]";
            } else if (startIndex != endIndex) {
                text = "[" + Integer.toString( startIndex ) + ":" + Integer.toString( endIndex ) + "]";
            } else {
                text = "[" + Integer.toString( startIndex ) + "]";
            }
        }
        return text;
    }
}
