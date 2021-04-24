package tp1.api.servers.ws.soap;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.jws.WebService;
import tp1.api.Spreadsheet;
import tp1.api.clients.soap.SoapClients;
import tp1.api.engine.SpreadSheetImpl;
import tp1.api.service.soap.SheetsException;
import tp1.api.service.soap.SoapSpreadsheets;
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
	public String createSpreadsheet(Spreadsheet sheet, String password) throws SheetsException {
		Log.info("createSheet : " + sheet + "; pwd = " + password);

		if (sheet.getOwner() == null || sheet.getRows() <= 0 || sheet.getColumns() <= 0) {
			Log.info("Sheet object invalid.");
			throw new SheetsException("Sheet object invalid.");
		}

		String usersURI = getUserURI(domain);

		try {
			String[] args = new String[4];
			args[0] = "user";
			args[1] = usersURI;
			args[2] = sheet.getOwner();
			args[3] = password;
			SoapClients sc = new SoapClients(args);
			sc.getUser();
			//(new GetUserClientSoap(usersURI, sheet.getOwner(), password)).getUser();
		} catch (Exception e) {
			throw new SheetsException("Cant get user.");
		}

		String sheetId = sheet.getOwner() + "-" + System.currentTimeMillis();
		sheet.setSheetId(sheetId);
		sheet.setSheetURL(serverURI + "/" + sheetId);
		if (sheet.getSharedWith() == null)
			sheet.setSharedWith(new HashSet<String>());
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
	public void deleteSpreadsheet(String sheetId, String password) throws SheetsException {
		synchronized (this) {

			Spreadsheet sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new SheetsException("Sheet does not exist.");
			}

			String usersURI = getUserURI(domain);

			try {
				//(new GetUserClientSoap(usersURI, sheet.getOwner(), password)).getUser();
				String[] args = new String[4];
				args[0] = "user";
				args[1] = usersURI;
				args[2] = sheet.getOwner();
				args[3] = password;
				SoapClients sc = new SoapClients(args);
				sc.getUser();
			} catch (Exception e) {
				throw new SheetsException("Cant get user.");
			}

			sheets.remove(sheetId);
		}

	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) throws SheetsException {

		String usersURI = getUserURI(domain);

		try {
			String[] args = new String[4];
			args[0] = "user";
			args[1] = usersURI;
			args[2] = userId;
			args[3] = password;
			SoapClients sc = new SoapClients(args);
			sc.getUser();
			//(new GetUserClientSoap(usersURI, userId, password)).getUser();
		} catch (Exception e) {
			throw new SheetsException("Cant get user.");
		}

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
	public void shareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		String[] tokens = userId.split("@");

		String usersURI = getUserURI(tokens[1]);

		String uId = tokens[0];

		try {
			String[] args = new String[4];
			args[0] = "delete";
			args[1] = usersURI;
			args[2] = uId;
			SoapClients sc = new SoapClients(args);
			sc.existsUser();
			//(new ExistsUserClient(usersURI, uId)).existsUser();
		} catch (Exception e) {
			throw new SheetsException("User does not exist.");
		}
		synchronized (this) {
			Spreadsheet sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new SheetsException("Sheet does not exist.");
			}

			try {
				String[] args = new String[4];
				args[0] = "user";
				args[1] = getUserURI(domain);
				args[2] = sheet.getOwner();
				args[3] = password;
				SoapClients sc = new SoapClients(args);
				sc.getUser();
				//(new GetUserClientSoap(getUserURI(domain), sheet.getOwner(), password)).getUser();
			} catch (Exception e) {
				throw new SheetsException("Cant get user.");
			}

			Set<String> sW = sheet.getSharedWith();

			if (sW.contains(userId))
				throw new SheetsException("Share already exists.");

			sW.add(userId);
			sheet.setSharedWith(sW);

		}
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) throws SheetsException {
		String[] tokens = userId.split("@");

		String usersURI = getUserURI(tokens[1]);

		String uId = tokens[0];

		try {
			String[] args = new String[4];
			args[0] = "delete";
			args[1] = usersURI;
			args[2] = uId;
			SoapClients sc = new SoapClients(args);
			sc.existsUser();
			//(new ExistsUserClient(usersURI, uId)).existsUser();
		} catch (Exception e) {
			throw new SheetsException("User does not exist.");
		}

		synchronized (this) {
			Spreadsheet sheet = sheets.get(sheetId);

			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new SheetsException("Sheet does not exist.");
			}

			try {
				String[] args = new String[4];
				args[0] = "user";
				args[1] = getUserURI(domain);
				args[2] = sheet.getOwner();
				args[3] = password;
				SoapClients sc = new SoapClients(args);
				sc.getUser();
				//(new GetUserClientSoap(getUserURI(domain), sheet.getOwner(), password)).getUser();
			} catch (Exception e) {
				throw new SheetsException("Cant get user.");
			}

			Set<String> sW = sheet.getSharedWith();

			if (!sW.contains(userId))
				throw new SheetsException("Share does not exist.");

			sW.remove(userId);
			sheet.setSharedWith(sW);

		}

	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password)
			throws SheetsException {

		try {
			String[] args = new String[4];
			args[0] = "user";
			args[1] = getUserURI(domain);
			args[2] = userId;
			args[3] = password;
			SoapClients sc = new SoapClients(args);
			sc.getUser();
			//(new GetUserClientSoap(getUserURI(domain), userId, password)).getUser();
		} catch (Exception e) {
			throw new SheetsException("Cant get user.");
		}

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
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) throws SheetsException {
		try {
			String[] args = new String[4];
			args[0] = "user";
			args[1] = getUserURI(domain);
			args[2] = userId;
			args[3] = password;
			SoapClients sc = new SoapClients(args);
			sc.getUser();
			//(new GetUserClientSoap(getUserURI(domain), userId, password)).getUser();
		} catch (Exception e) {
			throw new SheetsException("Cant get user.");
		}

		String[][] values = null;

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
					.computeSpreadsheetValues(new SpreadSheetImpl(sheet, userId + "@" + domain));

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
		if (sheet == null)
			throw new SheetsException("Sheet does not exist.");

		if (!sheet.getSharedWith().contains(userId))
			throw new SheetsException("User cant access sheet.");

		String[][] values = SpreadsheetEngineImpl.getInstance()
				.computeSpreadsheetValues(new SpreadSheetImpl(sheet, sheet.getOwner() + "@" + domain));

		return values;
	}

}