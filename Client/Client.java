package Client;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.DefaultCaret;
/**
 * Class client used to set a GUI,connect to the server,get messages from the user and send it
 * @author Giachetto Daniele
 *
 */
public class Client {

	private static Socket s;
	private static JTextArea textArea;
	private static JTextField input;
	private String serverIp;
	private Receiver first;
	private JPasswordField fieldConfirmPass;
	private JPasswordField fieldPass;
	private JTextField fieldSecondary;
	private JTextField fieldName;

	/**
	 * Constructor that gets through input the Server ip inputed from the client
	 * @param ip server ip
	 */
	public Client(String ip) {
		if (ip.equals("")) {
			this.serverIp = "127.0.0.1";
		} else {
			this.serverIp = ip;
		}
		createFrame();
	}

	/**
	 * Method that contains creates an interface and handle basic feature like disconnecting connecting and sending messages
	 */
	public void createFrame() {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				//Creazione dell'interfaccia e assegnazione di vari attributi ad essa
				JFrame frame = new JFrame("Chat | IP --> " + serverIp);
				JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
				panel.setOpaque(true);
				JButton loginButton = new JButton("Login");
				JButton registerButton = new JButton("Register");
				JButton msgButton = new JButton("Send private message");
				//Creazione textArea dove verranno visualizzati messaggi
				textArea = new JTextArea(15, 50);
				textArea.setWrapStyleWord(true);
				textArea.setEditable(false);
				textArea.setFont(Font.getFont(Font.SANS_SERIF));
				JScrollPane scroller = new JScrollPane(textArea);
				scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				JPanel inputpanel = new JPanel();
				inputpanel.setLayout(new FlowLayout());
				//Creazione textField da dove verranno prelevati i messaggi da spedire
				input = new JTextField(30);
				JButton enterButton = new JButton("Enter");
				JButton exitButton = new JButton("Disconnect");
				JButton reloadButton = new JButton("Re-establish connection");
				//Action listener per gestire l'invio utilizzando il tasto sulla tastiera
				input.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String inputText = input.getText();
						if (first.getStatus()) { // Se la connessione con il server è ancora up
							if (inputText == null || inputText.equals("")) { // se il messaggio non è vuoto

							} else if (inputText.equalsIgnoreCase("/exit")) { // se il messaggio è /exit provvedere a chiudere correttamente la connessione
								// Invio al server del messaggio di chiusura
								new Transmitter("/Exit", s);
								enterButton.setEnabled(false);
								exitButton.setEnabled(false);
								reloadButton.setEnabled(true);
								msgButton.setEnabled(false);
								registerButton.setEnabled(false);
								loginButton.setEnabled(false);
								first.setStatus(false);
								textArea.append("Connection with the server has been interrupted \n");
								input.setText("");
							} else { // Se è un normale messaggio con connessione up allora lo si analizza ed in caso invia al server
								messageFunction(inputText);
								input.setText("");
							}
						} else { // Se la connessione non è attiva mostrare un messaggio di errore
							input.setText("");
							textArea.append("Server unreachable,try re-establish button | Error : #9899 \n");
						}
					}
				});
				

				//Tentativo di instaurazione della connessione
				establishConnection();
				if (s!=null) { //Se la connessione riesce creazione di un thread separato per gestire i messaggi dal server
					first = new Receiver(s, textArea);
					reloadButton.setEnabled(false);
				}else {
					enterButton.setEnabled(false);
					exitButton.setEnabled(false);
					reloadButton.setEnabled(true);
					msgButton.setEnabled(false);
					registerButton.setEnabled(false);
					loginButton.setEnabled(false);
				}
				
				//Creazione di un listener per quando viene clickato il pulsante "enter"
				enterButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String inputText = input.getText();
						if (first!=null && first.getStatus()) {	//Se la connessione con il server è ancora up
							if (inputText == null || inputText.equals("")) { //se il messaggio non è vuoto

							} else if (inputText.equalsIgnoreCase("/exit")) { //se il messaggio è /exit provvedere a chiudere correttamente la connessione
								//Invio al server del messaggio di chiusura
								new Transmitter("/Exit", s);
								first.setStatus(false);
								enterButton.setEnabled(false);
								exitButton.setEnabled(false);
								reloadButton.setEnabled(true);
								msgButton.setEnabled(false);
								registerButton.setEnabled(false);
								loginButton.setEnabled(false);
								textArea.append("Connection with the server has been interrupted \n");
								System.out.println(first.getStatus());
								input.setText("");
							}else  { //Se è un normale messaggio con connessione up allora lo si analizza ed in caso invia al server
								messageFunction(inputText);
								input.setText("");
							}
						}else { //Se la connessione non è attiva mostrare un messaggio di errore
							input.setText("");
							textArea.append("Server unreachable,try re-establish button | Error : #9899 \n");
						}
					}
				});
				//Creazione di un listener per quando viene clickato il pulsante "Disconnect"
				
				exitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//Se la connessione è attiva provvedere ad interromperla correttamente
						if (first!=null && first.getStatus()) {
							//Invio al server del messaggio di chiusura
							new Transmitter("/Exit", s);
							enterButton.setEnabled(false);
							exitButton.setEnabled(false);
							reloadButton.setEnabled(true);
							msgButton.setEnabled(false);
							registerButton.setEnabled(false);
							loginButton.setEnabled(false);
							first.setStatus(false);
							System.out.println(first.getStatus());
							textArea.append("Connection with the server has been interrupted \n");
						}else { //Se la connessione non è attiva non fare nulla
							
						}
					}
				});
				//Creazione di un listener per quando viene clickato il pulsante ("Re-establish connection")
				
				reloadButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//Se la connessione è attiva provvedere ad avvisare il client di ciò
						if (first!=null && first.getStatus()) {
							System.out.println(first.getStatus());
							textArea.append("Connection is already established,reload only when connection is not working \n");	
						}else { //Se la connessione non è attiva provvedere ad instaurarla correttamente
							establishConnection();
							if (s!=null) { //Se la connessione è stata instaurata correttamente instaurare il thread per ricevere messaggi
								first = new Receiver(s, textArea);
								enterButton.setEnabled(true);
								exitButton.setEnabled(true);
								reloadButton.setEnabled(false);
								msgButton.setEnabled(true);
								registerButton.setEnabled(true);
								loginButton.setEnabled(true);
							}
						}
					}
				});
				//Definizione della modalita' in cui si potra' chiudere la finestra
				frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				frame.addWindowListener(new java.awt.event.WindowAdapter() {
					@Override
					public void windowClosing(java.awt.event.WindowEvent windowEvent) {
						//Mostrare finestra di dialogo in cui chiedere se si è sicuri di voler chiudere la finestra
						if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to close this window?",
								"Warning", JOptionPane.YES_NO_OPTION,
								//se l'utente conferma di voler chiudere la finestra essa verra' chiusa definitivamente
								JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
							//se la connessione è attiva provvedere a chiuderla correttamente contattando il server
							if (first!=null && first.getStatus()) {
								new Transmitter("/Exit", s);
							}
							System.exit(0);
						}
					}
				});
				
				
				loginButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JLabel lblMessage = new JLabel("Login here!");
						JCheckBox checkbox = new JCheckBox("Enable logging");
						JLabel insNameMsg = new JLabel("Enter name here");
						fieldName = new JTextField();
						JLabel insPassMsg = new JLabel("Enter password here");
						fieldPass = new JPasswordField();
						Object[] inputData = { lblMessage,insNameMsg, fieldName,insPassMsg, fieldPass,checkbox};
						JOptionPane.showMessageDialog(null, inputData);
						
						if (checkbox.isSelected()) {
							String password = new String(fieldPass.getPassword());
							new Transmitter("/login " + fieldName.getText() + " "+ password,s);
						}else {
							System.out.println("CheckBox not selected");
						}
					}
				});
				
				registerButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JLabel lblMessage = new JLabel("Register here!");
						JCheckBox checkbox = new JCheckBox("Enable register");
						JLabel insNameMsg = new JLabel("Enter name here");
						fieldName = new JTextField();
						JLabel insPassMsg = new JLabel("Enter password here");
						fieldPass = new JPasswordField();
						JLabel insConfPassMsg = new JLabel("Confirm password here");
						fieldConfirmPass = new JPasswordField();
						Object[] inputData = { lblMessage,insNameMsg, fieldName,insPassMsg, fieldPass,
								insConfPassMsg,fieldConfirmPass,checkbox};
						JOptionPane.showMessageDialog(null, inputData);
						
						if (checkbox.isSelected()) {
							String confirmPass = new String(fieldConfirmPass.getPassword());
							String password = new String(fieldPass.getPassword());
							new Transmitter("/register " + fieldName.getText() + " "+ password + " " + 
									confirmPass,s);
						}else {
							System.out.println("CheckBox not selected");
						}
					}
				});
				
				msgButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JLabel lblMessage = new JLabel("send private msg here!");
						JCheckBox checkbox = new JCheckBox("Enable message");
						JLabel insNameMsg = new JLabel("Enter name here");
						fieldName = new JTextField();
						JLabel insMsg = new JLabel("Enter message here");
						fieldSecondary = new JTextField();
						Object[] inputData = { lblMessage,insNameMsg, fieldName,insMsg, fieldSecondary,checkbox};
						JOptionPane.showMessageDialog(null, inputData);
						
						if (checkbox.isSelected()) {
							new Transmitter("/msg " + fieldName.getText() + " "+ fieldSecondary.getText(),s);
						}else {
							System.out.println("CheckBox not selected");
						}
					}
				});
				
				//Ultimi dettagli sugli attributi dell'interfaccia grafica
				DefaultCaret caret = (DefaultCaret) textArea.getCaret();
				caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
				panel.add(scroller);
				inputpanel.add(registerButton);
				inputpanel.add(loginButton);
				inputpanel.add(msgButton);
				inputpanel.add(input);
				inputpanel.add(enterButton);
				inputpanel.add(exitButton);
				inputpanel.add(reloadButton);
				panel.add(inputpanel);
				frame.getContentPane().add(BorderLayout.CENTER, panel);
				frame.pack();
				frame.setLocationByPlatform(true);
				frame.setVisible(true);
				frame.setResizable(false);
				input.requestFocus();
			}
		});
	}
	
	/**
	 * Method used to send inputed message to the server or if it's a specific message to display something to the client
	 * @param msg User's inputed message
	 */
	public void messageFunction(String msg) {
		//Spezzare la frase ad ogni spazio
		String[] arrPhrase = msg.split(" ");
		//Se la frase contiene due parole
		if (arrPhrase.length==2) {
			//Se la prima parola è "/show" ed
			 if (arrPhrase[0].equalsIgnoreCase("/show")) {
				 //Se la seconda parola è "help" allora il comando è corretto e viene mostrato all'utente il risultato richiesto
				 if (arrPhrase[1].equalsIgnoreCase("help")) {
						textArea.append("You can use the following commands : 1. show online/admin/banned/registered/permission \n");
						textArea.append("2. /msg DestName message 3. /login Name password 4. /register Name password password \n");
				 }else {
					 //Se la seconda parola non e' "help" allora il messaggio sara' inviato al server ed analizzato piu' approfonditamente
					 new Transmitter(msg, s);
				 }
			 }else {
				 //Se la prima parola e' diversa da "show" allora il messaggio sara' inviato al server ed analizzato piu' approfonditamente
				 new Transmitter(msg, s);
			 }
		}
		//Se la frase ha solo una parola
		else if(arrPhrase.length==1){
			//Se la prima parola della frase è uguale a "/clear"
			if(arrPhrase[0].equalsIgnoreCase("/clear")) {
				//Pulire lo schermo da tutti i messaggi
				textArea.setText("");
			}
			//Se la prima parola e' diversa da "/clear" il messaggio sara' inviato al server ed analizzato piu' approfonditamente
			else {
				new Transmitter(msg, s);
			}
		}
		//Se la frase non ha ne una ne due parole il messaggio sara' inviato al server ed analizzato piu' approfonditamente
		else {
			 new Transmitter(msg, s);
		}
	}
	
	/**
	 * Method used to establish connection with the server,it tries to do that with inputed ip,local host and set ip
	 */
	public void establishConnection() {
		try {
			//Prova a connettersi all'ip inserito dall'utente
			s = new Socket(serverIp, 55555);
		} catch (IOException e) {
			System.out.println("Connection failed with inputed ip");
			try {
				//Prova a connettersi a localhost
				s = new Socket("127.0.0.1", 55555);
				serverIp = "127.0.0.1";
			} catch (IOException e1) {
				System.out.println("Connection failed with local host");
				try {
					//Prova a connettersi con un ip di default
					s = new Socket("192.168.103.213", 55555);
					serverIp = "192.168.103.213";
				} catch (IOException e2) {
					//Se non riesce a connettersi a niente allora mostra all'utente che l'operazione non è stata possibile 
					textArea.append("Failed to establish a connection" + "\n");
					s=null;
				}
			}
		}
	}

}
