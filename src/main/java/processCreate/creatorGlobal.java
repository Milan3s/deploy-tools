package processCreate;

import com.google.gson.*;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class creatorGlobal extends framework {

    private static final Logger LOGGER = Logger.getLogger(creatorGlobal.class.getName());

    private static final Pattern PROJECT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern SEMVER_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    // =========================
    // CREATE PROJECT
    // =========================
    public static void createProject(String framework, String version, String projectName, String projectPath, ConsoleCallback console) {

        if (framework == null || framework.isBlank()) {
            console.log("Selecciona un framework.");
            return;
        }

        if (!PROJECT_NAME_PATTERN.matcher(projectName).matches()) {
            console.log("Nombre inválido.");
            return;
        }

        File baseDir = new File(projectPath);

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            console.log("Ruta inválida.");
            return;
        }

        try {

            String fw = framework.trim().toLowerCase();

            // =========================
            // REACT → EJECUCIÓN INTERNA
            // =========================
            if (fw.equals("react")) {

                console.log("Modo React (controlado)");

                List<String> cmds = buildReactCommands(version, projectName);
                runInternal(cmds, baseDir, console);

                return;
            }

            // =========================
            // RESTO → TERMINAL EXTERNA
            // =========================
            String command = buildCommand(framework, version, projectName);

            if (command.isBlank()) {
                console.log("Framework no soportado.");
                return;
            }

            console.log("Abriendo terminal externa...");
            runExternalCMD(command, baseDir);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creando proyecto", e);
            console.log("Error: " + e.getMessage());
        }
    }

    // =========================
    // REACT COMMANDS
    // =========================
    private static List<String> buildReactCommands(String version, String name) {

        String v = (version == null || version.isBlank()) ? "latest" : version;

        List<String> cmds = new ArrayList<>();

        // 🔥 crear proyecto SIN interacción
        cmds.add("npm create vite@latest " + name + " -- --template react -y");
        cmds.add("cd " + name);

        if (!v.equalsIgnoreCase("latest")) {

            // 🔥 instalar deps primero
            cmds.add("npm install");

            // 🔥 forzar versión exacta
            cmds.add("npm install react@" + v + " react-dom@" + v + " --save-exact");
        } else {
            cmds.add("npm install");
        }

        return cmds;
    }

    // =========================
    // RUN INTERNAL (REACT)
    // =========================
    private static void runInternal(List<String> commands, File baseDir, ConsoleCallback console) {

        new Thread(() -> {

            File currentDir = baseDir;

            try {

                for (String cmd : commands) {

                    console.log("> " + cmd);

                    if (cmd.startsWith("cd ")) {
                        currentDir = new File(currentDir, cmd.substring(3).trim());
                        continue;
                    }

                    ProcessBuilder pb = isWindows()
                            ? new ProcessBuilder("cmd.exe", "/c", cmd)
                            : new ProcessBuilder("bash", "-c", cmd);

                    pb.directory(currentDir);
                    pb.redirectErrorStream(true);

                    Process process = pb.start();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream())
                    );

                    String line;
                    while ((line = reader.readLine()) != null) {
                        console.log(line);
                    }

                    int exit = process.waitFor();

                    if (exit != 0) {
                        console.log("Error en: " + cmd);
                        return;
                    }
                }

                console.log("✔ React creado correctamente");

            } catch (Exception e) {
                console.log("Error: " + e.getMessage());
            }

        }).start();
    }

    // =========================
    // BUILD COMMAND (OTROS)
    // =========================
    private static String buildCommand(String framework, String version, String name) {

        String fw = framework.trim().toLowerCase();
        String v = (version == null || version.isBlank()) ? "latest" : version;

        switch (fw) {

            case "vue":
                return "npm create vue@latest " + name + " -- --default";

            case "astro":
                return "npm create astro@latest " + name + " -- --template minimal --yes";

            case "angular":
                return "npx @angular/cli@" + v + " new " + name + " --skip-install --defaults";

            case "laravel":
                return v.equalsIgnoreCase("latest")
                        ? "composer create-project laravel/laravel " + name
                        : "composer create-project laravel/laravel " + name + " \"" + v + ".*\"";

            case "react native cli":
                return v.equalsIgnoreCase("latest")
                        ? "npx @react-native-community/cli@latest init " + name
                        : "npx @react-native-community/cli@latest init " + name + " --version " + v;

            case "react native expo":
                return v.equalsIgnoreCase("latest")
                        ? "npx create-expo-app " + name
                        : "npx create-expo-app " + name + " --template expo-template-blank@sdk-" + v;

            default:
                return "";
        }
    }

    // =========================
    // TERMINAL EXTERNA
    // =========================
    private static void runExternalCMD(String command, File dir) throws Exception {

        if (isWindows()) {

            String full = "cd /d \"" + dir.getAbsolutePath() + "\" && " + command;

            new ProcessBuilder(
                    "cmd.exe",
                    "/c",
                    "start",
                    "\"\"",
                    "cmd.exe",
                    "/k",
                    full
            ).start();

        } else {

            String full = "cd \"" + dir.getAbsolutePath() + "\" && " + command + "; exec bash";

            List<String[]> terms = List.of(
                    new String[]{"gnome-terminal", "--", "bash", "-c", full},
                    new String[]{"konsole", "-e", "bash", "-c", full},
                    new String[]{"xfce4-terminal", "-e", "bash -c \"" + full + "\""},
                    new String[]{"x-terminal-emulator", "-e", "bash -c \"" + full + "\""}
            );

            for (String[] t : terms) {
                try {
                    new ProcessBuilder(t).start();
                    return;
                } catch (IOException ignored) {
                }
            }

            throw new RuntimeException("No terminal encontrada");
        }
    }

    // =========================
    // LOAD VERSIONS (FIX REAL)
    // =========================
    public static void loadVersions(String framework, ConsoleCallback console, VersionsCallback callback) {

        new Thread(() -> {

            try {

                String fw = framework.trim().toLowerCase();
                String cmd = buildVersionCommand(fw);

                if (cmd == null) {
                    callback.onVersions(List.of("latest"));
                    return;
                }

                ProcessBuilder pb = isWindows()
                        ? new ProcessBuilder("cmd.exe", "/c", cmd)
                        : new ProcessBuilder("bash", "-c", cmd);

                pb.redirectErrorStream(true);

                Process process = pb.start();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                );

                StringBuilder output = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }

                int exit = process.waitFor();

                if (exit != 0) {
                    console.log("Error ejecutando npm view");
                    callback.onVersions(List.of("latest"));
                    return;
                }

                JsonArray arr = JsonParser.parseString(output.toString()).getAsJsonArray();

                List<String> versions = parseVersions(arr);

                callback.onVersions(versions.isEmpty() ? List.of("latest") : versions);

            } catch (Exception e) {
                console.log("Error cargando versiones: " + e.getMessage());
                callback.onVersions(List.of("latest"));
            }

        }).start();
    }

    // =========================
    private static List<String> parseVersions(JsonArray arr) {

        List<String> clean = new ArrayList<>();

        for (JsonElement el : arr) {

            String v = el.getAsString().replace("v", "");

            if (!SEMVER_PATTERN.matcher(v).matches()) {
                continue;
            }

            clean.add(v);
        }

        clean.sort(semverComparator());

        Map<String, String> grouped = new LinkedHashMap<>();

        for (String v : clean) {
            grouped.putIfAbsent(v.split("\\.")[0], v);
        }

        List<String> result = new ArrayList<>(grouped.values());

        return result.size() > 10 ? result.subList(0, 10) : result;
    }

    private static Comparator<String> semverComparator() {
        return (a, b) -> {
            String[] p1 = a.split("\\.");
            String[] p2 = b.split("\\.");

            for (int i = 0; i < Math.max(p1.length, p2.length); i++) {
                int n1 = i < p1.length ? Integer.parseInt(p1[i]) : 0;
                int n2 = i < p2.length ? Integer.parseInt(p2[i]) : 0;
                if (n1 != n2) {
                    return Integer.compare(n2, n1);
                }
            }
            return 0;
        };
    }

    private static String buildVersionCommand(String fw) {

        switch (fw) {
            case "react":
                return "npm view react versions --json";
            case "vue":
                return "npm view vue versions --json";
            case "astro":
                return "npm view create-astro versions --json";
            case "angular":
                return "npm view @angular/cli versions --json";
            default:
                return null;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public interface ConsoleCallback {

        void log(String msg);
    }

    public interface VersionsCallback {

        void onVersions(List<String> versions);
    }
}
