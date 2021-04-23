package tp1.api.clients.rest;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.util.HashMap;
import java.util.Map;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;

public class GetValuesClient {

	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 1000;
	public final static int REPLY_TIMEOUT = 600;
	
	private static Map<String, String[][]> cache = new HashMap<String, String[][]>();

	private String sheetURL;
	private String serverUrl;
	private String sheetId;
	private String userId;

	public GetValuesClient(String sheetURL, String userId) {
		this.sheetURL = sheetURL;
		String[] sheetInfo = sheetURL.split(RestSpreadsheets.PATH+"/");
		this.serverUrl = sheetInfo[0];
		this.sheetId = sheetInfo[1];
		this.userId = userId;
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

		throw new WebApplicationException(Status.BAD_GATEWAY);

	}

}
