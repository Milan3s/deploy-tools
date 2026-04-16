package configuration;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

import utils_ui.directory;

public class ConfigController implements Initializable {

    @FXML private AnchorPane configRoot;

    @FXML private TextField configPathField;
    @FXML private Label configStatusLabel;
    @FXML private Button configSaveBtn;
    @FXML private Button configUnlockBtn;

    @FXML private TextField projectsPathField;
    @FXML private Label projectsStatusLabel;
    @FXML private Button projectsSaveBtn;
    @FXML private Button projectsUnlockBtn;

    @FXML private TextField backendPathField;
    @FXML private Label backendStatusLabel;
    @FXML private Button backendSaveBtn;
    @FXML private Button backendUnlockBtn;

    @FXML private TextField frontendPathField;
    @FXML private Label frontendStatusLabel;
    @FXML private Button frontendSaveBtn;
    @FXML private Button frontendUnlockBtn;

    @FXML private TextField gitPathField;
    @FXML private TextField gitUserField;
    @FXML private TextField gitEmailField;
    @FXML private Label gitStatusLabel;
    @FXML private Button gitSaveBtn;
    @FXML private Button gitUnlockBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        configService.ConfigData data = configService.loadConfig();

        initPathBlock(configPathField, configStatusLabel, configSaveBtn, configUnlockBtn,
                data.configPath, "Config");

        initPathBlock(projectsPathField, projectsStatusLabel, projectsSaveBtn, projectsUnlockBtn,
                data.projectsPath, "Projects");

        initPathBlock(backendPathField, backendStatusLabel, backendSaveBtn, backendUnlockBtn,
                data.backendPath, "Backend");

        initPathBlock(frontendPathField, frontendStatusLabel, frontendSaveBtn, frontendUnlockBtn,
                data.frontendPath, "Frontend");

