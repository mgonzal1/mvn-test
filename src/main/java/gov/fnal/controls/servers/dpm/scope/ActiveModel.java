// $Id: ActiveModel.java,v 1.3 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.scope;

import java.util.*;
import java.util.logging.*;
import java.text.SimpleDateFormat;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import gov.fnal.controls.service.proto.DPMScope;
import static gov.fnal.controls.servers.dpm.scope.DPMScopeMain.logger;

class ActiveModel extends AbstractTableModel
{
	private class Renderer extends DefaultTableCellRenderer
	{
		final Color color = new Color(0.90f, 0.90f, 1.f);
        final Font font = new Font("Dialog", Font.BOLD, 12);

		@Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                								boolean isSelected, boolean hasFocus, int row, int col)
        {
            Component c = super.getTableCellRendererComponent(table, value, isSelected,
                    											false, row, col);

			try {
				c.setBackground(((row & 1) > 0) ? color : Color.WHITE);

				if (col == IdColumn) {
					final List list = lists.get(row);

					if (list.age() < 10 * 60 * 1000)
						c.setBackground(Color.GREEN);
				}
			} catch (Exception ignore) {
			}

			if (c instanceof JLabel && col == PropertiesColumn)
				((JLabel) c).setToolTipText(value.toString());
			else
				((JLabel) c).setToolTipText(null);

            return c;
        }
	}

	private final ArrayList<List> lists = new ArrayList<>();

	private final String[] columnNames = {
		"Debug", "List Id", "Created", "Request Count", "Replies/Second",
		"Max Replies/Second", "Settings", "Host", "User", "Properties"
	};

	private final Class[] classTypes = { 
		String.class, String.class, String.class, Integer.class, Integer.class,
		Integer.class, Integer.class, String.class, String.class, String.class
	};

	private final List.Comparator[] comparators = {
		List.debugComparator, List.idComparator, List.createdComparator, 
		List.requestCountComparator, List.repliesPerSecondComparator,
		List.repliesPerSecondMaxComparator, List.settingCountComparator,
		List.hostNameComparator, List.userNameComparator, List.propertiesComparator
	};

	private final boolean[] order = {
		true, true, true, true, true,
		true, true, true, true, true
	};
	
	private final int[][] columnCharWidths = {
		{ 0, 15, 0 },
		{ 6, 10, 6 },
		{ 17, 20, 17 },
		{ 13, 17, 13 },
		{ 14, 18, 14 },
		{ 18, 22, 18 },
		{ 8, 12, 8 },
		{ 20, 24, 20 },
		{ 8, 12, 8 },
	};

	private static final int DebugColumn = 0;
	private static final int IdColumn = 1;
	private static final int CreatedColumn = 2;
	private static final int RequestCountColumn = 3;
	private static final int RepliesPerSecondColumn = 4;
	private static final int MaxRepliesPerSecondColumn = 5;
	private static final int SettingsColumn = 6;
	private static final int HostNameColumn = 7;
	private static final int UserColumn = 8;
	private static final int PropertiesColumn = 9;

	private int rowOf(int id)
	{
		for (int ii = 0; ii < lists.size(); ii++) {
			if (lists.get(ii).id() == id)
				return ii;
		}

		return -1;
	}

	List.Comparator getComparatorForColumn(int col)
	{
		final List.Comparator c = comparators[col];

		c.order = this.order[col];
		this.order[col] = !this.order[col];

		return c;
	}

	int[][] getColumnCharWidths()
	{
		return columnCharWidths;
	}

	synchronized void clear()
	{
		lists.clear();
		fireTableDataChanged();
	}

	synchronized void add(List list)
	{
		final int row = rowOf(list.id());
		
		if (row != -1) {
			lists.set(row, list);
			fireTableRowsUpdated(row, row);
		} else {
			lists.add(list);
			fireTableRowsInserted(lists.size() - 1, lists.size() - 1);
		}
	}

	synchronized List get(int row)
	{
		return lists.get(row);
	}

	synchronized int size()
	{
		return lists.size();
	}

	TableCellRenderer renderer()
	{
		return new Renderer();
	}

	synchronized void sortOnColumn(int col)
	{
		Collections.sort(lists, getComparatorForColumn(col));
		fireTableDataChanged();
	}

	synchronized void handle(DPMScope.Reply.ListStarted m)
	{
		final int row = rowOf(m.id);
		
		if (row != -1) {
			final List list = lists.get(row);

			list.requests.clear();
			list.requestCount = m.requestCount;
			fireTableCellUpdated(row, RequestCountColumn);
		} else {
			final List list = List.create(m.id);		

			list.requestCount = m.requestCount;
			add(list);
		}
	}

	synchronized void handle(DPMScope.Reply.ListSettingsStarted m)
	{
		final int row = rowOf(m.id);
		
		if (row != -1) {
			final List list = lists.get(row);

			list.requestCount = m.requestCount;
			fireTableCellUpdated(row, RequestCountColumn);
		} else {
			final List list = List.create(m.id);		

			list.requestCount = m.requestCount;
			add(list);
		}
	}

	synchronized void handle(DPMScope.Reply.ListSettingsComplete m)
	{
		final int row = rowOf(m.id);
		
		if (row != -1) {
			final List list = lists.get(row);

			list.settingCount = 0;
			list.settingRequests.clear();
		} else
			add(List.create(m.id));
	}

	synchronized void handle(DPMScope.Reply.ListProperties m)
	{
		final int row = rowOf(m.id);
		
		if (row != -1) {
			final List list = lists.get(row);

			list.setProperties(m.properties);
			fireTableCellUpdated(row, PropertiesColumn);
			fireTableCellUpdated(row, UserColumn);
		} else {
			final List list = List.create(m.id);		
				
			list.setProperties(m.properties);
			add(list);
		}
	}

	synchronized void handle(DPMScope.Reply.ListReplyCount m)
	{
		final int row = rowOf(m.id);
		
		if (row != -1) {
			final List list = lists.get(row);

			list.repliesPerSecond = m.repliesPerSecond;
			list.repliesPerSecondMax = m.repliesPerSecondMax;
			fireTableCellUpdated(row, RepliesPerSecondColumn);
			fireTableCellUpdated(row, MaxRepliesPerSecondColumn);
		} else {
			final List list = List.create(m.id);		
				
			list.repliesPerSecond = m.repliesPerSecond;
			list.repliesPerSecondMax = m.repliesPerSecondMax;
			add(list);
		}
	}

	synchronized void handle(DPMScope.Reply.ListHost m)
	{
		final int row = rowOf(m.id);
		
		if (row != -1) {
			final List list = lists.get(row);

			list.hostName = m.hostName;
			fireTableCellUpdated(row, HostNameColumn);
		} else {
			final List list = List.create(m.id);		
				
			list.hostName = m.hostName;
			add(list);
		}
	}

	synchronized void handle(DPMScope.Reply.ListUser m)
	{
		final int row = rowOf(m.id);
		
		if (row != -1) {
			final List list = lists.get(row);

			list.setUserName(m.userName);
			fireTableCellUpdated(row, UserColumn);
		} else {
			final List list = List.create(m.id);		
				
			list.setUserName(m.userName);
			add(list);
		}
	}

	synchronized void handle(DPMScope.Reply.Requests m)
	{
		final int row = rowOf(m.id);

		if (row != -1) {
			final List list = lists.get(row);

			if (list.requests.size() == 0)
				list.requestCount = 0;

			list.requestCount += m.requests.length;

			for (DPMScope.DrfRequest r : m.requests) {
				list.requests.add(new Request(r));	
			}
		}
	}

	synchronized void handle(DPMScope.Reply.SettingRequests m)
	{
		final int row = rowOf(m.id);

		if (row != -1) {
			final List list = lists.get(row);

			if (list.settingRequests.size() == 0)
				list.settingCount = 0;

			for (DPMScope.DrfRequest r : m.requests) {
				list.settingCount += r.setCount;
				list.settingRequests.add(new Request(r));	
			}

			fireTableCellUpdated(row, SettingsColumn);
		}
	}

	synchronized List handle(DPMScope.Reply.ListDisposed m)
	{
		int row = rowOf(m.id);
		
		if (row != -1) {
			final List list = lists.remove(row);

			fireTableRowsDeleted(row, row);
			list.disposed = m.date;
			list.setDisposedStatus(m.status);

			return list;
		}

		return null;
	}


	@Override
	public String getColumnName(int col)
	{
		if (col == DebugColumn) {
			try {
				final List list = lists.get(0);

				if (list != null && list.remoteId != 0)
					return "Remote ListId";
			} catch (Exception e) {
			}

			return "";
		}

		return columnNames[col];
	}

	@Override
	public int getColumnCount()
	{
		return columnNames.length;
	}

	@Override
	public Class getColumnClass(int col)
	{
		return classTypes[col];
	}

	@Override
	synchronized public int getRowCount()
	{
		return lists.size();
	}

	@Override
	synchronized public Object getValueAt(int row, int col) 
	{
		try {
			final List list = lists.get(row);

			switch (col) {
			 case DebugColumn:
			 	{
					if (list.remoteId != 0)
						return String.format("0x%04x", list.remoteId);

					return "";
				}
			 case IdColumn:
				return String.format("0x%04x", list.id());
			 case CreatedColumn:
				return DPMScopeMain.dateFormat.format(list.created);
			 case RequestCountColumn:
				return list.requestCount;
			 case RepliesPerSecondColumn:
				return list.repliesPerSecond;
			 case MaxRepliesPerSecondColumn:
				return list.repliesPerSecondMax;
			 case SettingsColumn: 
				return list.settingCount;
			 case HostNameColumn:
				return list.hostName;
			 case UserColumn:
				return list.userName();
			 case PropertiesColumn:
				return list.properties;
			}
		} catch (Exception e) {
		}
		
		return "-";
	}

	@Override
	public boolean isCellEditable(int row, int col)
	{
		return false;
	}
}
