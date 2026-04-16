package processDeploy;

import java.io.*;
import java.util.Properties;
import java.util.function.Consumer;

public class BuilderAPKService {

    private static final String CONFIG_FILE = "builder.properties";

    /*
    ============================================
    CONFIG
    ============================================
     */
    public static void savePath(String path, Consumer<String> log) {
        try {
            Properties props = new Properties();
            props.setProperty("androidPath", path);

            try (FileOutputStream out = new FileOutputStream(getConfigFile())) {
                props.store(out, "Builder config");
            }

            log.accept("[CONFIG] Ruta guardada");

        } catch (Exception e) {
            log.accept("[ERROR] Guardando config: " + safe(e));
        }
    }

    public static String loadPath() {
        try {
            File file = getConfigFile();
            if (!file.exists()) return null;

            Properties props = new Properties();

            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            }

            return props.getProperty("androidPath");

        } catch (Exception e) {
            return null;
        }
    }

    private static File getConfigFile() {
        return new File(System.getProperty("user.home"), CONFIG_FILE);
    }

    /*
    ============================================
    BUILD
    ============================================
     */
    public static void build(String projectPath, Consumer<String> log) {

        File androidDir = validateAndroid(projectPath, log);
        if (androidDir == null) return;

        try {
            String cmd =
                    "cd /d \"" + androidDir.getAbsolutePath() + "\"" +
                    " && echo ==== BUILD ====" +
                    " && gradlew.bat assembleRelease";

            launchCMD(cmd);

            log.accept("[BUILD] CMD lanzado");

        } catch (Exception e) {
            log.accept("[ERROR] Build: " + safe(e));
        }
    }

    /*
    ============================================
    GRADLE CLEAN
    ============================================
     */
    public static void cleanGradle(String projectPath, Consumer<String> log) {

        File androidDir = validateAndroid(projectPath, log);
        if (androidDir == null) return;

        try {
            String cmd =
                    "cd /d \"" + androidDir.getAbsolutePath() + "\"" +
                    " && echo ==== GRADLE CLEAN ====" +
                    " && gradlew.bat clean";

            launchCMD(cmd);

            log.accept("[CLEAN] Gradle clean ejecutado");

        } catch (Exception e) {
            log.accept("[ERROR] Gradle clean: " + safe(e));
        }
    }

    /*
    ============================================
    DELETE NODE MODULES (ROBUSTO)
    ============================================
     */
    public static void deleteNodeModules(String projectPath, Consumer<String> log) {

        File root = validateRoot(projectPath, log);
        if (root == null) return;

        try {
            String cmd =
                    "cd /d \"" + root.getAbsolutePath() + "\"" +

                    // 🔥 matar procesos node
                    " && echo ==== STOP NODE PROCESSES ====" +
                    " && taskkill /f /im node.exe >nul 2>&1" +

                    // 🔥 borrar node_modules con validación
                    " && echo ==== DELETE NODE_MODULES ====" +
                    " && if exist node_modules (" +
                    " rd /s /q node_modules" +
                    " ) else ( echo node_modules no existe )" +

                    // 🔥 borrar lock
                    " && if exist package-lock.json del /f /q package-lock.json";

            launchCMD(cmd);

            log.accept("[CLEAN] Eliminando node_modules...");

        } catch (Exception e) {
            log.accept("[ERROR] delete node_modules: " + safe(e));
        }
    }

    /*
    ============================================
    NPM INSTALL
    ============================================
     */
    public static void npmInstall(String projectPath, Consumer<String> log) {

        File root = validateRoot(projectPath, log);
        if (root == null) return;

        try {
            String cmd =
                    "cd /d \"" + root.getAbsolutePath() + "\"" +
                    " && echo ==== NPM INSTALL ====" +
                    " && npm install";

            launchCMD(cmd);

            log.accept("[NPM] Instalación lanzada");

        } catch (Exception e) {
            log.accept("[ERROR] npm install: " + safe(e));
        }
    }

    /*
    ============================================
    OPEN APK
    ============================================
     */
    public static void openAPK(String projectPath, Consumer<String> log) {

        try {
            File androidDir = validateAndroid(projectPath, log);
            if (androidDir == null) return;

            File apk = new File(androidDir,
                    "app/build/outputs/apk/release/app-release.apk");

            if (!apk.exists()) {
                log.accept("[ERROR] APK no encontrado");
                return;
            }

            new ProcessBuilder("explorer.exe", "/select,", apk.getAbsolutePath()).start();

            log.accept("[APK] Archivo localizado");

        } catch (Exception e) {
            log.accept("[ERROR] Open APK: " + safe(e));
        }
    }

    /*
    ============================================
    CMD EXTERNO
    ============================================
     */
    private static void launchCMD(String command) throws IOException {

        String finalCmd = command +
                " && echo." +
                " && echo ==== FIN DEL PROCESO ====" +
                " && pause";

        new ProcessBuilder(
                "cmd.exe",
                "/c",
                "start cmd.exe /k \"" + finalCmd + "\""
        ).start();
    }

    /*
    ============================================
    VALIDACIÓN
    ============================================
     */
    private static File validateRoot(String path, Consumer<String> log) {

        if (path == null || path.isBlank()) {
            log.accept("[ERROR] Ruta vacía");
            return null;
        }

        File root = new File(path);

        if (!root.exists()) {
            log.accept("[ERROR] Ruta no existe");
            return null;
        }

        return root;
    }

    private static File validateAndroid(String path, Consumer<String> log) {

        File root = validateRoot(path, log);
        if (root == null) return null;

        File androidDir = new File(root, "android");

        if (!androidDir.exists()) {
            log.accept("[ERROR] Falta carpeta android");
            return null;
        }

        File gradlew = new File(androidDir, "gradlew.bat");

        if (!gradlew.exists()) {
            log.accept("[ERROR] gradlew no encontrado");
            return null;
        }

        return androidDir;
    }

    private static String safe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Error desconocido";
    }
}