# 🧠 Agente de IA de BarrioGo: Registro de Movimientos por Voz

Documentación técnica, teórica y experimental del componente de **Inteligencia Artificial** de BarrioGo para la sustentación de la asignatura de **Inteligencia Artificial**.

---

## 👥 Integrantes del Grupo de Inteligencia Artificial

- **Juan Pablo Medina Quintero**
- **Mateo Abril García**
- **Laura Stefania**
- **Catalina García**

---

## 🔗 Enlaces Públicos a Repositorios

- **Repositorio de Inteligencia Artificial (Este repositorio):** [https://github.com/juanpablo922/BarrioGo-IA.git](https://github.com/juanpablo922/BarrioGo-IA.git)
- **Repositorio de la Aplicación Móvil Integrada:** [https://github.com/juanpablo922/-BarrioGo-App/tree/feature/autenticacion-y-perfiles](https://github.com/juanpablo922/-BarrioGo-App/tree/feature/autenticacion-y-perfiles)

---

> **Resumen en tres líneas**
> 1. El tendero **habla** ("Vendí 2 gaseosas por 8 mil" / "Fiado a Mateo Ramírez 20 mil de carne"). Un modelo de voz de aprendizaje profundo preentrenado convierte el audio en texto.
> 2. Un **agente de reglas NLU** propio extrae la intención (venta, gasto o fiado) y las entidades (cantidad, producto, monto, cliente, método de pago) con algoritmos deterministas y distancia Levenshtein.
> 3. El agente **propone** y el tendero **confirma** (*Human-in-the-loop*). Solo entonces se guarda en **Firestore (Firebase)**. Si el cliente no existe, se crea automáticamente.

---

## 📁 Estructura del Repositorio

```
BarrioGo-IA/
├── README.md                              # Documentación técnica completa
├── dataset/                               # Dataset de evaluación y comparativa de modelos
│   ├── Dataset_BarrioGo_1000_Frases.xlsx  # Dataset de 1.000 frases coloquiales etiquetadas
│   └── BarrioGo_Comparativa_Voz.xlsx      # Estudio comparativo de 32 modelos de voz
├── codigo_ia/                             # 1. CÓDIGO PURO DE INTELIGENCIA ARTIFICIAL (NLU)
│   ├── VoiceRecognizer.kt                 # Sensor de percepción de voz (android.speech es-CO)
│   ├── MovementAgent.kt                   # Agente de interpretación de movimientos y 4 patrones
│   ├── SpanishNumbers.kt                  # Parser de números hablados y jerga ("14mil", "lucas")
│   └── ClientMatcher.kt                   # Coincidencia difusa con distancia Levenshtein
├── integracion_app/                       # 2. IMPLEMENTACIÓN DE LA IA EN LA APLICACIÓN
│   ├── VoiceViewModel.kt                  # Orquestador de estado y máquina de estados
│   ├── VoiceCaptureScreen.kt              # Pantalla principal de captura por voz
│   └── ConfirmDraftScreen.kt              # Pantalla "Esto entendí" (Propuesta e interacción)
└── pruebas/                               # PRUEBAS UNITARIAS DEL AGENTE
    └── AgentTest.kt                       # Pruebas JUnit que validan la precisión del Agente
```

---

## Contenido Teórico y Técnico

1. [Problema que resuelve](#1-problema-que-resuelve)
2. [Objetivos](#2-objetivos)
3. [Qué tipo de IA es](#3-qué-tipo-de-ia-es)
4. [Modelo REAS (PEAS)](#4-modelo-reas-peas)
5. [Propiedades del entorno](#5-propiedades-del-entorno)
6. [Arquitectura del Sistema](#6-arquitectura-del-sistema)
7. [Tecnologías, librerías e importaciones](#7-tecnologías-librerías-e-importaciones)
8. [Algoritmos internos del agente](#8-algoritmos-internos-del-agente)
9. [Por qué un modelo ya entrenado y no uno desde cero](#9-por-qué-un-modelo-ya-entrenado-y-no-uno-desde-cero)
10. [Estudio Comparativo con otros 32 algoritmos](#10-estudio-comparativo-con-otros-32-algoritmos)
11. [Por qué no es una IA de árboles ni de búsqueda A*](#11-por-qué-no-es-una-ia-de-árboles-ni-de-búsqueda-a)
12. [Persistencia: CRUD en Firestore](#12-persistencia-crud-en-firestore)
13. [Ejemplo completo paso a paso](#13-ejemplo-completo-paso-a-paso)
14. [Evaluación y Dataset de 1.000 Frases](#14-evaluación-y-dataset-de-1000-frases)
15. [Limitaciones y mejoras identificadas](#15-limitaciones-y-mejoras-identificadas)

---

## 1. Problema que resuelve

En una tienda de barrio, el tendero atiende, cobra y despacha al mismo tiempo. Anotar cada venta, gasto o fiado en un cuaderno o en formularios quita tiempo. El resultado es que **muchos movimientos no se registran**, y al cierre la caja no cuadra.

**Solución:** un agente que escucha una frase corta en lenguaje natural, la convierte en un registro estructurado y lo muestra para confirmarlo con un toque.

| Sin la IA | Con la IA |
|---|---|
| Abrir formulario, elegir tipo, escribir producto, escribir monto, guardar | Tocar el micrófono, hablar y confirmar |
| Se olvidan movimientos en horas de alta demanda | Registro en 3 a 5 segundos |

---

## 2. Objetivos

### Objetivo general
Desarrollar un agente inteligente para la app móvil BarrioGo que registre ventas, gastos y fiados a partir de la voz del tendero, usando reconocimiento de voz preentrenado y reglas NLU adaptadas al español de Colombia (`es-CO`).

### Objetivos específicos
1. **Percibir:** integrar el reconocedor de voz de Android (`SpeechRecognizer`, idioma `es-CO`) para convertir el audio en texto.
2. **Interpretar:** diseñar reglas que identifiquen la **intención** (venta, gasto o fiado) y extraigan las **entidades** (cantidad, producto, monto, cliente y método de pago).
3. **Tolerar errores de transcripción:** reconocer clientes compuestos (*"Mateo Ramírez"*) mediante la distancia de edición de Levenshtein.
4. **Validar y pedir confirmación:** detectar datos faltantes y presentar una propuesta editable antes de guardar.
5. **Actuar:** registrar el movimiento confirmado en Firestore (`tiendas/{tienda}/movimientos`) y crear el cliente en tiempo real si no existe.

---

## 3. Qué tipo de IA es

| Pregunta | Respuesta |
|---|---|
| ¿IA fuerte o débil? | **IA débil (estrecha).** Resuelve una sola tarea de registro contable. |
| ¿Qué área? | **Procesamiento de Lenguaje Natural (PLN/NLU).** Combina ASR y NLU. |
| ¿Qué técnica? | **Sistema híbrido:** ASR de aprendizaje profundo preentrenado + **IA simbólica basada en reglas** para la interpretación. |
| ¿Qué tipo de agente? | **Agente reactivo basado en reglas** (condición → acción) con modelo interno de clientes. |
| ¿Decide solo? | **No.** Sistema con **humano en el ciclo** (*human-in-the-loop*). |

---

## 4. Modelo REAS (PEAS)

| Componente | Descripción |
|---|---|
| **R — Rendimiento** | Exactitud en intención y monto, latencia < 100 ms, cero registros sin confirmar. |
| **E — Entorno** | Tienda de barrio colombiana, jerga ("lucas", "14mil"), ruido ambiente. |
| **A — Actuadores** | Pantalla "Esto entendí", avisos de validación, creación de clientes y escrituras en Firestore. |
| **S — Sensores** | Micrófono del celular (`SpeechRecognizer`) y estado táctil. |

---

## 5. Propiedades del entorno

- **Parcialmente observable** (recibe texto del ASR).
- **Estocástico** (el audio cambia con el ruido).
- **Episódico** (cada frase es un evento independiente).
- **Estático** y **Discreto**.

---

## 6. Arquitectura del Sistema

```
[ TENDERO HABLA ] ──► [ VoiceRecognizer.kt ] ──► [ MovementAgent.kt ] ──► [ ConfirmDraftScreen ] ──► [ Firestore ]
    (Audio)              (ASR es-CO)               (NLU + Levenshtein)        (Human-in-the-loop)       (Persistencia)
```

---

## 7. Tecnologías e Importaciones

- **Kotlin** puro para los algoritmos del agente (`codigo_ia/`).
- **`android.speech.SpeechRecognizer`** para percepción de voz.
- **`java.text.Normalizer`** para procesamiento de acentos y texto.
- **Firebase Firestore** para sincronización en tiempo real.

---

## 8. Algoritmos Internos del Agente

### 8.1 Normalización y Tokenización
Separa palabras y divide números pegados a unidades (ej. `"14mil"` ➔ `"14 mil"`, `"20k"` ➔ `"20 k"`).

### 8.2 Parser de Números Hablados (`SpanishNumbers`)
Suma unidades, decenas y centenas en español e interpreta expresiones colombianas como `"cinco lucas"` ($ 5.000) o `"ocho mil quinientos"` ($ 8.500).

### 8.3 Algoritmo de Distancia Levenshtein (`ClientMatcher`)
Calcula la similitud entre 0 y 1 para asociar nombres completos o compuestos (*"Mateo Ramírez"*) tolerando errores de dicción:
$$\text{similitud} = 1 - \frac{\text{distancia\_levenshtein}(a, b)}{\max(|a|, |b|)}$$

---

## 9. Estudio Comparativo con otros 32 algoritmos

Se evaluaron 32 candidatos (Vosk, Whisper Tiny/Base, Google Speech API, etc.) en el archivo `dataset/BarrioGo_Comparativa_Voz.xlsx`.

| Criterio | Solución Elegida (Android + Reglas NLU) | Modelos Cloud (Google Cloud / OpenAI) |
|---|---|---|
| **Latencia** | **< 100 ms** (Instantáneo) | 1.000 ms - 3.000 ms |
| **Costo** | **$0 USD** | Cobro por minuto o tokens |
| **Privacidad** | **100% On-Device** | Transmisión a servidores externos |
| **Sin Internet** | **Funciona sin conexión** | Requiere internet continuo |

---

## 10. Evaluación y Dataset de 1.000 Frases

En la carpeta `dataset/Dataset_BarrioGo_1000_Frases.xlsx` se incluye la evaluación de **1.000 frases etiquetadas** con intenciones (*Venta, Gasto, Fiado*), entidades extraídas y acciones esperadas del agente.

Además, en `pruebas/AgentTest.kt` se incluyen **92 pruebas unitarias en JUnit** ejecutables sin celular para validar la exactitud del algoritmo.

---

## 11. Instrucciones para Ejecutar las Pruebas de IA

1. Abre el proyecto en Android Studio.
2. Navega a `pruebas/AgentTest.kt`.
3. Haz clic derecho sobre el archivo y selecciona **Run 'AgentTest'**.
4. Se ejecutarán las **92 pruebas unitarias** mostrando el resultado de precisión del agente.
