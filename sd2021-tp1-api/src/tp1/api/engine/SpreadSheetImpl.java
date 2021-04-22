package tp1.api.engine;

import tp1.api.Spreadsheet;
import tp1.api.clients.GetValuesClient;
import tp1.api.service.rest.RestSpreadsheets;
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
	public String[][] getRangeValues(String sheetURL, String range) {
		String[] sheetInfo = sheetURL.split(RestSpreadsheets.PATH+"/");
		
		CellRange c = new CellRange(range);
		
		System.out.println(sheetInfo[0]);
		System.out.println(sheetInfo[1]);
		System.out.println(sheetURL);
		
		GetValuesClient g = new GetValuesClient(sheetInfo[0], sheetInfo[1], userId);
						
		return c.extractRangeValuesFrom(g.getValues());
	}

}
