module transport {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.logging;

    // Open packages that need to be accessed via reflection (like for FXML)
    opens transport to javafx.fxml;
    opens transport.ui.controllers to javafx.fxml;
    opens transport.core to com.google.gson;

    // Export packages that need to be accessible to other modules
    exports transport;
    exports transport.ui.controllers;
}
