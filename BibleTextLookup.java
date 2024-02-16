package biblelookup;

import java.util.*;
import creek.*;

public class BibleTextLookup {

	// from https://stackoverflow.com/questions/9655181/java-convert-a-byte-array-to-a-hex-string
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
		int v = bytes[j] & 0xFF;
		hexChars[j * 2] = HEX_ARRAY[v >>> 4];
		hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	public static List<String> unicodeLetters ( String input ) throws Exception {
		byte[] unicodeBytes = input.getBytes( "UTF-16" );
		String unicodeHex = bytesToHex( unicodeBytes );
		List<String> output = new ArrayList<>();
		
		for (int i=4; i<unicodeHex.length(); i+=4) {
			output.add( unicodeHex.substring(i, i+4) );
		}
		
		return output;
	}

	// CSV columns: bookAbr, chapNum, verseNum, verseText

	// Arguments: <input_CSV> <replacements_CSV> <output_JSON> <regex> <book_order_JSON> <bool_unicode> <min_word_length>

	public static void main ( String[] args ) throws Exception {
		// load CSV data
		Table input = new CSVFile( args[0] ).table();
		
		// load replacement CSV lookup table
		LookupTable keyConv = new LookupTable( new CSVFile( args[1] ).table() );
		Map<String,String> replaceMap = keyConv.colLookup( 0, 1 );
		input.replace( replaceMap, 0 );
		
		// book order
		Tree bookOrder = (new JSON( FileActions.read( args[4] ) )).get( "books" );
		Table orderedInput = new CSV();
		for (String book : bookOrder.values()) {
			for (List<String> row : input.data()) {
				if (row.get(0).equals(book)) orderedInput.append( row );
			}
		}
		
		// JSON Tree
		Tree json = new JSON( JSON.RETAIN_ORDER, JSON.PEDANTIC );
		
		// min word length
		String minWord = null;
		if (args.length>6) minWord = args[6];

		// create giant lexicon lookup table
		int nextId = 0;
		int rowCount = 0;
		Map<String,Integer> everyWord = new LinkedHashMap<>();
		for (List<String> row : orderedInput.data()) {
			if (row.size()>4) throw new RuntimeException( "More items in line "+rowCount+" than there should be!" );
			String book = row.get(0);
			String chap = row.get(1);
			String verse = row.get(2);
			String verseText = row.get(3);
			List<String> words = Regex.groups( verseText, args[3] );
			for (String word : words) {
				if (! everyWord.containsKey(word)) everyWord.put( word, nextId++ ); // if it's a new word, register and increment id
				Integer id = everyWord.get( word ); // get the id for this word
				String idStr = id.toString();
				// fwd
				json.auto( "fwd" ).auto( book ).auto( chap ).auto( verse ).add( idStr ); // add to array at that verse
				// rev
				if (minWord==null || Regex.exists(word, minWord))
					json.auto( "rev" ).auto( idStr ).auto( book ).auto( chap ).auto( verse ); // add a key at that verse
			}
			rowCount++;
		}
		int totalWords = nextId+1;
		System.out.println( "Total words in "+args[2]+": "+totalWords );
		
		// convert reverse 'key:nulls's into array
		for (Tree id : json.get("rev").branches()) {
			for (Tree book : id.branches()) {
				for (Tree chap : book.branches()) {
					Set<String> verses = chap.keys(); // copy the key set (verses)
					chap.map( new LinkedHashMap<>() ); // clear the map
					for (String verse : verses) chap.add( verse ); // each verse now becomes an array value
				}
			}
		}
		
		// word lookup
		boolean unicode = true;
		if (args.length>5) unicode = Boolean.parseBoolean(args[5]);
		
		for (Map.Entry<String,Integer> entry : everyWord.entrySet()) {
			if (unicode) json.auto( "words" ).auto( entry.getValue().toString() ).add( unicodeLetters(entry.getKey()) );
			else json.auto( "words" ).add( entry.getValue().toString(), entry.getKey() );
		}
		
		// write in specified encoding
		FileActions.write( args[2], json.serialize(), "US-ASCII" );
	}


}

class GetVerse {

