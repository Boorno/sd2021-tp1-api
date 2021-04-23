package tp1.api.clients.soap;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;

import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.service.soap.SoapSpreadsheets;

public class DeleteUserSheetsClient {

	public final static String SHEETS_WSDL = "/sheets/?wsdl";
	
	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 1000;
	public final static int REPLY_TIMEOUT = 600;
	
	private String serverUrl;
	private String userId;
	
	public DeleteUserSheetsClient(String serverUrl, String userId, String password) {
		this.serverUrl = serverUrl;
		this.userId = userId;
	}
	
	public void deleteUserSheets() throws MalformedURLException {

		SoapSpreadsheets sheets = null;
		
		try {
			QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
			Service service = Service.create( new URL(serverUrl + SHEETS_WSDL), QNAME );
			sheets = service.getPort( tp1.api.service.soap.SoapSpreadsheets.class );
		} catch ( WebServiceException e) {
			System.err.println("Could not contact the server: " + e.getMessage());
			System.exit(1);
		}
		
		//Set timeouts for executing operations
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		((BindingProvider) sheets).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
	
		System.out.println("Sending request to server.");

		short retries = 0;
		boolean success = false;

		while(!success && retries < MAX_RETRIES) {

			try {
				sheets.deletedUser(userId);
				success = true;
			} catch (WebServiceException wse) {
				System.out.println("Communication error.");
				wse.printStackTrace();
				retries++;
				try { Thread.sleep( RETRY_PERIOD ); } catch (InterruptedException e) {
					//nothing to be done here, if this happens we will just retry sooner.
				}
				System.out.println("Retrying to execute request.");
			}
		}
	}

}
