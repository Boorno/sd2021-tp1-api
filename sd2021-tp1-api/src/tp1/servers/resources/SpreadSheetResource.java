package tp1.servers.resources;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.clients.GetUserClient;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.discovery.Discovery;

@Singleton
public class SpreadSheetResource implements RestSpreadsheets {

	private final Map<String, Spreadsheet> sheets = new HashMap<String, Spreadsheet>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());

	private String domain;
	
	private String serverURI;

	private Discovery discovery;

	public SpreadSheetResource() {
	}

	public SpreadSheetResource(String domain, String serverURI) {
		this.domain = domain;
		this.serverURI = serverURI;
		this.discovery = new Discovery(new InetSocketAddress("226.226.226.226", 2266));
		this.discovery.resourceStart();
	}
	
	private String getUserURI() {
		String usersURI;
		while(true) {
			URI[] usersURIs = discovery.knownUrisOf(domain + ":users");
			if (usersURIs != null) {
				usersURI = usersURIs[0].toString();
				break;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return usersURI;
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {
		Log.info("createSheet : " + sheet + "; pwd = " + password);
		
		if (sheet.getOwner() == null || sheet.getRows() <= 0 || sheet.getColumns() <= 0) {
			Log.info("Sheet object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		String usersURI = getUserURI();
				
		GetUserClient getuser = new GetUserClient(usersURI, sheet.getOwner(), password);
		
		int responseStatus = getuser.getUser();
		
		if (responseStatus == Status.OK.getStatusCode()) {
			synchronized (this) {
				if (sheets.containsKey(sheet.getSheetId())) {
					Log.info("Sheet already exists.");
					throw new WebApplicationException(Status.BAD_REQUEST);
				}

				String sheetId = sheet.getOwner() + "-" + System.currentTimeMillis();
				sheet.setSheetId(sheetId);
				sheet.setSheetURL(serverURI + "/" + sheetId);

				sheets.put(sheet.getSheetId(), sheet);
			}
		} else {

			if (responseStatus == Status.FORBIDDEN.getStatusCode())
				Log.info("Password incorrect.");

			if (responseStatus == Status.NOT_FOUND.getStatusCode())
				Log.info("No user exists with id: " + sheet.getOwner());

			throw new WebApplicationException(Status.BAD_REQUEST);

		}

		return sheet.getSheetId();
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {
		Log.info("deleteUser : user = " + sheetId + "; pwd = " + password);
		if (sheetId == null || password == null) {
			Log.info("SheetId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		synchronized (this) {

			Spreadsheet sheet = sheets.get(sheetId);

			// Check if user exists
			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			// Check if the password is correct
//			if (!sheet.getPassword().equals(password)) {
//				Log.info("Password is incorrect.");
//				throw new WebApplicationException(Status.FORBIDDEN);
//			}

			sheets.remove(sheetId);
		}
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
		
		String usersURI = getUserURI();
		
		GetUserClient getuser = new GetUserClient(usersURI, userId, password);
		
		int responseStatus = getuser.getUser();
		
		if (responseStatus == Status.NOT_FOUND.getStatusCode()) {
			Log.info("No user exists with id: " + userId);
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		if (responseStatus == Status.FORBIDDEN.getStatusCode()) {
			Log.info("Password incorrect.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		Spreadsheet sheet = null;
		
		if (responseStatus == Status.OK.getStatusCode()) {
			synchronized (this) {
				sheet = sheets.get(sheetId);
				
				if (sheet == null){
					Log.info("Sheet does not exist.");
					throw new WebApplicationException(Status.NOT_FOUND);
				}
				
				if (!sheet.getOwner().equals(userId) && !sheet.getSharedWith().contains(userId)) {
					throw new WebApplicationException(Status.FORBIDDEN);
				}
			}
		} else {
			
			throw new WebApplicationException(Status.BAD_REQUEST);

		}
		
		return sheet;
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		// TODO Auto-generated method stub

	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {
		// TODO Auto-generated method stub

	}

}
