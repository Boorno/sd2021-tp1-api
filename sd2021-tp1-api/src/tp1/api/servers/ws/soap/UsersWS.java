package tp1.api.servers.ws.soap;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.jws.WebService;
import tp1.api.User;
import tp1.api.clients.soap.DeleteUserSheetsClient;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.discovery.Discovery;

@WebService(serviceName=SoapUsers.NAME, 
targetNamespace=SoapUsers.NAMESPACE, 
endpointInterface=SoapUsers.INTERFACE)
public class UsersWS implements SoapUsers {

	private final Map<String,User> users;

	private static Logger Log = Logger.getLogger(UsersWS.class.getName());
	
	private String domain;
	
	private Discovery discovery;

	public UsersWS() {
		this.users = new HashMap<String, User>();	 
	}
	
	public UsersWS(String domain, Discovery d) {
		this.users = new HashMap<String, User>();
		this.domain = domain;
		this.discovery = d;
	}
	
	private String getSheetURI() {
		String sheetsURI;
		while (true) {
			URI[] sheetURIs = discovery.knownUrisOf(domain + ":sheets");
			if (sheetURIs != null) {
				sheetsURI = sheetURIs[0].toString();
				break;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return sheetsURI;
	}

	@Override
	public String createUser(User user) throws UsersException {
		Log.info("createUser : " + user);

		// Check if user is valid, if not throw exception
		if(user.getUserId() == null || user.getPassword() == null || user.getFullName() == null || 
				user.getEmail() == null) {
			Log.info("User object invalid.");
			throw new UsersException("Invalid user instance.");
		}

		synchronized (this) {
			// Check if userId does not exist exists, if not throw exception
			if( users.containsKey(user.getUserId())) {
				Log.info("User already exists.");
				throw new UsersException("User already exists.");
			}

			//Add the user to the map of users
			users.put(user.getUserId(), user);
		}

		return user.getUserId();
	}

	@Override
	public User getUser(String userId, String password) throws UsersException {
		Log.info("getUser : user = " + userId + "; pwd = " + password);

		// Check if user is valid, if not throw exception
		if(userId == null || password == null) {
			Log.info("UserId or passwrod null.");
			throw new UsersException("UserId or password are null.");
		}

		User user = null;
		
		synchronized (this) {
			user = users.get(userId);
		}
		 

		// Check if user exists, if yes throw exception
		if( user == null ) {
			Log.info("User does not exist.");
			throw new UsersException("User does not exist.");
		}

		//Check if the password is correct, if not throw exception
		if( !user.getPassword().equals( password)) {
			Log.info("Password is incorrect.");
			throw new UsersException("Password is incorrect.");
		}

		return user;
	}

	@Override
	public User updateUser(String userId, String password, User user) throws UsersException {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);
		
		User svUser = null;

		synchronized (this) {

			svUser = users.get(userId);

			if (svUser == null) {
				Log.info("User does not exist.");
				throw new UsersException("User does not exist.");
			}

			if (!svUser.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new UsersException("Password is incorrect.");
			}
			
			if(user.getEmail() != null) svUser.setEmail(user.getEmail());
			if(user.getFullName() != null) svUser.setFullName(user.getFullName());
			if(user.getPassword() != null) svUser.setPassword(user.getPassword());
			
		}

		return svUser;
	}

	@Override
	public User deleteUser(String userId, String password) throws UsersException {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password);
		
		System.out.println("\nDELETEDUSER\n");
		
		User user = null;
		
		synchronized (this) {

			user = users.get(userId);

			// Check if user exists
			if (user == null) {
				Log.info("User does not exist.");
				throw new UsersException("User does not exist.");
			}

			// Check if the password is correct
			if (!user.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new UsersException("Password is incorrect.");
			}

			users.remove(userId);

		}
		
		String sheetsURI = getSheetURI();
		
		try {
		(new DeleteUserSheetsClient(sheetsURI, userId)).deleteUserSheets();
		} catch (Exception e) {
			throw new UsersException("Cant delete sheets from user.");
		}
				
		return user;
	}

	@Override
	public List<User> searchUsers(String pattern) throws UsersException {
		Log.info("searchUsers : pattern = " + pattern);

		if (pattern == null || pattern.equals("")) {
			return (List<User>) users.values();
		}

		List<User> sUsers = new LinkedList<User>();

		synchronized (this) {
			for (User user : users.values()) {
				if (user.getFullName().toLowerCase().contains(pattern.toLowerCase())) {
					User u = new User(user.getUserId(), user.getFullName(), user.getEmail(), "");
					sUsers.add(u);
				}
			}
		}
		return sUsers;
	}

	@Override
	public void existsUser(String userId) throws UsersException {
		synchronized (this) {
			if(!users.containsKey(userId)) {
				Log.info("User does not exist.");
				throw new UsersException("User does not exist.");
			}
		}
	}

}
