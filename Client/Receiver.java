package Client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.StringTokenizer;

import javax.swing.JTextArea;

/**
 * Class Receiver used client side to handle all the messages received from the Server
 * @author Giachetto Daniele
 *
 */
public class Receiver extends Thread {

	private Socket clientSocket = null;
	private JTextArea textArea = null;
	private boolean isListening = false;

	/**
	 * 
	 * @param s Object of the class Socket used to get inputStream
	 * @param text Object of the class String used to output messages received to the TextArea
	 */
	public Receiver(Socket s,JTextArea text) {
		this.textArea = text;
		this.clientSocket = s;
		start();
	}

	/**
	 * run method that gets from the outputStream every message that the server sends
	 */
	@Override
	public void run() {
		try {
			String outputClient = null;
			setStatus(true);
			while (true) {
				BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				// Impostare il ricevitore come "in ascolto,online"	
				outputClient = br.readLine(); // Aspetta un messaggio dal server
				if (outputClient != null) {
					// Se il messaggio contiene "-->" allora verra' analizzato in una maniera
					if (outputClient.contains("-->")) {
						// Messaggio spezzato da dopo il ">" Questo viene usato per ricavare solo il
						// messaggio e non il mittente
						StringTokenizer st = new StringTokenizer(outputClient, ">");
						st.nextToken();
						String StringAnalize = st.nextToken().substring(1);
						// Se il messaggio per qualche motivo di errore da parte del server contiene
						// "exit" non fare nulla
						if (StringAnalize.equalsIgnoreCase("/Exit")) {
							// Se la stringa non è nulla allora inserire il testo nella textArea
						} else if (!StringAnalize.equals(""))
							textArea.append(outputClient + '\n');
					} else if (outputClient.equals("")) {
					} else if (outputClient.equalsIgnoreCase("/Exit")) { // Se il messaggio ricevuto dal server contiene /exit significa che si è stati kickati o bannati
						textArea.append("You have been banned or kicked,contact an administrator \n");
						setStatus(false); // Il ricevitore non è più in ascolto
						break;
					} else { // Se il messaggio non contiene destinatario e non è vuoto allora è un qualche
								// messaggio diretto all'utente
						textArea.append(outputClient + '\n');
					}
				}
			}
		} catch (IOException e) {
			//Se la connessione viene persa allora viene mostrato all'utente un messaggio di errore
			textArea.append("Problems with the connection detected,you have been disconnected | error : #2048 \n");
			isListening = false;
		}finally {
			try {
				clientSocket.close();
			} catch (IOException e) {
				//Se non si riesce a chiudere correttamente il socket e la connessione viene mostrato un messaggio di errore
				textArea.append("Problems closing the connection,turn off the application | error : #1999 \n");;
			}
		}
	}
	
	/**
	 * Method that is used to know if the Receiver is listening or not
	 * @return true if it's listening
	 */
	public boolean getStatus() {
		return isListening;
	}
	
	/**
	 * Method used to change if Receiver is listening or not
	 * @param status boolean used to set if the Receiver is listening or not
	 */
	public void setStatus(boolean status) {
		isListening = status;
	}
	
}