        initGit(data);
    }

    /*
    ============================================
    INIT
    ============================================
     */
    private void initPathBlock(TextField field, Label label,
                              Button saveBtn, Button unlockBtn,
                              String value, String name) {

        if (value != null && !value.isBlank()) {
            field.setText(value);
            lockField(field, saveBtn, unlockBtn);
            setStatus(label, name + " configurado", "ok");
        } else {
            unlockField(field, saveBtn, unlockBtn);
            setStatus(label, "Sin configurar", "info");
        }
    }

    private void initGit(configService.ConfigData data) {

        gitPathField.setText(nullSafe(data.gitPath));
        gitUserField.setText(nullSafe(data.gitUser));
        gitEmailField.setText(nullSafe(data.gitEmail));

        boolean valid = !gitPathField.getText().isBlank()
                && !gitUserField.getText().isBlank()
                && !gitEmailField.getText().isBlank();

        if (valid) {
            lockField(gitPathField, gitSaveBtn, gitUnlockBtn);
            lockGitConfig();
            setStatus(gitStatusLabel, "Git configurado", "ok");
        } else {
            unlockField(gitPathField, gitSaveBtn, gitUnlockBtn);
            unlockGitConfig();
            setStatus(gitStatusLabel, "Git incompleto", "info");
        }
    }

    private String nullSafe(String v) {
        return v != null ? v : "";
    }

    /*
    ============================================
    LOCK / UNLOCK
    ============================================
     */
    private void lockField(TextField field, Button saveBtn, Button unlockBtn) {
        field.setDisable(true);
        saveBtn.setDisable(true);
        unlockBtn.setDisable(false);
    }

    private void unlockField(TextField field, Button saveBtn, Button unlockBtn) {
        field.setDisable(false);
        saveBtn.setDisable(false);
        unlockBtn.setDisable(true);
    }

    private void lockGitConfig() {
        gitUserField.setDisable(true);
        gitEmailField.setDisable(true);
    }

    private void unlockGitConfig() {
        gitUserField.setDisable(false);
        gitEmailField.setDisable(false);
    }

    /*
    ============================================
    STATUS
    ============================================
     */
    private void setStatus(Label label, String text, String type) {

        label.setText(text);
        label.getStyleClass().removeAll("status-ok", "status-ko", "status-info");

        switch (type) {
            case "ok" -> label.getStyleClass().add("status-ok");
            case "ko" -> label.getStyleClass().add("status-ko");
            default -> label.getStyleClass().add("status-info");
        }
    }

    /*
    ============================================
    BROWSE
    ============================================
     */
    private void browse(TextField field, Label label) {

        File dir = new directory().show();
        if (dir == null) return;

        field.setText(dir.getAbsolutePath());
        setStatus(label, "Ruta seleccionada", "info");
    }

    /*
    ============================================
    SAVE
    ============================================
     */
    private void savePath(TextField field, Label label,
                          Button saveBtn, Button unlockBtn,
                          SaveAction action) {

        String path = field.getText();

        if (path == null || path.isBlank()) {
            setStatus(label, "Ruta vacía", "ko");
            return;
        }

        boolean ok = action.save(path);

        if (ok) {
            lockField(field, saveBtn, unlockBtn);
            setStatus(label, "Guardado correctamente", "ok");
        } else {
            setStatus(label, "Ruta inválida o sin permisos", "ko");
        }
    }

    /*
    ============================================
    CLEAR (🔥 NUEVO)
    ============================================
     */
    @FXML private void actionClearConfigField(ActionEvent e) { clearField(configPathField, configStatusLabel); }
    @FXML private void actionClearProjectsField(ActionEvent e) { clearField(projectsPathField, projectsStatusLabel); }
    @FXML private void actionClearBackendField(ActionEvent e) { clearField(backendPathField, backendStatusLabel); }
    @FXML private void actionClearFrontendField(ActionEvent e) { clearField(frontendPathField, frontendStatusLabel); }
    @FXML private void actionClearGitPath(ActionEvent e) { clearField(gitPathField, gitStatusLabel); }
    @FXML private void actionClearGitUser(ActionEvent e) { gitUserField.clear(); }
    @FXML private void actionClearGitEmail(ActionEvent e) { gitEmailField.clear(); }

    private void clearField(TextField field, Label label) {
        field.clear();
        field.setDisable(false);
        setStatus(label, "Campo limpiado", "info");
    }

    /*
    ============================================
    RESET GLOBAL (🔥 PRO)
    ============================================
     */
    @FXML
    private void actionResetAll(ActionEvent event) {

        clearField(configPathField, configStatusLabel);
        clearField(projectsPathField, projectsStatusLabel);
        clearField(backendPathField, backendStatusLabel);
        clearField(frontendPathField, frontendStatusLabel);
        clearField(gitPathField, gitStatusLabel);

        gitUserField.clear();
        gitEmailField.clear();

        unlockField(configPathField, configSaveBtn, configUnlockBtn);
        unlockField(projectsPathField, projectsSaveBtn, projectsUnlockBtn);
        unlockField(backendPathField, backendSaveBtn, backendUnlockBtn);
        unlockField(frontendPathField, frontendSaveBtn, frontendUnlockBtn);
        unlockField(gitPathField, gitSaveBtn, gitUnlockBtn);

        unlockGitConfig();

        setStatus(configStatusLabel, "Reset realizado", "info");
        setStatus(projectsStatusLabel, "Reset realizado", "info");
        setStatus(backendStatusLabel, "Reset realizado", "info");
        setStatus(frontendStatusLabel, "Reset realizado", "info");
        setStatus(gitStatusLabel, "Reset realizado", "info");
    }

    /*
    ============================================
    ACTIONS (igual que antes)
    ============================================
     */
    @FXML private void actionBrowseConfig() { browse(configPathField, configStatusLabel); }
    @FXML private void actionBrowseProjects() { browse(projectsPathField, projectsStatusLabel); }
    @FXML private void actionBrowseBackend() { browse(backendPathField, backendStatusLabel); }
    @FXML private void actionBrowseFrontend() { browse(frontendPathField, frontendStatusLabel); }

    @FXML private void actionSaveConfigPath() { savePath(configPathField, configStatusLabel, configSaveBtn, configUnlockBtn, configService::setConfigPath); }
    @FXML private void actionSaveProjectsPath() { savePath(projectsPathField, projectsStatusLabel, projectsSaveBtn, projectsUnlockBtn, configService::setProjectsPath); }
    @FXML private void actionSaveBackendPath() { savePath(backendPathField, backendStatusLabel, backendSaveBtn, backendUnlockBtn, configService::setBackendPath); }
    @FXML private void actionSaveFrontendPath() { savePath(frontendPathField, frontendStatusLabel, frontendSaveBtn, frontendUnlockBtn, configService::setFrontendPath); }

    @FXML private void actionUnlockConfig() { unlockField(configPathField, configSaveBtn, configUnlockBtn); }
    @FXML private void actionUnlockProjects() { unlockField(projectsPathField, projectsSaveBtn, projectsUnlockBtn); }
    @FXML private void actionUnlockBackend() { unlockField(backendPathField, backendSaveBtn, backendUnlockBtn); }
    @FXML private void actionUnlockFrontend() { unlockField(frontendPathField, frontendSaveBtn, frontendUnlockBtn); }

    /*
    ============================================
    GIT
    ============================================
     */
    @FXML private void actionBrowseGit() { browse(gitPathField, gitStatusLabel); }
    @FXML private void actionSaveGitPath() { savePath(gitPathField, gitStatusLabel, gitSaveBtn, gitUnlockBtn, configService::setGitPath); }
    @FXML private void actionUnlockGit() { unlockField(gitPathField, gitSaveBtn, gitUnlockBtn); }

    @FXML
    private void actionSaveGitConfig() {

        boolean ok = configService.setGitConfig(
                gitPathField.getText(),
                gitUserField.getText(),
                gitEmailField.getText()
        );

        if (ok) {
            lockGitConfig();
            setStatus(gitStatusLabel, "Git configurado", "ok");
        } else {
            setStatus(gitStatusLabel, "Datos inválidos o sin permisos", "ko");
        }
    }

    @FXML
    private void actionInitGit() {

        String path = gitPathField.getText();

        if (path == null || path.isBlank()) {
            setStatus(gitStatusLabel, "Ruta requerida", "ko");
            return;
        }

        setStatus(gitStatusLabel, "Inicializando...", "info");

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "init");
                pb.directory(new File(path));

                int exit = pb.start().waitFor();

                updateGitStatus(exit == 0 ? "Git inicializado" : "Error al inicializar",
                        exit == 0 ? "ok" : "ko");

            } catch (Exception e) {
                updateGitStatus("Error Git", "ko");
            }
        }).start();
    }

    private void updateGitStatus(String text, String type) {
        Platform.runLater(() -> setStatus(gitStatusLabel, text, type));
    }

    /*
    ============================================
    CLOSE
    ============================================
     */
    @FXML
    private void actionClose() {
        configRoot.getScene().getWindow().hide();
    }

    /*
    ============================================
    INTERFACE
    ============================================
     */
    @FunctionalInterface
    private interface SaveAction {
        boolean save(String path);
    }
}