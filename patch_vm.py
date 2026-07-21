import re

with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'r') as f:
    content = f.read()

target = "    val forceDirectCli = kotlinx.coroutines.flow.MutableStateFlow(true)"
replacement = """    val forceDirectCli = kotlinx.coroutines.flow.MutableStateFlow(true)
    val forceWifi7 = kotlinx.coroutines.flow.MutableStateFlow(true)"""

if target in content:
    content = content.replace(target, replacement)
    with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'w') as f:
        f.write(content)
    print("Patched VM!")
else:
    print("Target not found VM")
