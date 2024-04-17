//  $Id: DataRequest.java,v 1.1 2023/10/04 19:13:42 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public abstract class DataRequest 
{
    public static DataRequest parse(String s) throws RequestFormatException
	{
        return DiscreteRequest.parseDiscrete(s);
    }

    protected DataRequest() {}
}
