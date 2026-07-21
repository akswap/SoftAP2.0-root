with open('./app/src/main/java/com/example/ui/MainHotspotScreen.kt', 'r') as f:
    content = f.read()

import re

# Find the HotspotConfigTab call
config_tab_call_regex = re.compile(r"(\s*)(HotspotConfigTab\([\s\S]*?onDeleteProfile = \{ viewModel\.deleteProfile\(it\) \}\n\s*\))")

# Wrap it in a Column and add the terminal box
replacement = r"""\1Column(modifier = Modifier.fillMaxSize()) {
\1    Box(modifier = Modifier.weight(1f)) {
\1        \2
\1    }
\1    if (lastTerminalOutput.isNotBlank()) {
\1        Card(
\1            modifier = Modifier.fillMaxWidth().padding(8.dp).height(150.dp),
\1            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Black),
\1            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
\1        ) {
\1            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
\1                item {
\1                    Text(
\1                        text = lastTerminalOutput,
\1                        color = androidx.compose.ui.graphics.Color.Green,
\1                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
\1                        fontSize = 12.sp
\1                    )
\1                }
\1            }
\1        }
\1    }
\1}"""

new_content = config_tab_call_regex.sub(replacement, content)

with open('./app/src/main/java/com/example/ui/MainHotspotScreen.kt', 'w') as f:
    f.write(new_content)

