// $Id: DefaultDataEvent.java,v 1.2 2023/06/12 16:37:55 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

public class DefaultDataEvent implements DataEvent
{
	/**
	 * Set event to repetitive.
	 */
	//public DefaultDataEvent()
	//{
	//	setRepetitive();
	//}

	@Override
	public boolean isRepetitive()
	{
		return true;
	}

	@Override
	public String toString()
	{
		return ("u");
	}

	//public void addObserver(DataEventObserver observer) {
		// TODO: what does this method do? 199.01.19 KDE
	//}

	//public void deleteObserver(DataEventObserver observer) {
		// TODO: what does this method do? 199.01.19 KDE
	//}
}

