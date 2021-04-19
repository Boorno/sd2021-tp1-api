package tp1.servers.resources;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.clients.GetUserClient;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.discovery.Discovery;

@Singleton
public class SpreadSheetResource implements RestSpreadsheets {

	private final Map<String, Spreadsheet> sheets = new HashMap<String, Spreadsheet>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());

	private String domain;

	private Discovery discovery;

	public SpreadSheetResource() {
	}

	public SpreadSheetResource(String domain) {
		this.domain = domain;
		this.discovery = new Discovery(new InetSocketAddress("226.226.226.226", 2266));
		this.discovery.resourceStart();
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {
		Log.info("createSheet : " + sheet + "; pwd = " + password);
		
		System.out.println("ETAPA1");

		if (sheet.getOwner() == null || sheet.getRows() == 0 || sheet.getColumns() == 0) {
			Log.info("Sheet object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		System.out.println("ETAPA2");

		String usersURL = discovery.knownUrisOf(domain + ":users")[0].toString();

		System.out.println("ETAPA3");
		
		GetUserClient getuser = new GetUserClient(usersURL, sheet.getOwner(), password);
		
		System.out.println("ETAPA4");

		int responseStatus = getuser.getUser();
		
		System.out.println("ETAPA5");

		if (responseStatus == Status.OK.getStatusCode()) {
			synchronized (this) {
				if (sheets.containsKey(sheet.getSheetId())) {
					Log.info("Sheet already exists.");
					throw new WebApplicationException(Status.BAD_REQUEST);
				}

				String sheetId = sheet.getOwner() + "-" + System.currentTimeMillis();
				sheet.setSheetId(sheetId);
				sheet.setSheetURL(usersURL + "/" + sheetId);

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
		// TODO Auto-generated method stub
		return null;
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
