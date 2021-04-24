package tp1.servers.resources.rest;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.clients.rest.RestClients;
import tp1.api.service.rest.RestUsers;
import tp1.discovery.Discovery;

@Singleton
public class UsersResource implements RestUsers {

	private final Map<String, User> users = new HashMap<String, User>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());
	
	private String domain;

	private Discovery discovery;

	public UsersResource() {
	}
	
	public UsersResource(String domain, Discovery d) {
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
	public String createUser(User user) {
		Log.info("createUser : " + user);

		if (user.getUserId() == null || user.getPassword() == null || user.getFullName() == null
				|| user.getEmail() == null) {
			Log.info("User object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		synchronized (this) {

			// Check if userId does not exist exists, if not return HTTP CONFLICT (409)
			if (users.containsKey(user.getUserId())) {
				Log.info("User already exists.");
				throw new WebApplicationException(Status.CONFLICT);
			}
			
			// Add the user to the map of users
			users.put(user.getUserId(), user);

		}

		return user.getUserId();
	}

	@Override
	public User getUser(String userId, String password) {
		Log.info("getUser : user = " + userId + "; pwd = " + password);

		User user = null;

		synchronized (this) {
			user = users.get(userId);
		}

		// Check if user exists
		if (user == null) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		// Check if the password is correct
		if (!user.getPassword().equals(password)) {
			Log.info("Password is incorrect.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		return user;
	}

	@Override
	public User updateUser(String userId, String password, User user) {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);
		
		User svUser = null;

		synchronized (this) {

			svUser = users.get(userId);

			if (svUser == null) {
				Log.info("User does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			if (!svUser.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}
			
			if(user.getEmail() != null) svUser.setEmail(user.getEmail());
			if(user.getFullName() != null) svUser.setFullName(user.getFullName());
			if(user.getPassword() != null) svUser.setPassword(user.getPassword());
			
		}

		return svUser;
	}

	@Override
	public User deleteUser(String userId, String password) {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password);
		
		User user = null;
		
		synchronized (this) {

			user = users.get(userId);

			// Check if user exists
			if (user == null) {
				Log.info("User does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			// Check if the password is correct
			if (!user.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}

			users.remove(userId);

		}
		
		String sheetsURI = getSheetURI();

		//DeleteUserSheetsClient deluser = new DeleteUserSheetsClient(sheetsURI, userId);
		
		String[] args = {sheetsURI, userId};
		RestClients deluser = new RestClients(args);
		
		deluser.deleteUserSheets();
		
		return user;

	}

	@Override
	public List<User> searchUsers(String pattern) {
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

}
