package tp1.api.servers.ws.soap;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.jws.WebService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.clients.rest.GetUserClient;
import tp1.api.clients.soap.ExistsUserClient;
import tp1.api.clients.soap.GetUserClientSoap;
import tp1.api.engine.SpreadSheetImpl;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
import tp1.api.service.soap.UsersException;
import tp1.discovery.Discovery;
import tp1.impl.engine.SpreadsheetEngineImpl;

@WebService(serviceName = SoapSpreadsheets.NAME, targetNamespace = SoapSpreadsheets.NAMESPACE, endpointInterface = SoapSpreadsheets.INTERFACE)
public class SpreadsheetWS implements SoapSpreadsheets {

	private final Map<String, Spreadsheet> sheets;

	private static Logger Log = Logger.getLogger(SpreadsheetWS.class.getName());

	private String domain;
	private String serverURI;

	private Discovery discovery;

	public SpreadsheetWS() {
		this.sheets = new HashMap<String, Spreadsheet>();
	}

	public SpreadsheetWS(String domain, Discovery d, String serverURI) {
		this.sheets = new HashMap<String, Spreadsheet>();
		this.domain = domain;
		this.discovery = d;
		this.serverURI = serverURI;
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
	public String createSpreadsheet(Spreadsheet sheet, String password)
			throws SheetsException, MalformedURLException, UsersException {
		Log.info("createSheet : " + sheet + "; pwd = " + password);

		if (sheet.getOwner() == null || sheet.getRows() <= 0 || sheet.getColumns() <= 0) {
			Log.info("Sheet object invalid.");
			throw new SheetsException("Sheet object invalid.");
		}

		String usersURI = getUserURI(domain);

		(new GetUserClientSoap(usersURI, sheet.getOwner(), password)).getUser();

		String sheetId = sheet.getOwner() + "-" + System.currentTimeMillis();
		sheet.setSheetId(sheetId);
		sheet.setSheetURL(serverURI + "/" + sheetId);
		synchronized (this) {
			if (sheets.containsKey(sheet.getSheetId())) {
				Log.info("Sheet already exists.");
				throw new SheetsException("Sheet already exists.");
			}
			sheets.put(sheet.getSheetId(), sheet);
		}

		return sheet.getSheetId();
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password)
			throws SheetsException, MalformedURLException, UsersException {
		synchronized (this) {

			Spreadsheet sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new SheetsException("Sheet does not exist.");
			}

			String usersURI = getUserURI(domain);

			(new GetUserClientSoap(usersURI, sheet.getOwner(), password)).getUser();

			sheets.remove(sheetId);
		}

	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password)
			throws SheetsException, MalformedURLException, UsersException {
		String usersURI = getUserURI(domain);

		(new GetUserClientSoap(usersURI, userId, password)).getUser();

		Spreadsheet sheet = null;

		synchronized (this) {
			sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new SheetsException("Sheet does not exist.");
			}

			if (!sheet.getOwner().equals(userId) && !sheet.getSharedWith().contains(userId + "@" + domain)) {
				throw new SheetsException("User can't get this sheet.");
			}
		}

		return sheet;
	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password)
			throws SheetsException, MalformedURLException, UsersException {
		String[] tokens = userId.split("@");

		String usersURI = getUserURI(tokens[1]);

		String uId = tokens[0];

		(new ExistsUserClient(usersURI, uId)).existsUser();

		synchronized (this) {
			Spreadsheet sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new SheetsException("Sheet does not exist.");
			}

			(new GetUserClientSoap(getUserURI(domain), sheet.getOwner(), password)).getUser();

			Set<String> sW = sheet.getSharedWith();

			if (sW.contains(userId))
				throw new SheetsException("Share already exists.");

			sW.add(userId);
			sheet.setSharedWith(sW);

		}
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password)
			throws SheetsException, MalformedURLException, UsersException {
		String[] tokens = userId.split("@");

		String usersURI = getUserURI(tokens[1]);

		String uId = tokens[0];

		(new ExistsUserClient(usersURI, uId)).existsUser();

		synchronized (this) {
			Spreadsheet sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new SheetsException("Sheet does not exist.");
			}

			(new GetUserClientSoap(getUserURI(domain), sheet.getOwner(), password)).getUser();

			Set<String> sW = sheet.getSharedWith();

			if (!sW.contains(userId))
				throw new SheetsException("Share does not exist.");

			sW.remove(userId);
			sheet.setSharedWith(sW);

		}

	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
			throws SheetsException, MalformedURLException, UsersException {

		(new GetUserClientSoap(getUserURI(domain), userId, password)).getUser();

		synchronized (this) {
			Spreadsheet sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new SheetsException("Sheet does not exist.");
			}

			if (!sheet.getOwner().equals(userId) && !sheet.getSharedWith().contains(userId + "@" + domain)) {
				throw new SheetsException("User cant access sheet.");
			}

			sheet.setCellRawValue(cell, rawValue);
		}

	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException, MalformedURLException, UsersException {
		(new GetUserClientSoap(getUserURI(domain), userId, password)).getUser();
		
		String [][] values = null;
		
		synchronized (this) {
			Spreadsheet sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new SheetsException("Sheet does not exist.");
			}

			if (!sheet.getOwner().equals(userId) && !sheet.getSharedWith().contains(userId + "@" + domain)) {
				throw new SheetsException("User cant access sheet.");
			}

			values = SpreadsheetEngineImpl.getInstance()
					.computeSpreadsheetValues(new SpreadSheetImpl(sheet, userId + "@" + domain, false));
	
		}
		
		return values;
	}

	@Override
	public void deletedUser(String userId) {
		synchronized (this) {
			sheets.entrySet().removeIf(e -> e.getValue().getOwner().equals(userId));
		}
	}
	
	@Override
	public String[][] importRanges(String sheetId, String userId) throws SheetsException {
		Spreadsheet sheet = sheets.get(sheetId);
		if(sheet == null)
			throw new SheetsException("Sheet does not exist.");
		
		if(!sheet.getSharedWith().contains(userId))
			throw new SheetsException("User cant access sheet.");
		
		String[][] values = SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(new SpreadSheetImpl(sheet, sheet.getOwner()+"@"+domain, false));

		return values;
	}

}