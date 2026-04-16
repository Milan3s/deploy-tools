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
    private static final Pattern STABLE_VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

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

            String command = buildCommand(framework, version, projectName);

            if (command.isBlank()) {
                console.log("Framework no soportado.");
                return;
            }

            console.log("Ejecutando: " + command);

            runExternalCMD(command, baseDir);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creando proyecto", e);
            console.log("Error: " + e.getMessage());
        }
    }

    private static String buildCommand(String framework, String version, String name) {

        String v = (version == null || version.isBlank()) ? "latest" : version;

        switch (framework.toLowerCase()) {

            // 🔥 REACT NATIVE (FORMA CORRECTA ACTUAL)
            case "react native cli":
                if (v.equalsIgnoreCase("latest")) {
                    return "npx @react-native-community/cli@latest init " + name;
                }
                return "npx @react-native-community/cli@latest init " + name + " --version " + v;

            case "react native expo":
                return "npx create-expo-app@" + v + " " + name;

            case "react":
                return "npm create vite@" + v + " " + name + " -- --template react";

            case "vue":
                return "npm create vue@" + v + " " + name;

            case "astro":
                return "npm create astro@" + v + " " + name;

            case "angular":
                return "npx @angular/cli@" + v + " new " + name + " --skip-install";

            case "laravel":
                if (v.equalsIgnoreCase("latest")) {
                    return "composer create-project laravel/laravel " + name;
                }
                return "composer create-project laravel/laravel " + name + " \"" + v + ".*\"";

            default:
                return "";
        }
    }

    private static void runExternalCMD(String command, File dir) throws Exception {

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
                    "x-terminal-emulator",
                    "-e",
                    "bash -c 'cd \"" + dir.getAbsolutePath() + "\" && " + command + "; exec bash'"
            ).start();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static void loadVersions(String framework, ConsoleCallback console, VersionsCallback callback) {

        new Thread(() -> {

            try {

                if (framework.equalsIgnoreCase("laravel")) {
                    callback.onVersions(loadLaravelVersions());
                    return;
                }

                String cmd = buildVersionCommand(framework);

                if (cmd == null) {
                    callback.onVersions(List.of("latest"));
                    return;
                }

                ProcessBuilder pb = isWindows()
                        ? new ProcessBuilder("cmd.exe", "/c", cmd)
                        : new ProcessBuilder("bash", "-c", cmd);

                pb.redirectErrorStream(true);

                Process p = pb.start();

                JsonArray arr = JsonParser
                        .parseReader(new InputStreamReader(p.getInputStream()))
                        .getAsJsonArray();

                List<String> clean = new ArrayList<>();

                for (JsonElement el : arr) {

                    String v = el.getAsString().replace("v", "");

                    if (!STABLE_VERSION_PATTERN.matcher(v).matches()) continue;

                    int major = Integer.parseInt(v.split("\\.")[0]);
                    if (major > 100) continue;

                    clean.add(v);
                }

                clean.sort(descComparator());

                Map<String, String> grouped = new LinkedHashMap<>();

                for (String v : clean) {

                    String[] parts = v.split("\\.");

                    String key;

                    if (framework.toLowerCase().contains("react native")) {
                        key = parts[0] + "." + parts[1]; // 0.73, 0.72...
                    } else {
                        key = parts[0];
                    }

                    grouped.putIfAbsent(key, v);
                }

                List<String> result = new ArrayList<>(grouped.values());

                result.sort(descComparator());

                if (result.size() > 10) {
                    result = result.subList(0, 10);
                }

                if (result.isEmpty()) {
                    result = List.of("latest");
                }

                callback.onVersions(result);

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error cargando versiones", e);
                callback.onVersions(List.of("latest"));
            }

        }).start();
    }

    private static List<String> loadLaravelVersions() throws Exception {

        ProcessBuilder pb = isWindows()
                ? new ProcessBuilder("cmd.exe", "/c", "composer show laravel/laravel --all")
                : new ProcessBuilder("bash", "-c", "composer show laravel/laravel --all");

        pb.redirectErrorStream(true);

        Process p = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line;
        List<String> raw = new ArrayList<>();

        while ((line = reader.readLine()) != null) {

            if (line.trim().startsWith("versions")) {

                line = line.replace("versions :", "").trim();
                raw.addAll(Arrays.asList(line.split(", ")));
                break;
            }
        }

        List<String> clean = new ArrayList<>();

        for (String v : raw) {

            if (!v.matches("^v\\d+\\.\\d+\\.\\d+$")) continue;

            clean.add(v.substring(1));
        }

        clean.sort(descComparator());

        Map<String, String> majors = new LinkedHashMap<>();

        for (String v : clean) {
            String major = v.split("\\.")[0];
            majors.putIfAbsent(major, v);
        }

        List<String> result = new ArrayList<>(majors.values());

        result.sort(descComparator());

        return result.subList(0, Math.min(10, result.size()));
    }

    private static Comparator<String> descComparator() {
        return (a, b) -> {
            String[] p1 = a.split("\\.");
            String[] p2 = b.split("\\.");

            for (int i = 0; i < 3; i++) {
                int n1 = Integer.parseInt(p1[i]);
                int n2 = Integer.parseInt(p2[i]);
                if (n1 != n2) return Integer.compare(n2, n1);
            }
            return 0;
        };
    }

    private static String buildVersionCommand(String framework) {

        switch (framework.toLowerCase()) {

            case "react native cli":
                return "npm view react-native versions --json";

            case "react native expo":
                return "npm view create-expo-app versions --json";

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

    public interface ConsoleCallback {
        void log(String msg);
    }

    public interface VersionsCallback {
        void onVersions(List<String> versions);
    }
}