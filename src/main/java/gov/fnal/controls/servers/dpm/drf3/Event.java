//  $Id: Event.java,v 1.3 2024/09/23 18:56:04 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

public abstract class Event implements Comparable<Event>
{
    public static Event parse(String str) throws EventFormatException
	{
        if (str.length() > 0) {
            switch (str.charAt(0)) {
                case 'U':
                case 'u':
                    return DefaultEvent.parseDefault(str);
                case 'I':
                case 'i':
                    return ImmediateEvent.parseImmediate(str);
                case 'P':
                case 'p':
                case 'Q':
                case 'q':
                    return PeriodicEvent.parsePeriodic(str);
                case 'E':
                case 'e':
                    return ClockEvent.parseClock(str);
                case 'S':
                case 's':
                    return StateEvent.parseState(str);
				case 'N':
				case 'n':
					return NeverEvent.parseNever(str);
            }
        }

        throw new EventFormatException("Invalid event: \"" + str + "\"");
    }

    protected Event() {}

	public abstract boolean isRepetitive();
	public abstract long defaultTimeout();

	@Override
	public int compareTo(Event event)
	{
		return toString().compareTo(event.toString());
	}
}
