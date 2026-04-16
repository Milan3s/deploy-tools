package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {

        scene = new Scene(loadFXML("dashboard"));

        stage.setTitle("Deploy Tools");
        stage.setScene(scene);

        // =========================
        // BLOQUEAR REDIMENSIONADO GLOBAL
        // =========================
        stage.setResizable(false);     // No permite cambiar tamaño
        stage.setMaximized(false);     // Evita que se abra maximizado

        // Opcional (recomendado): tamaño fijo inicial
        stage.sizeToScene();

        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader =
                new FXMLLoader(App.class.getResource("/main/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}