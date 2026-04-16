package utils_ui;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class directory {

    private File selectedDirectory;
    private File currentDirectory = null;

    private final File desktop
            = new File(System.getProperty("user.home"), "Desktop");

    private final File documents
            = new File(System.getProperty("user.home"), "Documents");

    public File show() {

        selectedDirectory = null; // reset SIEMPRE

        Stage stage = new Stage();
        stage.setTitle("Seleccionar Directorio");
        stage.initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();

        Label pathLabel = new Label("Selecciona un directorio");

        ListView<File> listView = new ListView<>();

        // Mostrar nombre limpio
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    String name = item.getName();
                    if (name.isEmpty()) {
                        name = item.getAbsolutePath();
                    }
                    setText(name);
                }
            }
        });

        loadRoots(listView);

        // -------------------------------
        // DOBLE CLICK = entrar
        // -------------------------------
        listView.setOnMouseClicked(e -> {

            if (e.getClickCount() == 2) {

                File selected = listView.getSelectionModel().getSelectedItem();

                if (selected != null && selected.isDirectory()) {

                    currentDirectory = selected;
                    loadDirectory(listView, pathLabel, currentDirectory);

                }

            }

        });

        // -------------------------------
        // BOTONES
        // -------------------------------
        Button upButton = new Button("Subir");
        Button saveButton = new Button("Seleccionar");
        Button cancelButton = new Button("Cancelar");

        upButton.setOnAction(e -> {

            if (currentDirectory == null) {

                loadRoots(listView);
                pathLabel.setText("Selecciona un directorio");
                return;

            }

            File parent = currentDirectory.getParentFile();

            if (parent != null) {

                currentDirectory = parent;
                loadDirectory(listView, pathLabel, currentDirectory);

            } else {

                currentDirectory = null;
                loadRoots(listView);
                pathLabel.setText("Selecciona un directorio");

            }

        });

        // 🔴 AQUÍ ESTÁ LA CORRECCIÓN CLAVE
        saveButton.setOnAction(e -> {

            File selected = listView.getSelectionModel().getSelectedItem();

            if (selected != null && selected.isDirectory()) {

                selectedDirectory = selected;

            } else if (currentDirectory != null) {

                // usar la carpeta actual si no hay selección
                selectedDirectory = currentDirectory;

            } else {

                // fallback seguro (desktop)
                selectedDirectory = desktop.exists() ? desktop : new File(System.getProperty("user.home"));

            }

            stage.close();

        });

        cancelButton.setOnAction(e -> {

            selectedDirectory = null;
            stage.close();

        });

        HBox buttons = new HBox(10, upButton, saveButton, cancelButton);
        buttons.setStyle("-fx-padding:10; -fx-alignment:center-right;");

        // -------------------------------
        // LAYOUT
        // -------------------------------
        root.setTop(pathLabel);
        root.setCenter(listView);
        root.setBottom(buttons);

        BorderPane.setMargin(pathLabel, new javafx.geometry.Insets(10));

        Scene scene = new Scene(root, 500, 500);

        stage.setScene(scene);
        stage.showAndWait();

        return selectedDirectory;

    }

    /*
    ====================================
    ROOTS
    ====================================
     */
    private void loadRoots(ListView<File> listView) {

        listView.getItems().clear();

        if (desktop.exists()) {
            listView.getItems().add(desktop);
        }

        if (documents.exists()) {
            listView.getItems().add(documents);
        }

        File[] drives = File.listRoots();

        if (drives != null) {
            listView.getItems().addAll(Arrays.asList(drives));
        }

    }

    /*
    ====================================
    LOAD DIRECTORY
    ====================================
     */
    private void loadDirectory(ListView<File> listView, Label pathLabel, File directory) {

        pathLabel.setText("Ruta: " + directory.getAbsolutePath());

        File[] dirs = directory.listFiles(File::isDirectory);

        if (dirs != null) {

            Arrays.sort(dirs, Comparator.comparing(File::getName));

            listView.setItems(
                    FXCollections.observableArrayList(dirs)
            );

        }

    }

}
