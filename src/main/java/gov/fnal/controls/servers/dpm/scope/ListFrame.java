// $Id: ListFrame.java,v 1.1 2024/09/12 20:27:14 kingc Exp $
package gov.fnal.controls.servers.dpm.scope;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.ClosedByInterruptException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import gov.fnal.controls.service.proto.DPMScope;

class ListFrame extends JFrame implements Runnable, DPMScope.Reply.Receiver
{
	static final Logger log = Logger.getLogger(ListFrame.class.getName()); 

	final DPMInfo dpmInfo;
	private final JLabel activeTitle = new JLabel();
	private final JLabel disposedTitle = new JLabel();
	private final ByteBuffer obufs[] = { ByteBuffer.allocate(4), ByteBuffer.allocate(128 * 1024) };
	private final ActiveModel activeModel = new ActiveModel();
	private final DisposedModel disposedModel = new DisposedModel();
	private final Thread channelThread;
	private final JComboBox<String> logLevelComboBox;

	private SocketChannel channel;
	private DumpPoolsFrame dumpPoolsFrame = null;
	private RequestListFrame requestListFrame = null;

	boolean authenticated = false;

	ListFrame(DPMInfo dpmInfo) throws IOException
	{
		super("");

		log.setLevel(Level.FINER);

		this.dpmInfo = dpmInfo;

		connect();
		this.channelThread = new Thread(this, "ListFrame");
		this.channelThread.start();

		DPMScopeMain.center(this);

		final JPanel mainPanel = new JPanel();
		final GridBagConstraints gbc = new GridBagConstraints();

		mainPanel.setLayout(new GridBagLayout());

		final JPanel ctrlsPanel = new JPanel();
		
		ctrlsPanel.setLayout(new GridLayout(1, 2, 10, 0));

		final JButton restartBtn = new JButton("Schedule Restart");
		restartBtn.addActionListener(new ActionListener() {
		  		public void actionPerformed(ActionEvent e)
		    	{
					try {
						sendRequest(new DPMScope.Request.Restart());
					} catch (Exception ignore) {
						ignore.printStackTrace();
					}
			  	}
		});
		ctrlsPanel.add(restartBtn);

		final JButton dumpPoolsBtn = new JButton("DumpPools");
		dumpPoolsBtn.addActionListener(new ActionListener() {
		  		public void actionPerformed(ActionEvent e)
		    	{
					if (dumpPoolsFrame == null) {
						setCursor(DPMScopeMain.waitCursor);
						dumpPoolsFrame = new DumpPoolsFrame(ListFrame.this);
						setCursor(DPMScopeMain.defaultCursor);
					} else {
						dumpPoolsFrame.setVisible(true);
						dumpPoolsFrame.toFront();
						dumpPoolsFrame.repaint();
					}
			  	}
		});
		ctrlsPanel.add(dumpPoolsBtn);

		final String[] logLevels = { "ALL", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST", "OFF" };
		this.logLevelComboBox = new JComboBox<>(logLevels);
		logLevelComboBox.setSelectedItem(dpmInfo.logLevel);
		logLevelComboBox.addActionListener(new ActionListener() {
		  		public void actionPerformed(ActionEvent e)
		    	{
					try {
						final DPMScope.Request.SetLogLevel m = new DPMScope.Request.SetLogLevel();
						
						m.newLogLevel = (String) ((JComboBox) e.getSource()).getSelectedItem();
						sendRequest(m);
					} catch (Exception ignore) {
						ignore.printStackTrace();
					}
			  	}
		});
		ctrlsPanel.add(logLevelComboBox);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(10, 5, 5, 5);
		mainPanel.add(ctrlsPanel, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.insets = new Insets(10, 0, 2, 0);
		mainPanel.add(activeTitle, gbc);

		final JTable activeTable = new JTable(activeModel);
		final JTableHeader activeHeader = activeTable.getTableHeader();

		activeHeader.setReorderingAllowed(false);
		activeHeader.setFont(new Font("Dialog", Font.BOLD, 12));
		activeHeader.addMouseListener(new MouseAdapter() {
        	public void mouseClicked(MouseEvent evt)
			{
           		if (evt.getClickCount() == 1)
					activeModel.sortOnColumn(activeTable.columnAtPoint(evt.getPoint()));
			}
		});

		activeTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		activeTable.setDefaultRenderer(String.class, activeModel.renderer());
		activeTable.setDefaultRenderer(Integer.class, activeModel.renderer());
		activeTable.setRowSelectionAllowed(false);
		activeTable.setCellSelectionEnabled(false);
		activeTable.addMouseListener(new MouseAdapter() {
        	public void mouseClicked(MouseEvent evt)
			{
           		if (evt.getClickCount() == 2) {
                	Point p = evt.getPoint();
                 	int row = activeTable.rowAtPoint(p);

					if (requestListFrame == null) {
						setCursor(DPMScopeMain.waitCursor);
						requestListFrame = new RequestListFrame(ListFrame.this);
						setCursor(DPMScopeMain.defaultCursor);
					}

					requestListFrame.setList(activeModel.get(row));
            	}	
			}
        });

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.fill  = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, 0, 0, 0);

		JScrollPane scrollPane = new JScrollPane(activeTable);
		mainPanel.add(scrollPane, gbc);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.fill  = GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(10, 0, 2, 0);
		mainPanel.add(disposedTitle, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.fill  = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, 0, 14, 0);

		final JTable disposedTable = new JTable(disposedModel);
		final JTableHeader disposedHeader = disposedTable.getTableHeader();

		disposedHeader.setReorderingAllowed(false);
		disposedHeader.setFont(new Font("Dialog", Font.BOLD, 12));
		disposedHeader.addMouseListener(new MouseAdapter() {
        	public void mouseClicked(MouseEvent evt)
			{
           		if (evt.getClickCount() == 1)
					disposedModel.sortOnColumn(disposedTable.columnAtPoint(evt.getPoint()));
			}
		});

		disposedTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		disposedTable.setDefaultRenderer(String.class, disposedModel.renderer());
		disposedTable.setDefaultRenderer(Integer.class, disposedModel.renderer());
		disposedTable.setRowSelectionAllowed(false);
		disposedTable.setCellSelectionEnabled(false);
		disposedTable.addMouseListener(new MouseAdapter() {
        	public void mouseClicked(MouseEvent evt)
			{
           		if (evt.getClickCount() == 2) {
                	Point p = evt.getPoint();
                 	int row = disposedTable.rowAtPoint(p);

					if (requestListFrame == null) {
						setCursor(DPMScopeMain.waitCursor);
						requestListFrame = new RequestListFrame(ListFrame.this);
						setCursor(DPMScopeMain.defaultCursor);
					}

					requestListFrame.setList(disposedModel.get(row));
            	}	
			}
        });

		scrollPane = new JScrollPane(disposedTable);
		mainPanel.add(scrollPane, gbc);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		add(mainPanel);

		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				try {
					if (dumpPoolsFrame != null)
						dumpPoolsFrame.dispose();

					if (requestListFrame != null)
						requestListFrame.dispose();

					log.log(Level.INFO, "interrupting channel thread on window close");
					channelThread.interrupt();
					channel.close();
				} catch (Exception ignore) { 
					ignore.printStackTrace();
				}
			}
		});

		setVisible(true);
		updateFrameTitle();
		updateListTitles();

		setColumnCharWidths(activeTable, activeModel.getColumnCharWidths());
		setColumnCharWidths(disposedTable, disposedModel.getColumnCharWidths());
	}

	private void connect() throws IOException
	{
		log.log(Level.INFO, "connecting to " + dpmInfo.hostName + ":" + dpmInfo.scopePort);

		channel = SocketChannel.open(new InetSocketAddress(dpmInfo.hostName, dpmInfo.scopePort));

		channel.configureBlocking(true);
		channel.finishConnect();
		channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
		channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
		channel.setOption(StandardSocketOptions.SO_LINGER, -1);
	}

	private void setColumnCharWidths(JTable table, int[][] widths)
	{
		try {
			final int charWidth = table.getGraphics().getFontMetrics().charWidth('M');
			final TableColumnModel model = table.getColumnModel();

			for (int ii = 0; ii < widths.length; ii++) {
				final TableColumn c = model.getColumn(ii);

				c.setMinWidth(widths[ii][0] * charWidth);
				c.setMaxWidth(widths[ii][1] * charWidth);
				c.setPreferredWidth(widths[ii][2] * charWidth);
			}
		} catch (Exception ignore) { }
	}

	private void updateFrameTitle()
	{
		setTitle("DPMScope - " + dpmInfo.nodeName + "(" + 
							(channel.isConnected() ? "Connected" : "Disconnected") + ")");

	}

	private void updateListTitles()
	{
		activeTitle.setText("Active - " + activeModel.size());
		disposedTitle.setText("Disposed - " + disposedModel.size());
	}

	synchronized void sendRequest(DPMScope.Request m) throws IOException
	{
		obufs[1].clear();
		m.marshal(obufs[1]).flip();
		obufs[0].clear();
		obufs[0].putInt(obufs[1].remaining()).flip();
		
		channel.write(obufs);
	}

	void fetchList(int id) throws IOException
	{
		final DPMScope.Request.List m = new DPMScope.Request.List();

		m.id = id;
		sendRequest(m);
	}

	@Override
	public void run()
	{
		log.log(Level.INFO, "channel thread entry");

		final ByteBuffer ibuf = ByteBuffer.allocate(128 * 1024);

		while (true) {
			try {
				if (channel.isConnected()) {
					ibuf.clear().limit(4);

					while (ibuf.remaining() != 0) { 
						if (channel.read(ibuf) == -1)
							throw new IOException("end of stream");
					}
					ibuf.flip();

					final int len = ibuf.getInt();

					ibuf.clear().limit(len);
					while (ibuf.remaining() != 0) {
						if (channel.read(ibuf) == -1)
							throw new IOException("end of stream");
					}
					ibuf.flip();

					DPMScope.Reply.unmarshal(ibuf).deliverTo(this);
				} else {
					try {
						connect();
						//logLevelComboBox.setSelectedItem(dpmInfo.logLevel);
						disposedModel.clear();
						updateListTitles();
						updateFrameTitle();
					} catch (Exception e) {
						log.log(Level.INFO, "exception in channel thread", e);
						try {
							Thread.sleep(2000);
						} catch (InterruptedException ie) {
							log.log(Level.INFO, "channel thread interrupted");
							break;
						}
					}
				}
			} catch (ClosedByInterruptException e) {
				break;
			} catch (Exception e) {
				log.log(Level.FINER, "exception handling channel", e);
				try {
					channel.close();
					activeModel.clear();
					updateListTitles();
					updateFrameTitle();
				} catch (Exception ignore) { }
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ie) {
					//return;
				}
			}
		}

		try {
			channel.close();
		} catch (Exception ignore) {
		}

		activeModel.clear();
		disposedModel.clear();
		updateListTitles();
		updateFrameTitle();

		log.log(Level.INFO, "channel thread exit");
	}

	@Override
	public void handle(DPMScope.Reply.Restart m)
	{
		log.log(Level.INFO, "restart scheduled");
	}

	@Override
	public void handle(DPMScope.Reply.List m)
	{
		log.log(Level.FINE, m.toString()); 

		final List list = List.create(m);

		if (list.disposed())
			disposedModel.add(list);
		else
			activeModel.add(list);

		updateListTitles();
	}

	@Override
	public void handle(DPMScope.Reply.ListOpened m)
	{
		log.log(Level.FINE, m.toString()); 

		activeModel.add(List.create(m.id, m.date));
		updateListTitles();
	}

	@Override
	public void handle(DPMScope.Reply.ListStarted m)
	{
		log.log(Level.FINE, m.toString()); 

		activeModel.handle(m);

		try {
			fetchList(m.id);
		} catch (IOException ignore) {
			ignore.printStackTrace();
		}
	}

	@Override
	public void handle(DPMScope.Reply.ListSettingsStarted m)
	{
		log.log(Level.FINE, m.toString()); 

		activeModel.handle(m);
	}

	@Override
	public void handle(DPMScope.Reply.ListSettingsComplete m)
	{
		log.log(Level.FINE, m.toString()); 

		activeModel.handle(m);

		try {
			fetchList(m.id);
		} catch (IOException ignore) {
			ignore.printStackTrace();
		}
	}

	@Override
	public void handle(DPMScope.Reply.ListDisposed m)
	{
		log.log(Level.FINE, m.toString()); 

		final List list = activeModel.handle(m);

		if (list != null)
			disposedModel.add(list);

		updateListTitles();
	}

	@Override
	public void handle(DPMScope.Reply.ListProperties m)
	{
		log.log(Level.FINE, m.toString()); 

		activeModel.handle(m);
	}

	@Override
	public void handle(DPMScope.Reply.ListReplyCount m)
	{
		log.log(Level.FINE, m.toString()); 

		activeModel.handle(m);
	}

	@Override
	public void handle(DPMScope.Reply.ListHost m)
	{
		log.log(Level.FINE, m.toString()); 

		activeModel.handle(m);
	}

	@Override
	public void handle(DPMScope.Reply.ListUser m)
	{
		log.log(Level.FINE, m.toString()); 

		activeModel.handle(m);
	}

	@Override
	public void handle(DPMScope.Reply.Requests m)
	{
		log.log(Level.FINE, m.toString()); 

		List list = List.get(m.id);

		if (list == null)
			list = List.create(m.id);

		if (list.disposed())
			disposedModel.handle(m);
		else
			activeModel.handle(m);
	}

	@Override
	public void handle(DPMScope.Reply.SettingRequests m)
	{
		log.log(Level.FINE, m.toString()); 

		List list = List.get(m.id);

		if (list == null)
			list = List.create(m.id);

		if (list.disposed())
			disposedModel.handle(m);
		else
			activeModel.handle(m);
	}

	@Override
	public void handle(DPMScope.Reply.DisposeList m)
	{
		requestListFrame.handle(m);
	}

	@Override
	public void handle(DPMScope.Reply.SetLogLevel m)
	{
	}

	@Override
	public void handle(DPMScope.Reply.DumpPools m)
	{
		dumpPoolsFrame.handle(m);
	}

	@Override
	public void handle(DPMScope.Reply.ServiceDiscovery m)
	{
	}
}
