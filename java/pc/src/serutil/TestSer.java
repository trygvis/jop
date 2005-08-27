/**
*	BenchRead. java
*
*	Measure bandwith of serial or USB read.
*/

package serutil;

import java.io.*;
import javax.comm.*;

public class TestSer extends Thread {


	private static CommPortIdentifier portId;
	private static InputStream is;
	private static OutputStream os;
	private static SerialPort serialPort;

	private static long cntSend, cntRcv;
	private static long start;


	public static void main(String[] args) {

		if (args.length!=1) {
			System.out.println("usage java TestSer port");
			System.exit(-1);
		}
		TestSer ts = new TestSer(args[0]);

		byte[] buf = new byte[256];

		ts.start();

		start = System.currentTimeMillis();

		new Thread() {
			public void run() {

				for (;;) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {}

					long i = cntRcv;
					long t = System.currentTimeMillis()-start;

					if (t!=0) {
						System.out.print((cntSend/1024)+" KB sent "+(i/1024)+" KB received "+
								(cntSend/1024*1000/t)+" KB/s "+(i/1024*1000/t)+" KB/s   \r");
					}
				}
			}
		}.start();

		for (;;) {

			try {
				os.write(buf);
			} catch (Exception e) {
				System.out.println(e);
			}
			cntSend += buf.length;

		}
	}

	private static final int TIMEOUT = 20;

	public TestSer(String port) {

		try {
			portId = CommPortIdentifier.getPortIdentifier(port);
			serialPort = (SerialPort) portId.open("TestSer", 2000);
			is = serialPort.getInputStream();
			os = serialPort.getOutputStream();
/*
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
*/
		
			serialPort.setSerialPortParams(115200,
				SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1,
				SerialPort.PARITY_NONE);

			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT
										| SerialPort.FLOWCONTROL_RTSCTS_IN);

			serialPort.enableReceiveTimeout(TIMEOUT);
//			serialPort.enableReceiveThreshold(4);

		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(-1);
		}
	}

	public void run() {

		byte[] buf = new byte[256];
		int cnt = 0;

		for (;;) {
			try {
				cnt = is.read(buf);
// Thread.sleep(256);
			} catch (Exception e) {
				System.out.println(e);
			}
			if (cnt>0) cntRcv += cnt;
		}

	}



/** not used
	public void serialEvent(SerialPortEvent event) {
		switch(event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			byte[] readBuffer = new byte[20];

			try {
				while (is.available() > 0) {
					int numBytes = is.read(readBuffer);
				}
				System.out.print(new String(readBuffer));
			} catch (IOException e) {}
			break;
		}
	}
*/
}
