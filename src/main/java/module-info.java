module main {

    // =============================
    // REQUIRES
    // =============================
    requires javafx.controls;
    requires javafx.fxml;

    requires java.desktop;
    requires java.logging;

    requires com.google.gson;
    requires java.base;

    // =============================
    // OPENS (FXML + REFLECTION)
    // =============================

    // Config (JSON + FXML)
    opens configuration to javafx.fxml, com.google.gson;

    // Controladores FXML
    opens main to javafx.fxml;
    opens controller to javafx.fxml;
    opens deploy to javafx.fxml;
    opens creator to javafx.fxml;
    opens ports to javafx.fxml;
    opens npminstall to javafx.fxml;
    opens git to javafx.fxml;

    // Servicios usados por controladores
    opens processDeploy to javafx.fxml;
    opens processCreate to javafx.fxml;

    // =============================
    // EXPORTS
    // =============================
    exports main;
    exports git; // 🔥 IMPORTANTE si accedes desde fuera
}