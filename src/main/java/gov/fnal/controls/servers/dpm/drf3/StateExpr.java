//  $Id: StateExpr.java,v 1.1 2023/10/04 19:38:05 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public enum StateExpr
{
    EQUAL_EXPR            ( "="  ),
    NOT_EQUAL_EXPR        ( "!=" ),
    ANY_EXPR              ( "*"  ),
    GREATER_EXPR          ( ">"  ),
    GREATER_OR_EQUAL_EXPR ( ">=" ),
    LESS_EXPR             ( "<"  ),
    LESS_OR_EQUAL_EXPR    ( "<=" );

    public static StateExpr parse(String exprStr) throws RequestFormatException
	{
        if (exprStr == null) {
            throw new NullPointerException();
        }
        for (StateExpr e : StateExpr.values()) {
            if (exprStr.equals( e.toString())) {
                return e;
            }
        }
        throw new RequestFormatException( "Invalid state expression: " + exprStr );
    }

    private final String symbol;

    private StateExpr( String symbol )
	{
        this.symbol = symbol;
    }

    @Override
    public String toString()
	{
        return symbol;
    }
}
