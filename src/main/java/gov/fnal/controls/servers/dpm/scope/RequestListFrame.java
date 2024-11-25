// $Id: RequestListFrame.java,v 1.2 2024/11/19 22:34:44 kingc Exp $
package gov.fnal.controls.servers.dpm.scope;

import java.util.logging.*;
import java.util.ArrayList;
import java.util.Collections;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import gov.fnal.controls.servers.dpm.Errors;
import gov.fnal.controls.service.proto.DPMScope;

import static gov.fnal.controls.servers.dpm.scope.DPMScopeMain.logger;

class RequestListFrame extends JFrame
{
	private class RequestListModel extends AbstractTableModel
	{
		private final String[] columnNames = {
			"Ref Id", "Name", "Property", "Foreign", 
			"Length", "Offset", "Event", "Data Source"
		};

		private final Class[] classTypes = { 
			Integer.class, String.class, String.class, String.class,
			Integer.class, Integer.class, String.class, String.class
		};

		private final Request.Comparator[] comparators = {
			Request.refIdComparator, Request.nameComparator, 
			Request.propertyComparator, Request.foreignComparator,
			Request.lengthComparator, Request.offsetComparator, 
			Request.eventComparator, Request.dataSourceComparator
		};

		private final boolean[] order = {
			true, true, true, true,
			true, true, true, true
		};

		private final int RefIdColumn = 0;
		private final int NameColumn = 1;
		private final int PropertyColumn = 2;
		private final int ForeignColumn = 3;
		private final int LengthColumn = 4;
		private final int OffsetColumn = 5;
		private final int EventColumn = 6;
		private final int DataSourceColumn = 7;

		Request.Comparator getComparatorForColumn(int col)
		{
			final Request.Comparator c = comparators[col];

			c.order = this.order[col];
			this.order[col] = !this.order[col];

			return c;
		}

		@Override
		public String getColumnName(int col)
		{
			return columnNames[col];
		}

		@Override
		public Class getColumnClass(int col)
		{
			return classTypes[col];
		}

		@Override
		public int getColumnCount()
		{
			return columnNames.length;
		}

		@Override
		public int getRowCount()
		{
			return requests.size();
		}

		@Override
		public Object getValueAt(int row, int col) 
		{
			try {
				final Request request = requests.get(row);
				
				switch (col) {
				 case RefIdColumn:
					return request.refId;
				 case NameColumn:
					return request.name;
				 case PropertyColumn:
					return request.property;	
				 case ForeignColumn:
					return request.foreign;
				 case LengthColumn:
					return request.length;
				 case OffsetColumn:
					return request.offset;
				 case EventColumn:
					return request.event;
				 case DataSourceColumn:
					return request.dataSource;
				}
			} catch (Exception ignore) { }

			return "-";
		}

		@Override
		public boolean isCellEditable(int row, int col)
		{
			return false;
		}
	}

