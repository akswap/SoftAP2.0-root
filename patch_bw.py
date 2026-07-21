import re

with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'r') as f:
    content = f.read()

target = """                        if (bwMatch != null) {
                            val bwVal = bwMatch.groupValues[1].toIntOrNull()
                            if (bwVal != null) {
                                if (bwVal in 1..6) {
                                    bandwidth = when (bwVal) {
                                        1 -> "20"
                                        2 -> "40"
                                        3 -> "80"
                                        4 -> "160"
                                        6 -> "320"
                                        else -> bandwidth
                                    }
                                } else {
                                    bandwidth = bwVal.toString()
                                }
                            }
                        }"""

replacement = """                        if (bwMatch != null) {
                            val bwVal = bwMatch.groupValues[1].toIntOrNull()
                            if (bwVal != null) {
                                bandwidth = when (bwVal) {
                                    2 -> "20"
                                    3 -> "40"
                                    4 -> "80"
                                    6 -> "160"
                                    11 -> "320"
                                    20, 40, 80, 160, 320 -> bwVal.toString()
                                    else -> bwVal.toString()
                                }
                            }
                        }"""

if target in content:
    content = content.replace(target, replacement)
    with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'w') as f:
        f.write(content)
    print("Patched bandwidth mapping!")
else:
    print("Target not found")
