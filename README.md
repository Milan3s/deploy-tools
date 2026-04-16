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

✔ La carpeta **lib** es obligatoria\
✔ Dentro deben estar TODOS los `.jar` de JavaFX

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

✔ No se abre consola (usa `javaw`)\
✔ Lanza directamente la aplicación

------------------------------------------------------------------------

### 🐧 Linux / 🍏 macOS (.sh)

``` bash
#!/bin/bash

echo "==============================="
echo "  Iniciando Deploy Tools..."
echo "==============================="
echo ""

JAVA_HOME=/usr/lib/jvm/jdk-24
JAVAFX_PATH=/opt/javafx-sdk-24.0.2/lib

$JAVA_HOME/bin/java \
  --module-path "$JAVAFX_PATH" \
  --add-modules javafx.controls,javafx.fxml \
  -jar deploy-tools.jar
```

### Permisos

``` bash
chmod +x run.sh
```

### Ejecutar

``` bash
./run.sh
```

------------------------------------------------------------------------

## 📌 Cómo colocar correctamente JavaFX

1.  Descargar JavaFX\
2.  Extraer carpeta:

```{=html}
<!-- -->
```
    javafx-sdk-24.0.2

3.  Copiar en:

```{=html}
<!-- -->
```
    C:\Program Files\Java\

4.  Verificar:

```{=html}
<!-- -->
```
    C:\Program Files\Java\javafx-sdk-24.0.2\lib

✔ Dentro deben estar los `.jar`

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
