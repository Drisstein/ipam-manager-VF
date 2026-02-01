package com.ipam;

import com.ipam.util.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe principale de l'application IPAM Manager
 */
public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;
            
            // Initialiser la connexion à la base de données
            DatabaseManager.getConnection();
            logger.info("Application IPAM Manager démarrée");

            // Charger la vue de connexion
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 640, 420);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            stage.setTitle("IPAM Manager - Connexion");
            stage.setScene(scene);
            stage.setMinWidth(560);
            stage.setMinHeight(360);
            
            // Ajouter une icône si disponible
            try {
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app-icon.png")));
            } catch (Exception e) {
                logger.warn("Icône d'application non trouvée");
            }

            stage.show();

        } catch (Exception e) {
            logger.error("Erreur lors du démarrage de l'application", e);
            showErrorDialog("Erreur de démarrage", "Impossible de démarrer l'application: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        logger.info("Fermeture de l'application");
        DatabaseManager.closeConnection();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    private void showErrorDialog(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
