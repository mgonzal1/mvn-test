// $Id: ReplyId.java,v 1.3 2024/02/22 16:29:45 kingc Exp $
package gov.fnal.controls.servers.dpm.acnetlib;

import java.util.Objects;

public final class ReplyId
{
	final int node;
	final int id;

	ReplyId(int id)
	{
		this.node = 0;
		this.id = id & 0xffff;
	}

	ReplyId(int node, int id)
	{
		this.node = node;
		this.id = id & 0xffff;
	}

	public int value()
	{
		return id;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;

		if (o instanceof ReplyId)
			return ((ReplyId) o).id == id &&
					((ReplyId) o).node == node;

		return false;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(node, id);
	}

	@Override
	public String toString()
	{
		try {
			return String.format("%s 0x%04x", Node.get(node), id);
		} catch (Exception e) {
			return String.format("0x%04x 0x%04x", node, id);
		}
	}
}
