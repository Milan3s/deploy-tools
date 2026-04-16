package npminstall;

import com.google.gson.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class npmService {

    private String projectPath;
    private Process process;

    private static final Pattern ANSI_COLOR =
            Pattern.compile("\u001B\\[[;\\d]*m");

    private static final Pattern ANSI_CONTROL =
            Pattern.compile("\u001B\\[[0-9?;]*[A-Za-z]");

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private static final String GLOBAL_DIR =
            System.getProperty("user.home") + File.separator + ".deploy-tools";

    private static final String CONFIG_FILE =
            GLOBAL_DIR + File.separator + "npm.config";

    private static final String GLOBAL_DEP_FILE =
            GLOBAL_DIR + File.separator + "npm.dependencies.json";

    public npmService() {
        loadProjectPath();
    }

    // ------------------------------------------------
    public void setProjectPath(String path) {
        this.projectPath = path;
    }

    public String getProjectPath() {
        return projectPath;
    }

    // ------------------------------------------------
    public void saveProjectPath(String path, Consumer<String> log) {

        try {

            if (path == null || path.isBlank()) {
                log.accept("Ruta inválida");
                return;
            }

            File dir = new File(path);
            if (!dir.exists()) {
                log.accept("La ruta no existe");
                return;
            }

            new File(GLOBAL_DIR).mkdirs();

            Files.writeString(Path.of(CONFIG_FILE), path);
            this.projectPath = path;

            log.accept("Ruta guardada correctamente");

            generateDependenciesFile(log, false);

        } catch (Exception e) {
            log.accept("Error guardando ruta: " + e.getMessage());
        }
    }

    // ------------------------------------------------
    public void loadProjectPath() {

        try {

            Path path = Path.of(CONFIG_FILE);

            if (!Files.exists(path)) return;

            String saved = Files.readString(path).trim();

            if (!saved.isBlank()) {
                this.projectPath = saved;
            }

        } catch (Exception ignored) {}
    }

    // ------------------------------------------------
    private boolean validateProject(Consumer<String> log) {

        if (projectPath == null || projectPath.isBlank()) {
            log.accept("Ruta no configurada");
            return false;
        }

        File dir = new File(projectPath);

        if (!dir.exists()) {
            log.accept("Ruta inválida");
            return false;
        }

        File pkg = new File(dir, "package.json");

        if (!pkg.exists()) {
            log.accept("No se encontró package.json");
            return false;
        }

        return true;
    }

    // ------------------------------------------------
    public void listDependencies(Consumer<String> log) {

        new Thread(() -> {

            try {

                if (!validateProject(log)) return;

                JsonObject json = JsonParser.parseString(
                        Files.readString(
                                new File(projectPath, "package.json").toPath()
                        )
                ).getAsJsonObject();

                log.accept("");
                log.accept("========= DEPENDENCIAS =========");

                if (json.has("dependencies")) {
                    JsonObject deps = json.getAsJsonObject("dependencies");

                    deps.keySet().forEach(key ->
                            log.accept("• " + key + " → " + deps.get(key).getAsString())
                    );

                } else {
                    log.accept("No hay dependencies");
                }

                log.accept("");
                log.accept("====== DEV DEPENDENCIAS ======");

                if (json.has("devDependencies")) {
                    JsonObject dev = json.getAsJsonObject("devDependencies");

                    dev.keySet().forEach(key ->
                            log.accept("• " + key + " → " + dev.get(key).getAsString())
                    );

                } else {
                    log.accept("No hay devDependencies");
                }

                log.accept("================================");

            } catch (Exception e) {
                log.accept("Error listando dependencias: " + e.getMessage());
            }

        }).start();
    }

    // ------------------------------------------------
    public void runInstall(String manager, String parameters, Consumer<String> log) {

        if (!validateProject(log)) return;

        String params = (parameters == null || parameters.isBlank())
                ? "install"
                : "install " + parameters;

        runCommand(manager, params, "INSTALACIÓN", log);
    }

    public void removeDependency(String manager, String dependency, Consumer<String> log) {

        if (!validateProject(log)) return;

        if (dependency == null || dependency.isBlank()) {
            log.accept("Dependencia inválida");
            return;
        }

        runCommand(manager, "uninstall " + dependency, "ELIMINACIÓN", log);
    }

    public void updateDependency(String manager, String dependency, Consumer<String> log) {

        if (!validateProject(log)) return;

        if (dependency == null || dependency.isBlank()) {
            log.accept("Dependencia inválida");
            return;
        }

        runCommand(manager, "update " + dependency, "ACTUALIZACIÓN", log);
    }

    // ------------------------------------------------
    private void runCommand(String manager, String params, String action, Consumer<String> log) {

        final String cmd = (manager == null || manager.isBlank()) ? "npm" : manager;

        new Thread(() -> {

            try {

                if (process != null && process.isAlive()) {
                    log.accept("Ya hay un proceso en ejecución");
                    return;
                }

                File dir = new File(projectPath);

                ProcessBuilder builder = new ProcessBuilder(
                        "cmd", "/c", resolveCommand(cmd) + " " + params
                );

                builder.directory(dir);
                builder.redirectErrorStream(true);

                log.accept("");
                log.accept("Iniciando " + action.toLowerCase() + "...");
                log.accept("--------------------------------");

                process = builder.start();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                );

                String line;
                boolean errorDetected = false;

                while ((line = reader.readLine()) != null) {

                    line = cleanAnsi(line);

                    if (line.isBlank()) continue;

                    String lower = line.toLowerCase();

                    if (lower.contains("error") || lower.contains("failed")) {
                        errorDetected = true;
                    }

                    if (lower.startsWith("npm warn")) continue;

                    log.accept(line);
                }

                process.waitFor();

                log.accept("--------------------------------");

                if (process.exitValue() == 0 && !errorDetected) {

                    log.accept("✔ " + action + " finalizada correctamente");

                    generateDependenciesFile(log, false);

                } else {
                    log.accept("✖ Error durante " + action.toLowerCase());
                }

            } catch (Exception e) {
                log.accept("Error: " + e.getMessage());
            }

        }).start();
    }

    // ------------------------------------------------
    private void generateDependenciesFile(Consumer<String> log, boolean silent) {

        try {

            if (projectPath == null) return;

            File packageJson = new File(projectPath, "package.json");

            if (!packageJson.exists()) return;

            JsonObject json = JsonParser.parseString(
                    Files.readString(packageJson.toPath())
            ).getAsJsonObject();

            JsonObject result = new JsonObject();

            if (json.has("dependencies"))
                result.add("dependencies", json.getAsJsonObject("dependencies"));

            if (json.has("devDependencies"))
                result.add("devDependencies", json.getAsJsonObject("devDependencies"));

            result.addProperty("projectPath", projectPath);
            result.addProperty("timestamp", System.currentTimeMillis());

            Files.writeString(Path.of(GLOBAL_DEP_FILE), GSON.toJson(result));

            new File(projectPath + "/.deploy-tools").mkdirs();

        } catch (Exception ignored) {}
    }

    // ------------------------------------------------
    private String resolveCommand(String cmd) {

        boolean win = System.getProperty("os.name").toLowerCase().contains("win");

        if (!win) return cmd;

        switch (cmd) {
            case "npm": return "npm.cmd";
            case "yarn": return "yarn.cmd";
            case "pnpm": return "pnpm.cmd";
            case "bun": return "bun.cmd";
        }

        return cmd;
    }

    private String cleanAnsi(String line) {

        if (line == null) return "";

        line = ANSI_COLOR.matcher(line).replaceAll("");
        line = ANSI_CONTROL.matcher(line).replaceAll("");

        return line.trim();
    }

    // ------------------------------------------------
    public void stop() {
        try {
            if (process != null) process.destroy();
        } catch (Exception ignored) {}
    }
}