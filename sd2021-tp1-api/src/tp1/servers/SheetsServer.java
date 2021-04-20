package tp1.servers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tp1.servers.resources.SpreadSheetResource;
import tp1.discovery.Discovery;

public class SheetsServer {

	private static Logger Log = Logger.getLogger(SheetsServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}
	
	public static final int PORT = 8080;
	public static final String SERVICE = "sheets";
	
	public static void main(String[] args) {
		try {
		String ip = InetAddress.getLocalHost().getHostAddress();
		
		String serverURI = String.format("http://%s:%s/rest", ip, PORT);
		
		String domain = args[0];
		
		ResourceConfig config = new ResourceConfig();
		
		Discovery discovery = new Discovery( new InetSocketAddress("226.226.226.226", 2266), domain+":"+SERVICE, serverURI);
		discovery.start();
		
		config.register(new SpreadSheetResource(domain, serverURI, discovery));
		
		JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);
		
		Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
		
		//More code can be executed here...
		} catch( Exception e) {
			Log.severe(e.getMessage());
		}
	}
	
}
