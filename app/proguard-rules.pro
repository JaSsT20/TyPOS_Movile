# Reglas de optimización y preservación ProGuard/R8 para TyPOS Móvil

# Preservar información de líneas para informes de errores
-keepattributes SourceFile,LineNumberTable

# --- ROOM DATABASE ---
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.typdevstudio.typos_movil.datos.local.entidades.** { *; }
-keep class com.typdevstudio.typos_movil.datos.local.relaciones.** { *; }
-keep class com.typdevstudio.typos_movil.datos.local.daos.** { *; }

# --- MODELOS DE DATOS, REPOSITORIOS Y UTILIDADES ---
-keep class com.typdevstudio.typos_movil.utilidades.actualizador.** { *; }
-keep class com.typdevstudio.typos_movil.utilidades.licencia.** { *; }
-keep class com.typdevstudio.typos_movil.datos.repositorio.** { *; }
-keep class com.typdevstudio.typos_movil.ui.ventas.ItemCarrito { *; }

# --- ML KIT Y CAMERAX ---
-keep class com.google.mlkit.vision.barcode.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn androidx.camera.**

# --- COROUTINES Y KOTLIN ---
-dontwarn kotlinx.coroutines.**
-dontwarn java.lang.invoke.**