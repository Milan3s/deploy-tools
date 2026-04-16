package ports;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class portService {

    private static final String OS
            = System.getProperty("os.name").toLowerCase();

    private static final boolean WINDOWS = OS.contains("win");
    private static final boolean LINUX = OS.contains("nux");
    private static final boolean MAC = OS.contains("mac");

    // ------------------------------------------------
    // FRAMEWORK PORTS
    // ------------------------------------------------
    public static final int ASTRO_PORT = 4321;

    public static final int VUE_PORT = 5173;

    public static final int REACT_VITE_PORT = 5173;
    public static final int REACT_CRA_PORT = 3000;

    public static final int LARAVEL_PORT = 8000;

    // ------------------------------------------------
    // REACT NATIVE
    // ------------------------------------------------
    public static final int REACT_NATIVE_METRO_PORT = 8081;
    public static final int REACT_NATIVE_METRO_ALT_PORT = 8082;

    // ------------------------------------------------
    // ANDROID
    // ------------------------------------------------
    public static final int ANDROID_ADB_PORT = 5037;

    // ------------------------------------------------
    // PORT INFO
    // ------------------------------------------------
    public static class PortInfo {

        public final int port;
        public final int pid;

        public PortInfo(int port, int pid) {
            this.port = port;
            this.pid = pid;
        }

        @Override
        public String toString() {
            return "Puerto " + port + " (PID " + pid + ")";
        }
    }

    // ------------------------------------------------
    // PUBLIC API
    // ------------------------------------------------
    public List<PortInfo> scanFrameworkPorts(String framework) {

        List<Integer> frameworkPorts = getFrameworkPorts(framework);

        if (frameworkPorts.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, Set<Integer>> activePorts = scanPorts();

        List<PortInfo> result = new ArrayList<>();

        for (int port : frameworkPorts) {

            Set<Integer> pids = activePorts.get(port);

            if (pids != null) {

                for (Integer pid : pids) {

                    if (pid > 0) {
                        result.add(new PortInfo(port, pid));
                    }
                }
            }
        }

        return result;
    }

    // ------------------------------------------------
    // FREE FRAMEWORK PORTS
    // ------------------------------------------------
    public boolean freeFrameworkPorts(String framework) {

        try {

            if (framework == null) {
                return false;
            }

            // React Native limpieza especial
            if (framework.equalsIgnoreCase("react native")) {

                killPort(REACT_NATIVE_METRO_PORT);
                killPort(REACT_NATIVE_METRO_ALT_PORT);

                restartADB();

                return true;
            }

            List<PortInfo> ports = scanFrameworkPorts(framework);

            boolean ok = true;

            for (PortInfo p : ports) {

                if (p.port == ANDROID_ADB_PORT) {
                    restartADB();
                    continue;
                }

                if (!killProcess(p.pid)) {
                    ok = false;
                }
            }

            return ok;

        } catch (Exception e) {

            return false;
        }
    }

    // ------------------------------------------------
    // CORE SCAN
    // ------------------------------------------------
    private Map<Integer, Set<Integer>> scanPorts() {

        Map<Integer, Set<Integer>> ports = new HashMap<>();

        try {

            Process process;

            if (WINDOWS) {

                process = new ProcessBuilder(
                        "cmd", "/c", "netstat -ano"
                ).start();

            } else {

                process = new ProcessBuilder(
                        "bash", "-c", "lsof -i -P -n | grep LISTEN"
                ).start();
            }

            BufferedReader reader
                    = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {

                try {

                    if (WINDOWS) {

                        line = line.trim();

                        if (!line.contains("LISTENING")) {
                            continue;
                        }

                        String[] parts = line.split("\\s+");

                        if (parts.length < 5) {
                            continue;
                        }

                        String address = parts[1];
                        int pid = Integer.parseInt(parts[parts.length - 1]);

                        int portIndex = address.lastIndexOf(":");

                        if (portIndex == -1) {
                            continue;
                        }

                        int port = Integer.parseInt(address.substring(portIndex + 1));

                        ports
                                .computeIfAbsent(port, k -> new HashSet<>())
                                .add(pid);

                    } else {

                        String[] parts = line.split("\\s+");

                        int pid = Integer.parseInt(parts[1]);

                        String address = parts[8];

                        int portIndex = address.lastIndexOf(":");

                        int port = Integer.parseInt(address.substring(portIndex + 1));

                        ports
                                .computeIfAbsent(port, k -> new HashSet<>())
                                .add(pid);
                    }

                } catch (Exception ignored) {
                }
            }

        } catch (Exception ignored) {
        }

        return ports;
    }

    // ------------------------------------------------
    // KILL PORT
    // ------------------------------------------------
    private void killPort(int port) {

        if (port == ANDROID_ADB_PORT) {

            restartADB();
            return;
        }

        Map<Integer, Set<Integer>> ports = scanPorts();

        Set<Integer> pids = ports.get(port);

        if (pids != null) {

            for (Integer pid : pids) {

                if (pid > 0) {
                    killProcess(pid);
                }
            }
        }
    }

    // ------------------------------------------------
    // KILL PROCESS
    // ------------------------------------------------
    private boolean killProcess(int pid) {

        try {

            Process process;

            if (WINDOWS) {

                process = new ProcessBuilder(
                        "taskkill",
                        "/PID",
                        String.valueOf(pid),
                        "/F"
                ).start();

            } else {

                process = new ProcessBuilder(
                        "kill",
                        "-9",
                        String.valueOf(pid)
                ).start();
            }

            process.waitFor();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // ------------------------------------------------
    // ADB
    // ------------------------------------------------
    private void restartADB() {

        try {

            if (WINDOWS) {

                new ProcessBuilder("cmd", "/c", "adb kill-server")
                        .start().waitFor();

                new ProcessBuilder("cmd", "/c", "adb start-server")
                        .start().waitFor();

            } else {

                new ProcessBuilder("adb", "kill-server")
                        .start().waitFor();

                new ProcessBuilder("adb", "start-server")
                        .start().waitFor();
            }

        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------
    // FRAMEWORK PORT MAP
    // ------------------------------------------------
    private List<Integer> getFrameworkPorts(String framework) {

        if (framework == null) {
            return Collections.emptyList();
        }

        switch (framework.toLowerCase()) {

            case "astro":
                return List.of(ASTRO_PORT);

            case "vue":
            case "vite":
                return List.of(VUE_PORT);

            case "react":
                return List.of(REACT_VITE_PORT, REACT_CRA_PORT);

            case "react native":
                return List.of(
                        REACT_NATIVE_METRO_PORT,
                        REACT_NATIVE_METRO_ALT_PORT,
                        ANDROID_ADB_PORT
                );

            case "laravel":
                return List.of(LARAVEL_PORT, VUE_PORT);

            default:
                return Collections.emptyList();
        }
    }
}
