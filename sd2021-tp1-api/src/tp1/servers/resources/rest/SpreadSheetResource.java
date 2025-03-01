package tp1.servers.resources.rest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.clients.rest.RestClients;
import tp1.api.engine.SpreadSheetImpl;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.soap.SheetsException;
import tp1.discovery.Discovery;
import tp1.impl.engine.SpreadsheetEngineImpl;

@Singleton
public class SpreadSheetResource implements RestSpreadsheets {

	private final Map<String, Spreadsheet> sheets = new HashMap<String, Spreadsheet>();

	private static Logger Log = Logger.getLogger(SpreadSheetResource.class.getName());

	private String domain;

	private String serverURI;

	private Discovery discovery;

	public SpreadSheetResource() {
	}

	public SpreadSheetResource(String domain, String serverURI, Discovery d) {
		this.domain = domain;
		this.serverURI = serverURI;
		this.discovery = d;
	}

	private String getUserURI(String domain) {
		String usersURI;
		while (true) {
			URI[] usersURIs = discovery.knownUrisOf(domain + ":users");
			if (usersURIs != null) {
				usersURI = usersURIs[0].toString();
				break;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				
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

		String usersURI = getUserURI(domain);
		
		String[] args = {usersURI, sheet.getOwner(), password};
		RestClients getuser = new RestClients(args);

		int responseStatus = getuser.getUser();

		if (responseStatus == Status.OK.getStatusCode()) {

			if (sheets.containsKey(sheet.getSheetId())) {
				Log.info("Sheet already exists.");
				throw new WebApplicationException(Status.BAD_REQUEST);
			}

			String sheetId = sheet.getOwner() + "-" + System.currentTimeMillis();
			sheet.setSheetId(sheetId);
			sheet.setSheetURL(serverURI + PATH + "/" + sheetId);
			synchronized (this) {
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

		synchronized (this) {

			Spreadsheet sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			String usersURI = getUserURI(domain);
			
			String[] args = {usersURI, sheet.getOwner(), password};
			RestClients getuser = new RestClients(args);

			int responseStatus = getuser.getUser();

			if (responseStatus == Status.FORBIDDEN.getStatusCode()) {
				Log.info("Password incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
			} else if (responseStatus == Status.OK.getStatusCode()) {
				sheets.remove(sheetId);
			} else {
				throw new WebApplicationException(Status.BAD_REQUEST);
			}
		}
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {

		String usersURI = getUserURI(domain);
		
		String[] args = {usersURI, userId, password};
		RestClients getuser = new RestClients(args);

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

				if (sheet == null) {
					Log.info("Sheet does not exist.");
					throw new WebApplicationException(Status.NOT_FOUND);
				}

				if (!sheet.getOwner().equals(userId) && !sheet.getSharedWith().contains(userId + "@" + domain)) {
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
		String usersURI = getUserURI(domain);
		
		String[] args = {usersURI, userId, password};
		RestClients getuser = new RestClients(args);

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

				if (sheet == null) {
					Log.info("Sheet does not exist.");
					throw new WebApplicationException(Status.NOT_FOUND);
				}

				if (!sheet.getOwner().equals(userId) && !sheet.getSharedWith().contains(userId + "@" + domain)) {
					throw new WebApplicationException(Status.FORBIDDEN);
				}

				String[][] values = null;
				try {
					values = SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(new SpreadSheetImpl(sheet, userId + "@" + domain));
				} catch (SheetsException e) {
					//erro soap
					System.out.println("\nERRO?\n");
				}
				return values;
			}
		} else {

			throw new WebApplicationException(Status.BAD_REQUEST);

		}
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		Log.info("updateCell : sheet = " + sheetId + "; pwd = " + password);

		String usersURI = getUserURI(domain);
		
		String[] args = {usersURI, userId, password};
		RestClients getuser = new RestClients(args);

		int responseStatus = getuser.getUser();

		if (responseStatus == Status.FORBIDDEN.getStatusCode()) {
			Log.info("Password incorrect.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		Spreadsheet sheet = null;

		if (responseStatus == Status.OK.getStatusCode()) {
			synchronized (this) {
				sheet = sheets.get(sheetId);

				if (sheet == null) {
					Log.info("Sheet does not exist.");
					throw new WebApplicationException(Status.NOT_FOUND);
				}

				if (!sheet.getOwner().equals(userId) && !sheet.getSharedWith().contains(userId + "@" + domain)) {
					throw new WebApplicationException(Status.FORBIDDEN);
				}

				sheet.setCellRawValue(cell, rawValue);
			}
		} else {

			throw new WebApplicationException(Status.BAD_REQUEST);

		}
	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {

		String[] tokens = userId.split("@");
				
		String usersURI = getUserURI(tokens[1]);

		String uId = tokens[0];
		
		String[] args = {usersURI, uId, password};
		RestClients getuser = new RestClients(args);

		int responseStatus = getuser.getUser();

		if (responseStatus == Status.NOT_FOUND.getStatusCode()) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		synchronized (this) {
			Spreadsheet sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			
			String[] args2 = {getUserURI(domain), sheet.getOwner(), password};
			getuser = new RestClients(args2);

			responseStatus = getuser.getUser();

			if (responseStatus == Status.FORBIDDEN.getStatusCode()) {
				Log.info("Password incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
			} else if (responseStatus == Status.OK.getStatusCode()) {
				
				Set<String> sW = sheet.getSharedWith();

				if (sW.contains(userId))
					throw new WebApplicationException(Status.CONFLICT);

				sW.add(userId);
				sheet.setSharedWith(sW);

			} else {

				throw new WebApplicationException(Status.BAD_REQUEST);

			}
		}
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {
		
		String[] tokens = userId.split("@");
		
		String usersURI = getUserURI(tokens[1]);

		String uId = tokens[0];
		
		String[] args = {usersURI, uId, password};
		RestClients getuser = new RestClients(args);

		int responseStatus = getuser.getUser();

		if (responseStatus == Status.NOT_FOUND.getStatusCode()) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		synchronized (this) {
			Spreadsheet sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			
			String[] args2 = {getUserURI(domain), sheet.getOwner(), password};
			getuser = new RestClients(args2);

			responseStatus = getuser.getUser();

			if (responseStatus == Status.FORBIDDEN.getStatusCode()) {
				Log.info("Password incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
			} else if (responseStatus == Status.OK.getStatusCode()) {
				Set<String> sW = sheet.getSharedWith();
				
				if (!sW.contains(userId))
					throw new WebApplicationException(Status.NOT_FOUND);

				sW.remove(userId);
				sheet.setSharedWith(sW);

			} else {
				
				throw new WebApplicationException(Status.BAD_REQUEST);

			}
		}
	}

	@Override
	public void deletedUser(String userId) {
		synchronized (this) {
			sheets.entrySet().removeIf(e -> e.getValue().getOwner().equals(userId));
		}
	}

	@Override
	public String[][] importRanges(String sheetId, String userId) {
		Spreadsheet sheet = sheets.get(sheetId);
		if(sheet == null)
			throw new WebApplicationException(Status.NOT_FOUND);
		
		if(!sheet.getOwner().equals(userId) && !sheet.getSharedWith().contains(userId))
			throw new WebApplicationException(Status.FORBIDDEN);
		
		String[][] values = null;
		try {
			values = SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(new SpreadSheetImpl(sheet, sheet.getOwner() + "@" + domain));
		} catch (SheetsException e) {
		}
		return values;
	}

}
