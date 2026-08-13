find app/src/main/java/io/lunosfer/dreamap/ui/ -name "*.kt" | xargs grep -E 'text\s*=\s*"[^"]*[a-zA-ZçğıöşüÇĞİÖŞÜ][^"]*"' | grep -v '""' | grep -v 'route' | grep -v 'http' > hardcoded.txt
cat hardcoded.txt
