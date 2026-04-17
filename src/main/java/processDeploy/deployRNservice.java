package processDeploy;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class deployRNservice {

    public enum RNType {
        CLI,
        EXPO,
        UNKNOWN
    }

    private static final Map<String, Boolean> RUNNING = new ConcurrentHashMap<>();

    // =========================
    // DETECTAR TIPO
    // =========================
    public static RNType detectType(String path, Consumer<String> log) {

        try {

            File pkgFile = new File(path, "package.json");

            if (!pkgFile.exists()) {
                log.accept("No se encontró package.json en el proyecto");
                return RNType.UNKNOWN;
            }

            String content = Files.readString(pkgFile.toPath());
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            JsonObject deps = getObject(json, "dependencies");
            JsonObject devDeps = getObject(json, "devDependencies");

            boolean hasExpo = hasPackage(deps, "expo") || hasPackage(devDeps, "expo");
            boolean hasRN = hasPackage(deps, "react-native") || hasPackage(devDeps, "react-native");

            if (hasExpo) {
                log.accept("Proyecto detectado como EXPO");
                return RNType.EXPO;
            }

            if (hasRN) {
                log.accept("Proyecto detectado como React Native CLI");
                return RNType.CLI;
            }

        } catch (Exception e) {
            log.accept("Error detectando tipo: " + safe(e));
        }

        return RNType.UNKNOWN;
    }

    private static JsonObject getObject(JsonObject json, String key) {
        try {
            return json.has(key) && json.get(key).isJsonObject()
                    ? json.getAsJsonObject(key)
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasPackage(JsonObject obj, String name) {
        return obj != null && obj.has(name);
    }

    // =========================
    // METRO
    // =========================
    public static void startMetro(String key, String path, Consumer<String> log) {

        File dir = validateDir(path, log);
        if (dir == null) return;

        RNType type = detectType(path, log);

        // 🔥 EXPO NO USA METRO MANUAL
        if (type == RNType.EXPO) {
            log.accept("Expo gestiona Metro automáticamente");
            return;
        }

        if (isRunning(key)) {
            log.accept("Metro ya está en ejecución");
            return;
        }

        String command = "npx react-native start";

        if (runExternalTerminal(dir, command, log)) {
            RUNNING.put(key, true);
            log.accept("Metro iniciado");
        }
    }

    public static void stopMetro(String key, Consumer<String> log) {
        killWindowsProcesses(new String[]{"node.exe"}, log);
        RUNNING.put(key, false);
        log.accept("Metro detenido");
    }

    // =========================
    // ANDROID
    // =========================
    public static void startAndroid(String key, String path, Consumer<String> log) {

        File dir = validateDir(path, log);
        if (dir == null) return;

        RNType type = detectType(path, log);

        if (isRunning(key)) {
            log.accept("Android ya está en ejecución");
            return;
        }

        String command;

        switch (type) {
            case EXPO -> {
                // 🔥 SOLO ESTE (incluye metro)
                command = "npx expo start --android";
            }
            case CLI -> {
                command = "npx react-native run-android";
            }
            default -> {
                log.accept("Tipo de proyecto desconocido");
                return;
            }
        }

        if (runExternalTerminal(dir, command, log)) {
            RUNNING.put(key, true);
            log.accept("Android ejecutándose");
        }
    }

    public static void stopAndroid(String key, Consumer<String> log) {
        killWindowsProcesses(new String[]{"node.exe", "adb.exe"}, log);
        RUNNING.put(key, false);
        log.accept("Android detenido");
    }

    // =========================
    // BUILD APK
    // =========================
    public static void buildAPK(String path, Consumer<String> log) {

        File root = validateDir(path, log);
        if (root == null) return;

        File androidDir = new File(root, "android");

        if (!androidDir.exists()) {
            log.accept("No existe carpeta android (Expo no soporta build aquí)");
            return;
        }

        log.accept("Ejecutando build APK...");

        if (runGradle(androidDir, log)) {
            log.accept("Build lanzado");
        }
    }

    public static void stopBuild(Consumer<String> log) {
        killWindowsProcesses(new String[]{"gradle.exe", "java.exe"}, log);
        log.accept("Build detenido");
    }

    // =========================
    // KILL
    // =========================
    private static void killWindowsProcesses(String[] names, Consumer<String> log) {

        try {

            String os = System.getProperty("os.name").toLowerCase();

            for (String name : names) {

                if (os.contains("win")) {
                    new ProcessBuilder("cmd.exe", "/c", "taskkill /F /IM " + name)
                            .start().waitFor();
                } else {
                    new ProcessBuilder("bash", "-c", "pkill -f " + name)
                            .start().waitFor();
                }

                log.accept("Proceso eliminado: " + name);
            }

        } catch (Exception e) {
            log.accept("Error al matar procesos: " + safe(e));
        }
    }

    // =========================
    // VALIDACIÓN
    // =========================
    private static File validateDir(String path, Consumer<String> log) {

        if (path == null || path.isBlank()) {
            log.accept("Ruta vacía");
            return null;
        }

        File dir = new File(path);

        if (!dir.exists()) {
            log.accept("La ruta no existe");
            return null;
        }

        return dir;
    }

    // =========================
    // TERMINAL
    // =========================
    private static boolean runExternalTerminal(File dir, String command, Consumer<String> log) {

        try {

            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {

                new ProcessBuilder(
                        "cmd.exe",
                        "/c",
                        "start",
                        "cmd.exe",
                        "/k",
                        "cd /d \"" + dir.getAbsolutePath() + "\" && " + command
                ).start();

            } else {

                new ProcessBuilder(
                        "bash",
                        "-c",
                        "cd \"" + dir.getAbsolutePath() + "\" && " + command
                ).start();
            }

            return true;

        } catch (Exception e) {
            log.accept("Error terminal: " + safe(e));
            return false;
        }
    }

    private static boolean runGradle(File androidDir, Consumer<String> log) {

        try {

            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {

                new ProcessBuilder(
                        "cmd.exe",
                        "/c",
                        "start",
                        "cmd.exe",
                        "/k",
                        "cd /d \"" + androidDir.getAbsolutePath() + "\" && gradlew assembleRelease"
                ).start();

            } else {

                new ProcessBuilder(
                        "bash",
                        "-c",
                        "cd \"" + androidDir.getAbsolutePath() + "\" && ./gradlew assembleRelease"
                ).start();
            }

            return true;

        } catch (Exception e) {
            log.accept("Error build: " + safe(e));
            return false;
        }
    }

    private static String safe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Error desconocido";
    }

    public static boolean isRunning(String key) {
        return RUNNING.getOrDefault(key, false);
    }
}