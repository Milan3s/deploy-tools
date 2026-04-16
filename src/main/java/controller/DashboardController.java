package controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class DashboardController implements Initializable {

    @FXML
    private AnchorPane dashboardRoot;


    @FXML
    private Button creatorButton;
    @FXML
    private Button deployButton;
    @FXML
    private Button gitButton;
    @FXML
    private Button portsButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button closeButton;
    @FXML
    private Button npmInstallButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    // ------------------------------------------------
    // NAVIGATION
    // ------------------------------------------------
    @FXML
    private void openCreator(ActionEvent event) {

        openWindow("/frameworks/creator/creatorGlobal.fxml", "Framework Creator");

    }

    @FXML
    private void openDeploy(ActionEvent event) {

        openWindow("/frameworks/deploy/deployGlobal.fxml", "Framework Deploy");

    }

    @FXML
    private void openGit(ActionEvent event) {
        openWindow("/git/git.fxml", "Tool Git");

    }

    @FXML
    private void openPorts(ActionEvent event) {

        openWindow("/ports/ports.fxml", "Panel Ports");

    }

    @FXML
    private void openSettings(ActionEvent event) {

        openWindow("/configuration/config.fxml", "Panel Config");

    }

    @FXML
    private void openNpmInstall(ActionEvent event) {

        openWindow("/npminstall/npminstall.fxml", "Instalación de dependencias");

    }

    // ------------------------------------------------
    // OPEN WINDOW
    // ------------------------------------------------
    private void openWindow(String fxmlPath, String title) {

        try {

            URL url = getClass().getResource(fxmlPath);

            if (url == null) {
                throw new RuntimeException("FXML no encontrado: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            // =========================
            // OWNER (IMPORTANTE)
            // =========================
            stage.initOwner(dashboardRoot.getScene().getWindow());

            // =========================
            // BLOQUEO GLOBAL DE TAMAÑO
            // =========================
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.sizeToScene();

            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------------------
    // CLOSE APP
    // ------------------------------------------------
    @FXML
    private void closeApp(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();

    }

}
