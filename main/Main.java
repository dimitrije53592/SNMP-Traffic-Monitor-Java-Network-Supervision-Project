package main;

import java.util.ArrayList;

public class Main {
	
	private ArrayList<String> hosts = new ArrayList<String>();
	
	public ArrayList<String> getHosts() {
		return hosts;
	}

	public void setHosts(ArrayList<String> hosts) {
		this.hosts = hosts;
	}

	public static void main(String[] args) {
		Main main = new Main();
		
		HostAddressEntryFrame firstFrame = new HostAddressEntryFrame(main);
		firstFrame.setVisible(true);
		
		synchronized(main) {
			try {
				main.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		SNMPMibAccessManager manager = new SNMPMibAccessManager(main.getHosts());
		GraphFrame secondFrame = new GraphFrame(main.getHosts(), manager);
		secondFrame.updateGraphs();
	}

}
