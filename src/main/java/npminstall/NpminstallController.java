package npminstall;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import utils_ui.directory;

public class NpminstallController implements Initializable {

    @FXML
    private AnchorPane root;

    @FXML
    private TextField projectPathField;
    @FXML
    private TextField parameterField;
    @FXML
    private TextField removeField;
    @FXML
    private TextField updateField;

    @FXML
    private ComboBox<String> packageManagerCombo;

    @FXML
    private Button browseProjectButton;
    @FXML
    private Button installButton;
    @FXML
    private Button listDependenciesButton;
    @FXML
    private Button removeButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button savePathButton;
    @FXML
    private Button unlockPathButton;

    @FXML
    private Button btnCerrar;
    @FXML
    private Button copyConsoleButton;
    @FXML
    private Button clearConsoleButton;

    @FXML
    private TextArea console;
    @FXML
    private Label processStatusLabel;

    private final npmService npmService = new npmService();
    private final directory dirSelector = new directory();

    private boolean busy = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // =========================
        // BLOQUEO DE VENTANA
        // =========================
        Platform.runLater(() -> {
            Stage stage = (Stage) root.getScene().getWindow();

            if (stage != null) {
                stage.setResizable(false);
                stage.setMaximized(false);
            }
        });

        setStatus("No iniciado", "warning");

        packageManagerCombo.getItems().addAll("npm", "yarn", "pnpm", "bun");
        packageManagerCombo.setValue("npm");

        parameterField.setText("install");

        unlockPathButton.setDisable(true);

        if (npmService.getProjectPath() != null) {
            projectPathField.setText(npmService.getProjectPath());
            lockPath();
        }

        Platform.runLater(() -> root.requestFocus());
    }

    // ------------------------------------------------
    private void setStatus(String message, String type) {

        processStatusLabel.setText(message);

        processStatusLabel.getStyleClass().removeAll(
                "estado-warning", "estado-ok", "estado-error"
        );

        switch (type) {
            case "ok":
                processStatusLabel.getStyleClass().add("estado-ok");
                break;
            case "error":
                processStatusLabel.getStyleClass().add("estado-error");
                break;
            default:
                processStatusLabel.getStyleClass().add("estado-warning");
        }
    }

    private void lockPath() {

        projectPathField.setDisable(true);
        browseProjectButton.setDisable(true);

        savePathButton.setDisable(true);
        unlockPathButton.setDisable(false);

        setStatus("Ruta bloqueada", "ok");
    }

    @FXML
    private void desbloquearRuta(ActionEvent event) {

        projectPathField.setDisable(false);
        browseProjectButton.setDisable(false);

        savePathButton.setDisable(false);
        unlockPathButton.setDisable(true);

        setStatus("Ruta desbloqueada", "warning");

        Platform.runLater(() -> root.requestFocus());
    }

    // ------------------------------------------------
    private boolean validatePath() {

        String path = projectPathField.getText();

        if (path == null || path.isBlank()) {
            setStatus("Ruta no configurada", "error");
            return false;
        }

        File dir = new File(path);

        if (!dir.exists()) {
            setStatus("Ruta no existe", "error");
            return false;
        }

        npmService.setProjectPath(path);
        return true;
    }

    private void setBusy(boolean state) {

        busy = state;

        installButton.setDisable(state);
        listDependenciesButton.setDisable(state);
        removeButton.setDisable(state);
        updateButton.setDisable(state);
    }

    // ------------------------------------------------
    @FXML
    private void buscarProyecto(ActionEvent event) {

        File selected = dirSelector.show();

        if (selected != null) {
            projectPathField.setText(selected.getAbsolutePath());
            setStatus("Ruta seleccionada", "ok");
        }
    }

    @FXML
    private void guardarRutaProyecto(ActionEvent event) {

        String path = projectPathField.getText();

        if (path == null || path.isBlank()) {
            setStatus("Ruta vacía", "error");
            return;
        }

        npmService.saveProjectPath(path, log
                -> Platform.runLater(() -> {
                    setStatus(log, "ok");
                    lockPath();
                })
        );
    }

    // ------------------------------------------------
    @FXML
    private void instalarDependencias(ActionEvent event) {

        if (busy || !validatePath()) {
            return;
        }

        setBusy(true);
        setStatus("Instalando...", "warning");

        npmService.runInstall(
                packageManagerCombo.getValue(),
                parameterField.getText(),
                this::log
        );
    }

    @FXML
    private void listarDependencias(ActionEvent event) {

        if (busy || !validatePath()) {
            return;
        }

        setBusy(true);
        console.clear();

        setStatus("Listando dependencias...", "warning");

        npmService.listDependencies(text -> {
            log(text);
        });

        // 🔥 liberar manual (porque es lectura rápida)
        Platform.runLater(() -> {
            setBusy(false);
            setStatus("Dependencias listadas", "ok");
        });
    }

    @FXML
    private void eliminarDependencia(ActionEvent event) {

        if (busy || !validatePath()) {
            return;
        }

        String dep = removeField.getText();

        if (dep == null || dep.isBlank()) {
            setStatus("Dependencia vacía", "error");
            return;
        }

        setBusy(true);
        setStatus("Eliminando...", "warning");

        npmService.removeDependency(
                packageManagerCombo.getValue(),
                dep,
                this::log
        );
    }

    @FXML
    private void actualizarDependencia(ActionEvent event) {

        if (busy || !validatePath()) {
            return;
        }

        String dep = updateField.getText();

        if (dep == null || dep.isBlank()) {
            setStatus("Dependencia vacía", "error");
            return;
        }

        setBusy(true);
        setStatus("Actualizando...", "warning");

        npmService.updateDependency(
                packageManagerCombo.getValue(),
                dep,
                this::log
        );
    }

    // ------------------------------------------------
    private void log(String text) {

        Platform.runLater(() -> {

            if (console.getText().length() > 60000) {
                console.clear();
            }

            console.appendText(text + "\n");

            String lower = text.toLowerCase();

            if (lower.contains("✖") || lower.contains("error")) {
                setStatus("Error en proceso", "error");
                setBusy(false);

            } else if (lower.contains("✔")) {
                setStatus("Proceso completado", "ok");
                setBusy(false);
            }
        });
    }

    // ------------------------------------------------
    @FXML
    private void copiarLogs(ActionEvent event) {

        console.selectAll();
        console.copy();

        setStatus("Logs copiados", "ok");
    }

    @FXML
    private void limpiarLogs(ActionEvent event) {

        console.clear();
        setStatus("Consola limpiada", "warning");
    }

    // ------------------------------------------------
    @FXML
    private void actionBtnCerrar(ActionEvent event) {

        npmService.stop();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
