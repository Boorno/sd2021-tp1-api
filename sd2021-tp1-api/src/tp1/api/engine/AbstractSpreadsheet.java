package tp1.api.engine;

import java.net.MalformedURLException;

import tp1.api.service.soap.SheetsException;

/**
 * 
 * Interface used to feed a spreadsheet to the SpreadsheetEngine and compute its values.
 * 
 * The method getRangeValues() will be called by the engine to resolve "=importrange(...)" formulas
 * 
 * @author smd
 *
 */
public interface AbstractSpreadsheet {
	
	/**
	 * The number of rows of the spreadsheet
	 */
	int rows();
	
	/**
	 * The number of columns of the spreadsheet
	 */
	int columns();
	
	/**
	 * The id of the spreadsheet
	 */
	String sheetId();
	
	/**
	 * Called by the engine to obtain the raw value of a cell, given its row, col coordinates
	 */
	String cellRawValue(int row, int col);
	
	/**
	 * Called by the engine to resolve importrange formulas
	 * @param sheetURL - the url of the sheet referenced by the importrange formula
	 * @param range - the range of cells covered by the formula
	 * @return the computed values.
	 * @throws SheetsException 
	 * @throws MalformedURLException 
	 */
	String[][] getRangeValues(String sheetURL, String range) throws MalformedURLException, SheetsException;
}