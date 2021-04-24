package tp1.api.clients.rest;

import java.util.HashMap;
import java.util.Map;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;

public class RestClients {
	
	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 10000;
	public final static int REPLY_TIMEOUT = 600;
	
	private static Map<String, String[][]> cache = new HashMap<String, String[][]>();
	
	private String sheetURL;
	private String serverUrl;
	private String sheetId;
	private String userId;
	private String password;
	
	public RestClients(String[] args) {
		switch(args.length) {
		case 2: serverUrl = args[0];
				userId = args[1];
				break;
		case 3: serverUrl = args[0];
				userId = args[1];
				password = args[2];
				break;
		case 4: sheetURL = args[0];
				serverUrl = args[1];
				sheetId = args[2];
				userId = args[3];
		}		
	}
	
	public int getUser() {
		
		ClientConfig config = new ClientConfig();
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
		Client client = ClientBuilder.newClient(config);

		WebTarget target = client.target( serverUrl ).path( RestUsers.PATH );

		short retries = 0;

		while(retries < MAX_RETRIES) {
			
			try {
			Response r = target.path( userId).queryParam("password", password).request()
					.accept(MediaType.APPLICATION_JSON)
					.get();

			return r.getStatus();
			} catch (ProcessingException pe) {
				System.out.println("Timeout occurred");
				retries++;
				try { Thread.sleep( RETRY_PERIOD ); } catch (InterruptedException e) {
					//nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}
		
		return Status.BAD_GATEWAY.getStatusCode();

	}
	
	public void deleteUserSheets() {

		ClientConfig config = new ClientConfig();
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
		Client client = ClientBuilder.newClient(config);

		WebTarget target = client.target(serverUrl).path(RestSpreadsheets.PATH);

		short retries = 0;
		boolean success = false;

		while (!success && retries < MAX_RETRIES) {

			try {
				target.path("deletedUser/" + userId).request().accept(MediaType.APPLICATION_JSON).delete();
				success = true;

			} catch (ProcessingException pe) {
				System.out.println("Timeout occurred");
				retries++;
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException e) {
					// nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}

	}
	
	public String[][] getValues() {

		ClientConfig config = new ClientConfig();
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
		Client client = ClientBuilder.newClient(config);

		WebTarget target = client.target(serverUrl).path(RestSpreadsheets.PATH);

		short retries = 0;
		
		boolean success = false;
		
		while (!success && retries < MAX_RETRIES) {

			try {
								
				Response r = target.path(sheetId + "/import").queryParam("userId", userId)
						.request()
						.accept(MediaType.APPLICATION_JSON)
						.get();
								
				if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
					String[][] s = r.readEntity(String[][].class);
					cache.put(sheetURL, s);
					return s;
				} else
					System.out.println("Error, HTTP error status: " + r.getStatus() );
				
				success = true;
			} catch (ProcessingException pe) {
				System.out.println("Timeout occurred");
				retries++;
				String[][] values = cache.get(sheetURL);
				if(retries >= MAX_RETRIES && values != null) {
					return values;
				}
				try {
					Thread.sleep(RETRY_PERIOD);
				} catch (InterruptedException e) {
					// nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}

		return null;

	}

}
