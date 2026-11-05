# Módulo de Inteligencia Artificial (BarrioGo)

Este documento describe la arquitectura, diseño y componentes del **Módulo de Inteligencia Artificial** de la aplicación **BarrioGo**, orientado a simplificar el registro diario de transacciones para tenderos de barrio a través de **reconocimiento de voz** y **escaneo de facturas con OCR**.

---

## 1. Módulos Implementados

### 🎙️ Reconocimiento de Voz y Agente de Movimientos
Permite registrar ventas, gastos y fiados en lenguaje natural hablado en español de Colombia.

- **Captura de Voz (Percepción):** `VoiceRecognizer.kt`
  - Utiliza la API nativa de Android `SpeechRecognizer` (`es-CO`).
  - Convierte la onda de audio capturada en texto sin procesamiento externo en la nube.
- **Agente de Movimientos (Interpretación):** `MovementAgent.kt`
  - Analiza la frase en lenguaje natural e infiere el tipo de movimiento: **Venta**, **Gasto** o **Fiado**.
  - Lo que dice la frase manda sobre la pestaña elegida: *"Venta de..."* es venta aunque esté abierta la pestaña Fiado.
  - Suma varios valores en una frase y reconoce al cliente en ventas (*"Venta de 15 mil de leche y 25 mil de agua a Luisa Castro"* → $ 40.000). Un total dicho (*"son 40 mil"*) no se suma otra vez.
  - En una venta, el cliente es solo una referencia: no genera deuda ni crea una ficha de cliente.
  - Soporta **edición por voz** (*"Eran 3 gaseosas, no 2"*).
- **Procesamiento de Números:** `SpanishNumbers.kt`
  - Convierte texto numérico hablado (*"doce mil pesos"*, *"dos libras"*) a enteros.
- **Coincidencia Difusa de Clientes:** `ClientMatcher.kt`
  - Algoritmo de distancia de edición **Levenshtein** para asociar el nombre hablado con la lista de clientes guardados.

### 📸 Escáner de Facturas con OCR
Permite fotografiar facturas de proveedores y extraer los datos contables automáticamente.

- **Captura Visual y OCR (Percepción):** `InvoiceScanner.kt`
  - Combina **CameraX** con **Google ML Kit Text Recognition v2** ejecutado On-Device.
- **Agente de Facturas (Interpretación):** `InvoiceAgent.kt`
  - Extrae el proveedor, número de factura, fecha, detalle de productos y valor total.
  - Genera alertas si la suma de los productos difiere del valor total leído.

---

## 2. Ubicación del Código

```
app/src/main/java/com/barriogo/app/
├── ai/
│   ├── ClientMatcher.kt       # Búsqueda difusa Levenshtein de clientes
│   ├── InvoiceAgent.kt        # Agente para parsing de facturas de proveedores
│   ├── InvoiceScanner.kt      # CameraX + Google ML Kit OCR
│   ├── MovementAgent.kt       # Agente de interpretación de lenguaje natural
│   ├── SpanishNumbers.kt      # Conversor de números hablados en español
│   └── VoiceRecognizer.kt     # Wrapper de Android SpeechRecognizer
└── ui/
    ├── voice/                 # UI y ViewModel de voz
    └── scan/                  # UI y ViewModel de escáner OCR
```
