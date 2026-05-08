# VectorMinds ProGuard Rules
# Keep Retrofit interfaces
-keep,allowobfuscation interface com.vectorminds.core.network.VectorMindsApi
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep data classes used in serialization
-keepclassmembers class com.vectorminds.core.network.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
