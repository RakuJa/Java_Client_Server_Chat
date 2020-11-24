package Client;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Thread used to send messages to the server
 * @author Giachetto Daniele
 *
 */
public class Transmitter extends Thread {
		
		PrintWriter pw = null;
		String output = null;
		Socket clientSocket = null;
		
		/**
		 * Constructor used to get the message to send and the stream to send it
		 * @param output Object of the class String representing the message to send to the server
		 * @param s Object of the class Socket used to get the outputStream
		 */
		public Transmitter(String output,Socket s) {
			this.output = output;
			this.clientSocket = s;
			start();
		}
		/**
		 * run method that sends message to the stream
		 */
		@Override
		public void run() {
			try {
				pw = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			} catch (IOException e) {
				System.out.println("Generic error contacting server");
			}
			pw.println(output);
			pw.flush();
		}
		
	}
