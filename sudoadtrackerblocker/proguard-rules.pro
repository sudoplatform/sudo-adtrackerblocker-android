# Retrofit
-dontwarn okio.**
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit.http.** <methods>;
}

-dontwarn com.squareup.okhttp.**

# AWS
-keep class org.apache.commons.logging.**               { *; }
-keep class com.amazonaws.services.sqs.QueueUrlHandler  { *; }
-keep class com.amazonaws.javax.xml.transform.sax.*     { public *; }
-keep class com.amazonaws.javax.xml.stream.**           { *; }
-keep class com.amazonaws.services.**.model.*Exception* { *; }
-keep class org.codehaus.**                             { *; }
-keepattributes Signature,*Annotation*

-dontwarn javax.xml.stream.events.**
-dontwarn org.codehaus.jackson.**
-dontwarn org.apache.commons.logging.impl.**
-dontwarn org.apache.http.conn.scheme.**

-dontwarn com.amazonaws.util.json.**

-keepnames class com.amazonaws.**
-keepnames class com.amazon.**

-keep class com.amazonaws.services.**.*Handler

-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.logging.**

-dontwarn org.apache.http.**

-dontwarn com.amazonaws.http.**
-dontwarn com.amazonaws.metrics.**

-dontwarn java.awt.*
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }

### Ad Tracker Blocker SDK. Stop the JNA classes from being renamed or removed.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }