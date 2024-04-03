package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.snmp4j.PDU;
import org.snmp4j.smi.VariableBinding;

@SuppressWarnings("serial")
public class GraphFrame extends JFrame{
	
	private ArrayList<String> hosts;
	private SNMPMibAccessManager manager;
	
	private ArrayList<HashMap<String, XYSeriesCollection>> dataset = new ArrayList<>();
	private ArrayList<ChartPanel> chartPanels = new ArrayList<ChartPanel>();
	private ArrayList<JComboBox<String>> comboBoxes = new ArrayList<JComboBox<String>>();
	private ArrayList<Integer> choices = new ArrayList<Integer>();
	
	private HashMap<Integer, HashMap<Integer, Integer>> lastNumberOfBitsIn = new HashMap<Integer, HashMap<Integer,Integer>>();
	private HashMap<Integer, HashMap<Integer, Integer>> lastNumberOfBitsOut = new HashMap<Integer, HashMap<Integer,Integer>>();
	
	private void initializeDataset() {
		for(int i = 0; i < hosts.size(); i++) {
			HashMap<String, XYSeriesCollection> map1 = new HashMap<String, XYSeriesCollection>();
			dataset.add(map1);
			
			HashMap<Integer, Integer> map2 = new HashMap<Integer, Integer>();
			lastNumberOfBitsIn.put(i, map2);
			
			HashMap<Integer, Integer> map3 = new HashMap<Integer, Integer>();
			lastNumberOfBitsOut.put(i, map3);
			
			choices.add(1);
		}
	}
	
