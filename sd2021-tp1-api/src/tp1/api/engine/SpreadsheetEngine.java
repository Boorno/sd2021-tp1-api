package tp1.api.engine;

import java.net.MalformedURLException;

import tp1.api.service.soap.SheetsException;

/**
 * 
 * The SpreadsheeEngine class is used to compute the values of a spreadsheet from its raw values.
 * 
 * 
 * @author smd
 *
 */
public interface SpreadsheetEngine {

	/**
	 * 
	 * @param sheet - The spreadsheet whose cells will be used to compute the values
	 * @return the full "matrix" of cell values.
	 * @throws SheetsException 
	 * @throws MalformedURLException 
	 */
	public String[][] computeSpreadsheetValues( AbstractSpreadsheet sheet ) throws SheetsException;

}
