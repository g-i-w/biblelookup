package biblelookup;

import java.util.*;
import creek.*;

public class BibleTextLookupByLetter {

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

	// Arguments: <input_CSV> <replacements_CSV> <output_JSON> <regex> <book_order_JSON>

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

		// create giant lexicon lookup table
		int wordId = 0;
		int letterId = 0;
		int rowCount = 0;
		Map<String,Integer> everyWord = new LinkedHashSet<>();
		Map<String,Integer> everyLetter = new LinkedHashSet<>();
		for (List<String> row : orderedInput.data()) {
			if (row.size()>4) throw new RuntimeException( "More items in line "+rowCount+" than there should be!" );
			String book = row.get(0);
			String chap = row.get(1);
			String verse = row.get(2);
			String verseText = row.get(3);
			List<String> words = Regex.groups( verseText, args[3] );
			for (String word : words) {
				if (! everyWord.contains(word)) {
					everyWord.put( word, wordId++ ); // if it's a new word, register and increment
					for (String letter : unicodeLetters( word )) {
						if (! everyLetter.containsKey(letter)) everyLetter.put( letter, letterId++ );  // if it's a new letter, register and increment
					}
				}
				Integer id = everyWord.get(word);
				String wordIdStr = id.toString(); // get the id for this word
				json.auto( "fwd" ).auto( book ).auto( chap ).auto( verse ).add( wordIdStr ); // add to array at that verse
				json.auto( "rev" ).auto( wordIdStr ).auto( book ).auto( chap ).auto( verse );
			}
			rowCount++;
		}
		int totalWords = wordId+1;
		System.out.println( "Total words in "+args[2]+": "+totalWords );
		
		// letter lookup
		for (Map.Entry<String,Integer> entry : everyLetter.entrySet()) {
			json.auto( "letters" ).auto( entry.getValue().toString() ).add( entry.getKey() ); // reverse
		}
		// word lookup
		for (Map.Entry<String,Integer> entry : everyWord.entrySet()) {
			String word = entry.getKey();
			List<String> letterIdList = new ArrayList<>();
			for (String letter : unicodeLetters(word)) {
				Integer letter
				
			}
			json.auto( "words" ).auto( everyLetter.get(letter).toString() ).add(  );
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
