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

    private String projectPath;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        Platform.runLater(() -> {
            Stage stage = (Stage) root.getScene().getWindow();
            if (stage != null) {
                stage.setResizable(false);
            }
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

    private void detectType() {
        if (projectPath == null) return;

        RNType type = deployRNservice.detectType(projectPath, this::log);
        Platform.runLater(() -> projectTypeCombo.setValue(type.name()));
    }

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

    @FXML
    private void onStartMetro(ActionEvent event) {

        if (!validateProject()) return;

        log("[METRO] Iniciando...");
        deployRNservice.startMetro("metro", projectPath, this::log);
        refreshState();
    }

    @FXML
    private void onStopMetro(ActionEvent event) {
        log("[METRO] Deteniendo procesos...");
        deployRNservice.stopMetro("metro", this::log);
        refreshState();
    }

    @FXML
    private void onStartAndroid(ActionEvent event) {

        if (!validateProject()) return;

        log("[ANDROID] Iniciando...");
        deployRNservice.startAndroid("android", projectPath, this::log);
        refreshState();
    }

    @FXML
    private void onStopAndroid(ActionEvent event) {
        log("[ANDROID] Deteniendo procesos...");
        deployRNservice.stopAndroid("android", this::log);
        refreshState();
    }

    /*
    ============================================
    OPEN BUILD WINDOW (NO MODAL)
    ============================================
    */
    @FXML
    private void onOpenBuildAPK(ActionEvent event) {
        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/frameworks/deploy/builderRN.fxml")
            );

            AnchorPane pane = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Build Android APK");

            Scene scene = new Scene(pane);
            stage.setScene(scene);

            // MISMO patrón que DeployGlobalController
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.sizeToScene();

            stage.show();

            log("[BUILD] Ventana abierta");

        } catch (Exception e) {
            log("[ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

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
        return path != null
                && !path.isBlank()
                && new File(path, "package.json").exists();
    }

    private void refreshState() {

        Platform.runLater(() -> {

            boolean metro = deployRNservice.isRunning("metro");
            boolean android = deployRNservice.isRunning("android");

            setMetroStatus(metro);
            setAndroidStatus(android);
        });
    }

    private void setMetroStatus(boolean running) {
        metroStatus.setText(running ? "ACTIVO" : "DETENIDO");
        startMetroBtn.setDisable(running);
        stopMetroBtn.setDisable(!running);
    }

    private void setAndroidStatus(boolean running) {
        androidStatus.setText(running ? "ACTIVO" : "DETENIDO");
        startAndroidBtn.setDisable(running);
        stopAndroidBtn.setDisable(!running);
    }

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