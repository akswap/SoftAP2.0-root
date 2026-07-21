import re

with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'r') as f:
    content = f.read()

# Replace the updateRealActiveChannels function
# It starts with "private fun updateRealActiveChannels() {" and ends before "    fun requestWriteSettingsPermission(context: Context) {"
start_str = "    private fun updateRealActiveChannels() {"
end_str = "    fun requestWriteSettingsPermission(context: Context) {"

with open('patch_debug_terminal.txt', 'r') as f:
    patch = f.read()

start_idx = content.find(start_str)
end_idx = content.find(end_str)

if start_idx != -1 and end_idx != -1:
    new_content = content[:start_idx] + patch + "\n" + content[end_idx:]
    with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'w') as f:
        f.write(new_content)
    print("Patched successfully")
else:
    print("Could not find start or end index")
