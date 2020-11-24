package server;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

public class RicevitoreServer extends Thread {

	//TODO Optimize code
	//TODO Documentation
	//TODO change font to personal messages

	
	private static Vector<Persona> vettPersona = new Vector<Persona>();
	private Persona clientInfo;
	private PrintWriter pw;
	private static final String ipBanFile = "ipBanned.txt";
	private static final String userFile = "Credentials.txt";
	private static final String banFile = "bannedClient.txt";
	
	
	/**
	 * Constructor used to save the client that connected to the server
	 * and to assign a new client name for every client connected
	 * @param s socket used to receive and send messages 
	 * @param i counter used to create unique client name
	 */
	public RicevitoreServer(Socket s, int i) {
		clientInfo = new Persona(s, "Client" + i);
		synchronized (vettPersona) {
			vettPersona.add(clientInfo);
		}
		try {
			//Nel caso in cui non esistano vengono creati i file relativi al salvataggio degli utenti registrati,bannati e bannati per ip
			pw = new PrintWriter(new FileOutputStream(new File(userFile),
					true /* append = true */));
			pw = new PrintWriter(new FileOutputStream(new File(banFile),
					true /* append = true */));
			pw = new PrintWriter(new FileOutputStream(new File(ipBanFile),
					true /* append = true */));
		} catch (FileNotFoundException e) {
			System.out.println("Errore nella creazione o apertura dei file chiave");
		} finally {
			//Chiusura della risorsa
			pw.close();
		}
		new TrasmettitoreServer(clientInfo,"You are now logged in as : " + clientInfo.getClientName() + "   |  Type /show help to see a list of commands usable");
		start();
	}

