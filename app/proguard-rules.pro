# Proguard rules for Style Aeternum CRM
-keep class com.styleaeternum.crm.data.** { *; }
-keep class com.styleaeternum.crm.service.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
# Google API Client
-keep class com.google.api.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.api.**
