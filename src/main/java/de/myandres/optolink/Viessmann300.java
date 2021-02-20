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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Viessmann300 implements ViessmannProtocol {

	private static final Logger LOG = LogManager.getLogger(Viessmann300.class);

	private OptolinkInterface optolinkInterface;

	Viessmann300(OptolinkInterface optolinkInterface) {
		LOG.trace("Start Session for Protokoll '300' ....");
		this.optolinkInterface = optolinkInterface;
		startSession();
		LOG.trace("Start Session for Protokoll '300' started");
	}

	@Override
	public synchronized int getData(byte[] buffer, int address, int length) {

		byte[] localBuffer = new byte[16];
		int returNumberOfBytes;

		for (int i = 0; i < 3; i++) {

			LOG.debug(String.format("Get Data for address %04X .... ", address));

			// construct TxD

			localBuffer[0] = 0x00; // Request
			localBuffer[1] = 0x01; // reading Data
			localBuffer[2] = (byte) (address >> 8); // upper Byte of address
			localBuffer[3] = (byte) (address & 0xff); // lower Byte of address
			localBuffer[4] = (byte) length; // number of expected bytes

			if (transmit(localBuffer, 5)) {
				; // send Buffer

				// RxD
				returNumberOfBytes = receive(localBuffer); // read answer

				if (returNumberOfBytes > 0) {

					// check RxD
					int returnAddress;
					if (localBuffer[0] == 0x03)
						LOG.error("Answer Byte is 0x03: Return Error(Wrong Adress,maybe)");
					if (localBuffer[0] != 0x01)
						LOG.error("Answer Byte (0x01) expect, but: 0x{} received",
								String.format("%02X", localBuffer[0]));
					if (localBuffer[1] != 0x01)
						LOG.error("DataRead Byte (0x01) expect, but: 0x{} received", String.format("%02X", buffer[1]));
					returnAddress = ((localBuffer[2] & 0xFF) << 8) + ((int) localBuffer[3] & 0xFF); // Address
					if (returnAddress != address)
						LOG.error(String.format("Adress (%04X) expect, but: %04X received", address, returnAddress));
					for (int j = 0; j < localBuffer[4]; j++)
						buffer[j] = localBuffer[j + 5]; // copy Result
					LOG.debug(String.format("Data for address %04X got ", address));
					return (returNumberOfBytes - 5); // buffer length
				}
			}
			LOG.debug("Communication to OptolinkInterface fail");
			LOG.debug(" {}. Try to start session again .... ", i + 1);
			startSession();
			optolinkInterface.flush();
		}
		LOG.error("Trouble with communication to OptolinkInterface !!!!!!!!");
		LOG.error("If error continues pleace check hardware !!!!!!!!");
		return -1; // UPS
	}

	@Override
	public synchronized int setData(byte[] buffer, int address, int length, int value) {
		byte[] localBuffer = new byte[16];
		int returNumberOfBytes;

		for (int i = 0; i < 3; i++) {

			LOG.debug(String.format("Set Data for address %04X .... ", address));

			// construct TxD

			localBuffer[0] = 0x00; // Request
			localBuffer[1] = 0x02; // write Data
			localBuffer[2] = (byte) (address >> 8); // upper Byte of address
			localBuffer[3] = (byte) (address & 0xff); // lower Byte of address
			localBuffer[4] = (byte) length; // number bytes
			switch (length) {
			case 1:
				localBuffer[5] = (byte) (value & 0xff);
				break;
			case 2:
				localBuffer[5] = (byte) (value >> 8);
				localBuffer[6] = (byte) (value & 0xff);
				break;
			case 4:
				localBuffer[5] = (byte) (value >> 24);
				localBuffer[6] = (byte) (value >> 16);
				localBuffer[7] = (byte) (value >> 8);
				localBuffer[8] = (byte) (value & 0xff);
				break;
			}

			if (transmit(localBuffer, 5 + length)) {
				; // send Buffer

				// RxD
				returNumberOfBytes = receive(localBuffer); // read answer

				if (returNumberOfBytes > 0) {

					// check RxD
					int returnAddress;
					if (localBuffer[0] == 0x03)
						LOG.error("Answer Byte is 0x03: Return Error(Wrong Adress,maybe)");
					if (localBuffer[0] != 0x01)
						LOG.error("Answer Byte (0x01) expect, but: 0x{} received",
								String.format("%02X", localBuffer[0]));
					if (localBuffer[1] != 0x02)
						LOG.error("Data Write Byte (0x02) expect, but: 0x{} received",
								String.format("%02X", buffer[1]));
					returnAddress = ((localBuffer[2] & 0xFF) << 8) + ((int) localBuffer[3] & 0xFF); // Address
					if (returnAddress != address)
						LOG.error(String.format("Adress (%04X) expect, but: %04X received", address, returnAddress));
					for (int j = 0; j < localBuffer[4]; j++)
						buffer[j] = localBuffer[j + 5]; // copy Result
					LOG.debug(String.format("Data for address %04X got ", address));
					return (localBuffer[4]); // buffer length
				}
			}
			LOG.debug("Communication to OptolinkInterface fail");
			LOG.debug(" {}. Try to start session again .... ", i + 1);
			startSession();
			optolinkInterface.flush();
		}
		LOG.error("Trouble with communication to OptolinkInterface !!!!!!!!");
		LOG.error("If error continues pleace check hardware !!!!!!!!");
		return -1; // UPS
	}

	@Override
	public void close() {
		stopSession();

	}

	// Private Methods

	// OptolinkInterface Session handling

	private synchronized void startSession() {

		optolinkInterface.flush();
		optolinkInterface.write(0x04); // close communication, if open
		optolinkInterface.read(); // catch 0x15 or 0x05
		for (int i = 0; i < 5; i++) { // try 5 times to sync
			optolinkInterface.write(0x16); // send Init
			optolinkInterface.write(0x00);
			optolinkInterface.write(0x00);
			if (optolinkInterface.read() == 0x06) { // catch Ack
				LOG.trace(" [ACK]");
				LOG.debug("Session to Optolink opened");
				return; // Init is OK
			}
			LOG.trace("Open Session to Optolink [ACK] failed");
		}
		LOG.error("!!! Trouble with communication to OptolinkInterface !!!");
		LOG.error("!!! Please check hardware !!!");
	}

	public synchronized void stopSession() {
		int ret;
		LOG.debug("Try to Close Optolink Session");
		for (int i = 0; i < 5; i++) { // try 5 times to close
			optolinkInterface.write(0x04); // close communication
			ret = optolinkInterface.read();
			if (ret == 0x06) {
				LOG.trace("[ACK] received");
				LOG.debug("Session to optolink closed");
				return; // Close OK
			}

			if (ret == 0x05) { // Session already closed (why, i don't now)
				LOG.debug("Session to optolink already closed");
				return; // Close OK
			}
			LOG.error("Closing session to optolink failed");
		}
	}

	// RxD Telegram
	private synchronized int receive(byte[] buffer) {
		LOG.debug("Get Data from OptolinkInterface ...");
		int ret = optolinkInterface.read();
		if (ret != 0x41) {
			LOG.error(String.format("Start Byte (0x41) expect, but: %#02X", ret));
			optolinkInterface.flush();
			return -1;
		}

		// number of bytes in received data
		int returnLength = optolinkInterface.read();
		int returnChecksum = returnLength; // Checksum

		// reading all data bytes
		for (int i = 0; i < returnLength; i++) {
			buffer[i] = (byte) optolinkInterface.read();
			returnChecksum += buffer[i]; // count checksum
		}
		returnChecksum = returnChecksum & 0xFF; // expected checksum 8 low bit's .
		int bufferChecksum = optolinkInterface.read(); // read checksum
		if (returnChecksum != bufferChecksum) {
			LOG.error(String.format("Checksumme (%#02X) expect, but %#02X received", returnChecksum, bufferChecksum));
		}
		LOG.debug("Data from OptolinkInterface got [OK]");
		if (LOG.isTraceEnabled()) {
			// Dump Result if Trace is on
			LOG.trace("Dump Data (No. decimal hexadcimal binary)");
			int tempI;
			for (int i = 0; i < returnLength; i++) {
				tempI = buffer[i] & 0xFF;
				LOG.trace("[{}] {} {}", i, String.format("%03d %02X", tempI, tempI),
						String.format("%8s", Integer.toBinaryString(tempI)).replace(' ', '0'));

			}
		}
		return returnLength; // OK
	}

	// TxD Telegram
	private synchronized boolean transmit(byte[] buffer, int len) {

		LOG.debug("Send Data to OptolinkInterface");

		// TxD build Checksum
		int checksum = len;
		for (int i = 0; i < len; i++)
			checksum += buffer[i];

		optolinkInterface.write(0x41); // Telegram start byte
		optolinkInterface.write(len); // number of byte

		for (int i = 0; i < len; i++) { // send data
			optolinkInterface.write(buffer[i]);
		}
		optolinkInterface.write(checksum & 0xFF); // send Checksum

		// Wait for Acknowledge (0x06)
		int ret = optolinkInterface.read();
		if (ret != 0x06) {
			LOG.error(String.format("acknowledge (0x06) expect, but: %02X received", ret));
			if (ret == 0x05) {
				LOG.info(String.format("0x05 received: Session maybe stopped! Try to restart session"));
				startSession();
			}
			return false;
		}
		LOG.debug("Data to OptolinkInterface sended [OK]");
		return true;
	}

}
