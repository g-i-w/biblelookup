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

	public static List<String> unicodeLetters ( String input, boolean coreHebrewLetters ) throws Exception {
		byte[] unicodeBytes = input.getBytes( "UTF-16" );
		String unicodeHex = bytesToHex( unicodeBytes );
		List<String> output = new ArrayList<>();
		
		for (int i=4; i<unicodeHex.length(); i+=4) {
			String letter = unicodeHex.substring(i, i+4);
			if (coreHebrewLetters && !coreHebrewCode(letter)) continue;
			output.add( letter );
		}
		
		return output;
	}
	
	public static byte nibble ( char c ) {
		if (c >= '0' && c <= '9') return (byte)(c-0x30);
		else return (byte)(c-0x37);
	}
	
	public static String unicode ( List<String> codeList ) throws Exception {
		byte[] unicodeRaw = new byte[ codeList.size()*2 ];
		int b=0;
		for (int i=0; i<codeList.size(); i++) {
			String code = codeList.get(i);
			byte nib0 = (byte)(nibble(code.charAt(0))<<4);
			byte nib1 = nibble(code.charAt(1));
			byte nib2 = (byte)(nibble(code.charAt(2))<<4);
			byte nib3 = nibble(code.charAt(3));
			//System.out.println( code+": "+nib0+", "+nib1+", "+nib2+", "+nib3 );
			unicodeRaw[b]   = (byte)(nib0+nib1);
			unicodeRaw[b+1] = (byte)(nib2+nib3);
			b+=2;
		}
		return new String( unicodeRaw, "UTF-16" );
	}

	public static boolean coreHebrewCode ( String input ) {
		byte[] b = input.getBytes();
		if (b[0]=='0' && b[1]=='5') {
			if ( b[2]=='D') return true;
			else if (b[2]=='E' && b[3]>='0' && b[3]<='A') return true;
		}
		return false;
	}
	
	// input CSV columns: book, chap, verse, verseText

	// Arguments: <input_CSV> <replacements_CSV> <output_JSON> <regex> <book_order_JSON> <bool_unicode> <min_word_length> <coreHebrew>
	
	// takes simple CSV (book,chap,verse,verseText) files and creates JSON tree files:
	//    text lookup JSON:  verse-->id, id-->verse, id-->word
	//    word lookup JSON:  word-->id

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

		// core Hebrew letters
		boolean coreHebrew = false;
		if (args.length>7) coreHebrew = Boolean.parseBoolean( args[7] );

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
		
		// word reverse lookup minimum size
		boolean unicode = true;
		if (args.length>5) unicode = Boolean.parseBoolean(args[5]);
		
		// JSON object to export word reverse lookup
		Tree words = new JSON( JSON.RETAIN_ORDER );
		
		// example: "words": [ [ "05D0", "05D1" ], [ "05D0", "05D1", "05D2" ] ]
		for (Map.Entry<String,Integer> entry : everyWord.entrySet()) {
			String word = entry.getKey();
			String id = entry.getValue().toString();
			// id --> word
			if (unicode) json.auto( "words" ).auto( id ).add( unicodeLetters( word, coreHebrew ) );
			else json.auto( "words" ).add( id, word );
			// word --> id
			words.add( word, id );
		}
		
		// export fwd/rev/words lookup JSON in ASCII
		FileActions.write( args[2], json.serialize(), "US-ASCII" );
		
		// export id lookup JSON
		FileActions.write( FileActions.addSuffix(args[2],"-idlookup"), words.serialize(), "UTF-8", false );
	}


}


// parses verse-->id and id--> word in JSON text lookup and produces verse text

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


// tests unicodeLetters(  ) method

class PrintCodes {

	public static void main ( String[] args ) throws Exception {
		System.out.println( BibleTextLookup.unicodeLetters( args[0], Boolean.parseBoolean(args[1]) ) );
	}

}

// tests unicode(  ) method

class PrintUnicode {

	public static void main ( String[] args ) throws Exception {
		System.out.println( BibleTextLookup.unicode( Arrays.asList( args ) ) );
	}

}


// parses bsb_tables.csv (exported from bsb_tables.xlsx) and produces individual simple (book,chap,verse,verseText) CSV files

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
				firstLetter = ( lang.equals("Greek") ? "G" : "H" );
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

// produce lang-->word-->id
// <output_file> <he> <OTHebrew-lookup-words.json> <gr> <NTGreek...something> <en> <BSBEnglish...> <st> <Strongs...>

