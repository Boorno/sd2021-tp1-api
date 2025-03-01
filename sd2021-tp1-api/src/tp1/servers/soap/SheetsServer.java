package tp1.servers.soap;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;

import jakarta.xml.ws.Endpoint;
import tp1.api.servers.ws.soap.SpreadsheetWS;
import tp1.discovery.Discovery;

public class SheetsServer {

	private static Logger Log = Logger.getLogger(SheetsServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static final int PORT = 8080;
	public static final String SERVICE = "sheets";
	public static final String SOAP_SHEETS_PATH = "/soap/spreadsheets";

	public static void main(String[] args) {
		try {
			String ip = InetAddress.getLocalHost().getHostAddress();
			String serverURI = String.format("http://%s:%s/soap", ip, PORT);
			
			String domain = args[0];
			
			HttpServer server = HttpServer.create(new InetSocketAddress(ip, PORT), 0);
			
			server.setExecutor(Executors.newCachedThreadPool());
			
			Discovery discovery = new Discovery( new InetSocketAddress("226.226.226.226", 2266), domain+":"+SERVICE, serverURI);
			discovery.start();
			
			Endpoint soapSheetsEndpoint = Endpoint.create(new SpreadsheetWS(domain, discovery, serverURI+"/"+SERVICE));
			
			soapSheetsEndpoint.publish(server.createContext(SOAP_SHEETS_PATH));
			
			server.start();

			Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));

			//More code can be executed here...
		} catch( Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
