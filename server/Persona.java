package server;
import java.net.Socket;

/**
 * Class used to store all data needed to maintain a connection between client and server
 * @author Giachetto Daniele
 *
 */
public class Persona {
	
	private Socket clientSocket;
	private String clientName;
	private boolean admin;
	private boolean banned;
	private boolean kicked;
	
	/**
	 * Constructor of the Persona class
	 * @param cs Object of the class Socket 
	 * @param cn Object of the class String representing the client name
	 */
	public Persona(Socket cs,String cn) {
		setClientSocket(cs);
		setClientName(cn);
		setClientPermissions(false);
		setBanned(false);
		setKicked(false);
	}
	
	/**
	 * Method used to set the user as kicked or not
	 * @param b boolean to set if kicked or not
	 */
	public void setKicked(boolean b) {
		this.kicked = b;
	}
	
	/**
	 * Method used to get variable kicked
	 * @return true if the user has been kicked
	 */
	public boolean isKicked() {
		return kicked;
	}

	/**
	 * Method used to set User permission
	 * @param perm is the permission level of the user
	 */
	public void setClientPermissions(boolean perm) {
		this.admin = perm;
	}
	
	/**
	 * Method used to get permission level
	 * @return true if it's an admin
	 */
	public boolean isAdmin() {
		return admin;
	}
	
	/**
	 * Method used to get variable banned
	 * @return true if the user has been banned
	 */
	public boolean isBanned() {
		return banned;
	}
	
	/**
	 * Method used to set the user as banned or not
	 * @param banned used to set if it's banned or not
	 */
	public void setBanned(boolean banned) {
		this.banned = banned;
	}

	/**
	 * Method that is used to convert the Object into a String
	 * @return the Object into a String
	 */
	public String toString() {
		return getClientName() + "||" + getClientSocket();
	}
	
	/**
	 * Getter used to get the socket
	 * @return Socket Object representing the client socket
	 */
	public Socket getClientSocket() {
		return clientSocket;
	}
	
	/**
	 * Setter used to set the socket
	 * @param clientSocket socket to store into the object
	 */
	public void setClientSocket(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	/**
	 * Getter used to get the client name
	 * @return a String representing the client name
	 */
	public String getClientName() {
		return clientName;
	}

	/**
	 * Setter used to set the client name
	 * @param clientName String to store into the Object
	 */
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	
	/**
	 * equals method that checks socket and client name
	 * @param cs Socket to check
	 * @param cn String representing the client name to check
	 * @return true if socket and client name are equals to the variables inputed
	 */
	public boolean equals(Socket cs,String cn) {
		return ((cs.equals(getClientSocket())) && (cn.equalsIgnoreCase(getClientName()))) || cs.equals(getClientSocket());
	}
	
	/**
	 * equals method that checks only the client name
	 * @param cn String representing the client name to check
	 * @return true if the client name is equals to the input client name
	 */
	public boolean equals(String cn) {
		return cn.equalsIgnoreCase(getClientName());
	}

}
