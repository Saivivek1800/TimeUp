# Rules for the release build (code shrinking with R8).
#
# Hilt, Room, WorkManager, Coil and OkHttp bring their own keep rules, so very little is needed here.

# Hilt refers to this annotation at build time only. It is not needed when the app runs.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue

# Keep readable line numbers in crash reports from release builds, without exposing the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
