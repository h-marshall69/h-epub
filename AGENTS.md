# H EPUB — Instrucciones permanentes para OpenCode

## Objetivo
Construir incrementalmente un lector EPUB nativo Android, offline, con Kotlin, Jetpack Compose, Material 3, MVVM, Coroutines/StateFlow, Navigation Compose, Room para biblioteca y progreso, y DataStore para preferencias. API mínima: 26 (Android 8.0). El MVP debe importar EPUB locales, catalogarlos, leerlos con imágenes y TOC, guardar/restaurar posición y personalizar la lectura.

## Estado conocido del repositorio (verificar, no asumir)
- Es un proyecto Android YA CREADO con `android create empty-activity`, no un directorio vacío.
- Una compilación `./gradlew assembleDebug` terminó correctamente.
- Gradle Wrapper: 9.1.0. Android SDK: `/home/marshall/Android/Sdk`, con platform/build tools 36. Java: OpenJDK 17. No hay Android Studio y NO debe requerirse.
- El código generado reside en `app/src/main/java/com/example/hepub/` y dispone de `MainActivity.kt`, `Navigation.kt`, `NavigationKeys.kt`, `data/DataRepository.kt`, `ui/main/MainScreen.kt`, ViewModel, temas y pruebas.
- Hay versiones en `gradle/libs.versions.toml`. Deben inspeccionarse antes de elegir dependencias.
- El ID de aplicación deseado puede evolucionar a `com.abadeer.hepub`; no cambiar paquetes y `namespace` de forma improvisada, ni alterar código existente si no es necesario.

