package ports;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.*;
import javafx.stage.Stage;

public class PortsController implements Initializable {

    // ------------------------------------------------
    // FXML
    // ------------------------------------------------
    @FXML
    private ComboBox<String> frameworkCombo;

    @FXML
    private Button scanButton;

    @FXML
    private ListView<String> portsList;

    @FXML
    private Button freePortsButton;

    @FXML
    private Button closeButton;

    @FXML
    private Label statusLabel;

    // ------------------------------------------------
    // SERVICE
    // ------------------------------------------------
    private final portService service = new portService();

    // ------------------------------------------------
    // INIT
    // ------------------------------------------------
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        frameworkCombo.getItems().addAll(
                "Astro",
                "Vue",
                "React",
                "React Native",
                "Laravel"
        );

        frameworkCombo.setValue("Astro");

        freePortsButton.setDisable(true);

        setStatus("No iniciado", "gray");
    }

    // ------------------------------------------------
    // SCAN PORTS
    // ------------------------------------------------
    @FXML
    private void scanPorts(ActionEvent event) {

        String framework = frameworkCombo.getValue();

        if (framework == null) {

            setStatus("Seleccione un framework", "orange");
            return;
        }

        portsList.getItems().clear();

        disableActions(true);

        setStatus("Escaneando puertos...", "orange");

        Task<List<portService.PortInfo>> task = new Task<>() {

            @Override
            protected List<portService.PortInfo> call() {

                return service.scanFrameworkPorts(framework);
            }
        };

        task.setOnSucceeded(e -> {

            List<portService.PortInfo> ports = task.getValue();

            updatePortsList(ports);

            disableActions(false);
        });

        task.setOnFailed(e -> {

            setStatus("Error escaneando puertos", "red");

            disableActions(false);
        });

        new Thread(task).start();
    }

    // ------------------------------------------------
    // FREE PORTS
    // ------------------------------------------------
    @FXML
    private void freePorts(ActionEvent event) {

        String framework = frameworkCombo.getValue();

        if (framework == null) {

            setStatus("Seleccione un framework", "orange");
            return;
        }

        disableActions(true);

        setStatus("Liberando puertos...", "orange");

        Task<Boolean> task = new Task<>() {

            @Override
            protected Boolean call() {

                return service.freeFrameworkPorts(framework);
            }
        };

        task.setOnSucceeded(e -> {

            boolean result = task.getValue();

            if (result) {
                setStatus("Puertos liberados correctamente", "green");
            } else {
                setStatus("Error liberando puertos", "red");
            }

            refreshPorts();

            disableActions(false);
        });

        task.setOnFailed(e -> {

            setStatus("Error liberando puertos", "red");

            disableActions(false);
        });

        new Thread(task).start();
    }

    // ------------------------------------------------
    // REFRESH PORTS
    // ------------------------------------------------
    private void refreshPorts() {

        Task<List<portService.PortInfo>> task = new Task<>() {

            @Override
            protected List<portService.PortInfo> call() {

                return service.scanFrameworkPorts(
                        frameworkCombo.getValue()
                );
            }
        };

        task.setOnSucceeded(e -> {

            List<portService.PortInfo> ports = task.getValue();

            updatePortsList(ports);
        });

        new Thread(task).start();
    }

    // ------------------------------------------------
    // UPDATE LIST
    // ------------------------------------------------
    private void updatePortsList(List<portService.PortInfo> ports) {

        Platform.runLater(() -> {

            portsList.getItems().clear();

            if (ports == null || ports.isEmpty()) {

                setStatus("No se encontraron puertos activos", "orange");

                freePortsButton.setDisable(true);

                return;
            }

            for (portService.PortInfo p : ports) {

                portsList.getItems().add(
                        "Puerto " + p.port + " (PID " + p.pid + ")"
                );
            }

            setStatus("Puertos detectados: " + ports.size(), "green");

            freePortsButton.setDisable(false);
        });
    }

    // ------------------------------------------------
    // STATUS
    // ------------------------------------------------
    private void setStatus(String text, String color) {

        Platform.runLater(() -> {

            statusLabel.setText(text);

            switch (color) {

                case "green":
                    statusLabel.setStyle("-fx-text-fill: #2ecc71;");
                    break;

                case "orange":
                    statusLabel.setStyle("-fx-text-fill: #f39c12;");
                    break;

                case "red":
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    break;

                default:
                    statusLabel.setStyle("-fx-text-fill: gray;");
            }
        });
    }

    // ------------------------------------------------
    // UI LOCK
    // ------------------------------------------------
    private void disableActions(boolean state) {

        Platform.runLater(() -> {

            scanButton.setDisable(state);
            freePortsButton.setDisable(state);
            frameworkCombo.setDisable(state);
        });
    }

    // ------------------------------------------------
    // CLOSE WINDOW
    // ------------------------------------------------
    @FXML
    private void closeWindow(ActionEvent event) {

        Stage stage = (Stage) closeButton.getScene().getWindow();

        stage.close();
    }
}
