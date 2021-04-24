package tp1.api.engine;

import java.net.MalformedURLException;

import tp1.api.Spreadsheet;
import tp1.api.clients.rest.RestClients;
import tp1.api.clients.soap.GetValuesClientSoap;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.soap.SheetsException;
import tp1.util.CellRange;

public class SpreadSheetImpl implements AbstractSpreadsheet{
	
	private Spreadsheet sheet;
	private String userId;
	
	public SpreadSheetImpl(Spreadsheet sheet, String userId) {
		this.sheet = sheet;
		this.userId = userId;
	}
	
	@Override
	public int rows() {
		return sheet.getRows();
	}

	@Override
	public int columns() {
		return sheet.getColumns();
	}

	@Override
	public String sheetId() {
		return sheet.getSheetId();
	}

	@Override
	public String cellRawValue(int row, int col) {
		try {
			return sheet.getRawValues()[row][col];
		} catch (IndexOutOfBoundsException e) {
			return "#ERROR?";
		}
	}

	@Override
	public String[][] getRangeValues(String sheetURL, String range) throws SheetsException {
		CellRange c = new CellRange(range);
		
		String[][] values = null;
		
		String[] sheetURLinfo = sheetURL.split("/");
		
		System.out.println(sheetURLinfo[3]);
		
		boolean rest = sheetURLinfo[3].equals("rest");
		
		if(rest) {
			String[] sheetInfo = sheetURL.split(RestSpreadsheets.PATH+"/");
			//GetValuesClient gr = new GetValuesClient(sheetURL, sheetInfo[0], sheetInfo[1], userId);
			String[] args = {sheetURL, sheetInfo[0], sheetInfo[1], userId};
			RestClients gr = new RestClients(args);
			values = gr.getValues();
		} else {
			GetValuesClientSoap gs = new GetValuesClientSoap(sheetURL, userId);
			try {
			values = gs.getValues();
			} catch(Exception e) {
				throw new SheetsException("Cant access values.");
			}
		}
						
		return c.extractRangeValuesFrom(values);
	}

}
