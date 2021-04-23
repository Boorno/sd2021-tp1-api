package tp1.api.clients.soap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.servers.soap.SheetsServer;

public class GetValuesClientSoap {
	public final static String SHEETS_WSDL = "/sheets/?wsdl";

	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 1000;
	public final static int REPLY_TIMEOUT = 600;

	private static Map<String, String[][]> cache = new HashMap<String, String[][]>();

	private String sheetURL;
	private String serverUrl;
	private String sheetId;
	private String userId;

	public GetValuesClientSoap(String sheetURL, String userId) {
		this.sheetURL = sheetURL;
		String[] sheetInfo = sheetURL.split("/" + SheetsServer.SERVICE + "/");
		this.serverUrl = sheetInfo[0];
		this.sheetId = sheetInfo[1];
		this.userId = userId;
	}

	public String[][] getValues() throws SheetsException, MalformedURLException {

		SoapSpreadsheets sheets = null;

		try {
			QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
			Service service = Service.create(new URL(serverUrl + SHEETS_WSDL), QNAME);
			sheets = service.getPort(tp1.api.service.soap.SoapSpreadsheets.class);
		} catch (WebServiceException e) {
			System.err.println("Could not contact the server: " + e.getMessage());
			System.exit(1);
		}

		// Set timeouts for executing operations
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT,
				CONNECTION_TIMEOUT);
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);

		System.out.println("Sending request to server.");

		short retries = 0;
		boolean success = false;

		while (!success && retries < MAX_RETRIES) {

			try {
				String[][] values = sheets.importRanges(sheetId, userId);
				cache.put(sheetURL, values);
				return values;
			} catch (WebServiceException wse) {
				System.out.println("Communication error.");
				wse.printStackTrace();
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
		
		throw new SheetsException("Cant import range.");
	}
}
