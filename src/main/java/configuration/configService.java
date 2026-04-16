package configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class configService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String OS = System.getProperty("os.name").toLowerCase();

    private static String getBaseConfigDir() {
        if (OS.contains("win")) {
            return System.getenv("APPDATA") + File.separator + "DeployTool";
        }

        if (OS.contains("mac")) {
            return System.getProperty("user.home")
                    + File.separator + "Library"
                    + File.separator + "Application Support"
                    + File.separator + "DeployTool";
        }

        return System.getProperty("user.home")
                + File.separator + ".config"
                + File.separator + "deploytool";
    }

    private static final String CONFIG_DIR = getBaseConfigDir();
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "config.json";
    private static final String CONFIG_BACKUP = CONFIG_DIR + File.separator + "config.backup.json";

    public static void saveConfig(ConfigData global) {

        if (global == null) {
            System.err.println("[CONFIG] Datos nulos, no se guarda");
            return;
        }

        boolean ok = save(global);

        if (!ok) {
            System.err.println("[CONFIG] Fallo al guardar configuración");
        } else {
            System.out.println("[CONFIG] Configuración guardada correctamente");
        }
    }

    /*
    ============================================
    MODEL
    ============================================
     */
    public static class ConfigData {

        public String configPath;
        public String projectsPath;

        // 🔥 NUEVO
        public String backendPath;
        public String frontendPath;

        public String gitPath;
        public String gitUser;
        public String gitEmail;
    }

    /*
    ============================================
    LOAD
    ============================================
     */
    public static synchronized ConfigData loadConfig() {
        try {
            File file = new File(CONFIG_FILE);

            if (!file.exists()) {
                return new ConfigData();
            }

            try (FileReader reader = new FileReader(file)) {
                return GSON.fromJson(reader, ConfigData.class);
            }

        } catch (Exception e) {
            System.err.println("[CONFIG] Error leyendo config: " + e.getMessage());
            return loadBackup();
        }
    }

    private static ConfigData loadBackup() {
        try {
            File file = new File(CONFIG_BACKUP);

            if (!file.exists()) {
                return new ConfigData();
            }

            try (FileReader reader = new FileReader(file)) {
                return GSON.fromJson(reader, ConfigData.class);
            }

        } catch (Exception e) {
            System.err.println("[CONFIG] Error leyendo backup: " + e.getMessage());
            return new ConfigData();
        }
    }

    /*
    ============================================
    SAVE (ROBUSTO)
    ============================================
     */
    private static synchronized boolean save(ConfigData data) {

        try {
            ensureConfigDir();

            File mainFile = new File(CONFIG_FILE);
            File backupFile = new File(CONFIG_BACKUP);

            if (mainFile.exists()) {
                Files.copy(mainFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            File tempFile = new File(CONFIG_FILE + ".tmp");

            try (FileWriter writer = new FileWriter(tempFile)) {
                GSON.toJson(data, writer);
            }

            Files.move(tempFile.toPath(), mainFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return true;

        } catch (Exception e) {
            System.err.println("[CONFIG] Error guardando config: " + e.getMessage());
            return false;
        }
    }

    /*
    ============================================
    SETTERS
    ============================================
     */
    public static boolean setConfigPath(String path) {
        return setPath(path, data -> data.configPath = normalize(path));
    }

    public static boolean setProjectsPath(String path) {
        return setPath(path, data -> data.projectsPath = normalize(path));
    }

    // 🔥 NUEVOS
    public static boolean setBackendPath(String path) {
        return setPath(path, data -> data.backendPath = normalize(path));
    }

    public static boolean setFrontendPath(String path) {
        return setPath(path, data -> data.frontendPath = normalize(path));
    }

    public static boolean setGitPath(String path) {
        return setPath(path, data -> data.gitPath = normalize(path));
    }

    /*
    ============================================
    GIT CONFIG
    ============================================
     */
    public static boolean setGitConfig(String path, String user, String email) {

        if (!isValidDirectory(path)) {
            return false;
        }
        if (user == null || user.isBlank()) {
            return false;
        }
        if (email == null || email.isBlank()) {
            return false;
        }

        ConfigData data = loadConfig();

        data.gitPath = normalize(path);
        data.gitUser = user.trim();
        data.gitEmail = email.trim();

        return save(data);
    }

    /*
    ============================================
    GENERIC PATH SETTER
    ============================================
     */
    private interface PathUpdater {

        void apply(ConfigData data);
    }

    private static boolean setPath(String path, PathUpdater updater) {

        if (!isValidDirectory(path)) {
            return false;
        }

        ConfigData data = loadConfig();
        updater.apply(data);

        return save(data);
    }

    /*
    ============================================
    GETTERS
    ============================================
     */
    public static String getConfigPath() {
        return loadConfig().configPath;
    }

    public static String getProjectsPath() {
        return loadConfig().projectsPath;
    }

    public static String getBackendPath() {
        return loadConfig().backendPath;
    }

    public static String getFrontendPath() {
        return loadConfig().frontendPath;
    }

    public static String getGitPath() {
        return loadConfig().gitPath;
    }

    public static String getGitUser() {
        return loadConfig().gitUser;
    }

    public static String getGitEmail() {
        return loadConfig().gitEmail;
    }

    /*
    ============================================
    VALIDACIÓN REAL
    ============================================
     */
    private static boolean isValidDirectory(String path) {

        if (path == null || path.isBlank()) {
            return false;
        }

        File dir = new File(path);

        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }

        try {
            File test = new File(dir, ".write_test");
            if (test.createNewFile()) {
                test.delete();
                return true;
            }
        } catch (IOException e) {
            return false;
        }

        return false;
    }

    private static String normalize(String path) {
        return new File(path).getAbsolutePath();
    }

    private static void ensureConfigDir() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /*
    ============================================
    EXISTS
    ============================================
     */
    public static boolean hasConfig() {
        return new File(CONFIG_FILE).exists();
    }
}
