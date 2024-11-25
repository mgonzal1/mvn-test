// $Id: Request.java,v 1.2 2024/11/19 22:34:44 kingc Exp $
package gov.fnal.controls.servers.dpm.scope;

import gov.fnal.controls.service.proto.DPMScope;

class Request
{
	interface ComparatorInterface extends java.util.Comparator<Request>
	{
	}

	static abstract class Comparator implements ComparatorInterface
	{
		boolean order = true;
	}

	static final Comparator refIdComparator = new Comparator() {
		public int compare(Request r1, Request r2)
		{
			return (int) (order ? r1.refId - r2.refId :
							r2.refId - r1.refId);
		}
	};

	static final Comparator nameComparator = new Comparator() {
		public int compare(Request r1, Request r2)
		{
			return order ? r1.name.compareTo(r2.name) :
							r2.name.compareTo(r1.name);
		}
	};

	static final Comparator propertyComparator = new Comparator() {
		public int compare(Request r1, Request r2)
		{
			return order ? r1.property.compareTo(r2.property) :
							r2.property.compareTo(r1.property);
		}
	};

	static final Comparator lengthComparator = new Comparator() {
		public int compare(Request r1, Request r2)
		{
			return order ? r1.length - r2.length :
							r2.length - r1.length;
		}
	};

	static final Comparator offsetComparator = new Comparator() {
		public int compare(Request r1, Request r2)
		{
			return order ? r1.offset - r2.offset :
							r2.offset - r1.offset;
		}
	};

	static final Comparator foreignComparator = new Comparator() {
		public int compare(Request r1, Request r2)
		{
			return order ? r1.foreign.compareTo(r2.foreign) :
							r2.foreign.compareTo(r1.foreign);
		}
	};

	static final Comparator eventComparator = new Comparator() {
		public int compare(Request r1, Request r2)
		{
			return order ? r1.event.compareTo(r2.event) :
							r2.event.compareTo(r1.event);
		}
	};

	static final Comparator dataSourceComparator = new Comparator() {
		public int compare(Request r1, Request r2)
		{
			return order ? r1.dataSource.compareTo(r2.dataSource) :
							r2.dataSource.compareTo(r1.dataSource);
		}
	};

	static final Comparator settingsComparator = new Comparator() {
		public int compare(Request r1, Request r2)
		{
			return order ? r1.settings - r2.settings :
							r2.settings - r1.settings;
		}
	};


	final long refId;
	final String name;
	final String property;
	final int length;
	final int offset;
	final String event;
	final String dataSource;
	final String foreign;
	final int settings;

	Request(DPMScope.DrfRequest r)
	{
		this.refId = r.refId;
		this.name = r.name;
		this.property = r.property;
		this.length = r.length;
		this.offset = r.offset;
		this.event = r.event;
		this.dataSource = r.model;
		this.settings = r.setCount;

		if (!r.foreignType.equalsIgnoreCase("acnet")) {
			this.foreign = "[" + r.foreignType.toUpperCase() + "]" + r.foreignName; 
		} else
			this.foreign = "";
	}
}
