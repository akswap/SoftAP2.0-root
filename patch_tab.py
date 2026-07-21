import re

with open('./app/src/main/java/com/example/ui/HotspotConfigTab.kt', 'r') as f:
    content = f.read()

target = """    forceDirectCli: Boolean,
    onForceDirectCliChange: (Boolean) -> Unit,"""

replacement = """    forceDirectCli: Boolean,
    onForceDirectCliChange: (Boolean) -> Unit,
    forceWifi7: Boolean,
    onForceWifi7Change: (Boolean) -> Unit,"""

content = content.replace(target, replacement)

target2 = """            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = forceDirectCli, onCheckedChange = onForceDirectCliChange)
                Text("Force Direct CLI Mode (Skip Android Settings)", color = Color.White, fontSize = 14.sp)
            }"""

replacement2 = """            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = forceDirectCli, onCheckedChange = onForceDirectCliChange)
                Text("Force Direct CLI Mode (Skip Android Settings)", color = Color.White, fontSize = 14.sp)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = forceWifi7, onCheckedChange = onForceWifi7Change)
                Text("Force WiFi 7 / 802.11be (Fix Magisk Module Reset)", color = Color.White, fontSize = 14.sp)
            }"""

content = content.replace(target2, replacement2)

with open('./app/src/main/java/com/example/ui/HotspotConfigTab.kt', 'w') as f:
    f.write(content)
print("Patched HotspotConfigTab!")
