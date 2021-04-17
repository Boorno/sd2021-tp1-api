package tp1.api.clients;

import java.io.IOException;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import tp1.api.Spreadsheet;
import tp1.api.service.rest.RestSpreadsheets;

public class CreateSpreadSheetClient {


	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 1000;
	public final static int REPLY_TIMEOUT = 600;

	public static void main(String[] args) throws IOException {

		if( args.length != 8) {
			System.err.println( "Use: java sd2021.tp1.api.clients.CreateSpreadsheetClient url sheetId owner sheetURL lines columns sharedWith rawValues");
			return;
		}

		String serverUrl = args[0];
		String sheetId = args[1];
		String owner = args[2];
		String sheetURL = args[3];
		String lines = args[4];
		String columns = args[5];
		String sharedWith = args[6];
		String rawValues = args[7];
		
		Spreadsheet spreadsheet = new Spreadsheet(sheetId, owner, sheetURL, lines, columns, sharedWith, rawValues);

		System.out.println("Sending request to server.");

		ClientConfig config = new ClientConfig();
		//how much time until we timeout when opening the TCP connection to the server
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		//how much time do we wait for the reply of the server after sending the request
		config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
		Client client = ClientBuilder.newClient(config);

		WebTarget target = client.target( serverUrl ).path( RestUsers.PATH );

		short retries = 0;
		boolean success = false;

		while(!success && retries < MAX_RETRIES) {

			try {
				Response r = target.request()
						.accept(MediaType.APPLICATION_JSON)
						.post(Entity.entity(u, MediaType.APPLICATION_JSON));

				if( r.getStatus() == Status.OK.getStatusCode() && r.hasEntity() )
					System.out.println("Success, created user with id: " + r.readEntity(String.class) );
				else
					System.out.println("Error, HTTP error status: " + r.getStatus() );
				success = true;
			} catch (ProcessingException pe) {
				System.out.println("Timeout occurred");
				pe.printStackTrace();
				retries++;
				try { Thread.sleep( RETRY_PERIOD ); } catch (InterruptedException e) {
					//nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}
	}

}
