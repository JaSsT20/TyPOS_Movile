# 📱 TyPOS_Movil - Guía y Reglas de Arquitectura y Rendimiento

Este documento define los estándares técnicos, principios de diseño y reglas de rendimiento obligatorias para el desarrollo de **TyPOS_Movil**.

---

## 🎯 Objetivo Principal
Crear una aplicación de Punto de Venta (POS) móvil **ultra ligera, modular y de alto rendimiento**, optimizada para operar con total fluidez tanto en dispositivos de **gama baja** (2-3 GB RAM, CPUs modestas, Android 7.0 / API 24+) como en dispositivos de gama alta.

---

## 🇪🇸 Regla Fundamental de Código: 100% en Español
* **Nombres de Clases, Funciones, Variables y Paquetes:** Todo el código fuente debe estar nombrado en español (ej. `ProductoEntidad`, `VentaEntidad`, `DetalleVentaEntidad`, `obtenerProductos()`, `registrarVenta()`, `precioVenta`, `montoTotal`).
* **Base de Datos:** Tablas y columnas en español (`productos`, `ventas`, `detalles_venta`, `codigo_barras`, `precio_costo`).
* **Interfaz de Usuario (UI) y Textos:** 100% en español con formato claro y amigable.
* **Comentarios y Documentación:** Redactados completamente en español.

---

## 🏛️ 1. Arquitectura y Estructura de Paquetes (Clean Architecture + UDF)

El proyecto se estructura con separación estricta de responsabilidades:

```
com.typdevstudio.typos_movil/
 ├── data/
 │    ├── local/               # Persistencia Room (Entities, DAOs, TypeConverters, AppDatabase)
 │    ├── printer/             # Driver ESC/POS, BluetoothSocket, USB OTG
 │    └── repository/          # Implementaciones de Repositorios (ProductRepositoryImpl, SaleRepositoryImpl)
 ├── domain/
 │    ├── model/               # Modelos de negocio inmutables (Product, CartItem, Sale, Ticket)
 │    ├── repository/          # Interfaces abstractas de repositorios
 │    └── usecase/             # Casos de uso específicos (ej. ProcessSaleUseCase, PrintReceiptUseCase)
 └── ui/
      ├── theme/               # Paleta de colores, tipografía, formas (Material 3)
      ├── components/          # Componentes UI reutilizables (Botones, Inputs, Cards, Diálogos)
      ├── products/            # Pantallas y ViewModels de Catálogo / Inventario
      ├── pos/                 # Pantallas y ViewModels de Caja / Venta activa
      ├── history/             # Historial de Ventas, reportes y reimpresión de tickets
      └── settings/            # Configuración de impresora térmica (Bluetooth MAC, papel 58/80mm)
```

---

## ⚡ 2. Reglas de Rendimiento para Teléfonos de Gama Baja

### A. Jetpack Compose
1. **Modelos de Estado Inmutables:**
   * Todo estado en la UI debe ser inmutable (`data class` con solo `val`).
   * Aplicar `@Immutable` o `@Stable` cuando sea necesario para optimizar recomposiciones.
2. **Desacoplamiento en Composables:**
   * Los componentes reutilizables no deben recibir ViewModels. Deben recibir **datos planos** y **lambdas de eventos** (`onClick: (Long) -> Unit`).
3. **Listas Eficientes (`LazyColumn` / `LazyRow`):**
   * **Siempre** proporcionar `key = { it.id }` en `items()` para evitar recrear elementos al insertar/eliminar.
   * Usar `derivedStateOf` para cálculos reactivos dependientes (ej. subtotales, totales de carrito, listas filtradas).
4. **Gestión de Memoria e Imágenes:**
   * Cargar miniaturas de imágenes con límite de resolución (máx 200x200px) y caché estricto usando Coil.

### B. Base de Datos SQLite (Room)
1. **Consultas Reactivas Asíncronas:**
   * Lecturas continuas mediante `Flow<List<T>>` en DAOs.
   * Toda escritura (`@Insert`, `@Update`, `@Delete`) debe ser `suspend` y ejecutarse en `Dispatchers.IO`.
2. **Índices en Tablas:**
   * Agregar `@Index` en columnas clave para búsquedas rápidas (`code`, `name`, `createdAt`).
3. **Transacciones Atómicas:**
   * Usar `@Transaction` para operaciones complejas como registrar una venta y descontar stock simultáneamente.

---

## 🧩 3. Componentes UI Reutilizables (`ui/components`)

Para evitar duplicar código y mantener una interfaz coherente:
* **`PosButton`**: Botón primario/secundario con estados de carga (`isLoading`) y deshabilitado.
* **`PosTextField` / `PosNumberField`**: Inputs optimizados para números, precios y códigos de barra.
* **`PosCard`**: Tarjetas de producto/venta con elevación controlada para reducir el consumo de GPU.
* **`PosDialog` / `PosConfirmDialog`**: Modales reutilizables para confirmación de acciones destructivas o cobro.
* **`PosEmptyState`**: Indicador visual limpio cuando una lista o carrito no contiene elementos.
* **`PosSearchBar`**: Barra de búsqueda reactiva con debounce para no saturar el CPU con cada tecla.

---

## 🖨️ 4. Reglas de Impresión Térmica (ESC/POS & Bluetooth)

1. **Protocolo ESC/POS Ligero:**
   * Generación de bytes pura sin dependencias externas pesadas.
   * Soporte configurable para anchos estándar de papel: **58 mm (32 columnas)** y **80 mm (48 columnas)**.
2. **Conexión en Segundo Plano:**
   * La búsqueda, conexión por `BluetoothSocket` y transmisión de datos deben ejecutarse estrictamente fuera del hilo principal (`Dispatchers.IO`).
3. **Manejo de Errores y Reconexión:**
   * Validar permisos (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` en Android 12+ y permisos clásicos en Android 7-11).
   * Notificar fallos de conexión o falta de papel de forma amigable en la UI.

---

## 🛠️ 5. Stack Tecnológico
* **Lenguaje:** Kotlin
* **Min SDK:** API 24 (Android 7.0 Nougat)
* **Target/Compile SDK:** API 34+
* **UI Toolkit:** Jetpack Compose + Material 3
* **Persistencia:** AndroidX Room + SQLite
* **Concurrencia:** Kotlin Coroutines & StateFlow (UDF)
* **Inyección de Dependencias / Arquitectura:** ViewModel + Repository Pattern
