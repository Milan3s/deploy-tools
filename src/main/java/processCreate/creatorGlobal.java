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

    // =========================
    // BUILD COMMAND
    // =========================
    private static String buildCommand(String framework, String version, String name) {

        String fw = framework.trim().toLowerCase();
        String v = (version == null || version.isBlank()) ? "latest" : version;

        switch (fw) {

            case "react native cli":
                if (v.equalsIgnoreCase("latest")) {
                    return "npx @react-native-community/cli@latest init " + name;
                }
                return "npx @react-native-community/cli@latest init " + name + " --version " + v;

            case "react native expo":
                if (v.equalsIgnoreCase("latest")) {
                    return "npx create-expo-app " + name;
                }
                return "npx create-expo-app " + name
                        + " --template expo-template-blank@sdk-" + v;

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

    // =========================
    // LOAD VERSIONS
    // =========================
    public static void loadVersions(String framework, ConsoleCallback console, VersionsCallback callback) {

        new Thread(() -> {

            try {

                String fw = framework.trim().toLowerCase();

                if (fw.equals("laravel")) {
                    callback.onVersions(loadLaravelVersions());
                    return;
                }

                String cmd = buildVersionCommand(fw);

                if (cmd == null) {
                    callback.onVersions(List.of("latest"));
                    return;
                }

                ProcessBuilder pb = isWindows()
                        ? new ProcessBuilder("cmd.exe", "/c", cmd)
                        : new ProcessBuilder("bash", "-c", cmd);

                pb.redirectErrorStream(true);

                Process p = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                StringBuilder output = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }

                String raw = output.toString().trim();

                console.log("RAW OUTPUT: " + raw);

                int start = raw.indexOf("[");
                int end = raw.lastIndexOf("]");

                if (start == -1 || end == -1) {
                    callback.onVersions(List.of("latest"));
                    return;
                }

                String jsonClean = raw.substring(start, end + 1);

                JsonArray arr = JsonParser.parseString(jsonClean).getAsJsonArray();

                List<String> result = parseVersions(fw, arr);

                callback.onVersions(result.isEmpty() ? List.of("latest") : result);

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error cargando versiones", e);
                callback.onVersions(List.of("latest"));
            }

        }).start();
    }

    // =========================
    // PARSER (FIX REAL)
    // =========================
    private static List<String> parseVersions(String framework, JsonArray arr) {

        List<String> clean = new ArrayList<>();

        for (JsonElement el : arr) {

            String raw = el.getAsString();

            // =========================
            // EXPO → solo SDK (major)
            // =========================
            if (framework.equals("react native expo")) {

                if (!raw.matches("^\\d+\\.\\d+\\.\\d+$")) continue;

                String major = raw.split("\\.")[0];

                if (!clean.contains(major)) {
                    clean.add(major);
                }

                continue;
            }

            // =========================
            // CLI / NORMAL (SEMVER)
            // =========================
            String v = raw.replace("v", "");

            if (!SEMVER_PATTERN.matcher(v).matches()) continue;

            int major = Integer.parseInt(v.split("\\.")[0]);
            if (major > 100) continue;

            clean.add(v);
        }

        // =========================
        // SORT
        // =========================
        if (framework.equals("react native expo")) {
            clean.sort((a, b) -> Integer.compare(Integer.parseInt(b), Integer.parseInt(a)));
        } else {
            clean.sort(semverComparator());
        }

        // =========================
        // GROUPING
        // =========================
        Map<String, String> grouped = new LinkedHashMap<>();

        for (String v : clean) {

            String key;

            if (framework.equals("react native cli")) {
                String[] p = v.split("\\.");
                key = p[0] + "." + p[1];
            } else if (framework.equals("react native expo")) {
                key = v;
            } else {
                key = v.split("\\.")[0];
            }

            grouped.putIfAbsent(key, v);
        }

        List<String> result = new ArrayList<>(grouped.values());

        return result.size() > 10 ? result.subList(0, 10) : result;
    }

    // =========================
    // SEMVER SORT
    // =========================
    private static Comparator<String> semverComparator() {
        return (a, b) -> {
            String[] p1 = a.split("\\.");
            String[] p2 = b.split("\\.");

            int len = Math.max(p1.length, p2.length);

            for (int i = 0; i < len; i++) {

                int n1 = i < p1.length ? Integer.parseInt(p1[i]) : 0;
                int n2 = i < p2.length ? Integer.parseInt(p2[i]) : 0;

                if (n1 != n2) {
                    return Integer.compare(n2, n1);
                }
            }

            return 0;
        };
    }

    // =========================
    // VERSION COMMANDS
    // =========================
    private static String buildVersionCommand(String fw) {

        switch (fw) {

            case "react native cli":
                return "npm view react-native versions --json";

            case "react native expo":
                return "npm view expo-template-blank versions --json";

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

    // =========================
    // LARAVEL
    // =========================
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
            if (v.matches("^v\\d+\\.\\d+\\.\\d+$")) {
                clean.add(v.substring(1));
            }
        }

        clean.sort(semverComparator());

        Map<String, String> majors = new LinkedHashMap<>();

        for (String v : clean) {
            majors.putIfAbsent(v.split("\\.")[0], v);
        }

        List<String> result = new ArrayList<>(majors.values());

        return result.subList(0, Math.min(10, result.size()));
    }

    // =========================
    // UTIL
    // =========================
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

    public interface ConsoleCallback {
        void log(String msg);
    }

    public interface VersionsCallback {
        void onVersions(List<String> versions);
    }
}