package tp1.servers.soap;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;

import jakarta.xml.ws.Endpoint;
import tp1.api.servers.ws.soap.UsersWS;
import tp1.discovery.Discovery;

public class UsersServer {

	private static Logger Log = Logger.getLogger(UsersServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static final int PORT = 8080;
	public static final String SERVICE = "users";
	public static final String SOAP_USERS_PATH = "/soap/users";

	public static void main(String[] args) {
		try {
			String ip = InetAddress.getLocalHost().getHostAddress();
			String serverURI = String.format("http://%s:%s/soap", ip, PORT);
			
			String domain = args[0];
			
			HttpServer server = HttpServer.create(new InetSocketAddress(ip, PORT), 0);
			
			server.setExecutor(Executors.newCachedThreadPool());
			
			Discovery discovery = new Discovery( new InetSocketAddress("226.226.226.226", 2266), domain+":"+SERVICE, serverURI);
			discovery.start();
			
			Endpoint soapUsersEndpoint = Endpoint.create(new UsersWS(domain, discovery));
			
			soapUsersEndpoint.publish(server.createContext(SOAP_USERS_PATH));
			
			server.start();

			Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));

			//More code can be executed here...
		} catch( Exception e) {
			Log.severe(e.getMessage());
		}
	}

}
