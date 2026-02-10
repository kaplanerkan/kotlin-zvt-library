# ZVT Library - Consumer ProGuard Rules
# Bu kurallar kütüphaneyi kullanan projede otomatik uygulanır

-keep class com.erkan.zvt.model.** { *; }
-keep class com.erkan.zvt.ZvtClient { *; }
-keep class com.erkan.zvt.ZvtCallback { *; }
-keep class com.erkan.zvt.protocol.ZvtConstants { *; }
-keep class com.erkan.zvt.util.BcdHelper { *; }
-keep class com.erkan.zvt.util.TlvParser { *; }
-keep class com.erkan.zvt.util.TlvParser$TlvEntry { *; }
