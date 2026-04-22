package deploy;

import processDeploy.deployRNservice;
import processDeploy.deployRNservice.RNType;
import configuration.configService;
import utils_ui.directory;

import java.io.File;
import java.net.Socket;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    @FXML private VBox servicesContainer;
    @FXML private HBox cliContainer;
    @FXML private AnchorPane expoBlock;

    @FXML private Button saveButton;
    @FXML private Button unlockButton;
    @FXML private Button browseButton;
    @FXML private Button buildAPKButton;

    private String projectPath;
    private RNType currentType = RNType.UNKNOWN;
    @FXML
    private ImageView metroIcon;
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

        projectTypeCombo.getItems().setAll("AUTO", "EXPO", "CLI");
        projectTypeCombo.setValue("AUTO");

        loadConfig();
    }

    // =========================
    // CONFIG
    // =========================
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
            log("Error cargando configuración");
        }
    }

    private void detectType() {

        if (projectPath == null) return;

        currentType = deployRNservice.detectType(projectPath, this::log);

        Platform.runLater(() -> {
            projectTypeCombo.setValue(currentType.name());
            applyTypeUI();
            updateUI();
        });
    }

    // =========================
    // UI (SIN ROMPER DISEÑO)
    // =========================
    private void applyTypeUI() {

        if (currentType == RNType.EXPO) {

            expoBlock.setVisible(true);
            expoBlock.setManaged(true);

            cliContainer.setVisible(false);
            cliContainer.setManaged(false);

            startAndroidBtn.setText("Start Expo");

            log("Modo EXPO → botón arranca todo");

        } else {

            expoBlock.setVisible(false);
            expoBlock.setManaged(false);

            cliContainer.setVisible(true);
            cliContainer.setManaged(true);

            startAndroidBtn.setText("Run Android");

            log("Modo CLI → Metro opcional");
        }
    }

    // =========================
    // METRO
    // =========================
    @FXML
    private void onStartMetro(ActionEvent e) {

        if (currentType != RNType.CLI) {
            log("Metro no aplica en Expo");
            return;
        }

        if (!validateProject()) return;

        if (isMetroRunning()) {
            log("Metro ya activo");
            return;
        }

        deployRNservice.startMetro(projectPath, this::log);
        updateUI();
    }

    @FXML
    private void onStopMetro(ActionEvent e) {
        deployRNservice.stopMetro(this::log);
        updateUI();
    }

    // =========================
    // ANDROID / EXPO
    // =========================
    @FXML
    private void onStartAndroid(ActionEvent e) {

        if (!validateProject()) return;

        deployRNservice.startAndroid(projectPath, this::log);
        updateUI();
    }

    @FXML
    private void onStopAndroid(ActionEvent e) {
        deployRNservice.stopAndroid(this::log);
        updateUI();
    }

    // =========================
    // DETECTAR METRO REAL
    // =========================
    private boolean isMetroRunning() {
        try (Socket socket = new Socket("localhost", 8081)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================
    // UI STATE
    // =========================
    private void updateUI() {

        Platform.runLater(() -> {

            boolean metro = isMetroRunning();

            if (currentType == RNType.EXPO) {

                metroStatus.setText("AUTO");
                startMetroBtn.setDisable(true);
                stopMetroBtn.setDisable(true);

            } else {

                metroStatus.setText(metro ? "ACTIVO" : "DETENIDO");

                startMetroBtn.setDisable(metro);
                stopMetroBtn.setDisable(!metro);
            }

            androidStatus.setText("DETENIDO"); // simple (no fiable detectar)
        });
    }

    // =========================
    // VALIDACIÓN
    // =========================
    private boolean validateProject() {

        projectPath = projectPathField.getText();

        if (projectPath == null || projectPath.isBlank()
                || !new File(projectPath, "package.json").exists()) {

            log("Proyecto inválido");
            setStatus("Proyecto inválido");
            return false;
        }

        return true;
    }

    // =========================
    // BOTONES BASE
    // =========================
    @FXML
    private void onBrowse(ActionEvent e) {

        if (projectPathField.isDisabled()) return;

        File dir = new directory().show();
        if (dir == null) return;

        projectPath = dir.getAbsolutePath();
        projectPathField.setText(projectPath);

        detectType();
        setStatus("Ruta seleccionada");
    }

    @FXML
    private void onSave(ActionEvent e) {

        String path = projectPathField.getText();

        if (!validateProject()) return;

        try {
            if (configService.setProjectsPath(path)) {
                projectPath = path;
                projectPathField.setDisable(true);
                detectType();
                setStatus("Ruta guardada correctamente");
            }
        } catch (Exception ex) {
            log("Error guardando configuración");
        }
    }

    @FXML
    private void onUnlock(ActionEvent e) {
        projectPathField.setDisable(false);
        setStatus("Campo editable");
    }

    @FXML
    private void onClose(ActionEvent e) {
        ((Stage) root.getScene().getWindow()).close();
    }

    // =========================
    // BUILD
    // =========================
    @FXML
    private void onOpenBuildAPK(ActionEvent e) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/frameworks/deploy/builderRN.fxml")
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setResizable(false);
            stage.show();

            log("[BUILD] Ventana abierta");

        } catch (Exception ex) {
            log("[ERROR] " + ex.getMessage());
        }
    }

    // =========================
    // LOG
    // =========================
    private void setStatus(String text) {
        statusLabel.setText("Estado: " + text);
    }

    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    @FXML
    private void onCopyLog(ActionEvent event) {

        if (logArea.getText().isEmpty()) return;

        ClipboardContent content = new ClipboardContent();
        content.putString(logArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void onClearLog(ActionEvent event) {
        logArea.clear();
    }
}