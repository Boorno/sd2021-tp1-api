package tp1.api.clients;

import java.io.IOException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;

public class GetValuesClient {

	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 1000;
	public final static int REPLY_TIMEOUT = 600;

	private String serverUrl;
	private String sheetId;

	public GetValuesClient(String serverUrl, String sheetId) {
		this.serverUrl = serverUrl;
		this.sheetId = sheetId;
	}

	public String[][] getValues() {

		ClientConfig config = new ClientConfig();
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
		Client client = ClientBuilder.newClient(config);

		WebTarget target = client.target(serverUrl).path(RestSpreadsheets.PATH);

		short retries = 0;

		while (retries < MAX_RETRIES) {

			try {
				Response r = target.path("import/" + sheetId).request().accept(MediaType.APPLICATION_JSON).get();

				Spreadsheet s = null;

				if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
					s = r.readEntity(Spreadsheet.class);
					return s.getRawValues();
				}
			} catch (ProcessingException pe) {
				System.out.println("Timeout occurred");
				pe.printStackTrace();
				retries++;
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
