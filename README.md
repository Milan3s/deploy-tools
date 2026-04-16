# 🚀 Deploy Tools - Guía Completa de Build y Ejecución

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

-   Java JDK 21 (IMPORTANTE)
-   JavaFX 21.0.1
-   Maven 3.9+

------------------------------------------------------------------------

## 📁 Estructura recomendada

    C:\Program Files\Java\
    ├── jdk-21
    ├── javafx-sdk-21.0.1

------------------------------------------------------------------------

## 🧵 Importar en NetBeans

1.  File → Open Project\
2.  Seleccionar proyecto\
3.  Click derecho → Properties\
4.  Java Platform → JDK 21

------------------------------------------------------------------------

## 🏗️ Build del proyecto (IMPORTANTE)

Este proyecto usa:

-   ✅ `maven-assembly-plugin`
-   ✅ genera **FAT JAR automáticamente**
-   ✅ nombre final: `deploy-tools.jar`

### 🔨 Comando:

``` bash
mvn clean package
```

------------------------------------------------------------------------

## 📦 Resultado

    target/deploy-tools.jar

✔ Este JAR ya incluye TODAS las dependencias:

-   JavaFX
-   Gson
-   JSON

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

### Permisos:

``` bash
chmod +x run.sh
```

### Ejecutar:

``` bash
./run.sh
```

------------------------------------------------------------------------

## 🧠 Cómo funciona el POM (IMPORTANTE)

### 🔹 Java

``` xml
<java.version>21</java.version>
```

------------------------------------------------------------------------

### 🔹 JavaFX

``` xml
<javafx.version>21.0.1</javafx.version>
```

✔ Usa classifier `win` → compilado para Windows

------------------------------------------------------------------------

### 🔹 Fat JAR automático

``` xml
<descriptorRef>jar-with-dependencies</descriptorRef>
```

✔ Incluye TODO dentro del JAR\
✔ No necesitas classpath manual

------------------------------------------------------------------------

### 🔹 Clase principal

``` xml
<mainClass>main.App</mainClass>
```

------------------------------------------------------------------------

## 🔐 Seguridad

NO subir nunca:

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

## 📌 Notas importantes

-   JavaFX debe coincidir con la versión del POM
-   El JAR ya es ejecutable (fat jar)
-   Windows usa `javaw` (sin consola)
-   Linux/Mac usa `java`

------------------------------------------------------------------------

## 👨‍💻 Autor

Milan3s
