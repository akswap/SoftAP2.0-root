import re

with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'r') as f:
    content = f.read()

# Let's insert setIeee80211beEnabled into writeConfigToSystemSettings()
target = """                            try {
                                val setChannelMethod = builderClass.getMethod("setChannel", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                                if (bandsList.contains("6G") && currentCh6g != "Auto") {"""

replacement = """                            try {
                                val setBeMethod = builderClass.getMethod("setIeee80211beEnabled", Boolean::class.javaPrimitiveType)
                                setBeMethod.invoke(builderInstance, true)
                            } catch(e: Exception) {}
                            
                            try {
                                val setAxMethod = builderClass.getMethod("setIeee80211axEnabled", Boolean::class.javaPrimitiveType)
                                setAxMethod.invoke(builderInstance, true)
                            } catch(e: Exception) {}

                            try {
                                val setChannelMethod = builderClass.getMethod("setChannel", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                                if (bandsList.contains("6G") && currentCh6g != "Auto") {"""

if target in content:
    content = content.replace(target, replacement)
    with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'w') as f:
        f.write(content)
    print("Patched HotspotViewModel!")
else:
    print("Target not found")
