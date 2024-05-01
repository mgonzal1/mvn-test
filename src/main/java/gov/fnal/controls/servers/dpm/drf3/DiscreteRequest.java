//  $Id: DiscreteRequest.java,v 1.2 2024/01/23 23:34:55 kingc Exp $
package gov.fnal.controls.servers.dpm.drf3;

import java.util.Objects;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscreteRequest extends DataRequest
{
	private static final Pattern RE = Pattern.compile("(.{3,}?)"
			+ "(?:\\.(\\w+))?" + "(\\[[\\d:]*\\]|\\{[\\d:]*\\})?"
			+ "(?:\\.(\\w+))?" + "(?:@(.+))?");

	private static final Range DEFAULT_RANGE = new ArrayRange(0, 0);

	private static final Event DEFAULT_EVENT = new DefaultEvent();

	private static final long serialVersionUID = 1;

	protected final String device, deviceUC;
	protected final String[] fields = new String[2];
	protected final Range range;
	protected final Event event;
	protected final String modelStr;

	protected transient String text = null;

	static DataRequest parseDiscrete(String str) throws RequestFormatException {

		if (str == null) {
			throw new NullPointerException();
		}

		final String[] split = str.split("<-");

		final Matcher m = RE.matcher(split[0]);
		if (!m.matches()) {
			throw new RequestFormatException("Invalid request: \"" + str + "\"");
		}

		final String device = m.group(1);
		final String field1 = m.group(2);
		final String range = m.group(3);
		final String field2 = m.group(4);
		final String event = m.group(5);
		final String model = (split.length == 2 ? split[1] : "");

		return new DiscreteRequest(device, field1, range, field2, event, model);
	}

	protected DiscreteRequest(String str) throws RequestFormatException
	{
		final String[] split = str.split("<-");
		final Matcher m = RE.matcher(split[0]);

		if (!m.matches())
			throw new RequestFormatException("Invalid request: \"" + str + "\"");

		this.device = DeviceParser.parse(m.group(1));
		this.deviceUC = this.device.toUpperCase();

		String s = m.group(2); 
		this.fields[0] = (s == null ? "" : s);

		s = m.group(4);
		this.fields[1] = (s == null ? "" : s);

		s = m.group(3);
		this.range = (s == null) ? DEFAULT_RANGE : Range.parse(s);

		s = m.group(5);
		this.event = (s == null) ? DEFAULT_EVENT : Event.parse(s);

		this.modelStr = (split.length == 2 ? split[1] : "");
	}

	private DiscreteRequest(String device, String field1, String range, 
								String field2, String event, String model)
	{
		this.device = DeviceParser.parse(device);
		this.deviceUC = this.device.toUpperCase();
		this.fields[0] = (field1 == null ? "" : field1);
		this.fields[1] = (field2 == null ? "" : field2);
		this.range = (range == null) ? DEFAULT_RANGE : Range.parse(range);
		this.event = (event == null) ? DEFAULT_EVENT : Event.parse(event);
		this.modelStr = model;
	}

	public DiscreteRequest(String device, String property, Range range,
			String field, Event event) throws RequestFormatException,
			IllegalArgumentException {
		if (device == null || range == null || event == null) {
			throw new NullPointerException();
		}
		this.device = DeviceParser.parse(device);
		this.deviceUC = this.device.toUpperCase();
		this.range = range;
		this.event = event;
		this.modelStr = "";
	}

	public String getDevice()
	{
		return deviceUC;
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
		return Objects.hash(deviceUC, fields[0], fields[1], range, event, modelStr);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;

		if (obj instanceof DiscreteRequest) {
			final DiscreteRequest r = (DiscreteRequest) obj;

			return r.deviceUC.equals(deviceUC) &&
					r.fields[0].equals(fields[0]) &&
					r.range.equals(range) &&
					r.fields[1].equals(fields[1]) &&
					r.event.equals(event) &&
					r.modelStr.equals(modelStr);
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

			if (!event.equals(DEFAULT_EVENT)) {
				buf.append('@');
				buf.append(event);
			}

			buf.append(modelStr);

			text = buf.toString();
		}
		return text;
	}
}
