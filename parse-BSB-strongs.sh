# from inside the biblelookup directory (creek package in common parent directory)
cd ..

# English BSB, BSB-strongs-embedded, Strongs
rm biblelookup/compiled-texts/BSBEnglish*
rm biblelookup/compiled-texts/Strongs*
echo "combining bsb_tables.csv..."
###java creek.ExecCombine biblelookup/openbible.org/bsb_tables.csv
cat biblelookup/openbible.org/bsb_tables.1.csv biblelookup/openbible.org/bsb_tables.2.csv biblelookup/openbible.org/bsb_tables.3.csv > biblelookup/openbible.org/bsb_tables.csv
java biblelookup.EnglishStrongsFromBSBTables biblelookup/openbible.org/bsb_tables.csv biblelookup/compiled-texts/BSBEnglish.csv biblelookup/compiled-texts/Strongs.csv

# BSB
java biblelookup.BibleTextLookup biblelookup/compiled-texts/BSBEnglish.csv biblelookup/books.csv biblelookup/compiled-texts/BSBEnglish-lookup.json "(\\w+)" biblelookup/book-order.json false "\\w{5,}"
echo "bible.en = " > biblelookup/compiled-texts/BSBEnglish-lookup.js
cat biblelookup/compiled-texts/BSBEnglish-lookup.json >> biblelookup/compiled-texts/BSBEnglish-lookup.js

# BSB with stongs embedded
java biblelookup.BibleTextLookup biblelookup/compiled-texts/BSBEnglish-strongs.csv biblelookup/books.csv biblelookup/compiled-texts/BSBEnglish-strongs-lookup.json "(\\S+)" biblelookup/book-order.json false "[GH]\\d+"
echo "bible.en = " > biblelookup/compiled-texts/BSBEnglish-strongs-lookup.js
cat biblelookup/compiled-texts/BSBEnglish-strongs-lookup.json >> biblelookup/compiled-texts/BSBEnglish-strongs-lookup.js

# Strongs
java biblelookup.BibleTextLookup biblelookup/compiled-texts/Strongs.csv biblelookup/books.csv biblelookup/compiled-texts/Strongs-lookup.json "(\\w+)" biblelookup/book-order.json false
echo "bible.st = " > biblelookup/compiled-texts/Strongs-lookup.js
cat biblelookup/compiled-texts/Strongs-lookup.json >> biblelookup/compiled-texts/Strongs-lookup.js

# Translation lookup
java biblelookup.CompileIdLookups biblelookup/compiled-texts/BSBEnglish-word-id.json en biblelookup/compiled-texts/BSBEnglish-lookup-idlookup.json he biblelookup/compiled-texts/OTHebrew-lookup-idlookup.json gr biblelookup/compiled-texts/NTGreek-lookup-idlookup.json st biblelookup/compiled-texts/Strongs-lookup-idlookup.json
java biblelookup.TranslateFromBSBTables biblelookup/compiled-texts/BSBEnglish-word-id.json biblelookup/openbible.org/bsb_tables.csv
echo "translation = " > biblelookup/compiled-texts/BSBEnglish-word-id-translate.js
cat biblelookup/compiled-texts/BSBEnglish-word-id-translate.json >> biblelookup/compiled-texts/BSBEnglish-word-id-translate.js

rm biblelookup/openbible.org/bsb_tables.csv
