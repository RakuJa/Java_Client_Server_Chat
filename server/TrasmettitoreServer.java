package server;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Vector;
/**
 * Class TrasmettitoreServer used to send messages to the clients
 * @author Giachetto Daniele
 *
 */
public class TrasmettitoreServer extends Thread{
	
	private PrintWriter pw = null;
	private Vector<Persona> clientList = null;
	private String messageToSend = null;
	private Persona p = null;
	private boolean vectorUsed = true;
	
	/**
	 * Constructor used to get the data needed to send messages to all clients
	 * @param clientList Object of the class Vector representing a List of all the clients connect and to whom send messages
	 * @param messageToSend Object of the class String representing message to send
	 */
	public TrasmettitoreServer(Vector<Persona> clientList,String messageToSend) {
		this.clientList = clientList;
		this.messageToSend = messageToSend;
		vectorUsed = true;
		start();
	}
	
	/**
	 * Constructor used to get the data needed to send messages to one specific client
	 * @param p Object of the class Persona representing the client that need to receive the message
	 * @param messageToSend Object of the class String representing message to send
	 */
	public TrasmettitoreServer(Persona p,String messageToSend) {
		this.p = p;
		this.messageToSend = messageToSend;
		vectorUsed = false;
		start();
	}
	
	/**
	 * Method run that send a message to all client or one client depending on the constructor
	 */
	@Override
	public void run() {
		//Se nel costruttore sono stati passati più client allora
		if (vectorUsed) {
			//Viene scannerizzato il vettore passato ed inviato il messaggio ricevuto nel costruttore a tutti i client presenti nel vettore
			for (Persona p : clientList) {
				try {
					pw = new PrintWriter(new OutputStreamWriter(p.getClientSocket().getOutputStream()));
					pw.println(messageToSend);
					pw.flush();
				} catch (IOException e) {
					System.out.println("Generic error forwarding the message");
				}
			}
		}
		//Altrimenti se è stato passato solo un client 
		else {
			try {
				//Viene inviato il messaggio al client specificato
				pw = new PrintWriter(new OutputStreamWriter(p.getClientSocket().getOutputStream()));
				pw.println(messageToSend);
				pw.flush();
			} catch (IOException e) {
				System.out.println("Generic error sending messages");
			}
		}
	}

}
