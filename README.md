# DataBase Studio 🚀

**DataBase Studio** es una aplicación de escritorio moderna y visual construida con **Kotlin** y **Compose Multiplatform (Desktop)**, diseñada para gestionar, diseñar y visualizar bases de datos **SQLite** y **Microsoft Access (.accdb)** de forma ágil e intuitiva.

---

## ✨ Características Principales

- 📊 **Vista General (Overview):** Métricas en tiempo real, resumen de tablas, cantidad de registros y tipos de base de datos.
- 🛠️ **Diseñador de Tablas:** Interfaz visual para definir campos, tipos de datos, claves primarias, autoincremento, valores por defecto y restricciones sin escribir SQL manualmente.
- 📋 **Explorador de Datos:** Edición, filtrado, ordenación, inserción y eliminación de registros directamente en una cuadrícula interactiva.
- 💻 **Consola SQL:** Editor SQL integrado con resaltado de sintaxis, ejecución de consultas multilínea y visualización de resultados.
- 🕸️ **Diagrama Entidad-Relación (ER):** Visualización interactiva de esquemas de datos con detección de claves foráneas y relaciones entre tablas.
- 🔄 **Interoperabilidad:** Soporte nativo para SQLite y Microsoft Access (.accdb) mediante UCanAccess.
- 📖 **Manual de Usuario Integrado:** Documentación contextual accesible desde el panel lateral o presionando `F1`.
- 🌓 **Modo Oscuro y Modo Claro:** Tema adaptable y moderno.
- ⚡ **Paleta de Comandos:** Acceso rápido a cualquier acción mediante `Ctrl + K`.

---

## 🛠️ Tecnologías Utilizadas

- **Lenguaje:** Kotlin 2.x
- **UI Framework:** Compose Multiplatform (Desktop) con Material 3
- **Bases de Datos:** SQLite JDBC, UCanAccess (MS Access)
- **Concurrencia:** Kotlin Coroutines
- **Construcción e Instalación:** Gradle, Inno Setup 6

---

## 🚀 Cómo Ejecutar el Proyecto en Desarrollo

### Prerrequisitos
- JDK 21 o superior instalado.

### Ejecución
En la raíz del proyecto, ejecuta:
```bash
./gradlew run
```
O en Windows mediante el script:
```cmd
iniciar_app.bat
```

---

## 📦 Generar Instalador Nativo para Windows

1. Crear los binarios distribuibles:
```bash
./gradlew createDistributable
```
2. Compilar el instalador con Inno Setup:
```cmd
ISCC installer\setup.iss
```
El instalador generado se ubicará en la carpeta `dist_installer/`.