	/**
	 * Method that analyzed every message received and create a new thread to send
	 * it to other clients
	 */
	@Override
	public void run() {
		String inputMessage = null;
		try {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(this.clientInfo.getClientSocket().getInputStream()));
			while (true) {
				//se l'utente e' stato bannato,kickato o esistono errori nella ricezione il server smette di gestire il client e lo rimuove.
				if ((inputMessage = br.readLine()) == null || clientInfo.isBanned() || clientInfo.isKicked()) {
					break;
				} else {
					//Il client ha iniziato la disconnessione,verra eliminato dalla lista e interrotta la gestione da parte del server di esso
					if (inputMessage == null || inputMessage.equalsIgnoreCase("/EXIT")) {
						System.out.println("Connection with client lost Error #9899");
						break;
						//Se il messaggio è vuoto ma non nullo allora si aspetta il prossimo
					} else if (inputMessage.equals("")) {
						continue;
					} else {
						//Viene divisa la frase in un vettore di parole (dividendo la frase ad ogni spazio)
						String[] arrPhrase = inputMessage.split(" ");
						//Se il vettore ha almeno due parole non nulle e la prima di queste contiene "/" potrebbe essere che il messaggio contiene un comando
						if (arrPhrase.length >= 1 && isNotNull(arrPhrase) && arrPhrase[0].contains("/")) {
							//Se la prima parola è uguale a /login allora si chiama la funzione che gestisce questa evenienza
							if (arrPhrase[0].equalsIgnoreCase("/LOGIN")) {
								loginFunction(arrPhrase);
								//Se la prima parola è uguale a /register allora si chiama la funzione che gestisce questa evenienza
							} else if (arrPhrase[0].equalsIgnoreCase("/REGISTER")) {
								registerFunction(arrPhrase);
								//Se la prima parola è uguale a /msg allora si chiama la funzione che gestisce questa evenienza
							} else if (arrPhrase[0].equalsIgnoreCase("/MSG")) {
								privateMessageFunction(arrPhrase);
								//Se la prima parola è uguale a /ban allora si chiama la funzione che gestisce questa evenienza
							} else if (arrPhrase[0].equalsIgnoreCase("/ban")) {
								banFunction(arrPhrase);
								//Se la prima parola è uguale a /kick allora si chiama la funzione che gestisce questa evenienza
							} else if (arrPhrase[0].equalsIgnoreCase("/kick")) {
								kickFunction(arrPhrase);
								//Se la prima parola è uguale a /show  allora si chiama la funzione che gestisce questa evenienza
							} else if (arrPhrase[0].equalsIgnoreCase("/show")) {
								showFunction(arrPhrase);
								//Se la prima parola è uguale a /ban-ip allora si chiama la funzione che gestisce questa evenienza
							} else if (arrPhrase[0].equalsIgnoreCase("/ban-ip")) {
								banIpFunction(arrPhrase);
								//Se la prima parola è diversa da qualsiasi comando allora il messaggio viene inoltrato agli altri client
							}else {
								writeString(clientInfo.getClientName() + " -->" + inputMessage,"MessageLog.txt");
								new TrasmettitoreServer(vettPersona,clientInfo.getClientName() + " --> " + inputMessage);
							}
						}else {
							writeString(clientInfo.getClientName() + " -->" + inputMessage,"MessageLog.txt");
							//Se la frase non rispetta i canoni di lunghezza allora non può essere un comando ed il messaggio viene inoltrato agli altri client
							new TrasmettitoreServer(vettPersona,clientInfo.getClientName() + " --> " + inputMessage);
						}
					}
				}
			}
			
			clientInfo.getClientSocket().close();
			//In un metodo sincronizzato viene rimosso il client disconnesso dal vettore di client connessi
			synchronized (vettPersona) {
				vettPersona.remove(clientInfo);
			}
		} catch (IOException e) {
			//In un metodo sincronizzato viene rimosso il client disconnesso dal vettore di client connessi
			synchronized (vettPersona) {
				vettPersona.remove(clientInfo);
			}
		}
	}
	
	/**
	 * Method used to ban a user ip
	 * @param arrPhrase String array containing useful information to analyze and utilize
	 */
	private void banIpFunction(String[] arrPhrase) {
		//Controlla se la frase è di due parole
		if (arrPhrase.length == 2) {
			//Controlla se l'utente è admin
			if (clientInfo.isAdmin()) {
				//Se l'ip non e' gia' stato bannato e se l'ip non e' uguale a quello del client che vuole eseguire il comando
				if (!clientInfo.getClientSocket().getInetAddress().getHostAddress().equalsIgnoreCase(arrPhrase[1])  && !credentialsExist(arrPhrase[1],ipBanFile)) {
					//Scrive l'ip nel file destinato a contenere la lista di ip bannati
					writeString(arrPhrase[1] + "#/#--#/#",ipBanFile);
					//Vettore formato da tutti i client connessi con l'ip bannato
					Vector<Persona> vettP = this.getPersonaFromIp(arrPhrase[1]);
					//Se il vettore non è vuoto o nullo allora banna e kicka tutti i client contenuti in esso
					if (vettP!=null && !(vettP.isEmpty())) {
						for (Persona p : vettP) {
							p.setBanned(true);
							//Rimuove l'utente dal vettore
							synchronized (vettPersona) {
								vettPersona.remove(p);
							}
							new TrasmettitoreServer(p,"/exit");
							new TrasmettitoreServer(clientInfo,"User ip has been banned and kicked " + p.getClientName());
						}
					}else {
						//Se gli utenti non sono online allora banna solamente
						new TrasmettitoreServer(clientInfo,"User ip has been banned");
					}
				}else {
					//Non è stato possibile bannare l'ip richiesto per diversi motivi
					new TrasmettitoreServer(clientInfo,"Couldn't ban the user,you cannot ban yourself,user must exist and not be already banned");
				}
			}else {
				//Non è stato possibile bannare perchè non si hanno i permessi
				new TrasmettitoreServer(clientInfo,"You need to be an admin to use this service");
			}
		}
	}

	/**
	 * Method used to kick users from the chat
	 * @param arrPhrase String array containing useful information to analyze and utilize
	 */
	private void kickFunction(String[] arrPhrase) {
		//Controlla se la frase è di due parole
		if (arrPhrase.length == 2) {
			//Controlla se l'utente è admin
			if (clientInfo.isAdmin()) {
				//Controlla se l'utente che deve essere kickato è diverso dall'utente che richiede il comando
				if (!(clientInfo.equals(arrPhrase[1]))) {
					//Crea un oggetto persona dal nome inserito
					Persona p = this.getPersonFromUsername(arrPhrase[1]);
					//Se l'oggetto esiste significa che l'utente è online e viene kickato
					if (p!=null) {
						p.setKicked(true);
						//Rimuove l'utente dal vettore
						synchronized (vettPersona) {
							vettPersona.remove(p);
						}
						new TrasmettitoreServer(p,"/exit");
						new TrasmettitoreServer(clientInfo,"User has been kicked");
					}else {
						new TrasmettitoreServer(clientInfo,"User is not online");
					}
				}
			}else { //Non si hanno i permessi necessari
				new TrasmettitoreServer(clientInfo,"You need to be an admin to use this service");
			}
		}
	}

	/**
	 * Method used to kick and ban users from the chat
	 * @param arrPhrase String array containing useful information to analyze and utilize
	 */
	private void banFunction(String[] arrPhrase) {
		//Controlla se la frase è di due parole
		if (arrPhrase.length == 2) {
			//Controlla se l'utente è admin
			if (clientInfo.isAdmin()) {
				//Se il nome non e' gia' stato bannato e se il nome non e' uguale a quello del client che vuole eseguire il comando
				if (!(clientInfo.equals(arrPhrase[1])) && credentialsExist(arrPhrase[1],userFile) && !credentialsExist(arrPhrase[1],banFile)) {
					//Inserisce il nome nell'elenco dei nomi bannati
					writeString(arrPhrase[1] + "#/#--#/#",banFile);
					Persona p = this.getPersonFromUsername(arrPhrase[1]);
					//Se un utente è loggato con il nome bannato allora
					if (p!=null) {
						//Dice all'utente che è stato bannato
						p.setBanned(true);
						//Rimuove l'utente dal vettore
						synchronized (vettPersona) {
							vettPersona.remove(p);
						}
						//Forza l'utente a disconnettersi
						new TrasmettitoreServer(p,"/exit");
						new TrasmettitoreServer(clientInfo,"User has been banned and kicked");
					}else {
						//Nessun utente è online con il nome bannato
						new TrasmettitoreServer(clientInfo,"User has been banned");
					}
				}else {
					//Errore nel bannare
					new TrasmettitoreServer(clientInfo,"Couldn't ban the user,you cannot ban yourself,user must exist and not be already banned");
				}
			}else {
				//Non si hanno i permessi
				new TrasmettitoreServer(clientInfo,"You need to be an admin to use this service");
			}
		}
		
	}

	/**
	 * Method used to show which users are online currently
	 * @param arrPhrase String array containing useful information to analyze and utilize
	 */
	private void showFunction(String[] arrPhrase) {
		//Controlla se la frase è formata da due parole
		if (arrPhrase.length == 2) {
			//Se la seconda parola è help allora viene mostrata la lista di comandi generici
			if (arrPhrase[1].equalsIgnoreCase("help")) {
				new TrasmettitoreServer(clientInfo,"You can use the following commands : 1. show online/admin/banned/registered/permission ");
				new TrasmettitoreServer(clientInfo, "2. /msg DestName message 3. /login Name password 4. /register Name password password");
						
			}
			//Se la seconda parola è permission allora viene mostrato se si è admin o user generico
			else if (arrPhrase[1].equalsIgnoreCase("permission")) {
				if (clientInfo.isAdmin()) {
					new TrasmettitoreServer(clientInfo,"You are logged in as : Admin");
				}else {
					new TrasmettitoreServer(clientInfo,"You are logged in as : User");
				}
			}
			//Se la seconda parola è online allora viene mostrato l'elenco degli utenti online
			else if (arrPhrase[1].equalsIgnoreCase("online")) {
				String usersOnline = getUsersOnline().toString();
				new TrasmettitoreServer(clientInfo,usersOnline);
			}else if (arrPhrase[1].equalsIgnoreCase("registered")) { //Se la seconda parola è registered vengono mostrati gli utenti registrati
				showRegisteredFunction();
			}else if (arrPhrase[1].equalsIgnoreCase("banned")) { //Se la seconda parola è banned vengono mostrati gli utenti bannati
				showBannedFunction();
			}else if (arrPhrase[1].equalsIgnoreCase("admin")) { //Se la seconda parola è admin vengono mostrati gli utenti admin
				showAdminFunction();
			}else {//Nessun comando idoneo è stato trovato,messaggio di errore
				new TrasmettitoreServer(clientInfo,"Failed to use the show command,try typing show online/admin/banned/registered/permission");
			}
		}else {//Nessun comando idoneo è stato trovato,messaggio di errore
			new TrasmettitoreServer(clientInfo,"Failed to use the show command,try typing show online/admin/banned/registered/permission");
		}
	}
	

	/**
	 * Method used to send to the client the list of every admin in the chat
	 */
	private void showAdminFunction() {
		//Se il client è admin
		if (clientInfo.isAdmin()) {
			StringBuilder sb = getAllAdmin();
			//Viene preso l'elenco degli admin,se ne esistono allora vengono mostrati
			if (sb != null) {
				String adminList = sb.toString();
				new TrasmettitoreServer(clientInfo,adminList);
			}else { //Se non vi sono admin viene mostrato un errore
				System.out.println("Error #2703");
				new TrasmettitoreServer(clientInfo,"No admin has been found");
			}
		}else { //Se non si hanno i permessi viene mostrato un errore
			System.out.println("Error #9674");
			new TrasmettitoreServer(clientInfo,"You need to be admin to use this command");
		}
	}

	/**
	 * Method used to send to the client the list of every client banned
	 */
	private void showBannedFunction() {
		//Se il client è admin
		if (clientInfo.isAdmin()) {
			StringBuilder sb = getAllRegistered(banFile,1);
			//Viene preso l'elenco degli utenti bannati,se ne esistono allora vengono mostrati
			if (sb != null) {
				String registeredList = sb.toString();
				System.out.println(registeredList);
				new TrasmettitoreServer(clientInfo, registeredList);
			} else {  //Se non vi sono utenti bannati viene mostrato un errore
				System.out.println("Error #4316");
				new TrasmettitoreServer(clientInfo, "Users are yet to register");
			}
		}else { //Se non si hanno i permessi viene mostrato un errore
			System.out.println("Error #9674");
			new TrasmettitoreServer(clientInfo,"You need to be admin to use this command");
		}
	}
	
	/**
	 * Method used to send to the client the list of every client registered
	 */
	private void showRegisteredFunction() {
		//Se il client è admin
		if (clientInfo.isAdmin()) {
			StringBuilder sb = getAllRegistered(userFile,3);
			//Viene preso l'elenco degli utenti registrati,se ne esistono allora vengono mostrati
			if (sb != null) {
				String registeredList = sb.toString();
				new TrasmettitoreServer(clientInfo, registeredList);
			} else { //Se non vi sono utenti registrati viene mostrato un errore
				System.out.println("Error #4316");
				new TrasmettitoreServer(clientInfo, "Users are yet to register");
			}
		}else { //Se non si hanno i permessi viene mostrato un errore
			System.out.println("Error #9674");
			new TrasmettitoreServer(clientInfo,"You need to be admin to use this command");
		}
	}

	/**
	 * Method used to login user with their credentials
	 * @param arrPhrase String array containing useful information to analyze and utilize
	 */
	private void loginFunction(String[] arrPhrase) {
		//Se la frase è composta da tre parole
		if (arrPhrase.length == 3) {
			String userName = arrPhrase[1], password = arrPhrase[2];
			//Se il nome utente non è stato bannato
			if (!credentialsExist(userName, banFile)) {
				//Se non vi sono altri utenti online che utilizzano quel nome e le credenziali esistono e sono corrette allora l'utente viene loggato
				if (!checkUsernameUsed(userName) && credentialsExist(userName, password, userFile)) {
					clientInfo.setClientName(userName);
					new TrasmettitoreServer(clientInfo, "You are now logged in as : " + clientInfo.getClientName());
				} else { //Viene mostrato un errore nel loggarsi
					new TrasmettitoreServer(clientInfo, "Failed to login,try typing /login Name password");
				}
			}else { //Viene mostrato un errore (ban) nel loggarsi
				new TrasmettitoreServer(clientInfo,"Failed to log in,userName has been banned");
			}
		} else { //Viene mostrato un errore nel loggarsi
			new TrasmettitoreServer(clientInfo, "Failed to login,try typing /login Name password");
		}
	}
	
	/**
	 * Method used to handle a service that sends private messages from one client to another
	 * @param arrPhrase String array containing useful information to analyze and utilize
	 */
	private void privateMessageFunction(String[] arrPhrase) {
		//Controlla se la frase è formata da almeno tre parole
		if (arrPhrase.length >= 3) {
			Persona p = getPersonFromUsername(arrPhrase[1]);
			//Se l'utente è online viene assemblato e mandato il messaggio
			if (p != null) {
				StringBuilder sb = new StringBuilder("");
				for (int i = 2; i < arrPhrase.length; ++i) {
					System.out.println(arrPhrase[i]);
					sb.append(arrPhrase[i]);
					sb.append(" ");
				}
				new TrasmettitoreServer(p,
						"Private message from :" + "[" + clientInfo.getClientName() + "] :" + sb.toString());
				writeString("Private message from :" + "[" + clientInfo.getClientName() + "] :" + sb.toString(),"MessageLog.txt");
			} else { //Se l'utente non è online non viene mandato il messaggio
				new TrasmettitoreServer(clientInfo, "Failed to send message,try typing /msg DestName message");
			}
		} else { //Se la frase non rispetta le caratteristiche il messaggio non viene inviato
			new TrasmettitoreServer(clientInfo, "Failed to send message,try typing /msg DestName message");
		}
	}
	
	
	/**
	 * Method used to handle the register service
	 * @param arrPhrase String array containing useful information to analyze and utilize
	 */
	private void registerFunction(String[] arrPhrase) {
		//Registrazione con 4 parole
		if (arrPhrase.length == 4) {
			String userName = arrPhrase[1], password = arrPhrase[2];
			//Se le password inserite sono uguali e le credenziali non esistono di gia' allora viene salvato un nuovo utente
			if (password.equals(arrPhrase[3]) && !credentialsExist(userName,userFile)) {
				writeString(userName + "#/#--#/#" + password + "#/#--#/#n",userFile);
				new TrasmettitoreServer(clientInfo,"User " + userName + " successfully registered");
				writeString("User " + userName + " successfully registered","MessageLog.txt");
			}else {
				new TrasmettitoreServer(clientInfo,"Failed to register,try typing /register Name password password");
			}
		}
		//Registrazione segreta con 5 parole per registrarsi come admin,il codice deve essere corretto
		else if (arrPhrase.length == 5 && arrPhrase[4].equalsIgnoreCase("#3007")) {
			String userName = arrPhrase[1], password = arrPhrase[2];
			//Se le password inserite sono uguali e le credenziali non esistono di gia' allora viene salvato un nuovo utente
			if (password.equals(arrPhrase[3]) && !credentialsExist(userName, userFile)) {
				writeString(userName + "#/#--#/#" + password + "#/#--#/#y", userFile);
				new TrasmettitoreServer(clientInfo, "User " + userName + " successfully registered");
				writeString("Admin user " + userName + " successfully registered","MessageLog.txt");
			} else { 
				new TrasmettitoreServer(clientInfo, "Failed to register,try typing /register Name password password");
			}
		} else {
			new TrasmettitoreServer(clientInfo, "Failed to register,try typing /register Name password password");
		}
	}

	/**
	 * Method used to write a String to the file "Credentials.txt"
	 * @param data String containing userName and password that is going to be saved into the file
	 * @param fileName String representing the path that is going to be used
	 */
	private synchronized void writeString(String data,String fileName) {
		try {
			pw = new PrintWriter(new FileOutputStream(new File(fileName),
					true /* append = true */));
			//viene salvato il messaggio nel file dopo aver aperto lo stream dati
			pw.println(data);
		} catch (FileNotFoundException e) {
			System.out.println("Errore con la manipolazione del file richiesto");
		} finally {
			pw.close();
		}
		pw.flush();
		pw.close();
	}

	/**
	 * Method used to check if userName is already saved into the file
	 * @param userName String containing userName to check
	 * @param fileName String representing the path that is going to be used
	 * @return true if the userName is already saved into the file
	 */
	private synchronized boolean credentialsExist(String userName,String fileName) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(fileName)));
			//lettura linea per linea del file
			String fileLine = reader.readLine();
			String[] arrPhrase;
			while (fileLine != null && fileLine.contains("#/#--#/#")) {
				arrPhrase = fileLine.split("#/#--#/#");
				//Se ogni parola letta dal file non è nulla e la prima di queste è uguale al nome in input allora restituisce true
				if (isNotNull(arrPhrase) && arrPhrase[0].equalsIgnoreCase(userName)) {
					return true;
				}
				fileLine = reader.readLine();
			}
		} catch (IOException e) {
			System.out.println("Errore nella lettura del file");
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				System.out.println("Errore nella chiusura dello stream");
			}
		}
		return false;

	}

	/**
	 * Method used to check if the credentials are correct
	 * @param userName String that indicates user name
	 * @param password String that indicates user password
	 * @param fileName String representing the path that is going to be used
	 * @return true if parameters in input are equals to parameters into the file
	 */
	private synchronized boolean credentialsExist(String userName, String password,String fileName) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(fileName)));
			//Lettura prima linea del file
			String fileLine = reader.readLine();
			String[] arrPhrase;
			while (fileLine != null && fileLine.contains("#/#--#/#")) {
				arrPhrase = fileLine.split("#/#--#/#");
				//Se la lunghezza della frase è di 3 e non contiene parole nulle
				if (arrPhrase.length == 3 && isNotNull(arrPhrase)) {
					//Se la prima parola è uguale all'username
					if (arrPhrase[0].equalsIgnoreCase(userName)) {
						//Se la seconda parola è uguale alla password
						if (arrPhrase[1].equals(password)) {
							//all'utente vengono assegnati i permessi che ha l'utente registrato
							clientInfo.setClientPermissions(arrPhrase[2].equalsIgnoreCase("y"));
							return true;
						}
						return false;
					}
				}
				fileLine = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	/*
	private byte[] encrypt(String s) {
		if (!encryptFlag) {
			try {
				md = MessageDigest.getInstance("SHA-256");
				encryptFlag = false;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				encryptFlag = true;
			}
		}
		byte[] bytesOfMessage;
		try {
			bytesOfMessage = s.getBytes("UTF-8");
			return md.digest(bytesOfMessage);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private byte[] encrypt (String s) {
		try{
            KeyGenerator keygenerator = KeyGenerator.getInstance("DES");
            SecretKey myDesKey = keygenerator.generateKey();

            Cipher desCipher = Cipher.getInstance("DES");

            byte[] text = s.getBytes("UTF8");

            desCipher.init(Cipher.ENCRYPT_MODE, myDesKey);
            byte[] textEncrypted = desCipher.doFinal(text);

            String st = new String(textEncrypted);
            System.out.println(st);
            return textEncrypted;
        }catch(Exception e)
        {
            System.out.println("Exception");
        }
		return null;
	}
	
	private byte[] decrypt (String s) {
		try{
            KeyGenerator keygenerator = KeyGenerator.getInstance("DES");
            SecretKey myDesKey = keygenerator.generateKey();
            Cipher desCipher = Cipher.getInstance("DES");
            desCipher.init(Cipher.DECRYPT_MODE, myDesKey);
            byte[] textDecrypted = desCipher.doFinal(s.getBytes("UTF8"));
            return textDecrypted;
        }catch(Exception e)
        {
            System.out.println("Exception");
        }
		return null;
	}
	*/

	/**
	 * Method used to check if there is someone online using the username requested
	 * @param userName String that indicates user name
	 * @return true if the user name requested is already used
	 */
	private boolean checkUsernameUsed(String userName) {
		//Viene scannerizzato il vettore contenente le persone online
		for (Persona currentPersona : vettPersona) {
			//Se una di queste persona è uguale al nome inserito viene return true
			if (currentPersona.equals(userName)) {
				return true;
			}
		}
		return false;
	}
	/**
	 * Method used to get an Object from the user name
	 * @param userName String that indicates user name
	 * @return Persona object
	 */
	private Persona getPersonFromUsername(String userName) {
		////Viene scannerizzato il vettore contenente le persone online
		for (Persona currentPersona : vettPersona) {
			//Se una di queste persona è uguale al nome inserito viene restituita la persona
			if (currentPersona.equals(userName)) {
				return currentPersona;
			}
		}
		return null;
	}
	
	/**
	 * Method that is used to get a list of the users currently online
	 * @return an Object StringBuilder containing every user name online
	 */
	private StringBuilder getUsersOnline() {
		//Viene scannerizzato il vettore di persone online e restituita la stringa con tutti i nomi
		StringBuilder sb = new StringBuilder();
		for (Persona currentPersona : vettPersona) {
			sb.append(currentPersona.getClientName());
			sb.append(" | ");
		}
		return sb;
	}
	
	/**
	 * 
	 * @param arrPhrase String array containing useful information to analyze and utilize
	 * @return true if every cell in the array was not null
	 */
	private boolean isNotNull(String[] arrPhrase) {
		//Controlla se tutte le celle del vettore in input sono diverse da nullo
		boolean check = true;
		for (int i = arrPhrase.length-1;i>=0 && check;--i) {
			check = arrPhrase[i]!=null;
		}
		return check;
	}
	
	private Vector<Persona> getPersonaFromIp(String ip) {
		//Scannerizza il vettore,se l'ip di una delle persone è uguale a quello in input lo mette in un vettore da restituire alla fine
		Vector<Persona> vett = new Vector<Persona>(); 
		for (Persona currentPersona : vettPersona) {
			if (currentPersona.getClientSocket().getInetAddress().getHostAddress().equalsIgnoreCase(ip)) {
				vett.add(currentPersona);
			}
		}
		return vett;
	}
	
	/**
	 * Method that scan the file given and return a list of users
	 * @param fileName String with file path
	 * @param i int indicating the amount of word in the phrase
	 * @return a String with every registered user listed
	 */
	private StringBuilder getAllRegistered(String fileName, int i) {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(fileName)));
			//Legge la prima linea del file
			String fileLine = reader.readLine();
			String[] arrPhrase;
			while (fileLine != null && fileLine.contains("#/#--#/#")) {
				arrPhrase = fileLine.split("#/#--#/#");
				//Scannerizza il file e aggiunge ogni nome ad una stringa da restituire alla fine
				if (arrPhrase.length == i && isNotNull(arrPhrase)) {
					sb.append(arrPhrase[0]);
					sb.append(" | ");
				}
				fileLine = reader.readLine();
			}
			return sb;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * Method that gets who is admin and returns it
	 * @return a String listing every admin
	 */
	private StringBuilder getAllAdmin() {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(userFile)));
			String fileLine = reader.readLine();
			String[] arrPhrase;
			while (fileLine != null && fileLine.contains("#/#--#/#")) {
				arrPhrase = fileLine.split("#/#--#/#");
				//Scannerizza il file e aggiunge ogni nome di cui la terza cella equivale a "y" ad una stringa da restituire alla fine
				if (arrPhrase.length == 3 && isNotNull(arrPhrase) && arrPhrase[2].equalsIgnoreCase("y")) {
					sb.append(arrPhrase[0]);
					sb.append(" | ");
				}
				fileLine = reader.readLine();
			}
			return sb;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	

}