	public static void main ( String[] args ) throws Exception {

		String inputPath = args[0];
		String book = args[1];
		String chap = args[2];
		String verse = args[3];
		String outputPath = args[4];
		
		Tree json = new JSON( FileActions.read( inputPath ) );
		Tree verseArray = json.get( "fwd" ).get( book ).get( chap ).get( verse );
		Tree words = json.get( "words" );
		
		StringBuilder verseText = new StringBuilder();
		String delim = "";
		List<String> wordList = verseArray.values();
		System.out.println( "wordList: "+wordList );
		for (String wordId : wordList) {
			List<String> letterList = words.get( wordId ).values();
			System.out.println( "letterList for "+wordId+": "+letterList );
			verseText.append( delim );
			for (String letter : letterList) {
				verseText.append( "&#x" ).append( letter ).append( ";" );
			}
			delim = " ";
		}
		
		FileActions.write( outputPath, "<html><body>"+verseText+"</body></html>" );	
	}

}

class PrintCodes {

	public static void main ( String[] args ) throws Exception {
		System.out.println( BibleTextLookup.unicodeLetters( args[0] ) );
	}

}

class EnglishStrongsFromBSBTables {

	public static void main ( String[] args ) throws Exception {
		LookupTable bsbTables = new LookupTable( new CSV( FileActions.read(args[0]) ) );
		List<String> keyRow = bsbTables.data().get(1);
		Map<String,Integer> keyMap = bsbTables.rowLookup(1);
		
		Table bsbCSV = new CSV();
		Table bsbembedCSV = new CSV();
		Table strongsCSV = new CSV();
		
		List<String> ref = null;
		List<String> bsbRow = null;
		StringBuilder bsbSB = new StringBuilder();
		List<String> bsbembedRow = null;
		StringBuilder bsbembedSB = new StringBuilder();
		List<String> strongsRow = null;
		StringBuilder strongsSB = new StringBuilder();
		
		for (int i=2; i<bsbTables.rowCount(); i++) {
			// next row
			List<String> row = bsbTables.data().get(i);
			
			// Verse column
			String refStr = row.get( keyMap.get("Verse") );
			// if first row of verse (contains a reference)
			if (!refStr.equals("")) {
				// regex the verse reference
				ref = Regex.groups( refStr, "(\\d{0,1}\\s{0,1}\\w+)\\s+(\\d+):(\\d+)" );
				// verify the regex
				if (ref.size()!=3) {
					for (int j=0; j<row.size(); j++) {
						System.out.println( keyRow.get(j)+"\t"+row.get(j) );
					}
					throw new Exception( "error parsing verse reference on line "+i+":\n"+row );
				}
				// if it's not the first row...
				if (bsbRow!=null) {
					// add StringBuilder strings
					bsbRow.add( bsbSB.toString() );
					bsbembedRow.add( bsbembedSB.toString() );
					strongsRow.add( strongsSB.toString() );
					// append to CSV
					bsbCSV.append( bsbRow );
					bsbembedCSV.append( bsbembedRow );
					strongsCSV.append( strongsRow );
					// space between words
				}
				// new rows
				bsbRow = new ArrayList<>();
				bsbRow.addAll( ref );
				bsbembedRow = new ArrayList<>();
				bsbembedRow.addAll( ref );
				strongsRow = new ArrayList<>();
				strongsRow.addAll( ref );
				// new StringBuilders
				bsbSB = new StringBuilder();
				bsbembedSB = new StringBuilder();
				strongsSB = new StringBuilder();

			}

			// Other columns
			String strongs = row.get( keyMap.get("Strongs") );
			String firstLetter = "";
			if (!strongs.equals("")) {
				String lang = row.get( keyMap.get("Language") );
				if (lang.length()<1) throw new Exception( "error parsing verse reference on line "+i+":\n"+row );
				firstLetter = lang.substring(0,1);
				strongsSB.append(" ").append(firstLetter).append( strongs );
			}
			
			String english = row.get( keyMap.get("BSB Version") ).trim();
			if (!english.equals("")) {
				bsbSB.append(" ").append(english);
				bsbembedSB.append(" ").append(english).append(" ").append(firstLetter).append( strongs );
			}
		}
		
		FileActions.write( args[1], bsbCSV.serial(), "UTF-8" );
		FileActions.write( FileActions.addSuffix(args[1],"-strongs"), bsbembedCSV.serial(), "UTF-8", false );
		FileActions.write( args[2], strongsCSV.serial(), "UTF-8" );
	}
	
}

