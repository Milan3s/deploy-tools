package processDeploy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class deployService {

    private static final Map<String, Process> RUNNING = new ConcurrentHashMap<>();

    // 🔥 NUEVO: puertos por servicio
    private static final Map<String, Integer> PORTS = new ConcurrentHashMap<>();

    /*
    ============================================
    START
    ============================================
     */
    public static void start(String key,
            String path,
            String framework,
            String customCommand,
            Consumer<String> log) {

        new Thread(() -> {
            try {

                File projectDir = new File(path);

                if (!projectDir.exists()) {
                    log.accept("[ERROR] Ruta no existe: " + path);
                    return;
                }

                int port = resolvePort(framework);
                PORTS.put(key, port);

                // 🔥 SI YA ESTÁ OCUPADO → NO ARRANCAR
                if (isPortInUse(port)) {
                    log.accept("[INFO] Ya hay algo corriendo en puerto " + port);
                    log.accept("[INFO] http://localhost:" + port);
                    return;
                }

                stop(key, log);

                boolean useCustom = customCommand != null && !customCommand.isBlank();

                String command = useCustom
                        ? customCommand
                        : resolveCommand(projectDir, framework, log);

                if (command == null || command.isBlank()) {
                    log.accept("[ERROR] No se pudo resolver comando");
                    return;
                }

                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
                builder.directory(projectDir);
                builder.redirectErrorStream(true);

                Process process = builder.start();
                RUNNING.put(key, process);

                log.accept("[START] " + key + " -> " + command);

                readOutput(process, framework, useCustom, log);

            } catch (Exception e) {
                log.accept("[ERROR] " + e.getMessage());
            }
        }).start();
    }

    /*
    ============================================
    BUILD
    ============================================
     */
    public static void build(String key,
            String path,
            String framework,
            Consumer<String> log) {

        new Thread(() -> {
            try {

                File dir = new File(path);

                if (!dir.exists()) {
                    log.accept("[ERROR] Ruta no existe");
                    return;
                }

                String command = resolveBuildCommand(dir, framework);

                if (command == null) {
                    log.accept("[INFO] Este proyecto no requiere build");
                    return;
                }

                log.accept("[BUILD] Ejecutando: " + command);

                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
                builder.directory(dir);
                builder.redirectErrorStream(true);

                Process process = builder.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.accept(clean(line));
                    }
                }

                int exit = process.waitFor();

                if (exit == 0) {
                    log.accept("[BUILD] Completado correctamente");
                } else {
                    log.accept("[BUILD] Error (code " + exit + ")");
                }

            } catch (Exception e) {
                log.accept("[ERROR] Build: " + e.getMessage());
            }
        }).start();
    }

    /*
    ============================================
    RESOLVE BUILD
    ============================================
     */
    private static String resolveBuildCommand(File dir, String framework) {

        framework = framework == null ? "" : framework.toLowerCase();

        if (framework.equals("laravel")) {
            return "php artisan config:cache";
        }

        File pkg = new File(dir, "package.json");
        if (!pkg.exists()) {
            return null;
        }

        if (getScript(pkg, "build") != null) {
            return "npm run build";
        }

        if (framework.equals("angular")) {
            return "ng build";
        }

        return null;
    }

    /*
    ============================================
    RESOLVE START
    ============================================
     */
    private static String resolveCommand(File dir,
            String framework,
            Consumer<String> log) {

        framework = framework == null ? "" : framework.toLowerCase();

        if (framework.equals("laravel")) {
            return "php artisan serve --port=" + resolvePort(framework);
        }

        if (framework.equals("express") || framework.equals("node")) {

            File pkg = new File(dir, "package.json");

            if (pkg.exists() && getScript(pkg, "start") != null) {
                return "npm start";
            }

            if (new File(dir, "server.js").exists()) {
                return "node server.js";
            }
            if (new File(dir, "app.js").exists()) {
                return "node app.js";
            }
            if (new File(dir, "index.js").exists()) {
                return "node index.js";
            }

            return "node .";
        }

        File pkg = new File(dir, "package.json");

        if (!pkg.exists()) {
            log.accept("[ERROR] package.json no encontrado");
            return null;
        }

        if (getScript(pkg, "dev") != null) {
            return "npm run dev";
        }
        if (getScript(pkg, "start") != null) {
            return "npm start";
        }
        if (getScript(pkg, "serve") != null) {
            return "npm run serve";
        }

        log.accept("[ERROR] No hay scripts ejecutables");
        return null;
    }

    /*
    ============================================
    OUTPUT
    ============================================
     */
    private static void readOutput(Process process,
            String framework,
            boolean isCustom,
            Consumer<String> log) throws IOException {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        boolean opened = false;
        String line;

        while ((line = reader.readLine()) != null) {

            String clean = clean(line);
            log.accept(clean);

            if (isCustom) {
                continue;
            }

            if (!opened) {

                String url = extractLocalUrl(clean);

                if (url != null && matchesFramework(url, framework)) {
                    log.accept("[INFO] Servicio disponible en: " + url);
                    opened = true;
                }
            }
        }
    }

    /*
    ============================================
    PUERTOS
    ============================================
     */
    private static int resolvePort(String framework) {
        framework = framework == null ? "" : framework.toLowerCase();

        return switch (framework) {
            case "react" -> 3000;
            case "vue", "vite" -> 5173;
            case "angular" -> 4200;
            case "astro" -> 4321;
            case "laravel" -> 8000;
            default -> 3000;
        };
    }

    private static boolean isPortInUse(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    /*
    ============================================
    🔥 LIBERAR PUERTO (NUEVO)
    ============================================
     */
    private static void killByPort(int port, Consumer<String> log) {

        try {
            Process find = new ProcessBuilder(
                    "cmd.exe", "/c",
                    "netstat -ano | findstr :" + port
            ).start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(find.getInputStream())
            );

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                String pid = parts[parts.length - 1];

                new ProcessBuilder(
                        "cmd.exe", "/c",
                        "taskkill /PID " + pid + " /T /F"
                ).start().waitFor();

                log.accept("[STOP] Puerto " + port + " liberado (PID " + pid + ")");
            }

        } catch (Exception e) {
            log.accept("[ERROR] killByPort: " + e.getMessage());
        }
    }

    /*
    ============================================
    SOLO LOCALHOST
    ============================================
     */
    private static String extractLocalUrl(String text) {

        if (text == null) {
            return null;
        }

        text = text.toLowerCase();

        if (!text.contains("localhost") && !text.contains("127.0.0.1")) {
            return null;
        }

        try {
            int start = text.indexOf("http");
            int end = text.indexOf(" ", start);
            if (end == -1) {
                end = text.length();
            }

            return text.substring(start, end);

        } catch (Exception e) {
            return null;
        }
    }

    /*
    ============================================
    VALIDACIÓN FRAMEWORK
    ============================================
     */
    private static boolean matchesFramework(String url, String framework) {

        framework = framework == null ? "" : framework.toLowerCase();

        return switch (framework) {
            case "react" -> url.contains(":3000");
            case "vite", "vue" -> url.contains(":5173");
            case "angular" -> url.contains(":4200");
            case "astro" -> url.contains(":4321");
            case "laravel" -> url.contains(":8000");
            default -> true;
        };
    }

    /*
    ============================================
    SCRIPT READER
    ============================================
     */
    private static String getScript(File pkgFile, String key) {

        try {
            String content = Files.readString(pkgFile.toPath());
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            if (!json.has("scripts")) {
                return null;
            }

            JsonObject scripts = json.getAsJsonObject("scripts");

            return scripts.has(key) ? scripts.get(key).getAsString() : null;

        } catch (Exception e) {
            return null;
        }
    }

    /*
    ============================================
    STOP (MEJORADO)
    ============================================
     */
    public static void stop(String key, Consumer<String> log) {

        Integer port = PORTS.get(key);

        Process process = RUNNING.get(key);

        // 1. matar proceso interno
        if (process != null) {
            try {

                long pid = process.pid();

                new ProcessBuilder(
                        "cmd.exe", "/c",
                        "taskkill /PID " + pid + " /T /F"
                ).start().waitFor();

                process.destroyForcibly();
                RUNNING.remove(key);

                log.accept("[STOP] " + key + " detenido");

            } catch (Exception e) {
                log.accept("[ERROR] Stop: " + e.getMessage());
            }
        }

        // 2. 🔥 liberar puerto SI sigue ocupado
        if (port != null && isPortInUse(port)) {
            killByPort(port, log);
        }

        PORTS.remove(key);
    }

    /*
    ============================================
    CHECK RUNNING (MEJORADO)
    ============================================
     */
    public static boolean isRunning(String key) {

        Process p = RUNNING.get(key);

        if (p != null && p.isAlive()) {
            return true;
        }

        Integer port = PORTS.get(key);

        return port != null && isPortInUse(port);
    }

    /*
    ============================================
    BROWSER
    ============================================
     */
    public static void openBrowser(String framework, Consumer<String> log) {

        String url = "http://localhost:" + resolvePort(framework);
        openBrowserUrl(url, log);
    }

    private static void openBrowserUrl(String url, Consumer<String> log) {

        try {
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start " + url});
            log.accept("[BROWSER] " + url);

        } catch (Exception e) {
            log.accept("[ERROR] Browser: " + e.getMessage());
        }
    }

    /*
    ============================================
    CLEAN LOG
    ============================================
     */
    private static final String ANSI_REGEX = "\\u001B\\[[;\\d]*[ -/]*[@-~]";

    private static String clean(String line) {
        return line == null ? "" : line.replaceAll(ANSI_REGEX, "").trim();
    }
}