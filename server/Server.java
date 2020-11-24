package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * main class Server side,accepts connection and create a new Thread to handle them
 * @author Daniele Giachetto
 *
 */
class Server {
	
	public static void main(String arg[]) throws IOException {
		ServerSocket server = null;
		//Viene creato se non esiste il file contenente la lista degli ip bannati
		PrintWriter pw = new PrintWriter(new FileOutputStream(new File("ipBanned.txt"),
				true /* append = true */));
		int i = 0;
		try {
			//Apertura del socket destinato ad accettare connessioni
			server = new ServerSocket(55555);
			System.out.println("Opening port...");
			while(true) {
				//Viene attesa ed accettata una connessione
				Socket s = server.accept();
				//Se l'ip chje si è connesso è stato bannato viene rifiutata la connessione,altrimenti accettata ed assegnata ad un thread
				if (!credentialsExist(s.getInetAddress().getHostAddress())) {
					System.out.println(s.getInetAddress().getHostAddress() + " Connected");
					writeLog(s.getInetAddress().getHostAddress());
					++i;
					new RicevitoreServer(s, i);
				}else {
					new TrasmettitoreServer(new Persona(s,"BannedUser"),"/Exit");
				}
			}
		}catch(IOException e) {
			//Se l'apertura della porta fallisce viene mostrato un messaggio d'errore
			System.out.println("Problems encountered opening port,Server closing..");
		}finally{
			//Vengono chiusi il server e l'oggetto PrintWriter
			server.close();
			pw.close();
		}
	}
	
	/**
	 * Method used to check if userName is already saved into the file
	 * @param ip String containing userName to check
	 * @return true if the userName is already saved into the file
	 */
	private static boolean credentialsExist(String ip) {
		BufferedReader reader = null;
		try {
			//Viene creato un oggetto BufferedReader che leggera' dal file contenente la lista di ip bannati
			reader = new BufferedReader(new FileReader(new File("ipBanned.txt")));
			//viene letta una riga
			String fileLine = reader.readLine();
			String [] arrPhrase;
			//finchè la riga letta non è nulla e contiene caratteri specifici che definiscono il file
			while (fileLine != null && fileLine.contains("#/#--#/#")) {
				//viene spezzata la line letta a seconda dei caratteri specifici
				arrPhrase = fileLine.split("#/#--#/#");
				//Se la prima parola letta equivale all'ip dell'utente che si è connesso allora viene restituito true
				if (arrPhrase[0]!=null && arrPhrase[0].equalsIgnoreCase(ip)) {
					return true;
				}
				//lettura prossima linea
				fileLine = reader.readLine();
			}
		} catch (IOException e) {
			System.out.println("Problemi rilevati con la lettura dal file");
		} finally {
			try {
				//viene chiusa la risorsa
				reader.close();
			} catch (IOException e) {
				System.out.println("Problemi rilevati con la chiusura della risorsa");
			}
		}
		//Se l'ip non è contenuto nel file allora viene restituito false
		return false;
	}
	
	private static void writeLog(String ip) {
		PrintWriter logWriter = null;
		try {
			logWriter = new PrintWriter(new FileOutputStream(new File("EntryLog.txt"),
					true /* append = true */));
			//viene salvato il messaggio nel file dopo aver aperto lo stream dati
			logWriter.println(ip);
			System.out.println(ip);
		} catch (FileNotFoundException e) {
			System.out.println("Errore con la manipolazione del file richiesto");
		} finally {
			logWriter.close();
		}
		logWriter.flush();
		logWriter.close();
	}
	
}
