import sys

with open('app/src/main/java/io/lunosfer/dreamap/ui/screens/ProfileScreen.kt', 'r') as f:
    content = f.read()

start_marker = "            is ProfileUiState.Content -> {\n                Column(\n                    modifier = Modifier"
end_marker = "                // Edit Profile Dialog"

if start_marker in content and end_marker in content:
    pre = content.split(start_marker)[0]
    post = end_marker + content.split(end_marker, 1)[1]
    
    with open('new_content.kt', 'r') as f:
        mid = f.read()
        
    new_code = pre + "            is ProfileUiState.Content -> {\n" + mid + "\n                " + post
    
    with open('app/src/main/java/io/lunosfer/dreamap/ui/screens/ProfileScreen.kt', 'w') as f:
        f.write(new_code)
    print("Replaced successfully.")
else:
    print("Markers not found.")
