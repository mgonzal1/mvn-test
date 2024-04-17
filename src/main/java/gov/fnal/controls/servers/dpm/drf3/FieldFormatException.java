//  $Id: FieldFormatException.java,v 1.2 2023/10/04 19:38:05 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public class FieldFormatException extends RequestFormatException
{
    public FieldFormatException()
	{
        super();
    }

    public FieldFormatException( String message )
	{
        super(message);
    }

    public FieldFormatException( Throwable cause )
	{
        super(cause);
    }

    public FieldFormatException( String message, Throwable cause )
	{
        super(message, cause);
    }
}
