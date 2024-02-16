
var bible = {};

var globalWordId = {};

function loadChapter ( book, chap ) {

	console.log( `loadChapter: ${book} ${chap}` );
	//console.log( "wordList for 23349:", bible.OTHebrew.words[23349] );

	//console.log( bible.NTGreek.Gen['1'] );

	if (book==undefined || book=='') book = 'Gen';
	if (chap==undefined || chap=='') chap = '1';
	
	// ( add bibleCurrent to bibleHistory here )
	bibleCurrent = {};
	
	// set textCurrent based on Regex of user input

	// construct table HTML for textCurrentView
	var table = "<table>";
	for (const [lang, langObj] of Object.entries(bible)) {
		//console.log( `lang: ${lang}` );
		if (langObj.fwd[book]!==undefined && langObj.fwd[book][chap]!==undefined) {
			//console.log( `book: ${book}, chap: ${chap}` );
			//console.log(`building table HTML: ${lang} ${book} ${chap}`);
			for (var [verse, verseArray] of Object.entries(langObj.fwd[book][chap])) {
				//verseText = verseText.replace( new RegExp("(\\w+)","mg"),"<a href=\"#\" onclick=\"loadWord('$1')\">$1</a>");
				table += `<tr><td>${verse}</td><td>` + buildVerse( book, chap, verse ) + '</td></tr>';
			}
		}
	}
	table += "</table>";
	//console.log( table );
	document.getElementById("book-input").value = book;
	document.getElementById("chap-input").value = chap;
	document.getElementById("bible-view").innerHTML = table;
}

function reloadChapter () {
	loadChapter( document.getElementById("book-input").value, document.getElementById("chap-input").value );
}

function loadSearch ( lang, id ) {
	console.log( `loadSearch: ${lang} ${id}` );
	var table = "<table>";
	if (bible[lang]!==undefined && bible[lang].words[id]!==undefined) {
		var word = buildWord( lang, id );
		globalWordId = {};
		globalWordId[lang] = [];
		globalWordId[lang].push( id );
		console.log( "writing to globalWordId", globalWordId );
		if (bible[lang].rev[id]!==undefined) {
			for (const [book, bookObj] of Object.entries(bible[lang].rev[id])) {
				for (const [chap, verseObj] of Object.entries(bookObj)) {
					//for (const [verse, nothing] of Object.entries(verseObj)) {
					for (i in verseObj) {
						var verse = verseObj[i];
						table += `<tr><td><a href="#bible-view-anchor" onclick="loadChapter('${book}','${chap}')\">${book}&nbsp;${chap}</a>:${verse}</td><td class="separator">` + buildVerse( book, chap, verse ) + '</td></tr>';
					}
				}
			}
		} else {
			console.log( `Can't find ID ${id} (${word}) in ${lang}.rev` );
			table += `<tr><td>Can't find ID ${id} (${word}) in ${lang}.rev</td></tr>`;
		}
		document.getElementById("search-heading").innerHTML = '<p>'+word+'</p>';
	} else {
		console.log( `Can't find ID ${id} in ${lang}.words` );
		table += `<tr><td>Can't find ID ${id} in ${lang}.words</td></tr>`;
	}
	table += "</table>";
	//console.log( table );
	document.getElementById("search-view").innerHTML = table;
	reloadChapter();
}

function buildWord ( lang, id ) {
	var word = '';
	if (bible[lang]!==undefined && bible[lang].words[id]!==undefined) {
		var letters = bible[lang].words[id];
		console.log( id, letters );
		if(Array.isArray(letters)) {
			for (i in letters) {
				word += '&#x'+letters[i]+';';
			}
		} else {
			return letters;
		}
	} else {
		return '? ID:'+id;
	}
	return word;
}

function buildVerse ( book, chap, verse ) {
	//var verseText = '';
	var linkedVerseText = '';
	var delim = '';
	for (const [lang, langObj] of Object.entries(bible)) {
		console.log( `lang:${lang}, book:${book}, chap:${chap}, verse:${verse}` );
		if (langObj.fwd[book]!==undefined && langObj.fwd[book][chap]!==undefined && langObj.fwd[book][chap][verse]!==undefined) {
			var verseArray = langObj.fwd[book][chap][verse];
			//console.log( verseArray );
			linkedVerseText += '<p>';
			for (i in verseArray) {
				wordId = verseArray[i];
				var word = buildWord( lang, wordId );
				if (globalWordId[lang]!==undefined) {
					for (i in globalWordId[lang]) {
						if (wordId==globalWordId[lang][i]) word = "<b>"+word+"</b>";
					}
				}
				//verseText += delim+word;
				if (word.length>1 || (word!=="." && word!=="," && word!=="?" && word!=="!")) word = delim+word
				if (langObj.rev[wordId]!==undefined)
					linkedVerseText += `<a href="#search-view-anchor" onclick="loadSearch('${lang}','${wordId}')">${word}</a>`;
				else
					linkedVerseText += word;
				delim = ' ';
			}
			linkedVerseText += '</p>';
		}
	}
	return linkedVerseText;
}


