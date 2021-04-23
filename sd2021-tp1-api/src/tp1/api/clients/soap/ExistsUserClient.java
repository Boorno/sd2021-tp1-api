package tp1.api.clients.soap;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;

public class ExistsUserClient {
public final static String USERS_WSDL = "/users/?wsdl";
	
	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 1000;
	public final static int REPLY_TIMEOUT = 600;
	
	private String serverUrl;
	private String userId;
	
	public ExistsUserClient(String serverUrl, String userId) {
		this.serverUrl = serverUrl;
		this.userId = userId;
	}

	public void existsUser() throws MalformedURLException, UsersException {

		SoapUsers users = null;
		
		try {
			QName QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
			Service service = Service.create( new URL(serverUrl + USERS_WSDL), QNAME );
			users = service.getPort( tp1.api.service.soap.SoapUsers.class );
		} catch ( WebServiceException e) {
			System.err.println("Could not contact the server: " + e.getMessage());
			System.exit(1);
		}
		
		//Set timeouts for executing operations
		((BindingProvider) users).getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		((BindingProvider) users).getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, REPLY_TIMEOUT);
	
		System.out.println("Sending request to server.");

		short retries = 0;
		boolean success = false;

		while(!success && retries < MAX_RETRIES) {
			
			try {
				users.existsUser(userId);
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
