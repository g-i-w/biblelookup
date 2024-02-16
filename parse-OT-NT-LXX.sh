# from inside the biblelookup directory (creek package in common parent directory)
cd ..

# Old Testament (Hebrew)
rm biblelookup/compiled-texts/OTHebrew*
java creek.ExecRegexCSV biblelookup/ccel.org/hebrewot biblelookup/compiled-texts/OTHebrew.csv "(\d+):(\d+)\s*(.*)\s*" "(\w+)\.html"
java biblelookup.CSVtoJSON biblelookup/compiled-texts/OTHebrew.csv biblelookup/books.csv biblelookup/compiled-texts/OTHebrew-utf8.js "UTF-8"
java biblelookup.BibleTextLookup biblelookup/compiled-texts/OTHebrew.csv biblelookup/books.csv biblelookup/compiled-texts/OTHebrew-lookup.json "(\\S+)" biblelookup/book-order.json
echo "bible.OTHebrew = " > biblelookup/compiled-texts/OTHebrew-lookup.js
cat biblelookup/compiled-texts/OTHebrew-lookup.json >> biblelookup/compiled-texts/OTHebrew-lookup.js

# New Testament (Greek)
rm biblelookup/compiled-texts/NTGreek*
java creek.ExecRegexBlobCSV biblelookup/ebible.org/grctr biblelookup/compiled-texts/NTGreek.csv ">(\d+)&#160;</span>([^<]+)\s+<" "(\w{3})0*(\d+)\.htm"
java biblelookup.CSVtoJSON biblelookup/compiled-texts/NTGreek.csv biblelookup/books.csv biblelookup/compiled-texts/NTGreek-utf8.js "UTF-8"
java biblelookup.BibleTextLookup biblelookup/compiled-texts/NTGreek.csv biblelookup/books.csv biblelookup/compiled-texts/NTGreek-lookup.json "([^\\s«»·:;,\\-…\\(\\)\\.]+)" biblelookup/book-order.json
echo "bible.NTGreek = " > biblelookup/compiled-texts/NTGreek-lookup.js
cat biblelookup/compiled-texts/NTGreek-lookup.json >> biblelookup/compiled-texts/NTGreek-lookup.js

# OT & NT Septuegent (Greek)
rm biblelookup/compiled-texts/Septuagint*
java creek.ExecRegexBlobCSV biblelookup/ebible.org/grcbrent biblelookup/compiled-texts/Septuagint.csv ">(\d+)&#160;</span>([^<]+)\s+<" "(\w{3})0*(\d+)\.htm"
java biblelookup.CSVtoJSON biblelookup/compiled-texts/Septuagint.csv biblelookup/books.csv biblelookup/compiled-texts/Septuagint-utf8.js "UTF-8"
java biblelookup.BibleTextLookup biblelookup/compiled-texts/Septuagint.csv biblelookup/books.csv biblelookup/compiled-texts/Septuagint-lookup.json "([^\\s«»·:;,\\-…\\(\\)\\.]+)" biblelookup/book-order.json
echo "bible.Septuagint = " > biblelookup/compiled-texts/Septuagint-lookup.js
cat biblelookup/compiled-texts/Septuagint-lookup.json >> biblelookup/compiled-texts/Septuagint-lookup.js


