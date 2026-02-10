# ZVT Library - Consumer ProGuard Rules
# Bu kurallar kütüphaneyi kullanan projede otomatik uygulanır

-keep class com.panda.zvt_library.model.** { *; }
-keep class com.panda.zvt_library.ZvtClient { *; }
-keep class com.panda.zvt_library.ZvtCallback { *; }
-keep class com.panda.zvt_library.protocol.ZvtConstants { *; }
-keep class com.panda.zvt_library.util.BcdHelper { *; }
-keep class com.panda.zvt_library.util.TlvParser { *; }
-keep class com.panda.zvt_library.util.TlvParser$TlvEntry { *; }
