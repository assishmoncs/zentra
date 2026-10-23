# Zentra — Production R8 / ProGuard rules

# Preserve generic type signatures and annotations required by Gson and Room
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-dontwarn sun.misc.**

# Keep Gson TypeToken subclasses used for Room JSON column deserialization
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep serialized domain/data models stored as JSON in Room
-keep class com.hsissa.zentra.service.AppUsageInfo { *; }
-keep enum com.hsissa.zentra.service.AppCategory { *; }

# Keep Room entities and DAO interfaces
-keep class com.hsissa.zentra.data.local.** { *; }

# Keep source file names and line numbers for readable deobfuscated stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
