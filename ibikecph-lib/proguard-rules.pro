# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/markus/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# The following is taken from https://realm.io/docs/java/0.88.2/#proguard
-keep class io.realm.annotations.RealmModule
-keep @io.realm.annotations.RealmModule class *
-keep class io.realm.internal.Keep
-keep @io.realm.internal.Keep class * { *; }
-dontwarn javax.**
-dontwarn io.realm.**

# The following is taken from https://github.com/krschultz/android-proguard-snippets/pull/114/files
# Proguard configuration for Jackson 2.x (fasterxml package instead of codehaus package)
-keepnames class com.fasterxml.jackson.annotation.** { *; }
-keep public class com.spoiledmilk.ibikecph.persist.* {
    public void set*(*);
    public ** get*();
}
-dontwarn org.w3c.dom.bootstrap.DOMImplementationRegistry
-dontwarn java.beans.Transient
-dontwarn java.beans.ConstructorProperties

# Not sure if those are needed in other configurations
-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}
-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}
# This has been added to deal with the fact that com.fasterxml.jackson.databind.ext.PathDeserializer
# couldn't find the referenced classes.
-dontwarn java.nio.file.Path
-dontwarn java.nio.file.Paths

# The following is taken from https://github.com/square/okio/issues/60
-dontwarn okio.**

# The following is taken from
# http://stackoverflow.com/questions/27443241/proguard-not-working-with-okhttp
-dontwarn com.squareup.okhttp.internal.huc.**

# This fixes the RoundedImageView's warnings when minimizing
-dontwarn com.squareup.picasso.Transformation
-dontwarn java.awt.**

