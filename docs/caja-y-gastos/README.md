# Caja diaria y gastos

Este documento explica cómo y dónde se implementaron tres cosas:

- **La caja diaria:** apertura, corrección de la apertura, cierre, corrección del cierre y hora de cierre con avisos.
- **El CRUD de gastos:** registrar a mano, ver, editar y anular.
- **Los cambios del backend** que los soportan.

Rama de trabajo en ambos repositorios: `feature/caja-y-gastos`.

- App Android: `-BarrioGo-App`.
- Backend: `barriogo-backend`, con Cloud Functions, reglas y pruebas.

---

## 1. Reglas de negocio

| Regla | Dónde se cumple |
|---|---|
| La caja es **diaria**: una por día. La abre cualquier perfil, administrador o vendedor. | `domain/Caja.kt` → `ReglasCaja.estado()`, `idCaja()` · `data/caja/CajaRepository.abrir()` |
| **Sin la caja de hoy abierta no se registra nada**: ni ventas, ni gastos, ni fiados, ni abonos, ni voz o escáner. | `ui/cash/CajaRequerida.kt` envuelve esas rutas en `ui/nav/NavGraph.kt` (`ConCaja`) · `GastosRepository.registrar()` lo vuelve a revisar |
| Al entrar con la clave, si hoy no hay caja, la app lleva a **Abrir caja (K0)**. | `NavGraph.kt`, ruta `PIN_LOGIN` → `onSuccess` |
| La apertura debe ser mayor a $ 0 y como máximo $ 1.000.000.000. | `ReglasCaja.problemaApertura()` (igual que `montoValido()` en `firestore.rules`) |
| La apertura se corrige **máximo 3 veces**. Cada cambio pide confirmación y deja registro. | `CajaRepository.corregirApertura()` · tabla Room `cash_changes` · Firestore `cajas/{c}/cambiosApertura/{n}` |
| **Cualquier perfil puede cerrar** la caja. | `CajaRepository.cerrar()` · función `cerrarCaja` |
| El cierre se corrige **máximo 2 veces**, y solo antes de abrir la caja siguiente. | `CajaRepository.corregirCierre()` · nueva función `corregirCierre` del backend |
| Una caja de un día anterior que siga abierta hay que cerrarla antes de abrir la de hoy. | `EstadoCaja.ABIERTA_DIA_ANTERIOR` |
| **Hora de cierre configurable.** Solo la cambia un administrador; los vendedores la ven. | `CashScreen` → "Hora de cierre" · `CajaRepository.guardarHoraCierre()` · Firestore `tiendas/{t}.horaCierre` |
| **Avisos de cierre:** 30 minutos antes de la hora ("Administrador: pronto se cierra la caja") y después de la hora cada 30 minutos mientras siga abierta. | `notificaciones/AvisosCierre.kt` · franja `FranjaAviso` en Inicio y Caja |
| Gastos: el tipo es obligatorio, el monto va de 1 a 1.000 millones y "¿En qué fue?" de 1 a 200 letras. | `domain/Gastos.kt` → `ReglasGasto` (igual que `movimientoBienFormado()` en `firestore.rules`) |
| Solo se editan o anulan los gastos **de la caja abierta**. Anular no borra: el gasto queda tachado. | `GastosRepository.gastoCambiable()` · reglas `edicion()` / `anulacion()` |

## 2. Dónde está cada cosa (Android)

Ruta base: `app/src/main/java/com/barriogo/app/`

### Dominio (Kotlin puro, con pruebas JUnit)
- `domain/Caja.kt`
  - `EstadoCaja` y `ReglasCaja`: estado del día, validación de apertura y conteo, arqueo y totales (misma cuenta que `calcularTotalesCaja` del backend).
  - Hora de cierre, cálculo del aviso y del próximo aviso.
  - `MAX_CLOSING_CHANGES = 2`.
- `domain/Gastos.kt`: `ReglasGasto` (validaciones, mensaje bajo el botón, cambios al editar, filtros Hoy, Semana y Mes, id de categoría para Firestore) y `PeriodoGastos`.

