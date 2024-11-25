//  $Id: StateExpr.java,v 1.2 2024/09/23 18:56:04 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public enum StateExpr
{
    EQUAL_EXPR            ("=", 0x01),
    NOT_EQUAL_EXPR        ("!=",0x02),
    ANY_EXPR              ("*", 0x04),
    GREATER_EXPR          (">", 0x08),
    GREATER_OR_EQUAL_EXPR (">=", 0x81),
    LESS_EXPR             ("<", 0x10),
    LESS_OR_EQUAL_EXPR    ("<=", 0x11);

    public static StateExpr parse(String exprStr) throws RequestFormatException
	{
        if (exprStr == null)
            throw new NullPointerException();
        
        for (StateExpr e : StateExpr.values()) {
            if (exprStr.equals( e.toString()))
                return e;
        }
        throw new RequestFormatException( "Invalid state expression: " + exprStr );
    }

    private final String symbol;
	private int exprVal;

    private StateExpr(String symbol, int exprVal)
	{
        this.symbol = symbol;
		this.exprVal = exprVal;
    }

	static StateExpr fromValue(int exprVal)
	{
		for (StateExpr e : StateExpr.values())
			if (e.exprVal == exprVal)
				return e;

		return ANY_EXPR;
	}

    @Override
    public String toString()
	{
        return symbol;
    }
}
