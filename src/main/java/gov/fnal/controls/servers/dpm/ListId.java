// $Id: ListId.java,v 1.1 2021/07/16 15:29:30 kingc Exp $
package gov.fnal.controls.servers.dpm;

public class ListId implements Comparable<ListId>
{
	final private int value;

	public ListId(int value)
	{
		this.value = value;
	}

	@Override
	final public int hashCode()
	{
		return value;
	}

	@Override
	final public boolean equals(Object o)
	{
		if (o instanceof ListId)
			return ((ListId) o).value == value;

		return false;
	}
			
	@Override
	final public int compareTo(ListId that)
	{
		if (this.value < that.value) 
			return -1;
		else if (this.value > that.value)
			return 1;
		
		return 0;
	}

	@Override
	final public String toString()
	{
		return String.format("0x%04x", value);
	}

	final public int value()
	{
		return value;
	}
}

