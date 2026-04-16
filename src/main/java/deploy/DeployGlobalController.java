package deploy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class DeployGlobalController {

    @FXML
    private AnchorPane deployRoot;
    @FXML
    private Button reactNativeBtn;
    @FXML
    private Button webDynamicBtn;
    @FXML
    private Button closeButton;

    // =========================
    // OPEN VIEWS
    // =========================
    @FXML
    private void openReactNativeView(ActionEvent event) {
        openView("/frameworks/deploy/reactNative.fxml", "React Native");
    }

    @FXML
    private void openWebDynamicView(ActionEvent event) {
        openView("/frameworks/deploy/webStatic.fxml", "Web / API");
    }

    // =========================
    // GENERIC LOADER (FIXED WINDOW)
    // =========================
    private void openView(String path, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            // =========================
            // BLOQUEO GLOBAL DE TAMAÑO
            // =========================
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.sizeToScene();

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // CLOSE
    // =========================
    @FXML
    private void onClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}