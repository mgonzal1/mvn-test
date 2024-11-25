// $Id: DisposedModel.java,v 1.3 2024/11/22 20:04:25 kingc Exp $
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

class DisposedModel extends AbstractTableModel
{
	private class Renderer extends DefaultTableCellRenderer
	{
		final Color color = new Color(0.90f, 0.90f, 1.f);
        final Font font = new Font("Dialog", Font.BOLD, 12);

		@Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                								boolean isSelected, boolean hasFocus, int row, int col)
        {
            final Component c = super.getTableCellRendererComponent(table, value, isSelected,
                    													false, row, col);

			try {
				c.setBackground(((row & 1) > 0) ? color : Color.WHITE);
			} catch (Exception ignore) {
			}

			if (c instanceof JLabel && col == PropertiesColumn)
				((JLabel) c).setToolTipText(value.toString());
			else
				((JLabel) c).setToolTipText(null);

            return c;
        }
	}

	private final String[] columnNames = {
		"List Id", "Created", "Disposed", "Status",
		"Duration", "Request Count", "Max Replies/Second", "Host",
		"User", "Properties"
	};

	private final Class[] classTypes = { 
		String.class, String.class, String.class, String.class,
		String.class, Integer.class, Integer.class, String.class,
		String.class, String.class
	};

	private final List.Comparator[] comparators = {
		List.idComparator, List.createdComparator, 
		List.disposedComparator, List.disposedStatusComparator,
		List.durationComparator, List.requestCountComparator,
		List.repliesPerSecondMaxComparator, List.hostNameComparator,
		List.userNameComparator, List.propertiesComparator
	};

	private final boolean[] order = {
		true, true, true, true,
		true, true, true, true,
		true, true
	};

	private final int[][] columnCharWidths = {
		{ 6, 10, 6 },
		{ 17, 20, 17 },
		{ 17, 20, 17 },
		{ 15, 20, 15 },
		{ 15, 22, 15 },
		{ 14, 12, 14 },
		{ 18, 24, 18 },
		{ 20, 24, 20 },
		{ 20, 24, 20 },
	};

	private static final int IdColumn = 0;
	private static final int CreatedColumn = 1;
	private static final int DisposedColumn = 2;
	private static final int DisposedStatus = 3;
	private static final int DurationColumn = 4;
	private static final int RequestCountColumn = 5;
	private static final int MaxRepliesPerSecondColumn = 6;
	private static final int HostNameColumn = 7;
	private static final int UserColumn = 8;
	private static final int PropertiesColumn = 9;

	private final ArrayList<List> lists = new ArrayList<>();

	private int rowOf(int id, long created)
	{
		for (int ii = 0; ii < lists.size(); ii++) {
			final List list = lists.get(ii);

			if (list.id() == id && list.created == created)
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

	TableCellRenderer renderer()
	{
		return new Renderer();
	}

	synchronized void sortOnColumn(int col)
	{
		Collections.sort(lists, getComparatorForColumn(col));
		fireTableDataChanged();
	}

	synchronized void add(List list)
	{
		final int row = rowOf(list.id(), list.created);
		
		if (row != -1) {
			lists.set(row, list);
			fireTableRowsUpdated(row, row);
			logger.log(Level.INFO, String.format("Update for list id:0x%04x", list.id()));
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

	synchronized void clear()
	{
		lists.clear();
		fireTableDataChanged();
	}

	synchronized void handle(DPMScope.Reply.Requests m)
	{
		final int row = rowOf(m.id, m.date);

		if (row != -1) {
			final List list = lists.get(row);

			if (list.requests.size() == 0)
				list.requestCount = 0;

			list.requestCount += m.requests.length;

			for (DPMScope.DrfRequest r : m.requests)
				list.requests.add(new Request(r));	
		}
	}

	synchronized void handle(DPMScope.Reply.SettingRequests m)
	{
		final int row = rowOf(m.id, m.date);

		if (row != -1) {
			final List list = lists.get(row);

			if (list.settingRequests.size() == 0)
				list.settingCount = 0;

			for (DPMScope.DrfRequest r : m.requests) {
				list.settingCount += r.setCount;
				list.settingRequests.add(new Request(r));	
			}
		}
	}

	@Override
	public String getColumnName(int col)
	{
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
			 case IdColumn:
				return String.format("0x%04x", list.id());
			 case CreatedColumn:
				return DPMScopeMain.dateFormat.format(list.created);
			 case DisposedColumn:
				return DPMScopeMain.dateFormat.format(list.disposed);
			 case DisposedStatus:
				return list.disposedStatus;
			 case DurationColumn:
				return DPMInfo.duration(list.duration());
			 case RequestCountColumn:
				return list.requestCount;
			 case MaxRepliesPerSecondColumn:
				return list.repliesPerSecondMax;
			 case HostNameColumn:
				return list.hostName;
			 case UserColumn:
				return list.userName();
			 case PropertiesColumn:
				return list.properties;
			}
		} catch (Exception ignore) {
		}

		return "-";
	}

	@Override
	public boolean isCellEditable(int row, int col)
	{
		return false;
	}
}
