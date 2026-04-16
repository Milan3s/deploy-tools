# 🚀 Deploy Tools - Guía Completa de Build y Ejecución

![Java](https://img.shields.io/badge/Java-24-orange?logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-24-blue)
![Maven](https://img.shields.io/badge/Maven-Build-red?logo=apachemaven)
![NetBeans](https://img.shields.io/badge/IDE-NetBeans-green)
![Git](https://img.shields.io/badge/Git-VersionControl-black?logo=git)

------------------------------------------------------------------------

## 📦 Descarga del proyecto compilado

👉 Proyecto + JARs listos:

https://drive.google.com/drive/folders/1337WeN_O6zhZiIpocrbkbaEX32mn4xW5?usp=drive_link

------------------------------------------------------------------------

## ⚙️ Requisitos

-   Java JDK 24
-   JavaFX 24.0.2
-   NetBeans
-   Maven

------------------------------------------------------------------------

## 📁 Estructura correcta (IMPORTANTE)

    C:\Program Files\Java\
    ├── jdk-24
    │   └── bin\javaw.exe
    │
    ├── javafx-sdk-24.0.2
    │   └── lib\
    │       ├── javafx.base.jar
    │       ├── javafx.controls.jar
    │       ├── javafx.fxml.jar
    │       └── ...

------------------------------------------------------------------------

## 🧵 Importar en NetBeans

1.  File → Open Project\
2.  Seleccionar el proyecto\
3.  Click derecho → Properties\
4.  Java Platform → seleccionar JDK 24

------------------------------------------------------------------------

## 🏗️ Compilar el proyecto

Este proyecto necesita:

    maven-assembly-plugin

### 🔨 Compilar

``` bash
mvn clean package
```

------------------------------------------------------------------------

## 📦 Resultado

    target/deploy-tools.jar

------------------------------------------------------------------------

## ▶️ Ejecutar aplicación

------------------------------------------------------------------------

### 🪟 Windows (.bat)

#### 1️⃣ Crear archivo

Crear un archivo llamado:

    run.bat

#### 2️⃣ Contenido

``` bat
@echo off
title Deploy Tools - Iniciando

echo ===============================
echo   Iniciando Deploy Tools...
echo ===============================
echo.

REM ---- Lanzar la aplicación ----
start "" "C:\Program Files\Java\jdk-24\bin\javaw.exe" ^
  --module-path "C:\Program Files\Java\javafx-sdk-24.0.2\lib" ^
  --add-modules javafx.controls,javafx.fxml ^
  -jar "deploy-tools.jar"

exit
```

#### 3️⃣ Cómo ejecutarlo

-   Doble click sobre `run.bat`
-   O desde CMD:

``` bash
run.bat
```

------------------------------------------------------------------------

### 🐧 Linux / 🍏 macOS (.sh)

``` bash
#!/bin/bash

JAVA_HOME=/usr/lib/jvm/jdk-24
JAVAFX_PATH=/opt/javafx-sdk-24.0.2/lib

$JAVA_HOME/bin/java \
  --module-path "$JAVAFX_PATH" \
  --add-modules javafx.controls,javafx.fxml \
  -jar deploy-tools.jar
```

``` bash
chmod +x run.sh
./run.sh
```

------------------------------------------------------------------------

## 📌 Cómo instalar JavaFX correctamente (PASO A PASO)

### 1️⃣ Descargar JavaFX

Descarga JavaFX desde la web oficial o desde el Google Drive
proporcionado.

------------------------------------------------------------------------

### 2️⃣ Extraer el archivo

El archivo descargado será un `.zip`, por ejemplo:

    openjfx-24.0.2_windows-x64_bin.zip

👉 Haz clic derecho → **Extraer aquí** o **Extract All**

Esto generará una carpeta llamada:

    javafx-sdk-24.0.2

------------------------------------------------------------------------

### 3️⃣ Mover la carpeta a Java

Copia esa carpeta completa a:

    C:\Program Files\Java\

Debe quedar así:

    C:\Program Files\Java\javafx-sdk-24.0.2

------------------------------------------------------------------------

### 4️⃣ Verificar la instalación (MUY IMPORTANTE)

Entra dentro de la carpeta:

    C:\Program Files\Java\javafx-sdk-24.0.2\lib

✔ Dentro deben existir archivos como:

-   javafx.base.jar\
-   javafx.controls.jar\
-   javafx.fxml.jar

Si no ves estos archivos → JavaFX está mal instalado

------------------------------------------------------------------------

### 5️⃣ Resumen rápido

✔ Carpeta correcta:

    C:\Program Files\Java\javafx-sdk-24.0.2\lib

✔ Dentro hay `.jar` → OK\
❌ Si no hay `.jar` → error de instalación

------------------------------------------------------------------------

## 🔐 Seguridad

No subir:

    git_auth.json
    git.config.json
    .env

------------------------------------------------------------------------

## 🏷️ Crear release

``` bash
git tag v1.0.0
git push origin v1.0.0
```

------------------------------------------------------------------------

## 👨‍💻 Autor

Milan3s
