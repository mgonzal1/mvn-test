// $Id: DataEventFactory.java,v 1.5 2024/01/10 20:57:18 kingc Exp $
package gov.fnal.controls.servers.dpm.events;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

public final class DataEventFactory
{
	static final String format = "EEE MMM dd HH:mm:ss zzz yyyy";
	static final SimpleDateFormat formatter = new SimpleDateFormat(format);

	private static class TrimmingStringTokenizer extends StringTokenizer {
		TrimmingStringTokenizer(String str, String delim) {
			super(str, delim);
		}

		public String nextToken() {
			return super.nextToken().trim();
		}
	}

	private DataEventFactory() { }

	public static DataEvent stringToEvent(String string) throws ParseException, NumberFormatException
	{
		StringTokenizer st = new TrimmingStringTokenizer(string.trim(), ",");

		long delay = 0;
		boolean bFlag;

		if (!st.hasMoreTokens())
			throw new ParseException(string, 0);

		final String s = st.nextToken();

		switch (s.charAt(0)) {
		case 'u':
		case 'U':
			return new DefaultDataEvent();

		case 'a':
		case 'A': {
			final long t1 = formatter.parse(st.nextToken(), new ParsePosition(0)).getTime();
			final long before = Long.parseLong(st.nextToken());
			final long after = Long.parseLong(st.nextToken());

			//return new AbsoluteTimeEvent(t1, before, after);
			return new AbsoluteTimeEvent(t1);
		}

        case 'c':
        case 'C':
            if (s.toLowerCase().startsWith("client"))
				return new DataLoggerClientLoggingDataEvent();
            else 
				throw new ParseException(string, 0);

		case 'd':
		case 'D': {
			long t1 = formatter.parse(st.nextToken(), new ParsePosition(0)).getTime();
			long t2 = formatter.parse(st.nextToken(), new ParsePosition(0)).getTime();
			delay = (long) Long.parseLong(st.nextToken());

			return new DeltaTimeEvent(t1, t2, delay);
		}

		case 'e':
		case 'E':
			int event = Integer.parseInt(st.nextToken(), 16);
			String hard = st.nextToken().toLowerCase();
			boolean h = false,
			sub = false;

			if (hard.equals("e") || hard.equals("n")) {
				sub = true;
				h = true;
			} else if (hard.equals("h"))
				h = true;
			else if (!hard.equals("s"))
				throw new ParseException(string, 2);

			if (st.hasMoreElements())
				delay = (long) Long.parseLong(st.nextToken());
			else
				delay = 0;

			return new ClockEvent(event & 0xff, h, delay, sub);

		case 'i':
		case 'I':
			return new OnceImmediateEvent();

		case 'm':
		case 'M':
			return new MultipleImmediateEvent();

		case 'n':
		case 'N':
			return new NeverEvent();

		case 'P':
		case 'p':
			delay = (long) Long.parseLong(st.nextToken());
			bFlag = Boolean.valueOf(st.nextToken()).booleanValue();
			return new DeltaTimeEvent(delay, bFlag);

		case 'Q':
		case 'q':
			delay = (long) Long.parseLong(st.nextToken());
			bFlag = Boolean.valueOf(st.nextToken()).booleanValue();
			return new MonitorChangeEvent(delay, bFlag);

		case 's':
		case 'S':
			String tok = st.nextToken();
			int deviceIndex = 0;
			String deviceName = null;
			try {
				deviceIndex = Integer.parseInt(tok);
                if (deviceIndex == -1) throw new ParseException(string, 0);
			} catch (Exception e) {
				deviceName = tok;
			}
			short value = Short.parseShort(st.nextToken());
			delay = Long.parseLong(st.nextToken());
			String how = st.nextToken();
			byte flag = 0;
			if (how.equals("!="))
				flag = StateEvent.FLAG_NOT_EQUALS;
			else if (how.equals("*"))
				flag = StateEvent.FLAG_ALL_VALUES;
			else if (how.equals("="))
				flag = StateEvent.FLAG_EQUALS;
			else if (how.equals(">="))
				flag = StateEvent.FLAG_GREATER_THAN | StateEvent.FLAG_EQUALS;
			else if (how.equals(">"))
				flag = StateEvent.FLAG_GREATER_THAN;
			else if (how.equals("<="))
				flag = StateEvent.FLAG_LESS_THAN | StateEvent.FLAG_EQUALS;
			else if (how.equals("<"))
				flag = StateEvent.FLAG_LESS_THAN;
			else
				throw new ParseException(string, 0);

			if (deviceName != null)
				return new StateEvent(deviceName, value, delay, flag);
			else
				return new StateEvent(deviceIndex, value, delay, flag);

		default:
			throw new ParseException(string, 0);
		}
	}

	public static void main(String[] args) throws ParseException
	{
		stringToEvent("s,V:PING,13,0,*");
	}
}