### Datos
- `data/Entities.kt`, `data/Daos.kt`, `data/AppDatabase.kt`: Room pasa de la **v3 a la v4** con la migración `MIGRACION_3_4`.
  - `cash_sessions` gana `operationDate`, `closeEditsUsed` y `remoteId`.
  - `movements` gana `remoteId` y `category`.
  - Tabla nueva `cash_changes`: registro de cada cambio de apertura o cierre.
- `data/caja/CajaRepository.kt`: abrir, corregir apertura, cerrar, corregir cierre y hora de cierre. Room es lo que leen las pantallas; en una cuenta real cada paso se copia a Firestore.
- `data/caja/CajaRemota.kt`
  - Escrituras directas a Firestore, sin internet: apertura y cambios de apertura.
  - Callables: `cerrarCaja` y `corregirCierre`, que necesitan internet.
  - Escucha de `horaCierre`.
- `data/movimientos/GastosRepository.kt`: CRUD de gastos con todas las validaciones.
- `data/movimientos/MovimientosRemotos.kt`: copia de ventas y gastos en `tiendas/{t}/movimientos`, con los campos que exigen las reglas: `efectoCaja`, `categoriaGastoId`, `actualizadoPor`/`actualizadoEn` y la anulación.
- `data/SessionStore.kt`: `horaCierre` local, por defecto `"20:00"`.
- `BarrioGoApp.kt`: `AppContainer` expone `caja`, `gastos`, `movimientosRemotos`, `perfilActivo()` y `enLaNube()`, y programa los avisos al iniciar.

### Notificaciones
- `notificaciones/AvisosCierre.kt`
  - Canal `cierre_caja`, programado con `AlarmManager` (alarma inexacta).
  - `AvisoCierreReceiver` muestra el aviso y programa el siguiente. También reprograma al reiniciar el celular (`BOOT_COMPLETED`).
- `AndroidManifest.xml`: permisos `POST_NOTIFICATIONS` y `RECEIVE_BOOT_COMPLETED`, y el receiver.
- En Android 13 o superior, el permiso de notificaciones se pide al abrir la caja (`OpenCashScreen`).

### Pantallas (nodos del Figma)
| Pantalla | Archivo | Nodo Figma |
|---|---|---|
| K0 · Abrir caja del día | `ui/cash/OpenCashScreen.kt` | 643:13645 |
| K1 · Apertura (guardada, corregir, confirmar, límite) | `ui/cash/OpenCashScreen.kt` | 643:12075, 643:13387 |
| C1 · Caja (con hora de cierre y estados del día) | `ui/cash/CashScreen.kt` | 351:3185 |
| a1 · Arqueo de caja | `ui/cash/CashScreen.kt` → `ArqueoView` | 541:5011 |
| c2 · Cierre: arqueo → c3 · Resumen → c4 · Comprobante, y "Corregir cierre" | `ui/cash/CloseCashScreen.kt` | 541:5127, 541:5183, 541:5255 |
| H1 · Inicio (saldo con candado, Salió → Gastos, avisos) | `ui/home/HomeScreen.kt` | 646:14265 |
| G1 · Gastos (lista, filtros, detalle, anular, anulado, vacío) | `ui/expenses/ExpensesScreen.kt` | 568:5486 |
| G2 · Registrar gasto / Editar gasto | `ui/expenses/RegisterExpenseScreen.kt` | 568:5898, 568:5993 |
| Caja requerida (compuerta) | `ui/cash/CajaRequerida.kt` | — |

**ViewModels:**
- `ui/cash/CashViewModel.kt`: estado del día con un reloj que se actualiza cada 30 segundos.
- `ui/expenses/GastosViewModel.kt`.

**Componentes compartidos:**
- `ui/components/FigmaCaja.kt`: cabecera, botones, teclado de monto, diálogos, aviso flotante y filas de resumen.
- `ui/theme/FigmaColors.kt`: tokens del Figma en modo oscuro, con su equivalente en modo claro.

