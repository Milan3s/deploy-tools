# Deploy Tools - Guía de ejecución multiplataforma

![Java](https://img.shields.io/badge/Java-17+-orange?logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-UI-blue)
![Maven](https://img.shields.io/badge/Maven-Build-red?logo=apachemaven)
![NetBeans](https://img.shields.io/badge/IDE-NetBeans-green)
![Git](https://img.shields.io/badge/Git-VersionControl-black?logo=git)

------------------------------------------------------------------------

## Descarga del proyecto compilado

Proyecto + JARs listos:

https://drive.google.com/drive/folders/1337WeN_O6zhZiIpocrbkbaEX32mn4xW5?usp=drive_link

------------------------------------------------------------------------

## Requisitos

-   Java JDK 17+ (recomendado JDK 24)
-   JavaFX SDK compatible (ej: 24.0.2)
-   Maven (para build)

------------------------------------------------------------------------

## Windows (.bat)

Crea un archivo `run.bat`:

``` bat
@echo off
title Deploy Tools - Iniciando

echo ===============================
echo   Iniciando Deploy Tools...
echo ===============================
echo.

start "" "C:\Program Files\Java\jdk-24\bin\javaw.exe" ^
  --module-path "C:\Program Files\Java\javafx-sdk-24.0.2\lib" ^
  --add-modules javafx.controls,javafx.fxml ^
  -jar "deploy-tools.jar"

exit
```

------------------------------------------------------------------------

## Linux / macOS (.sh)

Crea un archivo `run.sh`:

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

## Build con Maven

``` bash
mvn clean install
mvn package
```

------------------------------------------------------------------------

## JAR generado

    /target/deploy-tools.jar

------------------------------------------------------------------------

## Fat JAR (opcional)

``` xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.5.0</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals><goal>shade</goal></goals>
    </execution>
  </executions>
</plugin>
```

------------------------------------------------------------------------

## Seguridad

No subir:

    git_auth.json
    git.config.json
    .env

------------------------------------------------------------------------

## Releases

``` bash
git tag v1.0.0
git push origin v1.0.0
```

------------------------------------------------------------------------

## Autor

Milan3s
