// $Id: StateEvent.java,v 1.5 2024/01/05 21:32:13 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

//import gov.fnal.controls.servers.dpm.pools.acnet.Lookup;
import gov.fnal.controls.servers.dpm.pools.DeviceCache;

public class StateEvent implements DataEvent
{
	public static final int FLAG_EQUALS = 1;
	public static final int FLAG_NOT_EQUALS = 2;
	public static final int FLAG_ALL_VALUES = 4;
	public static final int FLAG_GREATER_THAN = 8;
	public static final int FLAG_LESS_THAN = 16;

	public final String name;
	public final int di;
	public final int state;
	public final long delay;
	public final int flag;

	public StateEvent(int di, int state, long delay, int flag)
	{
		this.di = di;
		this.name = name(di);
		this.state = state;
		this.delay = delay;
		this.flag = flag;
	}

	public StateEvent(String name, int state, long delay, int flag)
	{
		this.name = name;
		this.di = di(name);
		this.state = state;
		this.delay = delay;
		this.flag = flag;
	}

	@Override
	public boolean isRepetitive()
	{
		return true;
	}

	//public void addObserver(DataEventObserver observer)
	//{
	//}

	/**
	 * converts a device index to device name
	 * 
	 * @param di
	 *            ACNET device index
	 * @return converted ACNET device name or null if conversion failed
	 */
	static private String name(int di)
	{
		try {
			return DeviceCache.name(di);
		} catch (Exception e) {
			return "" + di;
		}
	}

	/**
	 * converts a device name to device index
	 * 
	 * @param name
	 *            ACNET device name
	 * @return ACNET device index
	 */
	private int di(String name)
	{
		try {
			//return Lookup.getDeviceInfo(name).di;
			return DeviceCache.di(name);
		} catch (Exception e) {
			return -1;
		}
	}

	//public void deleteObserver(DataEventObserver observer)
	//{
		//stateEventDecoder.deleteObserver(observer, this);
	//}

	/**
	 * @return description of string
     * @exception Exception
	 */
	public String description() throws Exception
	{
		throw new Exception("description() not implemented");
		//return StatesDescription.description(DI, state);
	}

	/**
	 * returns the device index 'DI' associated with this StateEvent
	 * 
	 * @return device index
	 */
	//public int deviceIndex()
	//{
	//	return di;
	//}

	//public String deviceName()
//	{
//		return name;
//	}

//	public int flag()
//	{
//		return flag;
//	}

	private static String interpretFlag(int flag)
	{
		switch (flag) {
		case FLAG_EQUALS:
			return "=";
		case FLAG_NOT_EQUALS:
			return "!=";
		case FLAG_GREATER_THAN:
			return ">";
		case (FLAG_GREATER_THAN | FLAG_EQUALS):
			return ">=";
		case FLAG_LESS_THAN:
			return "<";
		case (FLAG_LESS_THAN | FLAG_EQUALS):
			return "<=";
		default:
			return "*";
		}
	}

//	public int state()
//	{
//		return state;
//	}

	@Override
    public String toString()
	{
        return "s," + di + "," + state + "," + delay + "," + interpretFlag(flag);
    }
}
