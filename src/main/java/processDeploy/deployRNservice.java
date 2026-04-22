package processDeploy;

import java.io.File;
import java.net.Socket;
import java.nio.file.Files;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class deployRNservice {

    public enum RNType {
        CLI,
        EXPO,
        UNKNOWN
    }

    private static final int METRO_PORT = 8081;

    // =========================
    // DETECT TYPE
    // =========================
    public static RNType detectType(String path, Consumer<String> log) {

        try {
            File pkgFile = new File(path, "package.json");

            if (!pkgFile.exists()) {
                log.accept("No se encontró package.json");
                return RNType.UNKNOWN;
            }

            String content = Files.readString(pkgFile.toPath());
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            JsonObject deps = getObject(json, "dependencies");
            JsonObject devDeps = getObject(json, "devDependencies");

            boolean hasExpo = hasPackage(deps, "expo") || hasPackage(devDeps, "expo");
            boolean hasRN = hasPackage(deps, "react-native") || hasPackage(devDeps, "react-native");

            if (hasExpo) return RNType.EXPO;
            if (hasRN) return RNType.CLI;

        } catch (Exception e) {
            log.accept("Error detectando tipo: " + safe(e));
        }

        return RNType.UNKNOWN;
    }

    private static JsonObject getObject(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonObject()
                ? json.getAsJsonObject(key)
                : null;
    }

    private static boolean hasPackage(JsonObject obj, String name) {
        return obj != null && obj.has(name);
    }

    // =========================
    // DETECTAR METRO REAL
    // =========================
    private static boolean isMetroRunning() {
        try (Socket socket = new Socket("localhost", METRO_PORT)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================
    // METRO (SOLO CLI)
    // =========================
    public static void startMetro(String path, Consumer<String> log) {

        File dir = validateDir(path, log);
        if (dir == null) return;

        RNType type = detectType(path, log);

        if (type != RNType.CLI) {
            log.accept("Metro no aplica en Expo");
            return;
        }

        if (isMetroRunning()) {
            log.accept("Metro ya está activo en 8081");
            return;
        }

        log.accept("Iniciando Metro...");
        runExternalTerminal(dir, "npx react-native start", log);
    }

    public static void stopMetro(Consumer<String> log) {
        killNode(log);
        log.accept("Metro detenido");
    }

    // =========================
    // ANDROID / EXPO ENTRYPOINT
    // =========================
    public static void startAndroid(String path, Consumer<String> log) {

        File dir = validateDir(path, log);
        if (dir == null) return;

        RNType type = detectType(path, log);

        switch (type) {

            case EXPO -> {

                log.accept("Expo → arranque completo (Metro + App)");

                // 🔥 ESTE ES EL BOTÓN DE EXPO
                runExternalTerminal(
                        dir,
                        "npx expo start --android",
                        log
                );
            }

            case CLI -> {

                if (isMetroRunning()) {

                    log.accept("Metro detectado → reutilizando");

                    runExternalTerminal(
                            dir,
                            "npx react-native run-android --no-packager",
                            log
                    );

                } else {

                    log.accept("No hay Metro → Android iniciará uno");

                    runExternalTerminal(
                            dir,
                            "npx react-native run-android",
                            log
                    );
                }
            }

            default -> log.accept("Tipo desconocido");
        }
    }

    public static void stopAndroid(Consumer<String> log) {
        killNode(log);
        killADB(log);
        log.accept("Procesos detenidos");
    }

    // =========================
    // BUILD APK
    // =========================
    public static void buildAPK(String path, Consumer<String> log) {

        File root = validateDir(path, log);
        if (root == null) return;

        File androidDir = new File(root, "android");

        if (!androidDir.exists()) {
            log.accept("Expo no soporta build aquí");
            return;
        }

        log.accept("Ejecutando build...");

        runExternalTerminal(
                androidDir,
                isWindows() ? "gradlew assembleRelease" : "./gradlew assembleRelease",
                log
        );
    }

    // =========================
    // UTILS
    // =========================
    private static File validateDir(String path, Consumer<String> log) {

        if (path == null || path.isBlank()) {
            log.accept("Ruta vacía");
            return null;
        }

        File dir = new File(path);

        if (!dir.exists()) {
            log.accept("Ruta no existe");
            return null;
        }

        return dir;
    }

    private static boolean runExternalTerminal(File dir, String command, Consumer<String> log) {

        try {

            if (isWindows()) {
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

    private static void killNode(Consumer<String> log) {
        execKill("node.exe", log);
    }

    private static void killADB(Consumer<String> log) {
        execKill("adb.exe", log);
    }

    private static void execKill(String name, Consumer<String> log) {

        try {

            if (isWindows()) {
                new ProcessBuilder("cmd.exe", "/c", "taskkill /F /IM " + name)
                        .start().waitFor();
            } else {
                new ProcessBuilder("bash", "-c", "pkill -f " + name)
                        .start().waitFor();
            }

            log.accept("Proceso eliminado: " + name);

        } catch (Exception e) {
            log.accept("Error matando " + name + ": " + safe(e));
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static String safe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Error desconocido";
    }
}