	public GraphFrame(ArrayList<String> hosts, SNMPMibAccessManager manager) {
		this.hosts = hosts;
		this.manager = manager;
		
		initializeDataset();
		
		setTitle("Graphs");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JTabbedPane tabs = new JTabbedPane();
		
		for(int i = 0; i < hosts.size(); i++) {
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			
			String[] options = {"1. Ukupan broj paketa", 
					"2. Broj Unicast paketa", 
					"3. Broj Non-Unicast paketa", 
					"4. Protok [bit/s]",
					"5. Protok Unicast paketa [paket/10s]",
					"6. Protok Non-Unicast paketa [paket/10s]"};
			
			JComboBox<String> optionsChoice = new JComboBox<String>(options);
			
			optionsChoice.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					String choice = (String) optionsChoice.getSelectedItem();
					
					for(JComboBox<String> cb : comboBoxes) {
						if(e.getSource() == cb) {
							choices.set(comboBoxes.indexOf(cb), (int) (choice.charAt(0)) - (int) ('0'));
						}
					}
					
					System.out.println(choices);
					showDataOnGraphs();
				
				}});
			
			panel.add(optionsChoice, BorderLayout.NORTH);
			comboBoxes.add(optionsChoice);
			
			JPanel graphPanel = new JPanel();
			graphPanel.setLayout(new FlowLayout());
			
			XYSeriesCollection inCollection = dataset.get(i).get("tip1in");
			
			JFreeChart inChart = ChartFactory.createXYLineChart("Incoming traffic",	
					"Vreme [s]", 
					"Paketi/Biti",
					inCollection, 
					PlotOrientation.VERTICAL,
					true, true, false);
			
			Plot plot = inChart.getPlot();
			plot.setBackgroundPaint(Color.WHITE);
			
			ChartPanel inChartPanel = new ChartPanel(inChart);
			graphPanel.add(inChartPanel);
			chartPanels.add(inChartPanel);
			
			XYSeriesCollection outCollection = dataset.get(i).get("tip1out");
			
			JFreeChart outChart = ChartFactory.createXYLineChart("Outgoing traffic",
					"Vreme [s]", 
					"Paketi/Biti",
					outCollection, 
					PlotOrientation.VERTICAL,
					true, true, false);
			
			plot = outChart.getPlot();
			plot.setBackgroundPaint(Color.WHITE);
			
			ChartPanel outChartPanel = new ChartPanel(outChart);
			graphPanel.add(outChartPanel);
			chartPanels.add(outChartPanel);
			
			panel.add(graphPanel, BorderLayout.CENTER);
			
			tabs.addTab(hosts.get(i), panel);
		}
		
		add(tabs);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	private void showDataOnGraphs() {		
		int chartCounter = 0;
		for(int i = 0;i < hosts.size(); i++) {
			chartPanels.get(chartCounter++).getChart().getXYPlot().setDataset(dataset.get(i).get("tip" + choices.get(i) + "in"));
			chartPanels.get(chartCounter++).getChart().getXYPlot().setDataset(dataset.get(i).get("tip" + choices.get(i) + "out"));
		}
		
		repaint();
	}
	
	public void updateGraphs() {
		int time = 0;
		
		while(true) {
			ArrayList<PDU> newData = manager.getMibData();
			
			for(int i = 0; i < hosts.size(); i++) {
				HashMap<String, XYSeriesCollection> temp = dataset.get(i);
				PDU newPDU = newData.get(i);
				
				temp.put("tip1in", getNumberOfPackets(newPDU, "in", dataset.get(i).get("tip1in"), time));
				temp.put("tip2in", getNumberOfUnicastPackets(newPDU, "in", dataset.get(i).get("tip2in"), time));
				temp.put("tip3in", getNumberOfNonUnicastPackets(newPDU, "in", dataset.get(i).get("tip3in"), time));
				temp.put("tip4in", getFlowOfBits(newPDU, "in", dataset.get(i).get("tip4in"), time, i));
				temp.put("tip5in", getFlowOfUnicastPackets(newPDU, "in", dataset.get(i).get("tip5in"), time, i));
				temp.put("tip6in", getFlowOfNonUnicastPackets(newPDU, "in", dataset.get(i).get("tip6in"), time, i));
			
				temp.put("tip1out", getNumberOfPackets(newPDU, "out", dataset.get(i).get("tip1out"), time));
				temp.put("tip2out", getNumberOfUnicastPackets(newPDU, "out", dataset.get(i).get("tip2out"), time));
				temp.put("tip3out", getNumberOfNonUnicastPackets(newPDU, "out", dataset.get(i).get("tip3out"), time));
				temp.put("tip4out", getFlowOfBits(newPDU, "out", dataset.get(i).get("tip4out"), time, i));
				temp.put("tip5out", getFlowOfUnicastPackets(newPDU, "out", dataset.get(i).get("tip5out"), time, i));
				temp.put("tip6out", getFlowOfNonUnicastPackets(newPDU, "out", dataset.get(i).get("tip6out"), time, i));
				
				
			}
			
			showDataOnGraphs();
			
			try {
				Thread.sleep(10000);
				time += 10;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private XYSeriesCollection getNumberOfPackets(PDU newPDU, String direction, XYSeriesCollection collection, int time) {		
		XYSeriesCollection temp = collection;
		
		if(temp == null) {
			temp = new XYSeriesCollection();
			
			for(int i = 0; i < newPDU.size() / 6; i++) {
				temp.addSeries(new XYSeries("Interface " + (i+1) ));
			}
		}
		
		List<XYSeries> series = temp.getSeries();
		
		for(int i = 0; i < series.size(); i++) {
			int res = 0;
			
			for(VariableBinding vb : newPDU.getVariableBindings()) {
				if(direction.equals("in") && (vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.11." + (i+1)) || vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.12." + (i+1)))) {
					res += vb.getVariable().toInt();
				}else if(direction.equals("out") && (vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.17." + (i+1)) || vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.18." + (i+1)))) {
					res += vb.getVariable().toInt();
				}
			}
			
			series.get(i).add(time, res);
		}
		
		return temp;
	}
	
	private XYSeriesCollection getNumberOfUnicastPackets(PDU newPDU, String direction, XYSeriesCollection collection, int time) {
		XYSeriesCollection temp = collection;
		
		if(temp == null) {
			temp = new XYSeriesCollection();
			
			for(int i = 0; i < newPDU.size() / 6; i++) {
				temp.addSeries(new XYSeries("Interface " + (i+1) ));
			}
		}
		
		List<XYSeries> series = temp.getSeries();
		
		for(int i = 0; i < series.size(); i++) {
			int res = 0;
			
			for(VariableBinding vb : newPDU.getVariableBindings()) {
				if(direction.equals("in") && (vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.11." + (i+1)))) {
					res += vb.getVariable().toInt();
				}else if(direction.equals("out") && (vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.17." + (i+1)))) {
					res += vb.getVariable().toInt();
				}
			}
			
			series.get(i).add(time, res);
		}
		
		return temp;
	}
	
	private XYSeriesCollection getNumberOfNonUnicastPackets(PDU newPDU, String direction, XYSeriesCollection collection, int time) {
		XYSeriesCollection temp = collection;
		
		if(temp == null) {
			temp = new XYSeriesCollection();
			
			for(int i = 0; i < newPDU.size() / 6; i++) {
				temp.addSeries(new XYSeries("Interface " + (i+1) ));
			}
		}
		
		List<XYSeries> series = temp.getSeries();
		
		for(int i = 0; i < series.size(); i++) {
			int res = 0;
			
			for(VariableBinding vb : newPDU.getVariableBindings()) {
				if(direction.equals("in") && (vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.12." + (i+1)))) {
					res += vb.getVariable().toInt();
				}else if(direction.equals("out") && (vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.18." + (i+1)))) {
					res += vb.getVariable().toInt();
				}
			}
			
			series.get(i).add(time, res);
		}
		
		return temp;
	}
	
	private XYSeriesCollection getFlowOfBits(PDU newPDU, String direction, XYSeriesCollection collection, int time,int host) {
		XYSeriesCollection temp = collection;
		
		if(temp == null) {
			temp = new XYSeriesCollection();
			
			for(int i = 0; i < newPDU.size() / 6; i++) {
				temp.addSeries(new XYSeries("Interface " + (i+1)));
			}
		}
		
		if(time < 10) {
			List<XYSeries> series = temp.getSeries();
			
			for(int i = 0; i < series.size(); i++) {
				int res = 0;
				
				for(VariableBinding vb : newPDU.getVariableBindings()) {
					if(direction.equals("in") && (vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.10." + (i+1)))) {
						res = vb.getVariable().toInt();
					}else if(direction.equals("out") && (vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.16." + (i+1)))) {
						res = vb.getVariable().toInt();
					}
					
					if(direction.equals("in")) {
						lastNumberOfBitsIn.get(host).put(i, res);
					}else if(direction.equals("out")) {
						lastNumberOfBitsOut.get(host).put(i, res);
					}
				}
			}
			
			return temp;
		}
		
		List<XYSeries> series = temp.getSeries();
		
		for(int i = 0; i < series.size(); i++) {
			int res = 0;
			
			for(VariableBinding vb : newPDU.getVariableBindings()) {
				if(direction.equals("in") && (vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.10." + (i+1)))) {
					res += vb.getVariable().toInt();
				}else if(direction.equals("out") && (vb.getOid().toString().equals("1.3.6.1.2.1.2.2.1.16." + (i+1)))) {
					res += vb.getVariable().toInt();
				}
			}
			
			if(direction.equals("in")) {
				series.get(i).add(time, 8 * (res - lastNumberOfBitsIn.get(host).get(i)) / 10);
				lastNumberOfBitsIn.get(host).put(i, res);
			}else if(direction.equals("out")) {
				series.get(i).add(time, 8 * (res - lastNumberOfBitsOut.get(host).get(i)) / 10);
				lastNumberOfBitsOut.get(host).put(i, res);
			}
		}
		
		return temp;
	}
	
	private XYSeriesCollection getFlowOfUnicastPackets(PDU newPDU, String direction, XYSeriesCollection collection, int time, int host) {
		XYSeriesCollection temp = collection;
		
		if(temp == null) {
			temp = new XYSeriesCollection();
			
			for(int i = 0; i < newPDU.size() / 6; i++) {
				temp.addSeries(new XYSeries("Interface " + (i+1)));
			}
		}
		
		if(time < 20) return temp;
		
		List<XYSeries> series = temp.getSeries();
		List<XYSeries> calcSeries = dataset.get(host).get("tip2" + direction).getSeries();
		
		for(int i = 0; i < series.size(); i++) {
			int res = calcSeries.get(i).getY(calcSeries.get(i).getItemCount()-1).intValue() - calcSeries.get(i).getY(calcSeries.get(i).getItemCount()-2).intValue();
			series.get(i).add(time, res);
		}
		
		return temp;
	}
	
	private XYSeriesCollection getFlowOfNonUnicastPackets(PDU newPDU, String direction, XYSeriesCollection collection, int time, int host) {
		XYSeriesCollection temp = collection;
		
		if(temp == null) {
			temp = new XYSeriesCollection();
			
			for(int i = 0; i < newPDU.size() / 6; i++) {
				temp.addSeries(new XYSeries("Interface " + (i+1)));
			}
		}
		
		if(time < 20) return temp;
		
		List<XYSeries> series = temp.getSeries();
		List<XYSeries> calcSeries = dataset.get(host).get("tip3" + direction).getSeries();
		
		for(int i = 0; i < series.size(); i++) {
			int res = calcSeries.get(i).getY(calcSeries.get(i).getItemCount()-1).intValue() - calcSeries.get(i).getY(calcSeries.get(i).getItemCount()-2).intValue();
			
			series.get(i).add(time, res);
		}
		
		return temp;
	}

}
