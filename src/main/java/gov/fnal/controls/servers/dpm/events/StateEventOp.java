// $Id: StateEventOp.java,v 1.1 2023/06/12 16:38:35 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

public enum StateEventOp
{
	EQUALS("="),
	NOT_EQUALS("!="),
	GREATER_THAN(">"),
	GREATER_THAN_OR_EQUALS(">="),
	LESS_THAN("<"),
	LESS_THAN_OR_EQUALS("<="),
	ALL_VALUES("*");

	final String opString;

	StateEventOp(String opString)
	{
		this.opString = opString;
	}

	@Override
	public String toString()
	{
		return opString;
	}

	public static StateEventOp get(String opString)
	{
		for (StateEventOp op : StateEventOp.values()) {
			if (op.equals(opString))
				return op;
		}

		return ALL_VALUES;
	}
}
