package deploy;

import processDeploy.deployRNservice;
import processDeploy.deployRNservice.RNType;
import configuration.configService;
import utils_ui.directory;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class ReactNativeController implements Initializable {

    @FXML private AnchorPane root;

    @FXML private TextField projectPathField;
    @FXML private Label statusLabel;

    @FXML private Label metroStatus;
    @FXML private Button startMetroBtn;
    @FXML private Button stopMetroBtn;

    @FXML private Label androidStatus;
    @FXML private Button startAndroidBtn;
    @FXML private Button stopAndroidBtn;

    @FXML private TextArea logArea;
    @FXML private ComboBox<String> projectTypeCombo;

    @FXML private Button browseButton;
    @FXML private Button saveButton;
    @FXML private Button unlockButton;
    @FXML private Button buildAPKButton;

    // 🔥 BLOQUES UI
    @FXML private AnchorPane metroBlock;
    @FXML private AnchorPane androidBlock;
    @FXML private AnchorPane expoBlock;

    private String projectPath;
    private RNType currentType = RNType.UNKNOWN;
    @FXML
    private ImageView metroIcon;
    @FXML
    private Label androidTitle;
    @FXML
    private ImageView androidIcon;

    // =========================
    // INIT
    // =========================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Platform.runLater(() -> {
            Stage stage = (Stage) root.getScene().getWindow();
            if (stage != null) stage.setResizable(false);
        });

        initCombo();
        loadConfig();
        refreshState();
    }

    private void initCombo() {
        projectTypeCombo.getItems().setAll("AUTO", "EXPO", "CLI");
        projectTypeCombo.setValue("AUTO");
    }

    private void loadConfig() {

        try {

            String path = configService.getProjectsPath();

            if (path != null && !path.isBlank()) {

                projectPath = path;
                projectPathField.setText(path);
                projectPathField.setDisable(true);

                detectType();
                setStatus("Configurado correctamente");

            } else {
                setStatus("Sin configurar");
            }

        } catch (Exception e) {
            setStatus("Error cargando configuración");
            log("Error cargando configuración");
        }
    }

    // =========================
    // DETECT TYPE
    // =========================
    private void detectType() {

        if (projectPath == null) return;

        currentType = deployRNservice.detectType(projectPath, this::log);

        Platform.runLater(() -> {
            projectTypeCombo.setValue(currentType.name());
            applyTypeUI();
        });
    }

    // =========================
    // UI SEGÚN TIPO (LIMPIO)
    // =========================
    private void applyTypeUI() {

        if (currentType == RNType.EXPO) {

            // 🔥 SOLO BLOQUE EXPO
            metroBlock.setVisible(false);
            metroBlock.setManaged(false);

            androidBlock.setVisible(false);
            androidBlock.setManaged(false);

            expoBlock.setVisible(true);
            expoBlock.setManaged(true);

            log("Modo EXPO → Metro + Android unificados");

        } else {

            // 🔥 CLI NORMAL
            metroBlock.setVisible(true);
            metroBlock.setManaged(true);

            androidBlock.setVisible(true);
            androidBlock.setManaged(true);

            expoBlock.setVisible(false);
            expoBlock.setManaged(false);

            log("Modo CLI → Metro separado");
        }
    }

    // =========================
    // BROWSE
    // =========================
    @FXML
    private void onBrowse(ActionEvent event) {

        if (projectPathField.isDisabled()) return;

        File dir = new directory().show();
        if (dir == null) return;

        projectPath = dir.getAbsolutePath();
        projectPathField.setText(projectPath);

        detectType();
        setStatus("Ruta seleccionada");
    }

    // =========================
    // SAVE
    // =========================
    @FXML
    private void onSave(ActionEvent event) {

        String path = projectPathField.getText();

        if (!isValidProject(path)) {
            setStatus("Proyecto no válido");
            log("Falta package.json");
            return;
        }

        try {

            boolean ok = configService.setProjectsPath(path);

            if (ok) {

                projectPath = path;
                projectPathField.setDisable(true);

                detectType();
                setStatus("Ruta guardada correctamente");

            } else {
                setStatus("No se pudo guardar");
            }

        } catch (Exception e) {
            setStatus("Error guardando");
            log("Error guardando configuración");
        }
    }

    @FXML
    private void onUnlock(ActionEvent event) {
        projectPathField.setDisable(false);
        setStatus("Campo editable");
    }

    // =========================
    // METRO (solo CLI)
    // =========================
    @FXML
    private void onStartMetro(ActionEvent event) {

        if (currentType == RNType.EXPO) {
            log("Expo usa Metro automáticamente");
            return;
        }

        if (!validateProject()) return;

        log("[METRO] Iniciando...");
        deployRNservice.startMetro("metro", projectPath, this::log);

        setMetroStatus(true);
    }

    @FXML
    private void onStopMetro(ActionEvent event) {

        if (currentType == RNType.EXPO) return;

        log("[METRO] Deteniendo...");
        deployRNservice.stopMetro("metro", this::log);

        setMetroStatus(false);
    }

    // =========================
    // ANDROID
    // =========================
    @FXML
    private void onStartAndroid(ActionEvent event) {

        if (!validateProject()) return;

        log("[ANDROID] Iniciando...");

        if (currentType == RNType.EXPO) {

            // 🔥 UN SOLO COMANDO
            deployRNservice.startAndroid("android", projectPath, this::log);

        } else {

            // CLI → primero metro
            deployRNservice.startMetro("metro", projectPath, this::log);
            deployRNservice.startAndroid("android", projectPath, this::log);
        }

        setAndroidStatus(true);
    }

    @FXML
    private void onStopAndroid(ActionEvent event) {

        log("[ANDROID] Deteniendo...");
        deployRNservice.stopAndroid("android", this::log);

        setAndroidStatus(false);
    }

    // =========================
    // BUILD APK
    // =========================
    @FXML
    private void onOpenBuildAPK(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/frameworks/deploy/builderRN.fxml")
            );

            AnchorPane pane = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Build Android APK");

            stage.setScene(new Scene(pane));
            stage.setResizable(false);
            stage.sizeToScene();

            stage.show();

            log("[BUILD] Ventana abierta");

        } catch (Exception e) {
            log("[ERROR] " + e.getMessage());
        }
    }

    // =========================
    // VALIDACIONES
    // =========================
    private boolean validateProject() {

        projectPath = projectPathField.getText();

        if (!isValidProject(projectPath)) {
            log("Proyecto inválido");
            setStatus("Proyecto inválido");
            return false;
        }

        return true;
    }

    private boolean isValidProject(String path) {
        return path != null &&
               !path.isBlank() &&
               new File(path, "package.json").exists();
    }

    // =========================
    // STATE
    // =========================
    private void refreshState() {

        Platform.runLater(() -> {

            boolean metro = deployRNservice.isRunning("metro");
            boolean android = deployRNservice.isRunning("android");

            setMetroStatus(metro);
            setAndroidStatus(android);
        });
    }

    private void setMetroStatus(boolean running) {

        if (currentType == RNType.EXPO) {
            metroStatus.setText("AUTO");
            return;
        }

        metroStatus.setText(running ? "ACTIVO" : "DETENIDO");
        startMetroBtn.setDisable(running);
        stopMetroBtn.setDisable(!running);
    }

    private void setAndroidStatus(boolean running) {
        androidStatus.setText(running ? "ACTIVO" : "DETENIDO");
        startAndroidBtn.setDisable(running);
        stopAndroidBtn.setDisable(!running);
    }

    // =========================
    // UI
    // =========================
    private void setStatus(String text) {
        statusLabel.setText("Estado: " + text);
    }

    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

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

    @FXML
    private void onClose(ActionEvent event) {
        ((Stage) root.getScene().getWindow()).close();
    }
}