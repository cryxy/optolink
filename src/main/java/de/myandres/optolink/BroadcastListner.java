/*******************************************************************************
 * Copyright (c) 2015,  Stefan Andres.  All rights reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *******************************************************************************/
package de.myandres.optolink;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BroadcastListner implements Runnable {

	private static final Logger LOG = LogManager.getLogger(BroadcastListner.class);

	static final String BROADCAST_MESSAGE = "@@@@VITOTRONIC@@@@/";
	int port;
	String connectedIP;
	String adapterID;

	BroadcastListner(int port, String adapterID) {
		LOG.debug("Init Broadcast Listener on Port", port);
		this.port = port;
		connectedIP = "";
		this.adapterID = adapterID;
	}

	@Override
	public void run() {
		// Runs Listner
		LOG.debug("Listening for Broadcast....");
		DatagramSocket datagramSocket = null;
		InetAddress remoteIPAddress;
		int remotePort;
		byte[] byteArray = new byte[1024];

		try {
			datagramSocket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
			datagramSocket.setBroadcast(true);

			String str;

			while (true) {
				try {
					DatagramPacket resivedPacket = new DatagramPacket(byteArray, byteArray.length);
					datagramSocket.receive(resivedPacket);
					str = new String(resivedPacket.getData()).trim();
					LOG.debug("Resived Broadcast Message: {}", str);
					remotePort = resivedPacket.getPort();
					LOG.debug("From Port: {}", remotePort);
					remoteIPAddress = resivedPacket.getAddress();
					LOG.debug("From Host: {}", remoteIPAddress.toString());

					if (str.startsWith(BROADCAST_MESSAGE + adapterID) || str.startsWith(BROADCAST_MESSAGE + "*")) {
						// Someone calls me
						str = BROADCAST_MESSAGE + adapterID;
						byteArray = str.getBytes();
						DatagramPacket sendPacket = new DatagramPacket(byteArray, byteArray.length, remoteIPAddress,
								remotePort);
						LOG.debug("Send: '{}' to {}:{}", str, remoteIPAddress.getHostAddress(), remotePort);
						datagramSocket.send(sendPacket);

					} else {
						LOG.debug("Host: {}:{} calls with wrong message: {}", remoteIPAddress.getHostAddress(),
								remotePort, str);
						LOG.debug("Message will be ignor!");
					}

				} catch (Exception e) {
					LOG.error("Something is wrong in broadcast listner thread!!! Diagnostic {}", e);
					LOG.error("Broadcast Listner die ");

				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOG.error("Something is wrong in broadcast listner thread!!! Diagnostic {}", e);
			LOG.error("Broadcast Listner die");

		} finally {
			try {
				if (datagramSocket != null)
					datagramSocket.close();
			} catch (Exception e) {
				// Ignore
			}
		}
	}

}
