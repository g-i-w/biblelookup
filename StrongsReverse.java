package biblelookup;

import java.util.*;
import creek.*;

public class StrongsReverse {

	public static void main ( String[] args ) throws Exception {
		Map<String,Set<String>> reverseLookup = new TreeMap<>();
		LookupTable bookLookup = new LookupTable( new CSVFile( args[0] ).table() );
	
		CSVFile strongsEmbedded = new CSVFile( args[1] );
		for (List<String> line : strongsEmbedded.table().data()) {
			String column = line.get( 4 ); // column with [H####] codes embedded
			List<String> strongsCodes = Regex.groups( column, "\\[([GH]\\d{1,4})\\]" );
			for (String code : strongsCodes) {
				if (!reverseLookup.containsKey(code)) reverseLookup.put( code, new TreeSet<String>() );
				StringBuilder verse = new StringBuilder();
				//verse.append( bookLookup.colLookup(1,0).get(line.get(0)) )
				//	.append( ":" ).append( line.get(1) ).append( ":" ).append( line.get(2) );
				verse.append( line.get(0) ).append( line.get(1) ).append( ":" ).append( line.get(2) );
				reverseLookup.get(code).add( verse.toString() );
			}
		}
		
		CSV output = new CSV();
		for (Map.Entry<String,Set<String>> entry : reverseLookup.entrySet()) {
			String code = entry.getKey();
			Set<String> verseSet = entry.getValue();
			List<String> verses = new ArrayList<String>( verseSet );
			//Collections.sort( verses );
			verses.add( 0, code );
			output.append( verses );
		}
		(new CSVFile( args[2] )).append( output );
	}

}