	private class RequestListRenderer extends DefaultTableCellRenderer
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
				if ((row & 1) > 0) {
					c.setBackground(color);
				} else {
					c.setBackground(Color.WHITE);
				}
			} catch (Exception ignore) {
			}

			return c;
		}
	}

	private class SettingRequestListModel extends AbstractTableModel
	{
		private final String[] columnNames = {
			"Ref Id", "Name", "Property", "Foreign",
			"Settings", "Length", "Offset", "Event",
			"Data Source"
		};

		private final Class[] classTypes = { 
			Integer.class, String.class, String.class, String.class,
			Integer.class, Integer.class, Integer.class, String.class,
			String.class
		};

		private final Request.Comparator[] comparators = {
			Request.refIdComparator, Request.nameComparator, 
			Request.propertyComparator, Request.foreignComparator,
			Request.settingsComparator, Request.lengthComparator,
			Request.offsetComparator, Request.eventComparator,
			Request.dataSourceComparator
		};

		private final boolean[] order = {
			true, true, true, true,
			true, true, true, true,
			true
		};

		private final int RefIdColumn = 0;
		private final int NameColumn = 1;
		private final int PropertyColumn = 2;
		private final int ForeignColumn = 3;
		private final int SettingsColumn = 4;
		private final int LengthColumn = 5;
		private final int OffsetColumn = 6;
		private final int EventColumn = 7;
		private final int DataSourceColumn = 8;

		Request.Comparator getComparatorForColumn(int col)
		{
			final Request.Comparator c = comparators[col];

			c.order = this.order[col];
			this.order[col] = !this.order[col];

			return c;
		}

		@Override
		public String getColumnName(int col)
		{
			return columnNames[col];
		}

		@Override
		public Class getColumnClass(int col)
		{
			return classTypes[col];
		}

		@Override
		public int getColumnCount()
		{
			return columnNames.length;
		}

		@Override
		public int getRowCount()
		{
			return settingRequests.size();
		}

		@Override
		public Object getValueAt(int row, int col) 
		{
			try {
				final Request request = settingRequests.get(row);
				
				switch (col) {
				 case RefIdColumn:
					return request.refId;
				 case NameColumn:
					return request.name;
				 case PropertyColumn:
					return request.property;	
				 case ForeignColumn:
					return request.foreign;
				 case SettingsColumn:
				 	return request.settings;
				 case LengthColumn:
					return request.length;
				 case OffsetColumn:
					return request.offset;
				 case EventColumn:
					return request.event;
				 case DataSourceColumn:
					return request.dataSource;
				}
			} catch (Exception ignore) { }

			return "-";
		}

		@Override
		public boolean isCellEditable(int row, int col)
		{
			return false;
		}
	}

	void setList(List list)
	{
		this.list = list;
		requests = new ArrayList<Request>(list.requests);
		settingRequests = new ArrayList<Request>(list.settingRequests);

		setTitle(String.format("%s - Requests list for 0x%04x", listFrame.dpmInfo.nodeName, list.id()));
		requestTitle.setText("Requests - " + requests.size());
		settingsRequestTitle.setText("Setting Requests - " + settingRequests.size());

		requestListModel.fireTableDataChanged();
		settingRequestListModel.fireTableDataChanged();

		disposeButton.setEnabled(!list.disposed());

		toFront();
		invalidate();
		repaint();
		setVisible(true);
	}

	public void handle(DPMScope.Reply.DisposeList m)
	{
		if (!m.succeeded)
			logger.log(Level.WARNING, "Dispose failed");

		disposeButton.setEnabled(false);
	}

	private final ListFrame listFrame;
	private List list;
	private ArrayList<Request> requests = new ArrayList<>();
	private ArrayList<Request> settingRequests = new ArrayList<>();

	private RequestListModel requestListModel = new RequestListModel();
	private SettingRequestListModel settingRequestListModel = new SettingRequestListModel();

	private final JLabel requestTitle;
	private final JLabel settingsRequestTitle;

	private final JButton disposeButton = new JButton("Dispose");
	private	final JComboBox<String> errorComboBox = new JComboBox<>(new String[] { "CANCELLED", "ACNET_DISCONNECTED",
																					"ACNET_SYS" });

	RequestListFrame(ListFrame listFrame) 
	{
		super();
		this.listFrame = listFrame;

		requestTitle = new JLabel();
		settingsRequestTitle = new JLabel();

		final JPanel mainPanel = new JPanel();
		final GridBagConstraints gbc = new GridBagConstraints();

		mainPanel.setLayout(new GridBagLayout());

		// Control elements panel

		{
			final JPanel ctrlsPanel = new JPanel();

			ctrlsPanel.setLayout(new GridLayout(1, 5, 10, 0));

			disposeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					try {
						final DPMScope.Request.DisposeList m = new DPMScope.Request.DisposeList();

						m.id = list.id();

						if (errorComboBox.getSelectedIndex() == 0)
							m.status = 0;
						else
							m.status = Errors.value((String) errorComboBox.getSelectedItem());

						listFrame.sendRequest(m);
					} catch (Exception ignore) { 
						ignore.printStackTrace();
					}
				}
			});
			disposeButton.setEnabled(false);
			ctrlsPanel.add(disposeButton);

			ctrlsPanel.add(errorComboBox);

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.insets = new Insets(8, 5, 8, 5);
			mainPanel.add(ctrlsPanel, gbc);
		}

		// Title over request list

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.insets = new Insets(10, 0, 2, 0);
		mainPanel.add(requestTitle, gbc);

		// Request list table

		{
			final JTable table = new JTable(requestListModel);
			final JScrollPane scrollPane = new JScrollPane(table);
			final JTableHeader header = table.getTableHeader();

			header.setReorderingAllowed(false);
			header.setFont(new Font("Dialog", Font.BOLD, 12));
			header.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent evt)
				{
					if (evt.getClickCount() == 1) {
						final int col = table.columnAtPoint(evt.getPoint());

						Collections.sort(requests, requestListModel.getComparatorForColumn(col));
						requestListModel.fireTableDataChanged();
					}	
				}
			});

			table.setFillsViewportHeight(true);
			table.setDefaultRenderer(String.class, new RequestListRenderer());
			table.setDefaultRenderer(Integer.class, new RequestListRenderer());

			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.fill  = GridBagConstraints.BOTH;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.insets = new Insets(0, 0, 0, 0);
			mainPanel.add(scrollPane, gbc);
		}

		// Title over setting request table

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.fill  = GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(10, 0, 2, 0);
		mainPanel.add(settingsRequestTitle, gbc);	

		// Setting request list table

		{
			final JTable table = new JTable(settingRequestListModel);
			final JScrollPane scrollPane = new JScrollPane(table);
			final JTableHeader header = table.getTableHeader();

			header.setReorderingAllowed(false);
			header.setFont(new Font("Dialog", Font.BOLD, 12));
			header.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent evt)
				{
					if (evt.getClickCount() == 1) {
						final int col = table.columnAtPoint(evt.getPoint());

						Collections.sort(settingRequests, settingRequestListModel.getComparatorForColumn(col));
						settingRequestListModel.fireTableDataChanged();
					}	
				}
			});
			table.setDefaultRenderer(String.class, new RequestListRenderer());
			table.setDefaultRenderer(Integer.class, new RequestListRenderer());

			gbc.gridx = 0;
			gbc.gridy = 4;
			gbc.fill  = GridBagConstraints.BOTH;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.insets = new Insets(0, 0, 14, 0);
			mainPanel.add(scrollPane, gbc);
		}

		add(mainPanel);

		DPMScopeMain.center(this);
		setVisible(true);
	}
}
