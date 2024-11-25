// $Id: DumpPoolsFrame.java,v 1.2 2024/11/19 22:34:44 kingc Exp $
package gov.fnal.controls.servers.dpm.scope;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.table.*;

import gov.fnal.controls.service.proto.DPMScope;

class DumpPoolsFrame extends JFrame
{
	private final ListFrame listFrame;
	private final String dpmNode;
	private final JTextArea textArea;
	private final Highlighter highlighter;

	private StringBuilder textBuf;
	int highlightIndex = 0;

	DumpPoolsFrame(ListFrame listFrame)
	{
		this.listFrame = listFrame;
		this.dpmNode = listFrame.dpmInfo.nodeName;

		this.setTitle("DumpPools - " + dpmNode);

		final JPanel mainPanel = new JPanel();
		final GridBagConstraints gbc = new GridBagConstraints();

		mainPanel.setLayout(new GridBagLayout());

		final JPanel ctrlsPanel = new JPanel();

		ctrlsPanel.setLayout(new GridLayout(1, 5, 10, 0));
		
		JButton btn = new JButton("Acnet Repetitive");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				request(DPMScope.PoolType.AcnetRepetitive);
			}
		});
		ctrlsPanel.add(btn);

		btn = new JButton("Acnet OneShot");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				request(DPMScope.PoolType.AcnetOneshot);
			}
		});
		ctrlsPanel.add(btn);

		btn = new JButton("PVA Montitors");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				request(DPMScope.PoolType.PVAMonitors);
			}
		});
		ctrlsPanel.add(btn);

		JTextField fld = new JTextField(20);
		fld.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				JTextField fld = (JTextField) e.getSource();
				Pattern pattern = Pattern.compile(fld.getText());
				Matcher matcher = pattern.matcher(textArea.getText());

				highlighter.removeAllHighlights();
				while (matcher.find())
					try {
						highlighter.addHighlight(matcher.start(), matcher.end(), DefaultHighlighter.DefaultPainter);
					} catch (Exception ignore) {
					}

				highlightIndex = 0;
				setCaretToHighlightIndex();
			}
		});
		ctrlsPanel.add(fld);

		btn = new JButton("Next");
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				highlightIndex++;
				setCaretToHighlightIndex();
			}
		});
		ctrlsPanel.add(btn);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(8, 5, 8, 5);
		mainPanel.add(ctrlsPanel, gbc);

		this.textArea = new JTextArea();
		this.textArea.setLineWrap(false);
		this.textArea.setEditable(false);
		this.textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

		this.highlighter = textArea.getHighlighter(); 

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill  = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, 0, 0, 0);

		JScrollPane scrollPane = new JScrollPane (textArea, 
   									JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
									JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		mainPanel.add(scrollPane, gbc);
		this.add(mainPanel);

		DPMScopeMain.center(this);
		this.setVisible(true);
	}

	private void setCaretToHighlightIndex()
	{
		final Highlighter.Highlight[] highlights = highlighter.getHighlights();

		if (highlights.length > 0) {
			if (highlightIndex >= highlights.length)
				highlightIndex = 0;
			textArea.setCaretPosition(highlights[highlightIndex].getStartOffset());	
		}
	}

	private void request(DPMScope.PoolType poolType)
	{
		try {
			textBuf = new StringBuilder();

			final DPMScope.Request.DumpPools m = new DPMScope.Request.DumpPools();

			m.poolType = poolType;

			listFrame.sendRequest(m);
		} catch (Exception ignore) { 
			ignore.printStackTrace();
		}
	}

	public void handle(DPMScope.Reply.DumpPools m)
	{
		if (m.text.isEmpty()) {
			textArea.setText(textBuf.toString());
			textArea.setCaretPosition(0);	
			textArea.getCaret().setVisible(true);
			highlighter.removeAllHighlights();
		} else
			textBuf.append(m.text);
	}
}
