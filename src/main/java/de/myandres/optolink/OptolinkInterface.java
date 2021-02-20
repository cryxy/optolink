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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class OptolinkInterface {

	private static final Logger LOG = LogManager.getLogger(OptolinkInterface.class);

	private OutputStream output;
	private InputStream input;
	private Config config;
	private CommPortIdentifier portIdentifier;
	private CommPort commPort;
	private Socket socket = null;

//TODO implement as runnable for URL-based optolinks	

	OptolinkInterface(Config config) throws Exception {

		// constructor with implicit open
		this.config = config;
		if (this.config.getTTYType().matches("URL")) { // device is at an URL
			open();
			close();
			LOG.debug("TTY type URL is present");
		} else { // device is local
			LOG.debug("Open TTY {} ...", this.config.getTTY());
			portIdentifier = CommPortIdentifier.getPortIdentifier(this.config.getTTY());

			if (portIdentifier.isCurrentlyOwned()) {
				LOG.error("TTY {} in use.", this.config.getTTY());
				throw new IOException();
			}
			commPort = portIdentifier.open(this.getClass().getName(), this.config.getTtyTimeOut());
			if (commPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(4800, SerialPort.DATABITS_8, SerialPort.STOPBITS_2,
						SerialPort.PARITY_EVEN);

				input = serialPort.getInputStream();
				output = serialPort.getOutputStream();
				commPort.enableReceiveTimeout(this.config.getTtyTimeOut()); // Reading Time-Out
			}
			LOG.debug("TTY {} opened", this.config.getTTY());
		}
	}

	public synchronized void close() {
		if (this.config.getTTYType().matches("URL")) {
			LOG.debug("Close TTY type URL {} ....", this.config.getTTY());
			if (socket != null) {
				try {
					socket.close();
					LOG.debug("TTY type URL {} closed", this.config.getTTY());
				} catch (IOException e) {
					LOG.debug("TTY type URL {} can't be closed", this.config.getTTY());
				}
			}
		} else {
			LOG.debug("Close TTY {} ....", this.config.getTTY());
			commPort.close();
			LOG.debug("TTY {} closed", this.config.getTTY());
		}
	}

	public synchronized void open() throws Exception {
		if (this.config.getTTYType().matches("URL")) {
			LOG.debug("Open TTY type URL {}", this.config.getTTY());
			socket = new Socket(this.config.getTTYIP(), this.config.getTTYPort());
			socket.setSoTimeout(this.config.getTtyTimeOut());
			input = socket.getInputStream();
			output = socket.getOutputStream();
			LOG.debug("TTY type URL is open");
		}
	}

	public synchronized void flush() {
		// Flush input Buffer
		if (this.config.getTTYType().matches("URL")) {
			// We have to wait a certain time. It seems that input.available always has the
			// count 0
			// right after connecting
			try {
				Thread.sleep(30); // 10 ms is too low. 30 ms chosen for a certain fail safe distance
			} catch (InterruptedException e) {
				LOG.debug("Error while sleeping to wait for buffer flush");
			}
		}
		try {
			input.skip(input.available());
			LOG.debug("Input Buffer flushed");
		} catch (IOException e) {
			LOG.error("Can't flush TTY: {}", this.config.getTTY(), e);
		}
	}

	public synchronized void write(int data) {
		LOG.trace("TxD: {}", String.format("%02X", (byte) data));
		try {
			output.write((byte) data);
		} catch (IOException e) {
			LOG.error("Can't write Data to TTY {}", this.config.getTTY(), e);
		}
	}

	public synchronized int read() {
		int data = -1;
		try {
			data = input.read();
			LOG.trace("RxD: {}", String.format("%02X", data));
			if (data == -1)
				LOG.trace("Timeout from TTY {}", this.config.getTTY());
			return data;
		} catch (SocketTimeoutException e) {
			LOG.trace("Timeout from TTY {}", this.config.getTTY());
			return data;
		} catch (Exception e) {
			LOG.error("Can't read Data from TTY {}", this.config.getTTY(), e);
			// FIX 100% CPU time
			throw new RuntimeException(e);
		}

	}

	public String getDeviceName() {
		return this.config.getDeviceType();
	}

}
