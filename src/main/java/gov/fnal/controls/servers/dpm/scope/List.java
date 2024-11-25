// $Id
package gov.fnal.controls.servers.dpm.scope;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

import gov.fnal.controls.servers.dpm.Errors;
import gov.fnal.controls.service.proto.DPMScope;

class List
{
	interface ComparatorInterface extends java.util.Comparator<List>
	{
	}

	static abstract class Comparator implements ComparatorInterface
	{
		boolean order = true;
	}

	static final Comparator debugComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			if (list1.remoteId != 0 && list2.remoteId != 0)
				return order ? list1.remoteId - list2.remoteId :
								list2.remoteId - list1.remoteId;

			return 0;
		}
	};

	static final Comparator idComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return order ? list1.id() - list2.id() :
							list2.id() - list1.id();
		}
	};

	static final Comparator createdComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return (int) (order ? list1.created - list2.created :
									list2.created - list1.created);
		}
	};

	static final Comparator hostNameComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return order ? list1.hostName.compareTo(list2.hostName) :
							list2.hostName.compareTo(list1.hostName);
		}
	};

	static final Comparator requestCountComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return order ? list1.requestCount - list2.requestCount :
							list2.requestCount - list1.requestCount;
		}
	};

	static final Comparator repliesPerSecondComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return order ? list1.repliesPerSecond - list2.repliesPerSecond :
							list2.repliesPerSecond - list1.repliesPerSecond;
		}
	};

	static final Comparator repliesPerSecondMaxComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return order ? list1.repliesPerSecondMax - list2.repliesPerSecondMax :
							list2.repliesPerSecondMax - list1.repliesPerSecondMax;
		}
	};

	static final Comparator userNameComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return order ? list1.userName().compareTo(list2.userName()) :
							list2.userName().compareTo(list1.userName());
		}
	};

	static final Comparator propertiesComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return order ? list1.properties.compareTo(list2.properties) :
							list2.properties.compareTo(list1.properties);
		}
	};

	static final Comparator disposedComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return (int) (order ? list1.disposed - list2.disposed : 
									list2.disposed - list1.disposed);
		}
	};

	static final Comparator durationComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return (int) (order ? list1.duration() - list2.duration() : 
									list2.duration() - list1.duration());
		}
	};

	static final Comparator settingCountComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return (int) (order ? list1.settingCount - list2.settingCount : 
									list2.settingCount - list1.settingCount);
		}
	};

	static final Comparator disposedStatusComparator = new Comparator() {
		public int compare(List list1, List list2)
		{
			return order ? list1.disposedStatus.compareTo(list2.disposedStatus) :
							list2.disposedStatus.compareTo(list1.disposedStatus);
		}
	};

	private static final HashMap<Integer, List> lists = new HashMap<>();

	final private int id;
	final long created;

	int remoteId = 0;
	String hostName = "";
	int requestCount = 0;
	int repliesPerSecond = 0;
	int repliesPerSecondMax = 0;
	String properties = "";
	long disposed = 0;
	String disposedStatus = "";
	final ArrayList<Request> requests = new ArrayList<>();
	final ArrayList<Request> settingRequests = new ArrayList<>();
	int settingCount = 0;

	private String userName = "";
	private final HashMap<String, String> propertyMap = new HashMap<>();

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof List) {
			final List list = (List) o;

			return id() == list.id() && created == list.created;
		}

		return false;
	}
	
	static List get(int id)
	{
		return lists.get(id);
	}

	static List create(int id)
	{
		return create(id, System.currentTimeMillis());
	}

	static List create(int id, long created)
	{
		final List list = new List(id, created);

		lists.put(id, list);
		return list;
	}

	static List create(DPMScope.Reply.List m)
	{
		final List list = create(m.id, m.date);

		list.setProperties(m.properties);
		list.hostName = m.hostName;
		list.setUserName(m.userName);
		list.repliesPerSecond = m.repliesPerSecond;
		list.repliesPerSecondMax = m.repliesPerSecondMax;
		list.disposed = m.disposedDate;
		list.setDisposedStatus(m.disposedStatus);
		list.requestCount = m.requestCount;

		return list;
	}

	private List(int id, long created)
	{
		this.id = id;
		this.created = created;
	}

	int id()
	{
		//try {
			//return Integer.parseInt(propertyMap.get("ID"), 16);
		//} catch (Exception ignore) { }

		return this.id;
	}

	void setDisposedStatus(int status)
	{
		disposedStatus = (status == 0 ? "CANCELLED" : Errors.name(status));
	}

	long age()
	{
		return System.currentTimeMillis() - created;
	}

	String userName()
	{
		if (userName.isEmpty() && propertyMap.containsKey("USER"))
			return propertyMap.get("USER");

		return userName;
	}

	void setUserName(String userName)
	{
		this.userName = userName;
	}

	long duration()
	{
		if (disposed != 0)
			return disposed - created;

		return System.currentTimeMillis() - created;
	}

	boolean disposed()
	{
		return disposed != 0;
	}

	synchronized void setProperties(String[] properties)
	{
		final StringBuilder buf = new StringBuilder("<html>");

		this.propertyMap.clear();

		for (String s : properties) {
			final String[] sp = s.split(":");

			if (sp.length == 2) {
				this.propertyMap.put(sp[0], sp[1]);

				buf.append(sp[0]);
				buf.append(':');
				buf.append("<strong>");
				buf.append(sp[1]);
				buf.append("</strong>");
				buf.append(' ');
			}
		}

		buf.append("</html>");
		this.properties = buf.toString();

		try {
			remoteId = Integer.parseInt(propertyMap.get("ID"), 16);
		} catch (Exception e) {
			remoteId = 0;
		}
	}
}
