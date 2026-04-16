package deploy;

import processDeploy.BuilderAPKService;
import utils_ui.directory;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class BuilderRNController implements Initializable {

    @FXML private TextField androidPathField;
    @FXML private TextArea logArea;
    @FXML private Button openAndroidButton;

    @FXML private Label operationsStatus;
    @FXML private Label buildStatus;

    private String androidPath;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        openAndroidButton.setDisable(true);

        String saved = BuilderAPKService.loadPath();

        if (saved != null && isValidPath(saved)) {
            androidPath = saved;
            androidPathField.setText(saved);
            androidPathField.setDisable(true);

            setOperationsStatus("Configurado", "ok");
            setBuildStatus("Listo", "ok");

            log("[CONFIG] Ruta cargada");
        } else {
            setOperationsStatus("Sin configurar", "info");
            setBuildStatus("-", "info");
        }
    }

    /*
    ============================================
    SAVE
    ============================================
     */
    @FXML
    private void onSaveAndroid(ActionEvent event) {

        String path = androidPathField.getText();

        if (!isValidPath(path)) {
            setOperationsStatus("Ruta inválida", "ko");
            log("[ERROR] Ruta no válida");
            return;
        }

        androidPath = path;

        BuilderAPKService.savePath(path, this::log);

        androidPathField.setDisable(true);

        setOperationsStatus("Guardado", "ok");
    }

    /*
    ============================================
    UNLOCK
    ============================================
     */
    @FXML
    private void onUnlockAndroid(ActionEvent event) {
        androidPathField.setDisable(false);
        setOperationsStatus("Editable", "info");
    }

    /*
    ============================================
    BROWSE
    ============================================
     */
    @FXML
    private void onBrowseAndroid(ActionEvent event) {

        File dir = new directory().show();
        if (dir == null) return;

        androidPath = dir.getAbsolutePath();
        androidPathField.setText(androidPath);

        setOperationsStatus("Ruta seleccionada", "info");
    }

    /*
    ============================================
    BUILD
    ============================================
     */
    @FXML
    private void onBuildAndroid(ActionEvent event) {

        String path = androidPathField.getText();

        if (!isValidPath(path)) {
            setBuildStatus("Ruta inválida", "ko");
            log("[ERROR] Ruta inválida");
            return;
        }

        openAndroidButton.setDisable(true);

        log("[BUILD] Abriendo CMD externo...");
        setBuildStatus("Ejecutando...", "info");

        BuilderAPKService.build(path, this::log);

        // WATCHER APK
        new Thread(() -> {
            File apk = new File(path + "/android/app/build/outputs/apk/release/app-release.apk");

            int attempts = 0;

            while (attempts < 120) {

                if (apk.exists()) {
                    Platform.runLater(() -> {
                        setBuildStatus("OK", "ok");
                        log("[BUILD] APK generado");
                        openAndroidButton.setDisable(false);
                    });
                    return;
                }

                sleep(1000);
                attempts++;
            }

            Platform.runLater(() -> {
                setBuildStatus("Timeout", "ko");
                log("[BUILD] No se detectó APK");
            });

        }).start();
    }

    /*
    ============================================
    OPEN APK
    ============================================
     */
    @FXML
    private void onOpenAndroidFolder(ActionEvent event) {

        String path = androidPathField.getText();

        if (!isValidPath(path)) {
            log("[ERROR] Ruta inválida");
            return;
        }

        BuilderAPKService.openAPK(path, this::log);
    }

    /*
    ============================================
    GRADLE CLEAN
    ============================================
     */
    @FXML
    private void onCleanGradle(ActionEvent event) {

        String path = androidPathField.getText();

        if (!isValidPath(path)) {
            setOperationsStatus("Ruta inválida", "ko");
            return;
        }

        log("[CLEAN] Gradle clean...");
        setOperationsStatus("Ejecutando...", "info");

        BuilderAPKService.cleanGradle(path, this::log);

        new Thread(() -> {
            sleep(2000);
            Platform.runLater(() ->
                setOperationsStatus("Gradle clean OK", "ok")
            );
        }).start();
    }

    /*
    ============================================
    DELETE NODE MODULES
    ============================================
     */
    @FXML
    private void onDeleteNodeModules(ActionEvent event) {

        String path = androidPathField.getText();

        if (path == null || path.isBlank()) {
            setOperationsStatus("Ruta inválida", "ko");
            return;
        }

        log("[CLEAN] Eliminando node_modules...");
        setOperationsStatus("Eliminando...", "info");

        BuilderAPKService.deleteNodeModules(path, this::log);

        new Thread(() -> {
            File nm = new File(path, "node_modules");

            int attempts = 0;

            while (attempts < 30) {

                if (!nm.exists()) {
                    Platform.runLater(() ->
                        setOperationsStatus("node_modules eliminado", "ok")
                    );
                    return;
                }

                sleep(500);
                attempts++;
            }

            Platform.runLater(() ->
                setOperationsStatus("No eliminado", "ko")
            );

        }).start();
    }

    /*
    ============================================
    NPM INSTALL
    ============================================
     */
    @FXML
    private void onNpmInstall(ActionEvent event) {

        String path = androidPathField.getText();

        if (path == null || path.isBlank()) {
            setOperationsStatus("Ruta inválida", "ko");
            return;
        }

        log("[NPM] Instalando...");
        setOperationsStatus("Instalando...", "info");

        BuilderAPKService.npmInstall(path, this::log);

        new Thread(() -> {
            File nm = new File(path, "node_modules");

            int attempts = 0;

            while (attempts < 120) {

                if (nm.exists()) {
                    Platform.runLater(() ->
                        setOperationsStatus("npm install OK", "ok")
                    );
                    return;
                }

                sleep(1000);
                attempts++;
            }

            Platform.runLater(() ->
                setOperationsStatus("Timeout install", "ko")
            );

        }).start();
    }

    /*
    ============================================
    VALIDACIÓN
    ============================================
     */
    private boolean isValidPath(String path) {

        if (path == null || path.isBlank()) return false;

        File root = new File(path);
        File androidDir = new File(root, "android");
        File gradlew = new File(androidDir, "gradlew.bat");

        return root.exists() && androidDir.exists() && gradlew.exists();
    }

    /*
    ============================================
    STATUS
    ============================================
     */
    private void setOperationsStatus(String text, String type) {
        updateStatus(operationsStatus, text, type);
    }

    private void setBuildStatus(String text, String type) {
        updateStatus(buildStatus, text, type);
    }

    private void updateStatus(Label label, String text, String type) {

        Platform.runLater(() -> {

            label.setText("Estado: " + text);

            label.getStyleClass().removeAll(
                    "status-ok",
                    "status-ko",
                    "status-info"
            );

            switch (type) {
                case "ok" -> label.getStyleClass().add("status-ok");
                case "ko" -> label.getStyleClass().add("status-ko");
                default -> label.getStyleClass().add("status-info");
            }
        });
    }

    /*
    ============================================
    LOG
    ============================================
     */
    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    /*
    ============================================
    COPY LOG
    ============================================
     */
    @FXML
    private void onCopyLog(ActionEvent event) {

        ClipboardContent content = new ClipboardContent();
        content.putString(logArea.getText());

        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void onClearLog(ActionEvent event) {
        logArea.clear();
    }

    /*
    ============================================
    CLOSE
    ============================================
     */
    @FXML
    private void onClose(ActionEvent event) {
        ((Button) event.getSource()).getScene().getWindow().hide();
    }

    /*
    ============================================
    UTILS
    ============================================
     */
    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception ignored) {}
    }
}