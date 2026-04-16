# 🚀 Deploy Tools - Guía de Build y Ejecución

![Java](https://img.shields.io/badge/Java-21-orange?logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![Maven](https://img.shields.io/badge/Maven-Build-red?logo=apachemaven)
![NetBeans](https://img.shields.io/badge/IDE-NetBeans-green)
![Git](https://img.shields.io/badge/Git-VersionControl-black?logo=git)

------------------------------------------------------------------------

## 📦 Descarga del proyecto compilado

👉 Proyecto + JARs listos:

https://drive.google.com/drive/folders/1337WeN_O6zhZiIpocrbkbaEX32mn4xW5?usp=drive_link

------------------------------------------------------------------------

## ⚙️ Requisitos

-   Java JDK 21
-   JavaFX 21.0.1
-   NetBeans
-   Maven

------------------------------------------------------------------------

## 📁 Estructura recomendada

    C:\Program Files\Java\
    ├── jdk-21
    ├── javafx-sdk-21.0.1

------------------------------------------------------------------------

## 🧵 Importar en NetBeans

1.  File → Open Project\
2.  Seleccionar el proyecto\
3.  Click derecho → Properties\
4.  Java Platform → seleccionar JDK 21

------------------------------------------------------------------------

## 🏗️ Compilar el proyecto (IMPORTANTE)

Este proyecto necesita el plugin de Maven:

    maven-assembly-plugin

✔ Es necesario para generar el JAR final con dependencias

------------------------------------------------------------------------

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

``` bat
@echo off
title Deploy Tools - Iniciando

echo ===============================
echo   Iniciando Deploy Tools...
echo ===============================
echo.

start "" "C:\Program Files\Java\jdk-21\bin\javaw.exe" ^
  --module-path "C:\Program Files\Java\javafx-sdk-21.0.1\lib" ^
  --add-modules javafx.controls,javafx.fxml ^
  -jar "deploy-tools.jar"

exit
```

------------------------------------------------------------------------

### 🐧 Linux / 🍏 macOS (.sh)

``` bash
#!/bin/bash

echo "==============================="
echo "  Iniciando Deploy Tools..."
echo "==============================="
echo ""

JAVA_HOME=/usr/lib/jvm/jdk-21
JAVAFX_PATH=/opt/javafx-sdk-21.0.1/lib

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

## 📌 Notas

-   Usar siempre Java 21
-   JavaFX debe coincidir con la versión instalada
-   El JAR generado es ejecutable directamente

------------------------------------------------------------------------

## 👨‍💻 Autor

Milan3s
