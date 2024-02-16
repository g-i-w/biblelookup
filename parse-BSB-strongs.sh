# from inside the biblelookup directory (creek package in common parent directory)
cd ..

# English BSB, BSB-strongs-embedded, Strongs
rm biblelookup/compiled-texts/BSBEnglish*
rm biblelookup/compiled-texts/Strongs*
echo "combining bsb_tables.csv..."
java creek.ExecCombine biblelookup/openbible.org/bsb_tables.csv
java biblelookup.EnglishStrongsFromBSBTables biblelookup/openbible.org/bsb_tables.csv biblelookup/compiled-texts/BSBEnglish.csv biblelookup/compiled-texts/Strongs.csv

# BSB
java biblelookup.BibleTextLookup biblelookup/compiled-texts/BSBEnglish.csv biblelookup/books.csv biblelookup/compiled-texts/BSBEnglish-lookup.json "(\\S+)" biblelookup/book-order.json false "\\w\\w\\w\\w\\w"
echo "bible.BSBEnglish = " > biblelookup/compiled-texts/BSBEnglish-lookup.js
cat biblelookup/compiled-texts/BSBEnglish-lookup.json >> biblelookup/compiled-texts/BSBEnglish-lookup.js

# BSB with stongs embedded
java biblelookup.BibleTextLookup biblelookup/compiled-texts/BSBEnglish-strongs.csv biblelookup/books.csv biblelookup/compiled-texts/BSBEnglish-strongs-lookup.json "(\\S+)" biblelookup/book-order.json false "[GH]\\d+"
echo "bible.BSBEnglish = " > biblelookup/compiled-texts/BSBEnglish-strongs-lookup.js
cat biblelookup/compiled-texts/BSBEnglish-strongs-lookup.json >> biblelookup/compiled-texts/BSBEnglish-strongs-lookup.js

# Strongs
java biblelookup.BibleTextLookup biblelookup/compiled-texts/Strongs.csv biblelookup/books.csv biblelookup/compiled-texts/Strongs-lookup.json "(\\S+)" biblelookup/book-order.json false
echo "bible.BSBEnglish = " > biblelookup/compiled-texts/Strongs-lookup.js
cat biblelookup/compiled-texts/Strongs-lookup.json >> biblelookup/compiled-texts/Strongs-lookup.js

rm biblelookup/openbible.org/bsb_tables.csv
