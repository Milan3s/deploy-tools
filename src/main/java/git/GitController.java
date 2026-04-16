package git;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import configuration.configService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import utils_ui.directory;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class GitController implements Initializable {

    @FXML
    private AnchorPane root;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField tokenField;
    @FXML
    private TextField localPathField;
    @FXML
    private TextField commitMessageField;
    @FXML
    private TextField newRepoField;
    @FXML
    private TextField newBranchField;

    @FXML
    private TextArea logArea;
    @FXML
    private ListView<String> repoListView;

    @FXML
    private Label connectionStatusLabel;
    @FXML
    private Label repoStatusLabel;
    @FXML
    private Label selectedRepoLabel;
    @FXML
    private Label projectStatusLabel;
    @FXML
    private Label gitStatusLabel;
    @FXML
    private Label commitStatusLabel;
    @FXML
    private Label pathStatusLabel;

    @FXML
    private MenuButton gitActionMenu;
    @FXML
    private MenuButton branchMenu;
    @FXML
    private MenuButton sourceBranchMenu;

    @FXML
    private Button connectBtn;
    @FXML
    private Button saveAuthBtn;
    @FXML
    private Button unlockConnectionBtn;
    @FXML
    private Button browseBtn;
    @FXML
    private Button unlockPathBtn;
    @FXML
    private Button createRepoBtn;
    @FXML
    private Button connectRepoBtn;
    @FXML
    private Button refreshReposBtn;
    @FXML
    private Button deleteRepoBtn;
    @FXML
    private Button executeGitActionBtn;
    @FXML
    private Button commitBtn;
    @FXML
    private Button clearLogBtn;
    @FXML
    private Button closeBtn;
    @FXML
    private Button copyLogBtn;
    @FXML
    private Button createBranchBtn;
    @FXML
    private Button deleteBranchBtn;
    @FXML
    private Button mergeBranchBtn;

    @FXML
    private TextField searchRepoField;

    private final Gson gson = new Gson();
    private final AtomicBoolean loading = new AtomicBoolean(false);

    private final File configFile = new File(
            System.getProperty("user.home") + "/.deploytool/git.config.json"
    );

    private String selectedAction = null;
    private String selectedRepo = null;
    private String selectedBranch = null;
    private String selectedSourceBranch = null;

    private final Map<String, String> repoOwners = new HashMap<>();

    private boolean locked = false;
    private boolean pathLocked = true;

    private final ObservableList<String> repoList = FXCollections.observableArrayList();
    private FilteredList<String> filteredRepos;

    // =================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        loadConfig();
        syncWithGlobalConfig();

        String token = gitService.loadToken();
        tokenField.setText(token);

        initMenu();
        initRepoSelection();
        initSearch();

        lockPath(true);

        boolean hasConfig = token != null && !token.isBlank() && !getUser().isBlank();

        if (hasConfig) {

            lockInputs(true);
            saveAuthBtn.setDisable(true);

            Platform.runLater(() -> {
                log("[AUTO] Reconectando...");
                onConnect(null);
            });

        } else {

            lockInputs(false);
            connectBtn.setDisable(false);
            saveAuthBtn.setDisable(false);

            connectionStatusLabel.setText("No conectado");
        }
    }

    private void initSearch() {

        filteredRepos = new FilteredList<>(repoList, s -> true);

        PauseTransition pause = new PauseTransition(Duration.millis(250));

        searchRepoField.textProperty().addListener((obs, oldVal, newVal) -> {

            pause.setOnFinished(e -> {
                String filter = newVal == null ? "" : newVal.toLowerCase();

                filteredRepos.setPredicate(repo -> {
                    if (filter.isBlank()) {
                        return true;
                    }
                    return repo.toLowerCase().contains(filter);
                });
            });

            pause.playFromStart();
        });

        repoListView.setItems(filteredRepos);
    }

    private void initMenu() {

        gitActionMenu.getItems().clear();

        for (String action : gitService.getAvailableActions()) {

            MenuItem item = new MenuItem(action);

            item.setOnAction(e -> {
                selectedAction = action;
                gitActionMenu.setText(action);
            });

            gitActionMenu.getItems().add(item);
        }

        gitActionMenu.setText("Seleccionar acción");
    }

    private void initRepoSelection() {

        repoListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {

            selectedRepo = n;
            selectedRepoLabel.setText(n != null ? n : "-");

            selectedBranch = null;
            branchMenu.setText("Seleccionar rama");
        });
    }

    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    private void setStatus(Label label, String msg) {
        Platform.runLater(() -> label.setText(msg));
    }

    private String getPath() {
        return localPathField.getText();
    }

    private String getUser() {
        return usernameField.getText();
    }

    private String getToken() {
        return tokenField.getText();
    }

    @FXML
    private void onSaveConnection(ActionEvent event) {

        if (getToken().isBlank()) {
            log("[ERROR] Token vacío");
            return;
        }

        try {

            gitService.saveToken(getToken());

            Map<String, String> data = new HashMap<>();
            data.put("username", getUser());
            data.put("path", getPath());

            configFile.getParentFile().mkdirs();

            try (Writer writer = new FileWriter(configFile)) {
                gson.toJson(data, writer);
            }

            configService.ConfigData global = configService.loadConfig();
            global.gitUser = getUser();
            global.gitPath = getPath();
            configService.saveConfig(global);

            log("[OK] Config guardada correctamente");

            lockInputs(true);
            lockPath(true);

            connectBtn.setDisable(false);

            setStatus(connectionStatusLabel, "Config guardada");

        } catch (Exception e) {
            log("[ERROR] " + e.getMessage());
        }
    }

    @FXML
    private void onUnlockConnection(ActionEvent event) {

        lockInputs(false);

        localPathField.setDisable(false);
        browseBtn.setDisable(false);

        connectBtn.setDisable(false);
    }

    private void lockInputs(boolean lock) {

        locked = lock;

        usernameField.setDisable(lock);
        tokenField.setDisable(lock);

        log(lock ? "[INFO] Config bloqueada" : "[INFO] Edición habilitada");
    }

    private void lockPath(boolean lock) {

        pathLocked = lock;

        localPathField.setDisable(lock);
        browseBtn.setDisable(lock);

        pathStatusLabel.setText(lock ? "Ruta bloqueada" : "Ruta desbloqueada");
    }

    @FXML
    private void onUnlockPath(ActionEvent event) {

        lockPath(false);

        localPathField.setDisable(false);
        browseBtn.setDisable(false);

        log("[INFO] Ruta desbloqueada");
    }

    @FXML
    private void onConnect(ActionEvent e) {

        if (getToken().isBlank()) {
            log("[ERROR] Token vacío");
            return;
        }

        setStatus(connectionStatusLabel, "Conectando...");

        gitService.testConnection(getToken(), this::log, ok -> {

            if (ok) {

                Platform.runLater(() -> {

                    setStatus(connectionStatusLabel, "Conectado");

                    lockInputs(true);
                    lockPath(true);

                    connectBtn.setDisable(true);
                    saveAuthBtn.setDisable(true);

                    log("[OK] Conectado correctamente");

                    loadRepos();
                });

            } else {

                Platform.runLater(() -> {
                    setStatus(connectionStatusLabel, "Error");
                    connectBtn.setDisable(false);
                });
            }
        });
    }

    private void loadRepos() {

        if (loading.getAndSet(true)) {
            return;
        }

        setStatus(repoStatusLabel, "Cargando...");

        new Thread(() -> {

            Map<String, String> repos = gitService.listRepos(getToken(), this::log);

            Platform.runLater(() -> {

                repoOwners.clear();
                repoOwners.putAll(repos);

                repoList.setAll(repos.keySet());

                repoListView.getSelectionModel().clearSelection();
                selectedRepo = null;
                selectedRepoLabel.setText("-");

                setStatus(repoStatusLabel, "Repos: " + repos.size());

                loading.set(false);
            });

        }).start();
    }

    @FXML
    private void onCreateRepo(ActionEvent event) {

        String name = newRepoField.getText();

        if (name == null || name.isBlank()) {
            log("[ERROR] Nombre vacío");
            return;
        }

        gitService.createRemoteRepo(name, getToken(), this::log, url -> {
            log("[OK] Repo creado en remoto");
            loadRepos();
        });
    }

    @FXML
    private void onDeleteRepo(ActionEvent event) {

        if (selectedRepo == null) {
            log("[ERROR] Selecciona repo");
            return;
        }

        String owner = repoOwners.get(selectedRepo);

        if (owner == null) {
            log("[ERROR] Owner no encontrado");
            return;
        }

        gitService.deleteRemoteRepo(owner, selectedRepo, getToken(), this::log);
    }

    @FXML
    private void onConnectRepo(ActionEvent event) {

        if (selectedRepo == null) {
            log("[ERROR] Selecciona repo");
            return;
        }

        String owner = repoOwners.get(selectedRepo);

        String url = gitService.buildAuthUrl(
                "https://github.com/" + owner + "/" + selectedRepo + ".git",
                getUser(),
                getToken()
        );

        gitService.ensureRemote(new File(getPath()), url, this::log);
        loadBranches();
    }

    private void loadBranches() {

        if (selectedRepo == null) {
            return;
        }

        String owner = repoOwners.get(selectedRepo);

        gitService.listRemoteBranches(
                owner,
                selectedRepo,
                getToken(),
                this::log,
                branches -> Platform.runLater(() -> {

                    branchMenu.getItems().clear();
                    sourceBranchMenu.getItems().clear();

                    selectedBranch = null;
                    selectedSourceBranch = null;

                    for (String b : branches) {

                        MenuItem destItem = new MenuItem(b);
                        destItem.setOnAction(e -> {
                            selectedBranch = b;
                            branchMenu.setText(b);
                        });
                        branchMenu.getItems().add(destItem);

                        MenuItem srcItem = new MenuItem(b);
                        srcItem.setOnAction(e -> {
                            selectedSourceBranch = b;
                            sourceBranchMenu.setText(b);
                        });
                        sourceBranchMenu.getItems().add(srcItem);
                    }

                    if (!branches.isEmpty()) {

                        selectedBranch = branches.get(0);
                        branchMenu.setText(selectedBranch);

                        selectedSourceBranch = branches.get(0);
                        sourceBranchMenu.setText(selectedSourceBranch);

                    } else {

                        branchMenu.setText("Sin ramas");
                        sourceBranchMenu.setText("Sin ramas");
                    }
                })
        );
    }

    @FXML
    private void onExecuteGitAction(ActionEvent e) {

        if (selectedAction == null || selectedRepo == null) {
            log("[ERROR] Acción o repo faltante");
            return;
        }

        String owner = repoOwners.get(selectedRepo);

        String url = gitService.buildAuthUrl(
                "https://github.com/" + owner + "/" + selectedRepo + ".git",
                getUser(),
                getToken()
        );

        switch (selectedAction) {

            case "Clone":
                gitService.cloneRepo(url, getPath(), selectedBranch, this::log);
                break;

            case "Push":
                gitService.push(getPath(), url, selectedBranch, this::log);
                break;

            case "Pull":
                if (selectedBranch == null) {
                    log("[ERROR] Selecciona rama");
                    return;
                }
                gitService.pull(getPath(), url, selectedBranch, this::log);
                break;

            case "Fetch":
                gitService.fetch(getPath(), url, this::log);
                break;

            case "Status":
                gitService.status(getPath(), this::log);
                break;
        }

        loadBranches();
    }

    @FXML
    private void onCommit(ActionEvent e) {
        gitService.commit(getPath(), commitMessageField.getText(), this::log);
    }

    @FXML
    private void onBrowse(ActionEvent e) {

        if (pathLocked) {
            log("[WARN] Ruta bloqueada");
            return;
        }

        File f = new directory().show();

        if (f != null) {
            localPathField.setText(f.getAbsolutePath());
            lockPath(true);
        }
    }

    @FXML
    private void onClearLog(ActionEvent e) {
        logArea.clear();
    }

    @FXML
    private void onClose(ActionEvent e) {
        ((Stage) root.getScene().getWindow()).close();
    }

    @FXML
    private void onLoadRepos(ActionEvent e) {
        loadRepos();
    }

    private void loadConfig() {
        try {
            if (!configFile.exists()) {
                return;
            }

            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> data = gson.fromJson(new FileReader(configFile), type);

            usernameField.setText(data.getOrDefault("username", ""));
            localPathField.setText(data.getOrDefault("path", ""));

        } catch (Exception ignored) {
        }
    }

    private void syncWithGlobalConfig() {

        configService.ConfigData data = configService.loadConfig();

        if (usernameField.getText().isBlank() && data.gitUser != null) {
            usernameField.setText(data.gitUser);
        }

        if (localPathField.getText().isBlank() && data.gitPath != null) {
            localPathField.setText(data.gitPath);
        }
    }

    @FXML
    private void onCopyLog(ActionEvent e) {

        String text = logArea.getText();

        if (text == null || text.isEmpty()) {
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(text);

        Clipboard.getSystemClipboard().setContent(content);

        log("[OK] Log copiado al portapapeles");
    }

    @FXML
    private void onCreateBranch(ActionEvent event) {

        if (selectedRepo == null) {
            log("[ERROR] Selecciona repo");
            return;
        }

        String branchName = newBranchField.getText();

        if (branchName == null || branchName.isBlank()) {
            log("[ERROR] Nombre de rama vacío");
            return;
        }

        String owner = repoOwners.get(selectedRepo);

        String url = gitService.buildAuthUrl(
                "https://github.com/" + owner + "/" + selectedRepo + ".git",
                getUser(),
                getToken()
        );

        gitService.createBranch(
                getPath(),
                url,
                branchName,
                this::log,
                success -> Platform.runLater(() -> {

                    if (success) {

                        log("[OK] Rama creada y seleccionada");

                        loadBranches();

                        branchMenu.setText(branchName);
                        selectedBranch = branchName;

                        newBranchField.clear();

                    } else {
                        log("[ERROR] No se pudo crear la rama");
                    }
                })
        );
    }

    @FXML
    private void onDeleteBranch(ActionEvent event) {

        if (selectedRepo == null) {
            log("[ERROR] Selecciona repo");
            return;
        }

        if (selectedBranch == null || selectedBranch.isBlank()) {
            log("[ERROR] Selecciona rama");
            return;
        }

        if (selectedBranch.equals("main") || selectedBranch.equals("master")) {
            log("[ERROR] No se puede eliminar la rama principal");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar rama");
        confirm.setHeaderText("¿Eliminar rama?");
        confirm.setContentText("Se eliminará la rama: " + selectedBranch);

        Optional<ButtonType> res = confirm.showAndWait();

        if (res.isEmpty() || res.get() != ButtonType.OK) {
            log("[INFO] Eliminación cancelada");
            return;
        }

        String owner = repoOwners.get(selectedRepo);

        String url = gitService.buildAuthUrl(
                "https://github.com/" + owner + "/" + selectedRepo + ".git",
                getUser(),
                getToken()
        );

        String branchToDelete = selectedBranch;

        gitService.deleteBranch(
                getPath(),
                url,
                branchToDelete,
                this::log,
                success -> Platform.runLater(() -> {

                    if (success) {

                        log("[OK] Rama eliminada: " + branchToDelete);

                        loadBranches();

                        selectedBranch = null;
                        branchMenu.setText("Seleccionar rama");

                    } else {
                        log("[ERROR] No se pudo eliminar la rama");
                    }
                })
        );
    }

    @FXML
    private void onMergeBranch(ActionEvent event) {

        if (selectedRepo == null) {
            log("[ERROR] Selecciona repo");
            return;
        }

        if (selectedBranch == null || selectedBranch.isBlank()) {
            log("[ERROR] Selecciona rama destino");
            return;
        }

        if (selectedSourceBranch == null || selectedSourceBranch.isBlank()) {
            log("[ERROR] Selecciona rama origen");
            return;
        }

        if (selectedSourceBranch.equals(selectedBranch)) {
            log("[ERROR] No puedes hacer merge de la misma rama");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Merge de ramas");
        confirm.setHeaderText("Confirmar merge");
        confirm.setContentText("Merge de:\n"
                + selectedSourceBranch + "  →  " + selectedBranch);

        Optional<ButtonType> res = confirm.showAndWait();

        if (res.isEmpty() || res.get() != ButtonType.OK) {
            log("[INFO] Merge cancelado");
            return;
        }

        String owner = repoOwners.get(selectedRepo);

        if (owner == null) {
            log("[ERROR] Owner no encontrado");
            return;
        }

        String url = gitService.buildAuthUrl(
                "https://github.com/" + owner + "/" + selectedRepo + ".git",
                getUser(),
                getToken()
        );

        log("[INFO] Ejecutando merge: "
                + selectedSourceBranch + " → " + selectedBranch);

        gitService.mergeBranch(
                getPath(),
                url,
                selectedSourceBranch,
                selectedBranch,
                this::log,
                success -> Platform.runLater(() -> {

                    if (success) {

                        log("[OK] Merge completado");

                        loadBranches();

                    } else {
                        log("[ERROR] Fallo en merge");
                    }
                })
        );
    }
}
