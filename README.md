# 🛒 BarrioGo — Aplicación Móvil Android con Inteligencia Artificial

> **Proyecto Universitario:** Aplicaciones Móviles e Inteligencia Artificial  
> **Repositorio Oficial:** [https://github.com/juanpablo922/-BarrioGo-App.git](https://github.com/juanpablo922/-BarrioGo-App.git)  
> **Rama de la Versión Funcional:** `feature/autenticacion-y-perfiles`

---

## 👥 1. Integrantes del Grupo

- **Juan Pablo Medina Quintero**
- **Carlos David Orozco Zamudio**
- **Martin Castro Joan Sebastian**
- **Mateo Abril García**

---

## 📌 2. Descripción de la Aplicación y del Repositorio

**BarrioGo** es una solución móvil nativa para Android diseñada específicamente para la gestión financiera y operativa de tiendas de barrio. La aplicación integra control de caja diaria, registro de ventas, administración de deudas e historial de fiados, backend en tiempo real con Firebase Firestore y dos agentes inteligentes de IA ejecutados localmente (*On-Device*).

### 🧠 Módulos de Inteligencia Artificial (On-Device)
- **Reconocimiento y Procesamiento de Voz:** Permite registrar ventas, gastos y fiados en lenguaje natural hablado en español (`es-CO`). Soporta creación automática de nuevos clientes por voz, suma de múltiples valores dictados y reconocimiento de nombres compuestos.
- **Escáner OCR de Facturas de Proveedores:** Captura fotos de facturas físicas con CameraX y procesa el texto con Google ML Kit Text Recognition v2 para extraer proveedor, fecha, desglose de ítems y valor total.

### 📐 Arquitectura del Proyecto
- **Interfaz de Usuario (UI):** Jetpack Compose + Material 3 con tema oscuro.
- **Persistencia Local y Remota:** Firebase Firestore para cajas, gastos y fiados en tiempo real con caché offline, y Room para persistencia local.
- **Navegación:** Jetpack Navigation con barra flotante y vistas adaptativas.

---

## 🛠️ 3. Rama de la Última Versión Funcional e Instrucciones de Instalación

### 🌿 Rama Principal del Proyecto Funcional:
La última versión totalmente funcional del proyecto se encuentra alojada en la rama:  
👉 **`feature/autenticacion-y-perfiles`**

---

### 📲 Guía Paso a Paso para Instalar y Ejecutar la Aplicación

#### **Requisitos Previos:**
- **Android Studio** (Iguana 2023.2.1 o superior).
- **JDK 17** o superior.
- Dispositivo móvil con **Android 8.0 (API 26) o superior** (Se recomienda usar un celular físico conectado por cable USB para probar las funciones de cámara y micrófono).

---

#### **Paso 1: Clonar el Repositorio de GitHub**
Abre una terminal o consola de comandos y ejecuta:
```bash
git clone https://github.com/juanpablo922/-BarrioGo-App.git
cd -BarrioGo-App
git checkout feature/autenticacion-y-perfiles
```

---

#### **Paso 2: Abrir el Proyecto en Android Studio**
1. Abre **Android Studio**.
2. Selecciona la opción **Open** (o *File > Open*).
3. Busca y selecciona la carpeta completa del proyecto `-BarrioGo-App`.
4. Espera a que Gradle descargue las dependencias y sincronice el proyecto (*Sync Project with Gradle Files*).

---

#### **Paso 3: Preparar tu Celular Android**
1. En tu teléfono Android, ve a **Ajustes** > **Acerca del teléfono**.
2. Toca **7 veces** sobre **Número de compilación** hasta activar el *Modo Desarrollador*.
3. Ve a **Opciones de desarrollador** y activa **Depuración por USB**.
4. Conecta tu celular a la computadora mediante un cable USB de datos y acepta el permiso emergente en la pantalla de tu celular (*"Permitir siempre desde esta computadora"*).

---

#### **Paso 4: Ejecutar e Instalar la Aplicación**
1. En Android Studio, verifica en la barra superior que esté seleccionada la app y el nombre de **tu celular físico**.
2. Haz clic en el botón verde de reproducción ▶️ (**Run 'app'**) o presiona el atajo `Shift + F10`.
3. Android Studio compilará el proyecto e instalará automáticamente la aplicación **BarrioGo** en tu dispositivo.

---

## 📊 Tabla de Pantallas y Módulos

| Módulo | Componente Principal | Descripción |
|---|---|---|
| Acceso y PIN | `ui/auth/` | Registro de tienda, selección de perfiles e ingreso seguro por PIN |
| Inicio / Home | `ui/home/HomeScreen.kt` | Resumen de ingresos, egresos y estado de la caja activa |
| Caja Diaria | `ui/cash/` | Apertura, arqueo, correcciones y cierre de caja diaria |
| Libro de Movimientos | `ui/movements/` | Lista detallada de transacciones con filtros y edición |
| Fiados y Clientes | `ui/fiados/` | Gestión de clientes, saldo de deudas y registro de abonos |
| Captura por Voz (IA) | `ui/voice/` | Interpretación y registro de ventas, gastos y fiados por voz |
| Escáner OCR (IA) | `ui/scan/` | Captura y extracción de facturas impresas de proveedores |
