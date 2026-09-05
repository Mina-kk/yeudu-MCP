-keep class io.modelcontextprotocol.** { *; }
-keep class io.ktor.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn com.sun.nio.file.SensitivityWatchEventModifier
-dontwarn javax.swing.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn org.htmlunit.corejs.javascript.tools.**

# Stable JSON/MCP/WebView contracts
-keep class com.mina.legadostudio.data.db.** { *; }
-keep class com.mina.legadostudio.runtime.** { *; }
-keep class com.mina.legadostudio.network.** { *; }
-keep class com.mina.legadostudio.device.** { *; }
-keep class com.mina.legadostudio.skills.** { *; }
-keep class com.mina.legadostudio.mcp.** { *; }
-keep class com.mina.legadostudio.export.** { *; }
-keep class io.legado.app.** { *; }
-keep class com.script.** { *; }
-keep class org.htmlunit.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
