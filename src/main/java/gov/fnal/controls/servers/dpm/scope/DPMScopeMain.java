// $Id: DPMScopeMain.java,v 1.4 2024/11/22 20:04:25 kingc Exp $
package gov.fnal.controls.servers.dpm.scope;

import java.util.List;
import java.util.logging.*;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.nio.ByteBuffer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;

import gov.fnal.controls.servers.dpm.Errors;
import gov.fnal.controls.servers.dpm.acnetlib.*;
import gov.fnal.controls.service.proto.DPMScope;

class DPMScopeMain extends Thread implements AcnetReplyHandler, AcnetErrors
{
	public static final Logger logger = Logger.getLogger(DPMScopeMain.class.getName());

    static {
	    System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-8s %1$tF %1$tT %1$tL] %5$s %6$s%n");
	}

	private static Dimension windowDimensions()
	{
    	final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		int w = Integer.MAX_VALUE, h = Integer.MAX_VALUE;

		for (GraphicsDevice gd : ge.getScreenDevices()) {
			final DisplayMode mode = gd.getDisplayMode();

			if (mode.getWidth() < w)
				w = mode.getWidth();
			if (mode.getHeight() < h)
				h = mode.getHeight();
		}    

		return new Dimension((w * 4) / 5, (h * 4) / 5);
	}

	static void center(JFrame frame)
	{
		frame.setPreferredSize(windowDimensions());
		frame.pack();
		frame.setLocationRelativeTo(null);
	}

	private class Requester extends TimerTask
	{
		final ByteBuffer buf = ByteBuffer.allocateDirect(64 * 1024);
		AcnetRequestContext context = new AcnetRequestContext();

		public void run()
		{
			try {
				context.cancel();

				final DPMScope.Request.ServiceDiscovery m = new DPMScope.Request.ServiceDiscovery();

				buf.clear();
				m.marshal(buf).flip();
				context = acnetConnection.requestMultiple("MCAST", "SCOPE", buf, 1800, DPMScopeMain.this); 

				dpmInfoModel.dpmResponseCheck();
				dpmInfoModel.updateTotals();
			} catch (Exception ignore) {
				ignore.printStackTrace();
			}
		}
	}

	private static class HeaderRenderer implements TableCellRenderer
	{
		final DefaultTableCellRenderer renderer;

		public HeaderRenderer(JTable table)
		{
			this.renderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
			this.renderer.setHorizontalAlignment(JLabel.LEFT);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
		{
			return renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
		}
	}

	private final JFrame mainFrame;
	private final DPMInfoModel dpmInfoModel;
	private final int defaultRowHeight;
	private final AcnetConnection acnetConnection;
	private final Timer timer;

	static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yy HH:mm:ss");
	static final Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
	static final Cursor waitCursor = new Cursor(Cursor.WAIT_CURSOR);

	long multiClickInterval;

	private DPMScopeMain() throws Exception
	{
		this.mainFrame = new JFrame();
		this.acnetConnection = AcnetInterface.open();
		this.timer = new java.util.Timer(true);

		center(mainFrame);

		try {
			multiClickInterval = ((Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval")).longValue();
		} catch (Exception e) {
			multiClickInterval = 500;
		}

		Runtime.getRuntime().addShutdownHook(this);
		timer.schedule(new Requester(), 100, 2000);

		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.mainFrame.setTitle("DPMScope"); 

		final JPanel mainPanel = new JPanel();
		final GridBagConstraints gbc = new GridBagConstraints();

		mainPanel.setLayout(new GridBagLayout());

		final JPanel ctrlsPanel = new JPanel();
		final Border border = BorderFactory.createTitledBorder("Refresh");
		
		ctrlsPanel.setBorder(border);
		ctrlsPanel.setLayout(new GridLayout(1, 2, 10, 0));

		final JButton btn1 = new JButton("Console Users");
		btn1.addActionListener(new ActionListener() {
		  		public void actionPerformed(ActionEvent e)
		    	{
					for (DPMInfo dpmInfo : dpmInfoModel)
						dpmInfo.sendRefreshMessage(acnetConnection, btn1.getText());
			  	}
		});
		ctrlsPanel.add(btn1);

		final JButton btn2 = new JButton("Logger Config");
		btn2.addActionListener(new ActionListener() {
		  		public void actionPerformed(ActionEvent e)
		    	{
					for (DPMInfo dpmInfo : dpmInfoModel)
						dpmInfo.sendRefreshMessage(acnetConnection, btn2.getText());
			  	}
		});
		ctrlsPanel.add(btn2);

		final JButton btn3 = new JButton("Exception Stack");
		btn3.addActionListener(new ActionListener() {
		  		public void actionPerformed(ActionEvent e)
		    	{
					for (DPMInfo dpmInfo : dpmInfoModel)
						dpmInfo.sendRefreshMessage(acnetConnection, btn3.getText());
			  	}
		});
		ctrlsPanel.add(btn3);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(10, 5, 5, 5);
		mainPanel.add(ctrlsPanel, gbc);

		final JTable dpmTable = new JTable();

		this.defaultRowHeight = dpmTable.getRowHeight();
		this.dpmInfoModel = new DPMInfoModel(defaultRowHeight);

		dpmTable.getTableHeader().setDefaultRenderer(new HeaderRenderer(dpmTable));

		dpmTable.setModel(dpmInfoModel);

		final TableColumnModel cModel = dpmTable.getColumnModel();

		cModel.getColumn(DPMInfo.StatusColumn).setMaxWidth(10);
		cModel.getColumn(DPMInfo.NodeNameColumn).setMaxWidth(100);
		cModel.getColumn(DPMInfo.CodeVersionColumn).setMaxWidth(100);
		//dpmTable.getColumnModel().getColumn(DPMInfoModel.StartedColumn).setMaxWidth(140);
		cModel.getColumn(DPMInfo.StartedColumn).setMinWidth(135);
		//dpmTable.getColumnModel().getColumn(DPMInfoModel.StartedColumn).setPreferredWidth(135);
		cModel.getColumn(DPMInfo.UpTimeColumn).setMaxWidth(100);
		cModel.getColumn(DPMInfo.PidColumn).setMaxWidth(100);
		cModel.getColumn(DPMInfo.HeapFreePctColumn).setMaxWidth(100);
		cModel.getColumn(DPMInfo.LoadPctColumn).setMaxWidth(100);
		cModel.getColumn(DPMInfo.LogLevelColumn).setMaxWidth(100);
	
		dpmTable.getTableHeader().setReorderingAllowed(false);
		dpmTable.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 12));

		dpmTable.setDefaultRenderer(String.class, dpmInfoModel.renderer());
		dpmTable.setDefaultRenderer(Integer.class, dpmInfoModel.renderer());
		
		dpmTable.setRowSelectionAllowed(false);
		dpmTable.setCellSelectionEnabled(false);
	
		dpmTable.addMouseListener(new MouseAdapter() {
			class SingleClickTimerTask extends TimerTask
			{
				final DPMInfo dpmInfo;
				final int row;

				SingleClickTimerTask(DPMInfo dpmInfo, int row)
				{
					this.dpmInfo = dpmInfo;
					this.row = row;
				}

				@Override
				public void run()
				{
					dpmInfo.selected = !dpmInfo.selected;
					dpmInfoModel.fireTableRowsUpdated(row, row);
					dpmTable.setRowHeight(row, dpmInfo.selected ? defaultRowHeight * 4 : defaultRowHeight);
				}
			}

			SingleClickTimerTask task = null;

        	public void mouseClicked(MouseEvent evt)
			{
               	final Point p = evt.getPoint();
				final int row = dpmTable.rowAtPoint(p);
               	final DPMInfo dpmInfo = dpmInfoModel.get(row);

           		if (evt.getClickCount() == 2) {
					if (task != null) {
						task.cancel();
						task = null;
					}
					if (!dpmInfo.down) {
						mainFrame.setCursor(waitCursor);
						try {
							new ListFrame(dpmInfo);
						} catch (Exception e) {
							e.printStackTrace();
						}
						mainFrame.setCursor(defaultCursor);
					}
            	} else if (evt.getClickCount() == 1) {
					task = new SingleClickTimerTask(dpmInfo, row);
					timer.schedule(task, multiClickInterval);
				}
			}
        });


		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill  = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, 0, 0, 0);

		mainPanel.add(new JScrollPane(dpmTable), gbc);

		mainFrame.add(mainPanel);
		mainFrame.setVisible(true);
	}


	private class DPMInfoModel extends AbstractTableModel implements Iterable<DPMInfo>
	{
		final int defaultRowHeight;

		private class Renderer extends DefaultTableCellRenderer
		{
			@Override
			synchronized public Component getTableCellRendererComponent(JTable table, Object value,
													boolean isSelected, boolean hasFocus, int row, int col)
			{
				final Component component = super.getTableCellRendererComponent(table, value, isSelected, false, row, col);

				return dpmInfoArray.get(row).component(row, col, component);
			}
		}

		private final Vector<DPMInfo> dpmInfoArray = new Vector<>();

		DPMInfoModel(int defaultRowHeight)
		{
			super();
			this.defaultRowHeight = defaultRowHeight;
			this.dpmInfoArray.add(new DPMInfoTotal());
		}

		@Override
		public Iterator<DPMInfo> iterator()
		{
			final ArrayList<DPMInfo> tmp = new ArrayList<>();

			for (DPMInfo dpmInfo : dpmInfoArray) {
				if (dpmInfo.nodeName != null)
					tmp.add(dpmInfo);
			}

			return tmp.iterator();
		}

		void updateTotals()
		{
			final DPMInfoTotal totals = (DPMInfoTotal) dpmInfoArray.lastElement();

			totals.activeListCount = 0;
			totals.totalListCount = 0;
			totals.requestCount = 0;
			totals.totalRequestCount = 0;
			totals.totalConsolidationHits = 0;
			totals.repliesPerSecond = 0;
			totals.repliesPerSecondMax = 0;

			for (DPMInfo dpmInfo : dpmInfoArray) {
				totals.activeListCount += dpmInfo.activeListCount;
				totals.totalListCount += dpmInfo.totalListCount;
				totals.requestCount += dpmInfo.requestCount;
				totals.totalRequestCount += dpmInfo.totalRequestCount;	
				totals.totalConsolidationHits += dpmInfo.consolidationHits;	
				totals.repliesPerSecond += dpmInfo.repliesPerSecond;
				totals.repliesPerSecondMax += dpmInfo.repliesPerSecondMax;
			}

			dpmInfoModel.fireTableRowsUpdated(dpmInfoArray.size() - 1, dpmInfoArray.size() - 1);
		}

		void update(DPMScope.Reply.ServiceDiscovery m)
		{
			final DPMInfo dpmInfo = new DPMInfo(m);

			for (int ii = 0; ii < dpmInfoArray.size(); ii++) {
				final int res = dpmInfo.compareTo(dpmInfoArray.get(ii));

				if (res < 0) {
					dpmInfoArray.add(ii, dpmInfo);
					dpmInfoModel.fireTableRowsInserted(ii, ii);
					return;
				} else if (res == 0) {
					dpmInfoArray.get(ii).update(dpmInfo);
					dpmInfoModel.fireTableRowsUpdated(ii, ii);
					return;
				}
			}

			dpmInfoArray.add(dpmInfo);
			dpmInfoModel.fireTableRowsInserted(dpmInfoArray.size() - 1, dpmInfoArray.size() - 1);
		}

		synchronized void clear()
		{
			dpmInfoArray.clear();
			fireTableDataChanged();
		}

		synchronized DPMInfo get(int row)
		{
			return dpmInfoArray.get(row);
		}

		TableCellRenderer renderer()
		{
			return new Renderer();
		}

		synchronized void dpmResponseCheck()
		{
			final long now = System.currentTimeMillis();

			for (int row = 0; row < dpmInfoArray.size(); row++) {
				final DPMInfo dpmInfo = dpmInfoArray.get(row);

				if ((now - dpmInfo.replyTime) >= 4000) {
					dpmInfo.down = true;
					dpmInfoModel.fireTableRowsUpdated(row, row);
				}
			}
		}

		@Override
		public String getColumnName(int col)
		{
			return DPMInfo.columnNames[col];
		}

		@Override
		public int getColumnCount()
		{
			return DPMInfo.columnNames.length;
		}

        @Override
		public Class getColumnClass(int col)
		{
	    	return DPMInfo.classTypes[col];
        }

		@Override
		synchronized public int getRowCount()
		{
			return dpmInfoArray.size();
		}

		@Override
		synchronized public Object getValueAt(int row, int col) 
		{
			return dpmInfoArray.get(row).value(col);
		}

		@Override
		public boolean isCellEditable(int row, int col)
		{
			return false;
		}
	}

	// Shutdown hook

	@Override
	public void run()
	{
		try {
		} catch (Exception ignore) { }
	}

	@Override
	public void handle(AcnetReply r)
	{
		try {
			if (r.status() == AcnetErrors.ACNET_SUCCESS) {
				final DPMScope.Reply m = DPMScope.Reply.unmarshal(r.data());

				if (m instanceof DPMScope.Reply.ServiceDiscovery) {
					dpmInfoModel.update((DPMScope.Reply.ServiceDiscovery) m);
				}
			} else if (r.status() == AcnetErrors.ACNET_DISCONNECTED)
				dpmInfoModel.clear();
		} catch (Exception ignore) { }
	}

	static public void main(String[] args) throws Exception
	{
		try {
			Errors.init();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						new DPMScopeMain();
					} catch (Exception e) {
						logger.log(Level.WARNING, "exception", e);
						System.exit(100);
					}
				}
			});
		} catch (Exception e) {
			logger.log(Level.WARNING, "exception", e);
		}
	}
}
