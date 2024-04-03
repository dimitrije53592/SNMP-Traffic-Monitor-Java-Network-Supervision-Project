package main;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SNMPMibAccessManager {
	
	private static final String oids = "1: 1.3.6.1.2.1.2.2.1.1 - interfaces\n"
			+ "   1.3.6.1.2.1.2.2.1.2 - description attr\n"
			+ "   all other attr immediately after\n"
			+ "\n"
			+ "2: 1.3.6.1.2.1.2.2.1.1 - interfaces\n"
			+ "   1.3.6.1.2.1.2.2.1.10 - inOctets\n"
			+ "   next 3 attr are immediately after\n"
			+ "   1.3.6.1.2.1.2.2.1.16 - outOctets\n"
			+ "   next 3 attr are immediately after\n"
			+ "\n"
			+ "3: 1.3.6.1.2.1.4.21.1.1 - routes\n"
			+ "   1.2.6.1.2.1.4.21.1.11 - masks\n"
			+ "   1.3.6.1.2.1.4.21.1.7 - NextHops\n"
			+ "   1.3.6.1.2.1.4.21.1.9 - protocol\n"
			+ "\n"
			+ "4: 1.3.6.1.2.1.15.3.1.1 - peers\n"
			+ "   1.3.6.1.2.1.15.3.1.2 - states\n"
			+ "   1.3.6.1.2.1.15.3.1.4 - versions\n"
			+ "   1.3.6.1.2.1.15.3.1.7 - peer ips\n"
			+ "   1.3.6.1.2.1.15.3.1.9 - ASs\n"
			+ "   1.3.6.1.2.1.15.3.1.10 - in updates\n"
			+ "   1.3.6.1.2.1.15.3.1.11 - out updates\n"
			+ "   1.3.6.1.2.1.15.3.1.19 - keepalives\n"
			+ "   1.3.6.1.2.1.15.3.1.24 - elapsed times\n"
			+ "\n"
			+ "5: 1.3.6.1.2.1.15.6.1.3 - routes\n"
			+ "   1.3.6.1.2.1.15.6.1.4 - origins\n"
			+ "   all other attr immediately after\n"
			+ "\n"
			+ "6: traps\n"
			+ "\n"
			+ "7: 1.3.6.1.2.1.11.1 - in SNMP packets\n"
			+ "   1.3.6.1.2.1.11.2 - out SNMP packets\n"
			+ "   1.3.6.1.2.1.11.15 - gets\n"
			+ "   1.3.6.1.2.1.11.17 - sets\n"
			+ "   1.3.6.1.2.1.11.19 - traps\n"
			+ "   1.3.6.1.2.1.11.4 - bad communities\n"
			+ "\n"
			+ "8: 1.3.6.1.4.1.9.9.109.1.1.1.3 - 5s CPU busy\n"
			+ "   1.3.6.1.4.1.9.9.109.1.1.1.4 - 1min CPU busy\n"
			+ "   1.3.6.1.4.1.9.9.109.1.1.1.5 - 5min CPU busy\n"
			+ "   1.3.6.1.4.1.9.9.48.1.1.1.2 - memory pools\n"
			+ "   1.3.6.1.4.1.9.9.48.1.1.1.5 - busy memories\n"
			+ "   1.3.6.1.4.1.9.9.48.1.1.1.6 - free memories\n"
			+ "\n"
			+ "9: 1.3.6.1.2.1.6.13.1.1 - TCP sessions\n"
			+ "   next 4 attr are immediately after\n"
			+ "   1.3.6.1.2.1.7.5.1 - UDP sessions (addresses)\n"
			+ "   1.3.6.1.2.1.7.5.1 - ports";
	
	private static final String community = "si2019";
	private static final int port = 161;
	private ArrayList<String> hosts;
	
	public SNMPMibAccessManager(ArrayList<String> hosts) {
		this.hosts = hosts;
	}

	public ArrayList<PDU> getMibData() {
		ArrayList<PDU> pdus = new ArrayList<PDU>();
		for(String host : hosts) pdus.add(getMibDataForHost(host));
		
		return pdus;
	}
	
	private PDU getMibDataForHost(String host) {
		ResponseEvent<Address> numOfInterfacesResponse = null, responseEvent = null;
		
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			Snmp snmp = new Snmp(transport);
			transport.listen();
			
			String targetAddress = "udp:" + host + "/" + port;
			CommunityTarget<Address> target = new CommunityTarget<Address>();
			target.setCommunity(new OctetString(community));
			target.setAddress(GenericAddress.parse(targetAddress));
			target.setRetries(2);
			target.setTimeout(1500);
			target.setVersion(SnmpConstants.version1);
			
			PDU temp = new PDU();
			temp.setType(PDU.GET);
			temp.setMaxRepetitions(10);
			temp.setNonRepeaters(0);
			
			temp.add(new VariableBinding(new OID(".1.3.6.1.2.1.2.1.0")));
			
			numOfInterfacesResponse = snmp.send(temp, target);
			temp = numOfInterfacesResponse.getResponse();
			int numOfInterfaces = Integer.parseInt(temp.get(0).getVariable().toString());
			
			PDU pdu = new PDU();
			pdu.setType(PDU.GETBULK);
			pdu.setMaxRepetitions(10);
			pdu.setNonRepeaters(0);
			
			for(int i = 0; i < numOfInterfaces; i++) {
				pdu.add(new VariableBinding(new OID(".1.3.6.1.2.1.2.2.1.10." + i)));
				pdu.add(new VariableBinding(new OID(".1.3.6.1.2.1.2.2.1.11." + i)));
				pdu.add(new VariableBinding(new OID(".1.3.6.1.2.1.2.2.1.12." + i)));
				
				pdu.add(new VariableBinding(new OID(".1.3.6.1.2.1.2.2.1.16." + i)));
				pdu.add(new VariableBinding(new OID(".1.3.6.1.2.1.2.2.1.17." + i)));
				pdu.add(new VariableBinding(new OID(".1.3.6.1.2.1.2.2.1.18." + i)));
			}
			
			responseEvent = snmp.send(pdu, target);
			
			snmp.close();
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return responseEvent.getResponse();
		
	}

}