class CompileIdLookups {
	public static void main ( String[] args ) throws Exception {
	
		Tree wordLookup = new JSON();
	
		for (int i=1; i<args.length; i+=2) {
			wordLookup.add(
				args[i],
				new JSON( FileActions.read( args[i+1] ) )
			);
		}
		
		FileActions.write( args[0], wordLookup.serialize(), "UTF-8" );
	}
	
}


// produce lang-->id-->otherLang-->[ otherId0, otherId1 ]
// <word_lookup_JSON> <bsb_tables_CSV>

class TranslateFromBSBTables {
	private static int notFound;
	private static int found;

	public static String id ( Tree lookup, String lang, String word ) {
		Tree langObj = lookup.get(lang);
		if (langObj==null) {
			System.out.println( "Can't find lang "+lang );
			return null;
		}
		Tree id = langObj.get(word);
		//System.out.println( langObj.serialize() );
		//if (true) throw new RuntimeException( "stopped" );
		if (id==null) {
			System.out.println( (notFound++)+": Can't find word "+word+" in lang "+lang );
			return null;
		} else {
			found++;
		}
		return id.value();
	}
	
	
	
	public static void main ( String[] args ) throws Exception {
	
		Tree wordLookup = new JSON( FileActions.read(args[0]) );
	
		LookupTable bsbTables = new LookupTable( new CSV( FileActions.read(args[1]) ) );
		List<String> keyRow = bsbTables.data().get(1);
		Map<String,Integer> keyMap = bsbTables.rowLookup(1);
		
		Tree translationLookup = new JSON( JSON.RETAIN_ORDER );
		Tree translationLookupUnicode = new JSON( JSON.RETAIN_ORDER );
		
		for (List<String> row : bsbTables.data()) {
			String lang = row.get( keyMap.get("Language") );
			String langWord = row.get( keyMap.get("WLC / Nestle Base {TR} ⧼RP⧽ (WH) 〈NE〉 [NA] ‹SBL› [[ECM]]") );
			String strongsPrefix;

			if (lang.equals("Greek")) {
				lang = "gr";
				strongsPrefix = "G";
			} else { // including both Hebrew and Aramaic
				langWord = BibleTextLookup.unicode( BibleTextLookup.unicodeLetters( langWord, true ) );
				lang = "he";
				strongsPrefix = "H";
			}
			
			Set<String> engSet = new HashSet<>( Regex.groups(
				row.get( keyMap.get("BSB Version") ),
				"(\\w{4,})"
			));
			String strongs = strongsPrefix+row.get( keyMap.get("Strongs") );
			
			for (String engWord : engSet) {
				String enId = id( wordLookup, "en", engWord );
				String langId = id( wordLookup, lang, langWord );
				String strongsId = id( wordLookup, "st", strongs );
				
				if (enId!=null && langId!=null && strongsId!=null) {
					translationLookup.auto( "en" ).auto( enId ).auto( lang ).auto( langId );
					translationLookup.auto( "en" ).auto( enId ).auto( "st" ).auto( strongsId );
					
					translationLookup.auto( lang ).auto( langId ).auto( "en" ).auto( enId );
					translationLookup.auto( lang ).auto( langId ).auto( "st" ).auto( strongsId );
					
					translationLookup.auto( "st" ).auto( strongsId ).auto( lang ).auto( langId );
					translationLookup.auto( "st" ).auto( strongsId ).auto( "en" ).auto( enId );

					translationLookupUnicode.auto( "en" ).auto( engWord ).auto( lang ).auto( langWord );
					translationLookupUnicode.auto( "en" ).auto( engWord ).auto( "st" ).auto( strongs );
					
					translationLookupUnicode.auto( lang ).auto( langWord ).auto( "en" ).auto( engWord );
					translationLookupUnicode.auto( lang ).auto( langWord ).auto( "st" ).auto( strongs );
					
					translationLookupUnicode.auto( "st" ).auto( strongs ).auto( lang ).auto( langWord );
					translationLookupUnicode.auto( "st" ).auto( strongs ).auto( "en" ).auto( engWord );
				}
			}
		}
		
		// convert reverse 'key:nulls's into array
		for (Tree lang0 : translationLookup.branches()) {
			for (Tree id0 : lang0.branches()) {
				for (Tree lang1 : id0.branches()) {
					Set<String> ids = lang1.keys(); // copy the key set
					lang1.map( new LinkedHashMap<>() ); // clear the map
					for (String id : ids) lang1.add( id ); // each id now becomes an array value
				}
			}
		}
		
		FileActions.write( FileActions.name(args[0])+"-translate.json", translationLookup.serialize(), "US-ASCII" );
		FileActions.write( FileActions.name(args[0])+"-translate-unicode.json", translationLookupUnicode.serialize(), "UTF-8" );
		System.out.println( "found: "+found );
	}
}


