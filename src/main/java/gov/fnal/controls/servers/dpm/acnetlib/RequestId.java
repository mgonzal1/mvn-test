// $Id: RequestId.java,v 1.2 2023/12/13 17:04:49 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.Objects;

public final class RequestId implements AcnetErrors
{
	//final int node;
	final int id;

	RequestId(int id)
	{
	//	this.node = 0;
		this.id = id & 0xffff;
	}

	//RequestId(int node, int id)
	//{
	//	this.node = node;
	//	this.id = id & 0xffff;
	//}

	public int value()
	{
		return id;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;

		if (o instanceof RequestId)
			return ((RequestId) o).id == id;// &&
	//				((RequestId) o).node == node;

		return false;
	}

	@Override
	public int hashCode()
	{
		//return Objects.hash(node, id);
		return Objects.hash(id);
	}

	@Override
	public String toString()
	{
		//return String.format("0x%04x 0x%04x", node, id);
		return String.format("0x%04x", id);
	}
}
