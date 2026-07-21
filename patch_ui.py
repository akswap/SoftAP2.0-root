import re

with open('./app/src/main/java/com/example/ui/MainHotspotScreen.kt', 'r') as f:
    content = f.read()

target = "    val forceDirectCli by viewModel.forceDirectCli.collectAsState()"
replacement = """    val forceDirectCli by viewModel.forceDirectCli.collectAsState()
    val forceWifi7 by viewModel.forceWifi7.collectAsState()"""

content = content.replace(target, replacement)

target2 = """                        forceDirectCli = forceDirectCli,
                        onForceDirectCliChange = { viewModel.forceDirectCli.value = it },"""
replacement2 = """                        forceDirectCli = forceDirectCli,
                        onForceDirectCliChange = { viewModel.forceDirectCli.value = it },
                        forceWifi7 = forceWifi7,
                        onForceWifi7Change = { viewModel.forceWifi7.value = it },"""

content = content.replace(target2, replacement2)

with open('./app/src/main/java/com/example/ui/MainHotspotScreen.kt', 'w') as f:
    f.write(content)
print("Patched MainHotspotScreen!")
