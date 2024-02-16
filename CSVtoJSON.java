package biblelookup;

import java.util.*;
import creek.*;

public class CSVtoJSON {

	// <input_CSV> <replacements_CSV> <output_JSON> <output_encoding>

	public static void main ( String[] args ) throws Exception {
		// load CSV data
		Table input = new CSVFile( args[0] ).table();
		
		// load replacement CSV lookup table
		LookupTable keyConv = new LookupTable( new CSVFile( args[1] ).table() );
		Map<String,String> replaceMap = keyConv.colLookup( 0, 1 );
		input.replace( replaceMap, 0 );
		
		// convert to JSON Tree
		Tree json = new JSON( JSON.RETAIN_ORDER, JSON.PEDANTIC );
		json.data( input.data() );
		
		// write in specified encoding
		FileActions.write( args[2], json.serialize(), args[3] );
	}


}
