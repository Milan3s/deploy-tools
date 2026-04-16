package git;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public class gitService {

    // =================================================
    public static final List<String> GIT_ACTIONS = Arrays.asList(
            "Clone", "Pull", "Push", "Fetch", "Status"
    );

    public static List<String> getAvailableActions() {
        return new ArrayList<>(GIT_ACTIONS);
    }

    // =================================================
    private static final File CONFIG_FILE = new File(
            System.getProperty("user.home") + "/.gittool.properties"
    );

    private static final String KEY_TOKEN = "token";

    public static void saveToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            Properties p = new Properties();
            p.setProperty(KEY_TOKEN, token);
            p.store(fos, "GitTool");
        } catch (Exception ignored) {
        }
    }

    public static String loadToken() {
        if (!CONFIG_FILE.exists()) {
            return "";
        }

        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            Properties p = new Properties();
            p.load(fis);
            return p.getProperty(KEY_TOKEN, "");
        } catch (Exception e) {
            return "";
        }
    }

    // =================================================
    private static HttpURLConnection conn(String url, String method, String token) throws Exception {

        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();

        c.setRequestMethod(method);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("User-Agent", "GitTool");

        if (token != null && !token.isBlank()) {
            c.setRequestProperty("Authorization", "Bearer " + token);
        }

        return c;
    }

    private static String read(HttpURLConnection c) throws Exception {

        InputStream s = (c.getResponseCode() >= 200 && c.getResponseCode() < 300)
                ? c.getInputStream()
                : c.getErrorStream();

        if (s == null) {
            return "";
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(s));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
    }

    // =================================================
    public static void testConnection(String token, Consumer<String> log, Consumer<Boolean> result) {

        new Thread(() -> {
            try {
                HttpURLConnection c = conn("https://api.github.com/user", "GET", token);

                if (c.getResponseCode() == 200) {
                    JsonObject j = JsonParser.parseString(read(c)).getAsJsonObject();
                    log.accept("[OK] Conectado como: " + j.get("login").getAsString());
                    result.accept(true);
                } else {
                    log.accept("[ERROR] Token inválido");
                    result.accept(false);
                }

            } catch (Exception e) {
                log.accept("[ERROR] " + e.getMessage());
                result.accept(false);
            }
        }).start();
    }

    // =================================================
    public static Map<String, String> listRepos(String token, Consumer<String> log) {

        Map<String, String> repos = new LinkedHashMap<>();

        try {
            HttpURLConnection c = conn(
                    "https://api.github.com/user/repos?per_page=100",
                    "GET", token
            );

            JsonArray arr = JsonParser.parseString(read(c)).getAsJsonArray();

            for (int i = 0; i < arr.size(); i++) {
                JsonObject repo = arr.get(i).getAsJsonObject();

                repos.put(
                        repo.get("name").getAsString(),
                        repo.getAsJsonObject("owner").get("login").getAsString()
                );
            }

            log.accept("[OK] Repos cargados (" + repos.size() + ")");

        } catch (Exception e) {
            log.accept("[ERROR] repos: " + e.getMessage());
        }

        return repos;
    }

    // =================================================
    public static void listRemoteBranches(
            String owner,
            String repo,
            String token,
            Consumer<String> log,
            Consumer<List<String>> result
    ) {

        new Thread(() -> {

            List<String> branches = new ArrayList<>();

            try {
                HttpURLConnection c = conn(
                        "https://api.github.com/repos/" + owner + "/" + repo + "/branches",
                        "GET", token
                );

                String res = read(c);

                if (res != null && res.startsWith("[")) {

                    JsonArray arr = JsonParser.parseString(res).getAsJsonArray();

                    for (int i = 0; i < arr.size(); i++) {
                        branches.add(arr.get(i).getAsJsonObject().get("name").getAsString());
                    }
                }

                log.accept("[OK] Ramas: " + branches.size());

            } catch (Exception e) {
                log.accept("[ERROR] branches: " + e.getMessage());
            }

            result.accept(branches);

        }).start();
    }

    // =================================================
    public static void createRemoteRepo(String name, String token, Consumer<String> log, Consumer<String> urlOut) {

        new Thread(() -> {
            try {

                HttpURLConnection c = conn("https://api.github.com/user/repos", "POST", token);
                c.setDoOutput(true);

                String json = "{ \"name\": \"" + name + "\" }";

                try (OutputStream os = c.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                if (c.getResponseCode() == 201) {

                    JsonObject obj = JsonParser.parseString(read(c)).getAsJsonObject();
                    String cloneUrl = obj.get("clone_url").getAsString();

                    log.accept("[OK] Repo creado");

                    if (urlOut != null) {
                        urlOut.accept(cloneUrl);
                    }

                } else {
                    log.accept("[ERROR] Crear repo");
                }

            } catch (Exception e) {
                log.accept("[ERROR] " + e.getMessage());
            }
        }).start();
    }

    public static void deleteRemoteRepo(String owner, String repo, String token, Consumer<String> log) {

        new Thread(() -> {
            try {

                HttpURLConnection c = conn(
                        "https://api.github.com/repos/" + owner + "/" + repo,
                        "DELETE", token
                );

                if (c.getResponseCode() == 204) {
                    log.accept("[OK] Repo eliminado");
                } else {
                    log.accept("[ERROR] Delete repo");
                }

            } catch (Exception e) {
                log.accept("[ERROR] " + e.getMessage());
            }
        }).start();
    }

    // =================================================
    private static int cmd(String command, File dir, Consumer<String> log) {

        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.directory(dir);
            pb.redirectErrorStream(true);

            Process p = pb.start();

            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = r.readLine()) != null) {
                log.accept(line);
            }

            return p.waitFor();

        } catch (Exception e) {
            log.accept("[ERROR] " + e.getMessage());
            return -1;
        }
    }

    private static String exec(String command, File dir) {

        StringBuilder out = new StringBuilder();

        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.directory(dir);

            Process p = pb.start();

            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append("\n");
            }

            p.waitFor();

        } catch (Exception ignored) {
        }

        return out.toString().trim();
    }

    // =================================================
    private static void ensureGitRepo(File dir, Consumer<String> log) {

        if (!new File(dir, ".git").exists()) {
            log.accept("[INFO] git init");
            cmd("git init", dir, log);
        }
    }

    private static String ensureBranch(File dir, Consumer<String> log) {

        String branch = exec("git branch --show-current", dir);

        if (branch == null || branch.isBlank()) {
            cmd("git checkout -b main", dir, log);
            return "main";
        }

        return branch.trim();
    }

    public static void ensureRemote(File dir, String repoUrl, Consumer<String> log) {

        String remotes = exec("git remote", dir);

        if (!remotes.contains("origin")) {
            cmd("git remote add origin " + repoUrl, dir, log);
        } else {
            cmd("git remote set-url origin " + repoUrl, dir, log);
        }
    }

    // =================================================
    public static void cloneRepo(String repoUrl, String path, String branch, Consumer<String> log) {

        new Thread(() -> {

            try {

                File targetDir = new File(path);

                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }

                String[] files = targetDir.list();
                if (files != null && files.length > 0) {
                    log.accept("[ERROR] Carpeta no vacía");
                    return;
                }

                File parent = targetDir.getParentFile();

                if (parent == null || !parent.exists()) {
                    log.accept("[ERROR] Ruta padre inválida");
                    return;
                }

                String repoName = targetDir.getName();

                String cmdStr = "git clone";

                if (branch != null && !branch.isBlank()) {
                    cmdStr += " -b " + branch;
                }

                cmdStr += " \"" + repoUrl + "\" \"" + repoName + "\"";

                log.accept("[INFO] Clonando repositorio...");

                int result = cmd(cmdStr, parent, log);

                if (result == 0) {
                    log.accept("[OK] Clone completado");
                } else {
                    log.accept("[ERROR] Clone fallido");
                }

            } catch (Exception e) {
                log.accept("[ERROR] clone: " + e.getMessage());
            }

        }).start();
    }

    // =================================================
    public static void push(String path, String repoUrl, String branchParam, Consumer<String> log) {

        new Thread(() -> {

            File dir = new File(path);

            ensureGitRepo(dir, log);
            ensureRemote(dir, repoUrl, log);

            final String branch = ensureBranch(dir, log);

            cmd("git add .", dir, log);
            cmd("git commit -m \"initial commit\"", dir, log);

            cmd("git push -u origin " + branch, dir, log);

        }).start();
    }

    // =================================================
    public static void pull(String path, String repoUrl, String branch, Consumer<String> log) {

        new Thread(() -> {

            File dir = new File(path);

            ensureRemote(dir, repoUrl, log);
            cmd("git pull origin " + branch, dir, log);

        }).start();
    }

    public static void fetch(String path, String repoUrl, Consumer<String> log) {

        new Thread(() -> {

            File dir = new File(path);

            ensureRemote(dir, repoUrl, log);
            cmd("git fetch origin", dir, log);

        }).start();
    }

    public static void commit(String path, String msg, Consumer<String> log) {

        new Thread(() -> {

            File dir = new File(path);

            if (msg == null || msg.isBlank()) {
                log.accept("[ERROR] mensaje vacío");
                return;
            }

            cmd("git add .", dir, log);
            cmd("git commit -m \"" + msg + "\"", dir, log);

        }).start();
    }

    public static void status(String path, Consumer<String> log) {
        new Thread(() -> cmd("git status", new File(path), log)).start();
    }

    // =================================================
    public static String buildAuthUrl(String url, String user, String token) {
        return url.replace("https://", "https://" + user + ":" + token + "@");
    }

    // =================================================
    public static void createBranch(
            String path,
            String repoUrl,
            String branchName,
            Consumer<String> log,
            Consumer<Boolean> result
    ) {

        new Thread(() -> {

            try {

                if (branchName == null || branchName.isBlank()) {
                    log.accept("[ERROR] Nombre de rama vacío");
                    result.accept(false);
                    return;
                }

                File dir = new File(path);

                if (!dir.exists()) {
                    log.accept("[ERROR] Ruta inválida");
                    result.accept(false);
                    return;
                }

                ensureGitRepo(dir, log);
                ensureRemote(dir, repoUrl, log);

                log.accept("[INFO] Creando rama: " + branchName);

                String branches = exec("git branch --list " + branchName, dir);

                int exit;

                if (branches != null && !branches.isBlank()) {

                    log.accept("[INFO] La rama ya existe, cambiando...");
                    exit = cmd("git checkout " + branchName, dir, log);

                } else {

                    exit = cmd("git checkout -b " + branchName, dir, log);
                }

                if (exit != 0) {
                    log.accept("[ERROR] No se pudo crear/cambiar rama");
                    result.accept(false);
                    return;
                }

                log.accept("[INFO] Publicando rama en remoto...");
                cmd("git push -u origin " + branchName, dir, log);

                log.accept("[OK] Rama lista: " + branchName);

                result.accept(true);

            } catch (Exception e) {

                log.accept("[ERROR] createBranch: " + e.getMessage());
                result.accept(false);
            }

        }).start();
    }

    // =================================================
    public static void deleteBranch(
            String path,
            String repoUrl,
            String branchName,
            Consumer<String> log,
            Consumer<Boolean> result
    ) {

        new Thread(() -> {

            try {

                if (branchName == null || branchName.isBlank()) {
                    log.accept("[ERROR] Nombre de rama vacío");
                    result.accept(false);
                    return;
                }

                File dir = new File(path);

                if (!dir.exists()) {
                    log.accept("[ERROR] Ruta inválida");
                    result.accept(false);
                    return;
                }

                ensureGitRepo(dir, log);
                ensureRemote(dir, repoUrl, log);

                String current = exec("git branch --show-current", dir);

                if (branchName.equals(current)) {
                    log.accept("[ERROR] No puedes borrar la rama actual");
                    result.accept(false);
                    return;
                }

                log.accept("[INFO] Eliminando rama local...");

                int exit = cmd("git branch -d " + branchName, dir, log);

                if (exit != 0) {
                    log.accept("[WARN] Forzando eliminación...");
                    cmd("git branch -D " + branchName, dir, log);
                }

                log.accept("[INFO] Eliminando rama remota...");
                cmd("git push origin --delete " + branchName, dir, log);

                log.accept("[OK] Rama eliminada: " + branchName);

                result.accept(true);

            } catch (Exception e) {

                log.accept("[ERROR] deleteBranch: " + e.getMessage());
                result.accept(false);
            }

        }).start();
    }

    // =================================================
    public static void mergeBranch(
            String path,
            String repoUrl,
            String sourceBranch,
            String targetBranch,
            Consumer<String> log,
            Consumer<Boolean> result
    ) {

        new Thread(() -> {

            try {

                if (sourceBranch == null || sourceBranch.isBlank()) {
                    log.accept("[ERROR] Rama origen vacía");
                    result.accept(false);
                    return;
                }

                if (targetBranch == null || targetBranch.isBlank()) {
                    log.accept("[ERROR] Rama destino vacía");
                    result.accept(false);
                    return;
                }

                if (sourceBranch.equals(targetBranch)) {
                    log.accept("[ERROR] No puedes hacer merge de la misma rama");
                    result.accept(false);
                    return;
                }

                File dir = new File(path);

                if (!dir.exists()) {
                    log.accept("[ERROR] Ruta inválida");
                    result.accept(false);
                    return;
                }

                ensureGitRepo(dir, log);
                ensureRemote(dir, repoUrl, log);

                // 🔥 CLAVE: sincronizar
                log.accept("[INFO] Fetch remoto...");
                cmd("git fetch origin", dir, log);

                // 🔥 asegurar ramas
                cmd("git fetch origin " + sourceBranch, dir, log);
                cmd("git fetch origin " + targetBranch, dir, log);

                // 🔥 cambiar a destino
                log.accept("[INFO] Checkout destino: " + targetBranch);
                int checkout = cmd("git checkout " + targetBranch, dir, log);

                if (checkout != 0) {
                    log.accept("[ERROR] No se pudo cambiar a la rama destino");
                    result.accept(false);
                    return;
                }

                // 🔥 actualizar destino
                cmd("git pull origin " + targetBranch, dir, log);

                // 🔥 merge real
                log.accept("[INFO] Merge: " + sourceBranch + " → " + targetBranch);

                int merge = cmd("git merge origin/" + sourceBranch, dir, log);

                if (merge != 0) {
                    log.accept("[ERROR] Conflicto o fallo en merge");
                    result.accept(false);
                    return;
                }

                // 🔥 push final
                log.accept("[INFO] Push cambios...");
                int push = cmd("git push origin " + targetBranch, dir, log);

                if (push != 0) {
                    log.accept("[ERROR] Fallo al hacer push");
                    result.accept(false);
                    return;
                }

                log.accept("[OK] Merge completado correctamente");

                result.accept(true);

            } catch (Exception e) {

                log.accept("[ERROR] mergeBranch: " + e.getMessage());
                result.accept(false);
            }

        }).start();
    }
}
