# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- kotlinx.serialization -------------------------------------------------
# The library ships its own rules, and they are enough to keep a serializer that R8 can see being
# used. The backup models are the case where that is worth stating outright rather than trusting:
# their serializers are reached through generated companions, a backup file written before shrinking
# has to stay readable after it, and a rule that turns out to be redundant costs a few kilobytes
# while a missing one corrupts the one feature whose whole job is not losing anything.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.persondic.data.backup.**$$serializer { *; }
-keepclassmembers class com.persondic.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class com.persondic.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- crash reports ---------------------------------------------------------
# Without this a stack trace from a release build names neither the file nor the line, which for an
# app with one user and no crash reporting means a report nobody can act on.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
