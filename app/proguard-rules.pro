# ==============================================================================
# REGLAS PROGUARD / R8 PARA TYPOS MÓVIL
# ==============================================================================

# Preservar anotaciones, firmas genéricas y líneas de depuración
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# --- PRESERVAR CÓDIGO COMPLETO DE LA APLICACIÓN ---
# Protege todas las clases, ViewModels, Repositorios, Entidades y Room DAOs de la app
-keep class com.typdevstudio.typos_movil.** { *; }
-keep interface com.typdevstudio.typos_movil.** { *; }
-keepenum class com.typdevstudio.typos_movil.** { *; }

# --- ROOM DATABASE ---
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
    *;
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.migration.Migration { *; }

# --- JETPACK LIFECYCLE & VIEWMODELS ---
# Evita que R8 elimine o modifique constructores de ViewModels requeridos por Compose
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
    *;
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
    *;
}
-keep class androidx.lifecycle.ViewModelProvider$Factory { *; }

# --- JETPACK COMPOSE ---
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# --- COROUTINES & KOTLIN ---
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn java.lang.invoke.**

# --- ML KIT Y CAMERAX ---
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**