while true; do
  clear
  echo "--- $(date) ---"
  for file in app/src/main/res/values*/strings.xml; do
    count=$(grep -oP '(?<=name=")[^"]*' $file | wc -l)
    echo "$file: $count"
  done
  sleep 10
done
