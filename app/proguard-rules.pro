# Reflection metadata used by Jackson, TypeReference and ModelField<T>.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Framework entry names are stored in assets/xposed_init / META-INF/xposed/java_init.list.
-keep class com.surexu.sesame.hook.LegacyEntry { public *; }
-keep class com.surexu.sesame.hook.LibXposedEntry { public *; }

# Model.initAllModel invokes public no-arg constructors. Simple names are config keys.
-keep class com.surexu.sesame.model.** extends com.surexu.sesame.data.Model {
    public <init>();
}
-keepclassmembers class com.surexu.sesame.model.task.antOrchard.AntOrchard {
    *** getWua(...);
}

# Jackson persists these beans using field/getter/setter names, including @JsonIgnore.
# Keep the data boundary, not the task implementations or UI dependencies.
-keep class com.surexu.sesame.data.* { *; }
-keep class com.surexu.sesame.data.modelFieldExt.** { *; }
-keep class com.surexu.sesame.entity.** { *; }
-keep class com.surexu.sesame.hook.RpcRequest { *; }
-keep class com.surexu.sesame.util.Status { *; }
-keep class com.surexu.sesame.util.Statistics { *; }
-keep class com.surexu.sesame.util.Statistics$* { *; }

# Both Jackson 2.13 and 2.18 use reflective constructors and enum fields internally.
-keepnames class com.fasterxml.jackson.** { *; }
-keepclassmembers class com.fasterxml.jackson.** { <init>(...); }
-keep enum com.fasterxml.jackson.** { *; }
-keep class com.fasterxml.jackson.core.type.TypeReference { *; }
-keep,allowobfuscation class * extends com.fasterxml.jackson.core.type.TypeReference { *; }

# Optional desktop JavaBeans annotations; Jackson catches their absence on Android.
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient

# New dual-entry hook backends and runtime reflection.
-keep class com.surexu.sesame.hook.** { *; }
-keep class com.surexu.sesame.util.compat.** { *; }
-keep class com.surexu.sesame.model.extensions.** { *; }
-dontwarn de.robv.android.xposed.**
-dontwarn io.github.libxposed.**
