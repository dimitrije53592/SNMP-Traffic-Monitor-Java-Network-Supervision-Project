package main;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class HostAddressEntryFrame extends JFrame{
	
	private DefaultListModel<String> listModel;
	private ArrayList<String> hosts = new ArrayList<String>();
	
	public HostAddressEntryFrame(Main main) {
		super("SNMP Router Traffic Meter");
		setSize(480, 360);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				synchronized(main) {
					main.notify();
				}
			}
			
		});
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
        JPanel headerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        
		JTextField addressEntry = new JTextField(30);
		headerPanel.add(addressEntry, constraints);
		
		JButton addButton = new JButton("Add");
		addButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String host = addressEntry.getText();
				
				listModel.add(listModel.getSize(), host);
				hosts.add(host);
			}
			
		});
		headerPanel.add(addButton, constraints);
		
		mainPanel.add(headerPanel, BorderLayout.NORTH);
		
		listModel = new DefaultListModel<String>();
		JList<String> hostsList = new JList<String>(listModel);
		mainPanel.add(new JScrollPane(hostsList), BorderLayout.CENTER);
		
		JButton showButton = new JButton("Show graphs");
		showButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				main.setHosts(hosts);
				dispose();
			}
			
		});
		mainPanel.add(showButton, BorderLayout.SOUTH);
		
		add(mainPanel);
	}

}
