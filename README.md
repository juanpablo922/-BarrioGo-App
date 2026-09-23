# BarrioGo — proyecto Android (Kotlin + Jetpack Compose)

App de administración para tiendas de barrio, para las materias de **Aplicaciones Móviles** e
**Inteligencia Artificial**. El diseño sigue el prototipo de Figma; el agente de IA es el mismo que
se describe en el documento de Avance de Proyecto II.

## Qué IA usa la app (y por qué)

**Nada de modelos de lenguaje grandes.** El enfoque, igual que en el documento entregado, es:

| Parte | Herramienta | Archivo |
|---|---|---|
| Voz → texto | `android.speech.SpeechRecognizer` (Android, en español de Colombia) | `ai/VoiceRecognizer.kt` |
| Interpretar la frase | Reglas: números en español, palabras clave, coincidencia aproximada de nombres | `ai/MovementAgent.kt`, `ai/SpanishNumbers.kt`, `ai/ClientMatcher.kt` |
| Foto de factura → texto | ML Kit Text Recognition v2, escritura latina, en el dispositivo | `ai/InvoiceScanner.kt` |
| Interpretar la factura | Reglas: proveedor, fecha, líneas "concepto + valor", validación de que la suma cuadre | `ai/InvoiceAgent.kt` |

Estas clases **no dependen de Android** (salvo `VoiceRecognizer.kt` e `InvoiceScanner.kt`, que son el
sensor): se pueden probar con JUnit normal. Ya existe una prueba en
`app/src/test/java/com/barriogo/app/ai/AgentTest.kt` — córranla en Android Studio
(clic derecho sobre el archivo → *Run*) para ver el agente funcionando sin necesidad de un celular.

El agente **nunca guarda nada solo**: arma una propuesta (`MovementDraft` / `InvoiceDraft`) y la
pantalla "Esto entendí" / "Revisar factura" es la que pide confirmación humana antes de escribir en
la base de datos. Esa regla es la que sostiene el apartado de rendimiento del PEAS en el documento.

## Arquitectura

- **UI:** Jetpack Compose + Material 3, sin Hilt (un `AppContainer` simple en `BarrioGoApp.kt`
  reparte la base de datos, el repositorio y las preferencias).
- **Datos:** Room (`data/Entities.kt`, `data/Daos.kt`, `data/AppDatabase.kt`), todo local en el
  celular. `data/Repository.kt` es la única puerta de entrada a los datos desde la UI.
- **Sesión:** `data/SessionStore.kt` (SharedPreferences) guarda los datos de la tienda, la
  contraseña con sal+hash (`data/Security.kt`) y los intentos fallidos de PIN por perfil.
- **Navegación:** `ui/nav/Routes.kt` + `ui/nav/NavGraph.kt`, con una barra inferior de 4 pestañas
  (Inicio, Movimientos, Registrar, Caja) igual que el prototipo.
- Cada módulo del negocio (`ui/home`, `ui/cash`, `ui/movements`, `ui/expenses`, `ui/fiados`,
  `ui/voice`, `ui/scan`, `ui/auth`) tiene su propio ViewModel liviano, creado con
  `ui/ViewModelUtils.kt` (`barrioViewModel { ... }`) en vez de Hilt.

## Cómo abrirlo

1. Abre la carpeta `BarrioGo/` completa en Android Studio (Iguana o más reciente) como proyecto
   existente.
2. Deja que Gradle sincronice (necesita internet la primera vez, para bajar las dependencias:
   Compose, Room, CameraX, ML Kit). El wrapper (`gradle-wrapper.properties`) ya apunta a
   Gradle 8.9; si Android Studio pide regenerar el `.jar` del wrapper, acepta — es automático.
3. Corre la app en un emulador o celular con **Android 8.0 (API 26) o superior**. Para probar la voz
   y el escáner usa un celular real: los emuladores no siempre tienen micrófono/cámara funcionales.
4. La primera vez, la app pedirá registrar la tienda (nombre, categoría, contraseña) y crear un PIN:
   con eso queda creado el perfil de Administrador.

## Qué está simplificado a propósito

Para que el proyecto compile y se pueda revisar completo, estas partes del prototipo quedaron
resueltas de forma más simple que en el Figma — decisiones para conversar con el docente si
preguntan por ellas en la sustentación:

- **Recuperar contraseña por código (OTP):** no está implementado; hay que pensar si se simula o
  se deja como trabajo futuro (el documento de IA ya declara que el backend no está en el alcance).
- **Tema claro:** el prototipo lo define para unas pocas pantallas; la app solo trae el tema oscuro,
  que es el que domina el diseño.
- **Editar/eliminar perfiles:** se puede crear un perfil nuevo, pero no editarlo ni borrarlo desde la
  interfaz todavía.
- **Huella:** el botón aparece en la pantalla de PIN si el perfil la tiene activada, pero falta
  conectarlo a `BiometricPrompt` (la dependencia ya está en `build.gradle.kts`).
- **Cliente nuevo por voz ("Martina" no existe):** en vez de bloquear el registro, la app lo crea
  automáticamente al confirmar. Es una decisión de diseño razonable, pero distinta a "elegir
  cliente" que se menciona como ejemplo en el documento — vale la pena decidir en equipo cuál
  prefieren y ajustarlo.
- **Anular movimientos:** mantener presionado un movimiento en el Libro para anularlo (no hay un
  botón visible todavía).

## Dónde está cada pantalla del Figma

| Prototipo | Archivo |
|---|---|
| Splash | `ui/auth/SplashScreen.kt` |
| L2 Ingreso, R1-R3 Registro, R5 Crear PIN, L3 Selector de perfil, L4/L6 PIN | `ui/auth/` |
| H1 Inicio | `ui/home/HomeScreen.kt` |
| K1 Apertura, C1 Caja (+ arqueo/cierre) | `ui/cash/` |
| M1 Registrar movimiento, M2 Libro | `ui/movements/` |
| G1/G2 Gastos | `ui/expenses/` |
| F1 Fiados, nuevo fiado, abono | `ui/fiados/` |
| V1 Registrar por voz, V2/V3 Confirmar, V4 Editar por voz | `ui/voice/` |
| C1/C2 Escanear, C3 Revisar factura | `ui/scan/` |

## Si algo no compila

No pude compilar este proyecto en el entorno donde lo escribí (no tiene Android SDK). Lo revisé
línea por línea contra las clases de datos y el repositorio para que los nombres coincidan, pero si
Android Studio marca algún error, es casi seguro un detalle menor (un import que falta, un nombre
mal escrito) — cópienmelo tal cual lo muestra Android Studio y lo corrijo.
