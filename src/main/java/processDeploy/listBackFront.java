package processDeploy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class listBackFront {

    // =========================
    // TIPOS
    // =========================
    public enum WebType {
        LARAVEL,
        EXPRESS,
        NODE,
        NEXT,
        NUXT,
        ASTRO,
        REACT,
        VUE,
        ANGULAR,
        VITE,
        UNKNOWN
    }

    // =========================
    // DETECTOR PRINCIPAL
    // =========================
    public static WebType detect(String path) {

        File root = new File(path);
        if (!root.exists()) return WebType.UNKNOWN;

        // Laravel
        if (new File(root, "artisan").exists()) {
            return WebType.LARAVEL;
        }

        File pkg = new File(root, "package.json");
        if (!pkg.exists()) return WebType.UNKNOWN;

        try {

            String content = Files.readString(pkg.toPath());
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            JsonObject deps = get(json, "dependencies");
            JsonObject devDeps = get(json, "devDependencies");

            // =========================
            // BACKEND
            // =========================
            if (has(deps, "express") || has(devDeps, "express")) {
                return WebType.EXPRESS;
            }

            // =========================
            // FRAMEWORKS COMPLETOS
            // =========================
            if (has(deps, "next")) return WebType.NEXT;
            if (has(deps, "nuxt")) return WebType.NUXT;
            if (has(deps, "astro")) return WebType.ASTRO;

            // =========================
            // TOOLING
            // =========================
            if (has(deps, "vite") || has(devDeps, "vite")) {
                return WebType.VITE;
            }

            // =========================
            // FRONTEND BASE
            // =========================
            if (has(deps, "react")) return WebType.REACT;
            if (has(deps, "vue")) return WebType.VUE;
            if (has(deps, "@angular/core")) return WebType.ANGULAR;

            // =========================
            // NODE GENERICO
            // =========================
            if (json.has("scripts")) {
                return WebType.NODE;
            }

        } catch (Exception ignored) {}

        return WebType.UNKNOWN;
    }

    // =========================
    // DETECCIÓN MULTIPLE (CLAVE)
    // =========================
    public static List<WebType> detectAll(String path) {

        List<WebType> result = new ArrayList<>();

        File root = new File(path);
        if (!root.exists()) return result;

        // Laravel
        if (new File(root, "artisan").exists()) {
            result.add(WebType.LARAVEL);
            return result;
        }

        File pkg = new File(root, "package.json");
        if (!pkg.exists()) return result;

        try {

            String content = Files.readString(pkg.toPath());
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            JsonObject deps = get(json, "dependencies");
            JsonObject devDeps = get(json, "devDependencies");

            // BACKEND
            if (has(deps, "express") || has(devDeps, "express")) {
                result.add(WebType.EXPRESS);
            }

            // FULL FRAMEWORKS
            if (has(deps, "next")) result.add(WebType.NEXT);
            if (has(deps, "nuxt")) result.add(WebType.NUXT);
            if (has(deps, "astro")) result.add(WebType.ASTRO);

            // TOOLING
            if (has(deps, "vite") || has(devDeps, "vite")) {
                result.add(WebType.VITE);
            }

            // FRONTEND BASE
            if (has(deps, "react")) result.add(WebType.REACT);
            if (has(deps, "vue")) result.add(WebType.VUE);
            if (has(deps, "@angular/core")) result.add(WebType.ANGULAR);

            // NODE
            if (json.has("scripts")) {
                result.add(WebType.NODE);
            }

        } catch (Exception ignored) {}

        return result;
    }

    // =========================
    // LISTAS DINÁMICAS PARA UI
    // =========================
    public static List<String> getFrontendOptions(String path) {

        List<String> list = new ArrayList<>();

        for (WebType type : detectAll(path)) {
            if (isFrontend(type)) {
                list.add(toDisplay(type));
            }
        }

        return list;
    }

    public static List<String> getBackendOptions(String path) {

        List<String> list = new ArrayList<>();

        for (WebType type : detectAll(path)) {
            if (isBackend(type)) {
                list.add(toDisplay(type));
            }
        }

        return list;
    }

    // =========================
    // CLASIFICACIÓN
    // =========================
    private static boolean isBackend(WebType t) {
        return t == WebType.LARAVEL
                || t == WebType.EXPRESS
                || t == WebType.NODE;
    }

    private static boolean isFrontend(WebType t) {
        return switch (t) {
            case REACT, VUE, ANGULAR, VITE, NEXT, NUXT, ASTRO -> true;
            default -> false;
        };
    }

    // =========================
    // DISPLAY
    // =========================
    public static String toDisplay(WebType type) {
        return switch (type) {
            case LARAVEL -> "Laravel";
            case EXPRESS -> "Express";
            case NODE -> "Node";
            case NEXT -> "Next";
            case NUXT -> "Nuxt";
            case ASTRO -> "Astro";
            case REACT -> "React";
            case VUE -> "Vue";
            case ANGULAR -> "Angular";
            case VITE -> "Vite";
            default -> "";
        };
    }

    // =========================
    // HELPERS
    // =========================
    private static JsonObject get(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonObject()
                ? json.getAsJsonObject(key)
                : null;
    }

    private static boolean has(JsonObject obj, String key) {
        return obj != null && obj.has(key);
    }
}