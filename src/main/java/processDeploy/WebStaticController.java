package processDeploy;

import configuration.configService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class WebStaticController implements Initializable {

    @FXML
    private TextField backendPathField;
    @FXML
    private ComboBox<String> backendTypeCombo;
    @FXML
    private TextField backendCommandField;

    @FXML
    private TextField frontendPathField;
    @FXML
    private ComboBox<String> frontendTypeCombo;
    @FXML
    private TextField frontendCommandField;

    @FXML
    private TextArea backendLogArea;
    @FXML
    private TextArea frontendLogArea;

    @FXML
    private Label configStatusLabel;

    @FXML
    private Button backendStartBtn;
    @FXML
    private Button backendStopBtn;
    @FXML
    private Button backendBrowserBtn;
    @FXML
    private Button backendBuildBtn;

    @FXML
    private Button frontendStartBtn;
    @FXML
    private Button frontendStopBtn;
    @FXML
    private Button frontendBrowserBtn;
    @FXML
    private Button frontendBuildBtn;

    @FXML
    private Button backendCopyBtn;
    @FXML
    private Button backendClearBtn;
    @FXML
    private Button frontendCopyBtn;
    @FXML
    private Button frontendClearBtn;

    @FXML
    private Button closeButton;
    @FXML
    private Button configButton;

    private boolean backendRunning = false;
    private boolean frontendRunning = false;

    private String backendFramework;
    private String frontendFramework;

    // =========================
    // INIT
    // =========================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        initConfig();

        backendPathField.textProperty().addListener((obs, o, n) -> detectBackend());
        frontendPathField.textProperty().addListener((obs, o, n) -> detectFrontend());

        detectBackend();
        detectFrontend();

        resetBackendButtons();
        resetFrontendButtons();

        backendBrowserBtn.setDisable(true);
        frontendBrowserBtn.setDisable(true);
    }

    // =========================
    // CONFIG
    // =========================
    private void initConfig() {

        String backendPath = configService.getBackendPath();
        String frontendPath = configService.getFrontendPath();

        if (backendPath != null && !backendPath.isBlank()) {
            backendPathField.setText(backendPath);
            backendPathField.setDisable(true);
        }

        if (frontendPath != null && !frontendPath.isBlank()) {
            frontendPathField.setText(frontendPath);
            frontendPathField.setDisable(true);
        }

        setStatus("Configurado", "ok");
    }

    // =========================
    // DETECCIÓN
    // =========================
    private void detectBackend() {

        String path = backendPathField.getText();
        if (path == null || path.isBlank()) {
            return;
        }

        List<String> options = listBackFront.getBackendOptions(path);

        Platform.runLater(() -> {
            backendTypeCombo.getItems().setAll(options);

            if (!options.isEmpty()) {
                backendFramework = options.get(0);
                backendTypeCombo.setValue(backendFramework);
            }
        });

        logBackend("[AUTO] Backend detectado: " + options);
    }

    private void detectFrontend() {

        String path = frontendPathField.getText();
        if (path == null || path.isBlank()) {
            return;
        }

        List<String> options = listBackFront.getFrontendOptions(path);

        Platform.runLater(() -> {
            frontendTypeCombo.getItems().setAll(options);

            if (!options.isEmpty()) {
                frontendFramework = options.get(0);
                frontendTypeCombo.setValue(frontendFramework);
            }
        });

        logFrontend("[AUTO] Frontend detectado: " + options);
    }

    // =========================
    // BACKEND
    // =========================
    @FXML
    private void onStartBackend(ActionEvent event) {

        if (backendRunning) {
            logBackend("[INFO] Backend ya está en ejecución");
            return;
        }

        String path = backendPathField.getText();
        backendFramework = backendTypeCombo.getValue();
        String command = backendCommandField.getText();

        if (path == null || path.isBlank()) {
            logBackend("[ERROR] Ruta backend inválida");
            return;
        }

        backendRunning = true;
        updateBackendButtons();
        backendBrowserBtn.setDisable(false);

        deployService.start("backend", path, backendFramework, command, this::handleBackendLog);
    }

    @FXML
    private void onStopBackend(ActionEvent event) {

        if (!backendRunning) {
            return;
        }

        deployService.stop("backend", this::handleBackendLog);

        backendRunning = false;
        updateBackendButtons();
        backendBrowserBtn.setDisable(true);
    }

    @FXML
    private void onBuildBackend(ActionEvent event) {

        String path = backendPathField.getText();
        backendFramework = backendTypeCombo.getValue();

        deployService.build("backend", path, backendFramework, this::handleBackendLog);
    }

    // =========================
    // FRONTEND
    // =========================
    @FXML
    private void onStartFrontend(ActionEvent event) {

        if (frontendRunning) {
            logFrontend("[INFO] Frontend ya está en ejecución");
            return;
        }

        String path = frontendPathField.getText();
        frontendFramework = frontendTypeCombo.getValue();
        String command = frontendCommandField.getText();

        if (path == null || path.isBlank()) {
            logFrontend("[ERROR] Ruta frontend inválida");
            return;
        }

        frontendRunning = true;
        updateFrontendButtons();
        frontendBrowserBtn.setDisable(false);

        deployService.start("frontend", path, frontendFramework, command, this::handleFrontendLog);
    }

    @FXML
    private void onStopFrontend(ActionEvent event) {

        if (!frontendRunning) {
            return;
        }

        deployService.stop("frontend", this::handleFrontendLog);

        frontendRunning = false;
        updateFrontendButtons();
        frontendBrowserBtn.setDisable(true);
    }

    @FXML
    private void onBuildFrontend(ActionEvent event) {

        String path = frontendPathField.getText();
        frontendFramework = frontendTypeCombo.getValue();

        deployService.build("frontend", path, frontendFramework, this::handleFrontendLog);
    }

    // =========================
    // BROWSER
    // =========================
    @FXML
    private void onOpenBackendBrowser(ActionEvent event) {

        if (deployService.isRunning("backend") && backendFramework != null) {
            deployService.openBrowser(backendFramework, this::logBackend);
        } else {
            logBackend("[INFO] Backend no activo");
        }
    }

    @FXML
    private void onOpenFrontendBrowser(ActionEvent event) {

        if (deployService.isRunning("frontend") && frontendFramework != null) {
            deployService.openBrowser(frontendFramework, this::logFrontend);
        } else {
            logFrontend("[INFO] Frontend no activo");
        }
    }

    // =========================
    // BOTONES
    // =========================
    private void updateBackendButtons() {

        backendStartBtn.setDisable(backendRunning);
        backendStopBtn.setDisable(!backendRunning);

        backendStopBtn.getStyleClass().setAll(
                "button",
                backendRunning ? "button-stop-active" : "button-clear"
        );
    }

    private void updateFrontendButtons() {

        frontendStartBtn.setDisable(frontendRunning);
        frontendStopBtn.setDisable(!frontendRunning);

        frontendStopBtn.getStyleClass().setAll(
                "button",
                frontendRunning ? "button-stop-active" : "button-clear"
        );
    }

    private void resetBackendButtons() {
        backendRunning = false;
        updateBackendButtons();
    }

    private void resetFrontendButtons() {
        frontendRunning = false;
        updateFrontendButtons();
    }

    // =========================
    // LOG
    // =========================
    private void handleBackendLog(String msg) {
        Platform.runLater(() -> backendLogArea.appendText(msg + "\n"));
    }

    private void handleFrontendLog(String msg) {
        Platform.runLater(() -> frontendLogArea.appendText(msg + "\n"));
    }

    private void logBackend(String msg) {
        Platform.runLater(() -> backendLogArea.appendText(msg + "\n"));
    }

    private void logFrontend(String msg) {
        Platform.runLater(() -> frontendLogArea.appendText(msg + "\n"));
    }

    // =========================
    // CLIPBOARD
    // =========================
    @FXML
    private void onCopyBackendLog(ActionEvent event) {

        ClipboardContent content = new ClipboardContent();
        content.putString(backendLogArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void onClearBackendLog(ActionEvent event) {
        backendLogArea.clear();
    }

    @FXML
    private void onCopyFrontendLog(ActionEvent event) {

        ClipboardContent content = new ClipboardContent();
        content.putString(frontendLogArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void onClearFrontendLog(ActionEvent event) {
        frontendLogArea.clear();
    }

    // =========================
    // STATUS
    // =========================
    private void setStatus(String text, String type) {

        configStatusLabel.setText(text);
        configStatusLabel.getStyleClass().removeAll("status-ok", "status-ko", "status-info");

        switch (type) {
            case "ok" ->
                configStatusLabel.getStyleClass().add("status-ok");
            case "ko" ->
                configStatusLabel.getStyleClass().add("status-ko");
            default ->
                configStatusLabel.getStyleClass().add("status-info");
        }
    }

    // =========================
    // VENTANA
    // =========================
    @FXML
    private void onCloseWindow(ActionEvent event) {
        ((Stage) closeButton.getScene().getWindow()).close();
    }

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
            stage.initOwner(closeButton.getScene().getWindow());

            // =========================
            // BLOQUEO GLOBAL DE TAMAÑO
            // =========================
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.sizeToScene();

            stage.showAndWait();

            initConfig();
            detectBackend();
            detectFrontend();

        } catch (Exception e) {
            logBackend("[ERROR] Config: " + e.getMessage());
        }
    }
}