**Navegación** (`ui/nav/Routes.kt` y `NavGraph.kt`):
- `open_cash?corregir=`
- `close_cash?corregir=`
- `expenses?aviso=`
- `register_expense?id=`

En M1 · Registrar movimiento, la tarjeta "Gasto" abre G2. Así hay un solo formulario de gastos con todas las validaciones.

## 3. Validaciones, alertas y cargando

- **Validaciones en el campo.** El mensaje sale al tocar el campo o al intentar guardar, con el espacio reservado como en el Figma. Bajo el botón, una línea dice qué falta ("Elige el tipo de gasto", "Ahora escribe cuánto pagaste", "Todavía no has cambiado nada"…). Los botones se deshabilitan mientras falte algo.
- **Alertas:**
  - Errores del servidor o de las reglas: `AvisoDialogo`.
  - Confirmaciones: `DialogoFigma`, con "Revisa antes de guardar", "Este es tu último cambio", "Llegaste al límite de hoy", "¿Anular este gasto?" y "¿Salir sin guardar?".
  - Éxitos: `AvisoFlotante`, por ejemplo "Gasto guardado. Ya resta en tu caja." o "Gasto anulado. Tu caja se actualizó.".
- **Cargando:** `LoadingOverlay` en cada guardado ("Guardando la apertura…", "Cerrando la caja…", "Guardando el gasto…"). Bloquea toques y el botón atrás, y dura al menos medio segundo para que no parpadee.

## 4. Backend (`barriogo-backend`)

- `functions/src/dominio/caja.ts`: `MAXIMO_CAMBIOS_CIERRE = 2`, `horaCierreValida()` y `motivoParaNoCorregirCierre()`.
- `functions/src/casos/caja.ts`: nueva función **`corregirCierre`**.
  - Valida que la caja esté cerrada, que tenga menos de 2 correcciones y que no haya otra caja abierta.
  - Recalcula la diferencia y el resultado.
  - Escribe `cajas/{c}/cambiosCierre/{n}` y regenera el comprobante PDF.
- `functions/src/lib/esquemas.ts` (esquema zod), `lib/rutas.ts` e `index.ts` (callable exportada).
- `firestore.rules`:
  - `cajas/{c}/cambiosCierre/{n}`: solo lectura para miembros; la escribe el servidor.
  - `cambiaHoraCierre()`: la app puede cambiar solo `tiendas/{t}.horaCierre` en formato `HH:mm`.
- Pruebas: `functions/test/dominio.test.ts`, `functions/test/casos.test.ts` y `pruebas/reglas/firestore.test.ts`.
- Guía de la app actualizada: `docs/INTEGRACION_ANDROID.md` §10.

## 5. Datos en Firestore (cuenta real)

| Acción | Escritura | Sin internet |
|---|---|---|
| Abrir caja | batch `tiendas/{t}/cajas/{AAAA-MM-DD-1}` + `tiendas/{t}.cajaAbiertaId` | Sí |
| Corregir apertura | `cajas/{c}` + `cajas/{c}/cambiosApertura/{n}` | Sí |
| Cerrar caja | callable `cerrarCaja` | No: muestra "Sin conexión…" |
| Corregir cierre | callable `corregirCierre` | No |
| Gasto: crear, editar, anular | `tiendas/{t}/movimientos/{id}` | Sí |
| Hora de cierre | `tiendas/{t}.horaCierre` | Sí |

La cuenta demo (`tienda@barriogo.com`) trabaja solo en el celular: todo queda en Room.

## 6. Revisión contra el Figma

Un subagente comparó cada pantalla contra sus nodos del Figma con las herramientas MCP de Figma. Primer resultado:

- **G1:** cumple.
- **c2–c4, a1 y G2:** cumplen con ajustes menores.
- **K0/K1, H1 y C1:** no cumplían.

Después se corrigió lo siguiente:

- **K1:**
  - Diálogos "Revisa antes de guardar", "Este es tu último cambio" (naranja) y "Llegaste al límite de hoy".
  - Contador naranja cuando queda 1 cambio, y "Ya usaste los 3 cambios de hoy".
  - Textos del pie según los cambios que quedan.
  - "Listo" deshabilitado en $ 0, con "$ 0" en naranja.
  - Acciones al fondo de la pantalla.
