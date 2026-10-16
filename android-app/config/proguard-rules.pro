# uni-app SDK 混淆规则
-keep class io.dcloud.** { *; }
-keep class io.dcloud.feature.weex_amap.** { *; }
-keep class com.amap.api.** { *; }
-keep class com.tencent.** { *; }
-keep class uni.** { *; }
-keep class com.uni.** { *; }
-keep class com.unicom.** { *; }
-keep class * extends android.webkit.WebViewClient
-keep class * extends android.webkit.WebChromeClient

# 保留 R 资源
-keep class **.R$* { *; }

# 保留泛型
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
