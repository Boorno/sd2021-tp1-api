package tp1.servers.resources;

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
import tp1.api.service.rest.RestSpreadsheets;

@Singleton
public class SpreadSheetResource implements RestSpreadsheets {

	private final Map<String, Spreadsheet> sheets = new HashMap<String, Spreadsheet>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());

	public SpreadSheetResource() {
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {
		Log.info("createSheet : " + sheet+ "; pwd = " + password);
		if (password == null) {
			Log.info("Passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		if (sheet.getOwner() == null || sheet.getRows() == 0 || sheet.getColumns() == 0) {
			Log.info("Sheet object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		synchronized (this) {

			if (sheets.containsKey(sheet.getSheetId())) {
				Log.info("Sheet already exists.");
				throw new WebApplicationException(Status.BAD_REQUEST);
			}

			sheets.put(sheet.getSheetId(), sheet);

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
			if (!sheet.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}

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