- **C1 / H1:**
  - Tarjetas #22593F sin borde, con etiqueta dorada y monto de 34.
  - Entró/Ventas en verde y Salió/Gastos en naranja.
  - "Cerrar caja" relleno amarillo y "Arqueo de caja" con borde dorado.
  - Cabecera "Caja" amarilla, píldora "N clientes" y filas de movimientos planas con divisor.
- **c2–c4:**
  - Botones de 48 de alto.
  - Etiquetas a 6 del campo.
  - Botón final anclado al fondo.
- **G2:** "Ahora escribe cuánto pagaste" antes de intentar guardar, y la línea de estado en naranja cuando hay error.

**Diferencias que se dejaron a propósito:**
- Íconos Material en lugar de los SVG del Figma.
- No se replica la barra de estado iOS.
- El menú de vidrio no aparece en K1 ni en G2: son pantallas de formulario.
- Al guardar un gasto se vuelve a G1 con un aviso, en vez del estado "Guardado" verde de G2.
- En c3/c4 las filas dicen "Ventas en efectivo" y "Abonos recibidos", y el saldo **suma los abonos**. El Figma muestra 548.000 y la cuenta correcta es 578.000, como ya señalaba una prueba del backend.

## 7. Pruebas y cómo correrlas

| Qué | Comando | Resultado |
|---|---|---|
| App: compilación y pruebas unitarias | `gradle :app:assembleDebug :app:testDebugUnitTest` | 55 pruebas en verde (nuevas: `CajaTest` 11, `GastosTest` 7) |
| Backend: pruebas | `pnpm --filter @barriogo/functions test` | 62 en verde |
| Backend: lint y tipos | `pnpm lint --filter @barriogo/functions` | En verde |
| Reglas con emulador | `pnpm --filter @barriogo/pruebas-emulador test:reglas` | 37 en verde |

El repositorio Android no trae `gradlew`. Se usó el Gradle 8.13 que Android Studio ya descargó en `~/.gradle/wrapper/dists/`.

### Cómo se usó `/loop`
`/loop` repite una tarea en Claude Code.

- **Con intervalo** (por ejemplo `/loop 5m …`), la repite cada cierto tiempo.
- **Sin intervalo**, Claude decide cuándo volver: corre la tarea, deja un "despertador" de respaldo y vuelve cuando termina un proceso en segundo plano o cuando se cumple ese tiempo.

Aquí se usó sin intervalo para "compilar la app, correr pruebas Android y backend, corregir si algo falla y detenerse en verde". Pasó en la primera vuelta y el loop se detuvo solo. Sirve para tareas largas que hay que vigilar, como una compilación, un despliegue o un CI, sin estar revisando a mano.

## 8. Limitaciones conocidas y siguientes pasos

1. **Fiados y abonos todavía no se copian a Firestore.** Necesitan los clientes en Firestore, que es parte del módulo de fiados. Por eso, en una cuenta real, el comprobante del servidor (`cerrarCaja`) no incluye los abonos. Las pantallas sí los incluyen, porque calculan desde Room.
2. **Cajas de otros celulares.** Room no copia todavía las cajas abiertas en otro celular de la misma tienda; solo la hora de cierre se sincroniza. Si dos celulares abren el mismo día, Firestore rechaza el segundo porque el id `{fecha}-1` ya existe y queda en el log.
3. **Los avisos son locales**, en cada celular, con alarma inexacta: Android puede retrasarlos unos minutos. Un aviso por push al administrador en otro celular requiere FCM.
4. Una vez cerrada la caja de hoy, no se puede abrir otra el mismo día: solo se corrige el cierre.
5. Tocar la notificación abre la app en el inicio de siempre (selector de perfil y clave); no lleva directo a Caja.
6. **Pendiente de probar a mano en un dispositivo o emulador:**
   - La migración v3 → v4 sobre una base existente.
   - Los avisos, cambiando la hora del celular.
   - El modo claro.
