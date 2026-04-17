package creator;

import processCreate.creatorGlobal;
import processCreate.framework;
import configuration.configService;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import utils_ui.directory;

public class CreatorGlobalController extends creatorGlobal implements Initializable {

    @FXML private AnchorPane creatorRoot;
    @FXML private TextField projectNameField;
    @FXML private TextField basePathField;
    @FXML private ComboBox<String> frameworkCombo;
    @FXML private ComboBox<String> versionCombo;
    @FXML private TextArea logArea;
    @FXML private Label configStatusLabel;
    @FXML private Button configButton;
    @FXML private Button closeBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        frameworkCombo.getItems().setAll(framework.WEB);

        versionCombo.setDisable(true);
        versionCombo.setPromptText("Selecciona framework");

        frameworkCombo.setOnAction(e -> onFrameworkSelected());

        initBasePath();
    }

    // =========================
    // FRAMEWORK SELECT
    // =========================
    private void onFrameworkSelected() {

        String fw = frameworkCombo.getValue();

        versionCombo.getItems().clear();
        versionCombo.setDisable(true);

        if (fw == null) return;

        logUI("Cargando versiones...");

        // 🔥 IMPORTANTE: no usar this::log
        loadVersions(
                fw,
                this::logInternal, // 👈 debug silencioso
                versions -> Platform.runLater(() -> applyVersions(versions))
        );
    }

    // =========================
    // APPLY VERSIONS
    // =========================
    private void applyVersions(List<String> versions) {

        versionCombo.getItems().clear();

        if (versions == null || versions.isEmpty()) {
            logUI("No se pudieron cargar versiones");
            versionCombo.setPromptText("Error");
            return;
        }

        if (versions.size() == 1 && versions.get(0).equalsIgnoreCase("latest")) {
            logUI("Error cargando versiones");
            versionCombo.setPromptText("Error");
            return;
        }

        versionCombo.getItems().setAll(versions);
        versionCombo.getSelectionModel().selectFirst();
        versionCombo.setDisable(false);

        logUI("Versiones cargadas correctamente");
    }

    // =========================
    // RUN COMMAND
    // =========================
    @FXML
    private void onRunCommand(ActionEvent event) {

        String fw = frameworkCombo.getValue();
        String version = versionCombo.getValue();
        String name = projectNameField.getText();
        String path = basePathField.getText();

        if (fw == null) {
            logUI("Selecciona un framework.");
            return;
        }

        if (name == null || name.isBlank()) {
            logUI("Introduce nombre del proyecto.");
            return;
        }

        if (path == null || path.isBlank()) {
            setStatus("Configura la ruta primero", "ko");
            return;
        }

        if (version == null || version.isBlank()) {
            version = "latest";
        }

        logUI("Creando proyecto...");

        createProject(fw, version, name, path, this::logUI);
    }

    // =========================
    // BASE PATH
    // =========================
    private void initBasePath() {

        String path = configService.getProjectsPath();

        if (path != null && !path.isBlank()) {

            basePathField.setText(path);
            basePathField.setDisable(true);

            setStatus("Configurado", "ok");
            lockConfigButton();

        } else {

            basePathField.clear();
            basePathField.setDisable(false);

            setStatus("Sin configurar", "info");
            unlockConfigButton();
        }
    }

    @FXML
    private void onBrowseBasePath(ActionEvent event) {

        if (basePathField.isDisabled()) return;

        File dir = new directory().show();
        if (dir == null) return;

        basePathField.setText(dir.getAbsolutePath());
        setStatus("Ruta seleccionada", "info");
    }

    @FXML
    private void onSaveBasePath(ActionEvent event) {

        String path = basePathField.getText();

        if (path == null || path.isBlank()) {
            setStatus("Ruta vacía", "ko");
            return;
        }

        boolean ok = configService.setProjectsPath(path);

        if (ok) {
            basePathField.setDisable(true);
            setStatus("Guardado correctamente", "ok");
            lockConfigButton();
        } else {
            setStatus("Ruta inválida o sin permisos", "ko");
        }
    }

    // =========================
    // CONFIG WINDOW
    // =========================
    @FXML
    private void onOpenConfig(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/configuration/config.fxml")
            );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(creatorRoot.getScene().getWindow());

            stage.setResizable(false);
            stage.setMaximized(false);
            stage.sizeToScene();

            stage.showAndWait();

            initBasePath();

        } catch (Exception e) {
            logUI("Error configuración: " + e.getMessage());
        }
    }

    // =========================
    // UI
    // =========================
    private void lockConfigButton() {
        configButton.setDisable(false);
        configButton.getStyleClass().removeAll("button-primary");
        configButton.getStyleClass().add("button-create");
    }

    private void unlockConfigButton() {
        configButton.setDisable(false);
        configButton.getStyleClass().remove("button-create");
        configButton.getStyleClass().add("button-primary");
    }

    private void setStatus(String text, String type) {

        configStatusLabel.setText(text);
        configStatusLabel.getStyleClass().removeAll("status-ok", "status-ko", "status-info");

        switch (type) {
            case "ok" -> configStatusLabel.getStyleClass().add("status-ok");
            case "ko" -> configStatusLabel.getStyleClass().add("status-ko");
            default -> configStatusLabel.getStyleClass().add("status-info");
        }
    }

    // =========================
    // LOGS
    // =========================

    // 👉 SOLO UI
    private void logUI(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    // 👉 DEBUG INTERNO (NO UI)
    private void logInternal(String msg) {
        System.out.println("[DEBUG] " + msg);
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
    private void onClearFields(ActionEvent event) {

        frameworkCombo.getSelectionModel().clearSelection();

        versionCombo.getItems().clear();
        versionCombo.setDisable(true);
        versionCombo.setPromptText("Selecciona framework");

        projectNameField.clear();
    }

    @FXML
    private void onClose(ActionEvent e) {
        ((Stage) creatorRoot.getScene().getWindow()).close();
    }
}