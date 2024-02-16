package biblelookup;

import java.util.*;
import creek.*;

public class TableToJSON {

	public static void main ( String[] args ) {
		CSVFile input = new CSVFile( args[0] );
		StringBuilder json = new StringBuilder();
		
		String[] langNames = new String[]{ "english", "russian", "chinese", "spanish" };
		int[] langCols = new int[]{ 4, 6, 8, 10 };
		int strongsCol = 4;
		int bookCol = 0;
		int chapCol = 1;
		int verseCol = 2;
		
		json.append( "{ \"bible\":\n" );
		
		for ( List<String> row : input.table()) {
			List<String> strongsCodes = Regex.groups( row.get( strongsCol ), "(\\[[GH]\\d{1,4}\\])" );
			for (int i=0; i<langNames.length; i++) {
			
			}
		}
	}
	
}
