# H EPUB

Lector EPUB nativo Android, offline, construido con Kotlin, Compose y Material 3.

## Estado

La importación mediante Storage Access Framework copia el EPUB al almacenamiento privado, valida su estructura (`mimetype`, `META-INF/container.xml` y paquete OPF), extrae título/autores y registra el libro en una biblioteca Room. La biblioteca se recupera al reiniciar, evita duplicados por SHA-256, muestra estado vacío/errores y elimina la fila y la copia privada sin tocar el original seleccionado.

El analizador de importación usa `android.util.Xml` con namespaces habilitados, DTD desactivado y rechazo explícito de eventos `DOCTYPE`/entidades. No depende de la característica Apache `http://apache.org/xml/features/disallow-doctype-decl`, que no está disponible en algunos parsers XML de Android y era la causa del fallo al importar EPUB válidos. Los errores internos de configuración XML no se exponen al usuario.

Las referencias XML numéricas legítimas (`&#8217;`, `&#x2019;`) y entidades predefinidas se aceptan; las entidades declaradas por DTD o externas se rechazan. La importación no exige idioma, autor ni portada, por lo que EPUB en inglés (`en`, `en-US`, `en-GB`), español o sin esos metadatos siguen siendo candidatos válidos. Las causas originales se registran con `HEPUB-Importer`.

La apertura del lector consulta Room mediante `Dispatchers.IO`; las operaciones de Fragment y Readium permanecen en el hilo principal. También se guarda el locator mediante IO, evitando bloqueos de la interfaz y mostrando un mensaje controlado si falta el registro o la copia privada.

Readium Kotlin Toolkit 3.2.0 está integrado desde Maven Central mediante `readium-shared`, `readium-streamer` y `readium-navigator`; se habilitó core library desugaring. La apertura usa `AssetRetriever` sobre la copia privada y `PublicationOpener`/`DefaultPublicationParser`. El contenido se renderiza con `EpubNavigatorFragment` creado por `EpubNavigatorFactory`, aislado en `ReaderActivity`/`ReaderFragment` para respetar el ciclo de vida de Fragment. El factory recibe `publication.readingOrder`; `tableOfContents` se reserva para el índice de la interfaz.

La biblioteca abre los libros pulsando su título. El lector ofrece controles Material 3 para anterior/siguiente mediante `OverflowableNavigator`, un índice jerárquico basado exclusivamente en `publication.tableOfContents` y navegación con `navigator.go(Link)`. Los controles pueden ocultarse y volver a mostrarse. `currentLocator.locations.totalProgression` alimenta el porcentaje y la barra global; si falta, se informa que el progreso no está disponible.

El `Locator` se serializa como JSON, se guarda con debounce de 500 ms y se restaura por libro al reabrir. La `ReaderFragment` usa `EpubNavigatorFragment.createDummyFactory()` cuando el proceso se recrea antes de que Readium pueda reabrir la copia privada; la actividad vuelve a cargar el libro desde Room y reemplaza el fragmento provisional. Un locator corrupto se descarta de forma segura. DRM/LCP no está soportado.

El `ComposeView` de controles usa altura `WRAP_CONTENT` y sólo cubre sus controles; así no intercepta los gestos del navegador Readium. La actividad registra `readingOrder`, recursos, MIME, TOC y locators bajo `HEPUB-Readium` para diagnosticar EPUB reflowable/fixed-layout. Los locators guardados se validan contra el nuevo `readingOrder` y se descartan de forma segura si pertenecen a una secuencia anterior. El AAR Maven 3.2.0 disponible no contiene `DirectionalNavigationAdapter`; se mantienen los gestos nativos y las APIs oficiales `goForward`, `goBackward` y `go(Link)`.

## Requisitos y comandos

- JDK 17, Android SDK 36 y Gradle Wrapper; no requiere Android Studio.
- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest`
- `./gradlew connectedDebugAndroidTest`

Las pruebas específicas `EpubImporterTest` cubren metadatos con namespaces y caracteres especiales, ZIP no EPUB, archivo corrupto y DTD/entidades externas. La ejecución instrumentada requiere un dispositivo conectado.

`ReaderBookLoaderTest` comprueba la recuperación del registro usado por el flujo del lector sin consultas de Room en Main. La prueba manual de abrir/renderizar un EPUB real debe realizarse importando un EPUB sin DRM en el dispositivo y pulsando su título; no se marca como automatizada porque requiere interacción SAF.

## Traducción offline (primera integración)

Se añadió `com.google.mlkit:translate:17.0.3`, la versión estable indicada por la documentación oficial de ML Kit. `TranslationEngine` abstrae disponibilidad, descarga y traducción; `MlKitTranslationEngine` usa `Translation.getClient`, `TranslatorOptions` inglés→español, `downloadModelIfNeeded`, `RemoteModelManager` y `Translator.close()`. `TranslationViewModel` expone estados `Checking`, `ModelUnavailable`, `Downloading`, `Ready`, `Translating`, `Completed` y `Error`. La biblioteca muestra un panel Compose de prueba: el usuario consiente la descarga (se recomienda Wi-Fi) y después puede traducir texto local sin enviar el contenido del libro a servidores.

La integración aún no modifica XHTML ni traduce capítulos. La documentación oficial de Readium 3.2.0 describe `publication.content(startLocator)` como API experimental: devuelve elementos iterables con `Content.Element.locator`; los párrafos son `Content.TextElement` con segmentos y `text`. La siguiente integración debe procesar un recurso o ventana acotada, identificar cada bloque por `bookId + locator.href + locator.locations.fragments/position` y un hash del texto, y cachear traducciones por esa clave; nunca debe extraer todo el libro a memoria ni cambiar `readingOrder` o los locators originales.

La prueba offline real requiere: descargar ambos modelos desde el panel con Wi-Fi, esperar `Ready`, desactivar Wi-Fi/datos, introducir un segundo párrafo no usado antes y traducirlo. Esta interacción no está automatizada en esta entrega.

`minSdk` es 26. El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Prueba manual del lector con EPUB real

Con un dispositivo conectado, instala el APK, importa un EPUB sin DRM de procedencia legítima, pulsa su título y verifica XHTML/imágenes, anterior/siguiente, índice, barra de progreso, ocultación de controles, cierre y reapertura. Para probar rotación usa `adb shell settings put system accelerometer_rotation 1` y gira el dispositivo. Para simular muerte de proceso ejecuta `adb shell am force-stop com.example.hepub`, vuelve a abrir la aplicación y pulsa el libro. Estas pruebas requieren intervención manual y no se declaran automatizadas.
