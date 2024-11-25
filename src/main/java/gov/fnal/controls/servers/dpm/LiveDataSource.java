// $Id: LiveDataSource.java,v 1.1 2024/11/19 22:34:43 kingc Exp $
package gov.fnal.controls.servers.dpm;
 
public class LiveDataSource extends DataSource
{
	public static final LiveDataSource instance = new LiveDataSource();

	protected LiveDataSource() { }

	@Override
	public Job createJob(DPMList list)
	{
		return new AcceleratorJob(list);
	}

	@Override
	public boolean equals(Object o)
	{
		return o == instance;
	}

	@Override
	public int hashCode()
	{
		return getClass().hashCode();
	}

	@Override
	public String toString()
	{
		return "LiveData";
	}
}
