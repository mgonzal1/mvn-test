//  $Id: ClockType.java,v 1.1 2023/10/04 19:13:42 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public enum ClockType
{
    HARDWARE( "H" ),
    SOFTWARE( "S" ),
    EITHER( "E" );

    public static ClockType parse(String clockStr) throws RequestFormatException
	{
        if (clockStr == null) {
            throw new NullPointerException();
        }
        if (clockStr.length() > 0) {
            switch (Character.toUpperCase( clockStr.charAt( 0 ))) {
                case 'H' :
                    return HARDWARE;
                case 'S':
                    return SOFTWARE;
                case 'E':
                    return EITHER;
            }
        }
        throw new RequestFormatException( "Invalid clock type: " + clockStr );
    }

    private final String symbol;

    private ClockType( String symbol )
	{
        this.symbol = symbol;
    }

    @Override
    public String toString()
	{
        return symbol;
    }

}
