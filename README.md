# 🛒 BarrioGo — Asistente Inteligente para la Gestión de Micronegocios

Documentación técnica, teórica y experimental del componente de **Inteligencia Artificial** de BarrioGo para la sustentación final de la asignatura de **Inteligencia Artificial**.

> **Repositorio de la Aplicación Completa:** [https://github.com/juanpablo922/-BarrioGo-App/tree/feature/autenticacion-y-perfiles](https://github.com/juanpablo922/-BarrioGo-App/tree/feature/autenticacion-y-perfiles)  
> **Repositorio de la Entrega de IA:** [https://github.com/juanpablo922/-BarrioGo-App/tree/entrega-final-ia](https://github.com/juanpablo922/-BarrioGo-App/tree/entrega-final-ia)

---

## 👥 Integrantes del Grupo de Inteligencia Artificial

- **Catalina Hernández**
- **Juan Pablo Medina**
- **Laura Devia**
- **Mateo Abril**

---

## 📌 1. ¿Qué hace BarrioGo y para qué funciona?

**BarrioGo** es una solución móvil diseñada para la gestión financiera y operativa diaria de tiendas de barrio en Colombia.

### El Problema que Resuelve:
En una tienda de barrio, el tendero atiende clientes, despacha productos y cobra dinero al mismo tiempo. Anotar cada venta, gasto o fiado a mano en un cuaderno o llenar formularios en una pantalla le resta tiempo operativo. El resultado es que **muchos movimientos no se registran**, generando descuadres de caja al final del día.

### La Solución:
BarrioGo permite registrar movimientos contables **en 3 a 5 segundos sin escribir**, utilizando lenguaje natural hablado en español de Colombia (`es-CO`). El tendero habla libremente (ej. *"Vendí 2 gaseosas por 8 mil"*, *"Fiado para Mateo Ramírez 20 mil de carne"*, *"Pagué 150 mil a Coca-Cola"*), la IA interpreta la intención y extrae las entidades, y con un solo toque el movimiento queda registrado.

---

## 🔗 2. ¿Cómo está integrada la Inteligencia Artificial en BarrioGo?

La IA se integra dentro de la arquitectura de la aplicación Android a través de un flujo directo de 5 etapas:

```
[ TENDERO HABLA ] ──► [ VoiceRecognizer.kt ] ──► [ MovementAgent.kt ] ──► [ ConfirmDraftScreen ] ──► [ Firebase Firestore ]
    (Audio)              (ASR 16 kHz es-CO)         (Agente Reactivo)          (Agente Utilidad)             (Persistencia)
```

1. **Percepción ASR (`VoiceRecognizer.kt`):** Captura el audio del micrófono a **16 kHz** y genera la transcripción textual.
2. **Procesamiento NLU (`MovementAgent.kt`):** Analiza el texto, clasifica la intención (**Venta, Gasto o Fiado**) y extrae las entidades (**monto, producto, cantidad, cliente**). Usa `ClientMatcher.kt` (distancia Levenshtein) para asociar el cliente o crear uno nuevo.
3. **Agente Reactivo:** Mantiene el estado interno de la operación y gestiona los borradores de movimientos (`MovementDraft`).
4. **Agente Basado en Utilidad:** Evalúa las reglas financieras (verifica si hay caja diaria abierta, límites de crédito del cliente y validación de valores mayor a $0).
5. **Estrategia *Human-in-the-Loop*:** Presenta la pantalla *"Esto entendí"* para que el tendero revise y apruebe con un toque antes de enviar la transacción a **Firebase Firestore**.

---

## 💡 3. ¿Por qué escogimos este Modelo de IA? (Justificación Técnica)

Para la percepción de voz se seleccionó **Google SpeechRecognizer (`es-CO`) + Agente NLU en Kotlin puro** tras comparar 32 candidatos (incluyendo modelos pesados como Whisper, Vosk y NVIDIA Canary).

### Razones Clave de la Selección:
1. **Operación On-Device / Local:** Corre directamente en el celular del tendero sin requerir servidores externos ni GPUs dedicadas.
2. **Latencia Instantánea (< 100 ms):** Permite registrar movimientos en 3 segundos, superando a APIs en la nube que tardan entre 1 y 3 segundos.
3. **Costo de Operación $0 USD:** No requiere pagar suscripciones ni tokens por cada consulta dictada.
4. **Cero Alucinación Contable (0.8%):** Esencial para no inventar montos de dinero ni alterar los saldos de la caja.
5. **Máxima Exactitud Acústica y NLU:** Alcanzó un **99.9% de exactitud acústica** y un **99.2% de exactitud NLU** en el contexto de tiendas colombianas.

---

## 📊 4. Evaluación Supervisada y Metodología Ground Truth

La evaluación se realizó sobre **132 audios reales** grabados en tiendas de barrio y un dataset de **1.000 frases etiquetadas** en lenguaje coloquial colombiano (`dataset/BarrioGO_Comparación_Modelos.xlsx` y `dataset/Dataset_BarrioGo_1000_Frases.xlsx`).

### Tabla Comparativa de Resultados Oficiales (Diapositiva 4):

| Modelo ASR Evaluado | Exactitud Acústica | WER (Word Error Rate) | Alucinación | Exactitud NLU |
|---|---|---|---|---|
| **Google SpeechRecognizer** *(Seleccionado)* | **99.9%** | **0.1%** | **0.8%** | **99.2%** |
| NVIDIA Canary 1B ASR | 49.2% | 50.8% | 10.6% | 98.5% |
| Whisper | 77.1% | 22.9% | 6.9% | 93.9% |
| Vosk ASR | 36.7% | 63.3% | 34.8% | 65.2% |

---

## 🔊 5. Procesamiento Digital del Sonido

- **Frecuencia de Muestreo:** **16 kHz (16.000 muestras por segundo)**, óptimo para la voz humana.
- **Representación Digital:** Conversión de la señal sonora continua en muestras de amplitud discretas.
- **Análisis Acústico:** Agrupación de muestras en ventanas para extracción de características frecuenciales de la voz.

---

## 📁 6. Mapa de Archivos del Repositorio

```
BarrioGo-IA/
├── README.md                                  # Documentación técnica completa
├── dataset/                                   # Datasets, Presentación y Pruebas
│   ├── BarrioGo-Entrega_Final.pptx            # Presentación oficial de la sustentación
│   ├── Dataset_BarrioGo_1000_Frases.xlsx      # Dataset con 1.000 frases etiquetadas
│   ├── BarrioGO_Comparación_Modelos.xlsx      # Evaluación Ground Truth de los 132 audios
│   └── Audios/                                # Carpeta con los 132 audios reales (.ogg)
├── codigo_ia/                                 # 1. CÓDIGO PURO DE INTELIGENCIA ARTIFICIAL (VOZ Y NLU)
│   ├── VoiceRecognizer.kt                     # Sensor ASR (Audio 16 kHz, es-CO)
│   ├── MovementAgent.kt                       # Agente Reactivo NLU e interpretación
│   ├── SpanishNumbers.kt                      # Parser numérico y jerga colombiana ("14mil", "lucas")
│   └── ClientMatcher.kt                       # Agente con distancia Levenshtein
├── integracion_app/                           # 2. IMPLEMENTACIÓN EN LA APP ANDROID
│   ├── VoiceViewModel.kt                      # Agente de Utilidad (Validación de reglas y saldos)
│   ├── VoiceCaptureScreen.kt                  # Pantalla de captura por voz
│   └── ConfirmDraftScreen.kt                  # Pantalla "Esto entendí" (Human-in-the-loop)
└── pruebas/                                   # PRUEBAS AUTOMÁTICAS
    └── AgentTest.kt                           # 92 Pruebas unitarias en JUnit
```

---

## 🧪 7. Cómo Ejecutar las Pruebas de IA

1. Abre la suite de pruebas en `pruebas/AgentTest.kt` en Android Studio.
2. Haz clic derecho sobre el archivo y selecciona **Run 'AgentTest'**.
3. Se ejecutarán las **92 pruebas unitarias** mostrando la precisión del agente.
