//  $Id: DataRequest.java,v 1.6 2024/11/19 22:34:43 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataRequest
{
	private static final Pattern RE = Pattern.compile("(.{3,}?)"
			+ "(?:\\.(\\w+))?" + "(\\[[\\d:]*\\]|\\{[\\d:]*\\})?"
			+ "(?:\\.(\\w+))?" + "(?:@(.+))?");

	private static final Range DEFAULT_RANGE = new ArrayRange(0, 0);
	private static final Event DEFAULT_EVENT = new DefaultEvent();

	protected final String device;
	protected final String[] fields;
	protected final Range range;
	protected final Event event;
	protected final String dataSource;

	protected transient String text = null;

	protected DataRequest(String str, Event event) throws RequestFormatException
	{
		final String[] split = str.split("<-");
		final Matcher m = RE.matcher(split[0]);

		if (!m.matches())
			throw new RequestFormatException("Invalid request: \"" + str + "\"");

		this.device = DeviceParser.parse(m.group(1));
		this.fields = new String[2];

		String s = m.group(2); 
		this.fields[0] = (s == null ? "" : s);

		s = m.group(4);
		this.fields[1] = (s == null ? "" : s);

		s = m.group(3);
		this.range = (s == null) ? DEFAULT_RANGE : Range.parse(s);

		s = m.group(5);
		this.event = (event == null) ? ((s == null) ? DEFAULT_EVENT : Event.parse(s)) : event;

		this.dataSource = (split.length == 2 ? split[1] : "");
	}

	protected DataRequest(String str) throws RequestFormatException
	{
		this(str, null);
	}

	public String getDevice()
	{
		return device;
	}

	public String[] getFields()
	{
		return fields;
	}

	public Range getRange()
	{
		return range;
	}

	public boolean hasRange()
	{
		return range != DEFAULT_RANGE;
	}

	public Event getEvent()
	{
		return event;
	}

	public boolean hasEvent()
	{
		return event != DEFAULT_EVENT;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(device, fields[0], fields[1], fields[2], range, event, dataSource);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;

		if (obj instanceof DataRequest) {
			final DataRequest r = (DataRequest) obj;

			return r.device.equals(device) &&
					r.fields[0].equals(fields[0]) &&
					r.range.equals(range) &&
					r.fields[1].equals(fields[1]) &&
					r.fields[2].equals(fields[2]) &&
					r.event.equals(event) &&
					r.dataSource.equals(dataSource);
		}

		return false;
	}

	@Override
	public String toString()
	{
		if (text == null) {
			final StringBuilder buf = new StringBuilder();

			buf.append(device);

			if (!fields[0].isEmpty())
				buf.append('.');

			buf.append(fields[0]);

			if (!range.equals(DEFAULT_RANGE)) {
				buf.append(range);
			}

			if (!fields[1].isEmpty())
				buf.append(".");

			buf.append(fields[1]);

			//if (!fields[2].isEmpty())
				//buf.append(".");

			//buf.append(fields[2]);

			if (!event.equals(DEFAULT_EVENT)) {
				buf.append('@');
				buf.append(event);
			}

			if (!dataSource.isEmpty())
				buf.append("<-");

			buf.append(dataSource);

			text = buf.toString();
		}

		return text;
	}
}
