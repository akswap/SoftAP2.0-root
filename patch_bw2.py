import re

with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'r') as f:
    content = f.read()

target = """                                    2 -> "20"
                                    3 -> "40"
                                    4 -> "80"
                                    6 -> "160"
                                    11 -> "320\""""

replacement = """                                    0, 1, 2 -> "20"
                                    3 -> "40"
                                    4 -> "80"
                                    6 -> "160"
                                    11 -> "320\""""

if target in content:
    content = content.replace(target, replacement)
    with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'w') as f:
        f.write(content)
    print("Patched bandwidth mapping 2!")
else:
    print("Target not found 2")
