package tp1.api.engine;

import tp1.api.Spreadsheet;
import tp1.api.clients.rest.GetValuesClient;
import tp1.api.clients.soap.GetValuesClientSoap;
import tp1.util.CellRange;

public class SpreadSheetImpl implements AbstractSpreadsheet{
	
	private Spreadsheet sheet;
	private String userId;
	private boolean rest;
	
	public SpreadSheetImpl(Spreadsheet sheet, String userId, boolean rest) {
		this.sheet = sheet;
		this.userId = userId;
		this.rest = rest;
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
	public String[][] getRangeValues(String sheetURL, String range) {
		CellRange c = new CellRange(range);
		
		String[][] values = null;
		
		if(rest) {
			GetValuesClient gr = new GetValuesClient(sheetURL, userId);
			values = gr.getValues();
		} else {
			GetValuesClientSoap gs = new GetValuesClientSoap(sheetURL, userId);
			try {
				values = gs.getValues();
			} catch(Exception e){
			}
		}
						
		return c.extractRangeValuesFrom(values);
	}

}