## Primera actividad en cada sesión
1. Inspecciona `settings.gradle.kts`, los dos archivos `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `AndroidManifest.xml`, el código de `app/src`, pruebas y el contenido de `local.properties` SOLO para identificar rutas (no divulgar secretos ni guardar rutas sensibles en Git).
2. Registra en un resumen las versiones REALES de Kotlin, Android Gradle Plugin, Compose, SDK, `minSdk` y dependencias presentes.
3. Ejecuta `./gradlew assembleDebug` para confirmar la línea de base, salvo que el usuario indique que ya acaba de validarla y no ha cambiado el código.
4. Reutiliza la plantilla y la configuración compatibles. No recrees el proyecto, no elimines archivos o pruebas sin justificación, no instales Android Studio, no cambies FVM ni la configuración de Flutter en otro proyecto.

## Biblioteca EPUB
- Primera candidata: Readium Kotlin Toolkit 3.2.0 (documentación: https://readium.org/kotlin-toolkit/3.2.0/). Según la matriz oficial, coincide con compileSdk 36 y Gradle 9.1 para compilar Readium desde fuente; comprobar por separado la compatibilidad REAL de sus artefactos de Maven Central con el Android Gradle Plugin, Kotlin y Java usados por esta aplicación antes de integrarla.
- Existe Readium 3.4.0, con requisitos superiores documentados para compilar su fuente (SDK 37 / Gradle 9.7); no actualizar el entorno automáticamente sin necesidad y verificación.
- Consultar la guía oficial de `AssetRetriever`, `PublicationOpener`, el navegador EPUB, configuración de preferencias y locators; no inventar APIs ni versiones.
- Utilizar `readium-shared`, `readium-streamer` y `readium-navigator` cuando correspondan, preferiblemente desde Maven Central. No incorporar OPDS, LCP ni PDF si no son necesarios para el MVP.
- Activar core library desugaring cuando sea requerido por la versión de Readium y su documentación.
- Priorizar el navegador EPUB estable `EpubNavigatorFragment` e integrarlo correctamente con Compose mediante interoperabilidad de Fragment (p. ej. soporte oficial Fragment Compose según compatibilidad), no asumir que `AndroidView` admite directamente un Fragment.
- Los navegadores Compose/web nuevos de Readium pueden ser experimentales; no utilizarlos como base sin justificar riesgos.
- EPUB con DRM no soportado: mostrar error claro. No prometer soporte LCP sin disponer de componentes/licencias requeridos.

## Arquitectura objetivo (adaptable al código existente)
- `data/local`: persistencia Room; almacenamiento privado de copias EPUB y portadas.
- `data/repository`: contratos e implementaciones de lectura, importación y biblioteca.
- `domain/model` y `domain/usecase`: lógica de negocio cuando aporte claridad, sin abstracciones ficticias.
- `epub`: integración con Readium (apertura, metadatos, navegador, locators).
- `ui/library`, `ui/reader`, `ui/settings`, `ui/navigation`, `ui/theme`: vistas Compose, navegación y ViewModels.
- Los composables presentan estado; no procesan archivos ni acceden directamente a Room. ViewModels exponen `StateFlow` y ejecutan IO asíncrono mediante Coroutines. Gestionar estados loading/empty/success/error.
- Room: libros, URI/ruta privada, metadatos, portada cacheada, locator/progreso, marcas de tiempo. DataStore: preferencias de lectura (tema, texto, márgenes/espaciado si el motor lo permite).

## Comportamiento funcional
### Importación
- Elegir archivos con SAF (`ACTION_OPEN_DOCUMENT` o contratos Activity Result adecuados), sin permisos invasivos de almacenamiento.
- Copiar EPUB seleccionado a almacenamiento PRIVADO de la app para lectura offline duradera; verificar espacio y cerrar streams. Eliminar un libro del catálogo solo borra la copia privada y metadatos, NUNCA el archivo original del usuario.
- Validar el contenido con Readium; MIME/extensión son filtros iniciales, no pruebas suficientes. Manejar corruptos, formatos incorrectos y DRM no compatible con mensajes adecuados. Extraer título, autor y portada cuando estén disponibles.
### Lector
- XHTML, imágenes, TOC, navegación, ubicación, progreso y restauración de posición mediante locators de Readium (no usar únicamente porcentaje o desplazamiento en píxeles).
- Persistir de forma segura la posición, evitando escrituras en exceso; recuperar al reabrir.
- La biblioteca y el lector deben funcionar sin Internet; no implementar backend, cuentas ni telemetría en el MVP.
### Apariencia
- Material 3, modo claro/oscuro/sepia, tamaño de fuente; preferencias persistidas mediante DataStore; adaptarse a distintos tamaños de pantalla.

## Método de trabajo
1. Presenta un plan breve con archivos concretos y criterios de aceptación.
2. Implementa solo una fase comprobable a la vez: preparación -> importación/metadatos -> biblioteca persistente -> lector -> preferencias -> pruebas.
3. No generar stubs vacíos, lógica falsa ni pantallas que aparenten funciones no implementadas.
4. No actualizar indiscriminadamente Kotlin, AGP, Gradle, SDK o FVM; preferir cambios mínimos y compatibles.
5. Antes y después de cada fase ejecuta `./gradlew assembleDebug` y las pruebas disponibles (`./gradlew testDebugUnitTest` o tareas equivalentes verificadas); identifica y corrige los fallos introducidos.
6. Si una prueba necesita un teléfono/emulador o un archivo EPUB que no existe, indica claramente que está PENDIENTE y cómo probarlo, sin declararla superada.
7. Mantén `README.md` con prerequisitos SIN Android Studio, instrucciones de compilación, instalación, uso, estructura, límites de DRM y comandos de prueba.
8. Al terminar cada fase, resume archivos cambiados, funcionalidades REALES, verificaciones ejecutadas y siguiente fase. No declarar implementada una funcionalidad hasta comprobar el código que la realiza.

## Comandos de verificación
- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest` (si existe; confirmar tareas)
- `./gradlew installDebug` con un dispositivo autorizado conectado por ADB
- APK esperado: `app/build/outputs/apk/debug/app-debug.apk`

## Fase de arranque solicitada
Inspeccionar y mantener el proyecto existente; verificar que `minSdk` sea 26; justificar elección y compatibilidad de Readium con versiones REALES; proponer arquitectura mínima; comenzar por el flujo real de selección SAF + validación + metadatos, sin fingir persistencia de biblioteca ni lectura completa antes de implementar esas fases. Compilar al finalizar.
