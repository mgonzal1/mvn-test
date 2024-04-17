//  $Id: FullRange.java,v 1.2 2024/03/05 17:19:45 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class FullRange extends Range
{
    public FullRange() {}

    @Override
    public RangeType getType()
	{
        return RangeType.FULL;
    }

    @Override
    public int getStartIndex()
	{
        return 0;
    }

    @Override
    public int getEndIndex()
	{
        return MAXIMUM;
    }

    @Override
    public int getLength()
	{
        return MAXIMUM;
    }

    @Override
    public int hashCode()
	{
        return 0;
    }

    @Override
    public boolean equals(Object obj)
	{
        return (obj instanceof FullRange);
    }

    @Override
    public String toString()
	{
        return "[]";
    }
}
