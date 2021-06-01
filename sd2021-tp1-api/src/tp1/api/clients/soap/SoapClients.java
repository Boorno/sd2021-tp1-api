package tp1.api.clients.soap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.sun.xml.ws.client.BindingProviderProperties;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;
import tp1.servers.soap.SheetsServer;

public class SoapClients {
	
	public final static String USERS_WSDL = "/users/?wsdl";
	public final static String SHEETS_WSDL = "/spreadsheets/?wsdl";
	
	public final static int MAX_RETRIES = 3;
	public final static long RETRY_PERIOD = 1000;
	public final static int CONNECTION_TIMEOUT = 10000;
	public final static int REPLY_TIMEOUT = 600;
	
	private String serverURL;
	private String userId;
	private String password;
	private String sheetURL;
	private String sheetId;
	private static Map<String, String[][]> cache = new HashMap<String, String[][]>();
	
	public SoapClients(String[] args) {
		
		switch (args[0]) {
		
		case "delete":
			this.serverURL = args[1];
			this.userId = args[2];
			break;
		case "user":
			this.serverURL = args[1];
			this.userId = args[2];
			this.password = args[3];
			break;
		case "values":
			this.sheetURL = args[1];
			String[] sheetInfo = sheetURL.split("/" + SheetsServer.SERVICE + "/");
			this.serverURL = sheetInfo[0];
			this.sheetId = sheetInfo[1];
			this.userId = args[2];
			break;
		
		}
		
	}
	
	public void deleteUserSheets() throws MalformedURLException {

		SoapSpreadsheets sheets = null;
		
		try {
			QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
			Service service = Service.create( new URL(serverURL + SHEETS_WSDL), QNAME );
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
	
	public void existsUser() throws MalformedURLException, UsersException {

		SoapUsers users = null;
		
		try {
			QName QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
			Service service = Service.create( new URL(serverURL + USERS_WSDL), QNAME );
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
	
	public void getUser() throws MalformedURLException, UsersException {

		SoapUsers users = null;
		
		try {
			QName QNAME = new QName(SoapUsers.NAMESPACE, SoapUsers.NAME);
			Service service = Service.create( new URL(serverURL + USERS_WSDL), QNAME );
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
				users.getUser(userId, password);
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
	
	public String[][] getValues() throws SheetsException, MalformedURLException {

		SoapSpreadsheets sheets = null;

		try {
			QName QNAME = new QName(SoapSpreadsheets.NAMESPACE, SoapSpreadsheets.NAME);
			Service service = Service.create(new URL(serverURL + SHEETS_WSDL), QNAME);
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
