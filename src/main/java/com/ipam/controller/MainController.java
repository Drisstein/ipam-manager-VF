package com.ipam.controller;

import com.ipam.service.IPAddressService;
import com.ipam.service.SubnetService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ipam.util.AuthManager;
import com.ipam.MainApp;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur principal de l'application
 */
public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private Label statusLabel;

    @FXML
    private Label userLabel;

    @FXML
    private Button dashboardButton;
    @FXML
    private Button subnetsButton;
    @FXML
    private Button ipAddressesButton;
    @FXML
    private Button auditLogButton;

    private final SubnetService subnetService;
    private final IPAddressService ipAddressService;

    public MainController() {
        this.subnetService = new SubnetService();
        this.ipAddressService = new IPAddressService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initialisation du contrôleur principal");
        setupToolbarIcons();
        updateUserLabel();
        loadDashboard();
    }

    private void setupToolbarIcons() {
        dashboardButton.setGraphic(FontIcon.of(FontAwesomeSolid.TACHOMETER_ALT));
        subnetsButton.setGraphic(FontIcon.of(FontAwesomeSolid.SITEMAP));
        ipAddressesButton.setGraphic(FontIcon.of(FontAwesomeSolid.LIST_OL));
        auditLogButton.setGraphic(FontIcon.of(FontAwesomeSolid.HISTORY));
    }

    @FXML
    private void handleDashboard() {
        loadDashboard();
    }

    @FXML
    private void handleSubnets() {
        loadView("/fxml/SubnetView.fxml", "Gestion des Sous-réseaux");
    }

    @FXML
    private void handleIPAddresses() {
        loadView("/fxml/IPAddressView.fxml", "Gestion des Adresses IP");
    }

    @FXML
    private void handleAuditLog() {
        loadView("/fxml/AuditLogView.fxml", "Historique d'Audit");
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("À propos");
        alert.setHeaderText("IPAM Manager v1.0");
        alert.setContentText(
            "Application de Gestion d'Adressage IP et Sous-réseaux\n\n" +
            "Développé avec Java 17 et JavaFX\n" +
            "Base de données: SQLite\n\n" +
            "© 2025 - Tous droits réservés"
        );
        alert.showAndWait();
    }

    @FXML
    private void handleExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quitter");
        alert.setHeaderText("Voulez-vous vraiment quitter l'application ?");
        alert.setContentText("Toutes les modifications ont été sauvegardées.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                logger.info("Fermeture de l'application demandée par l'utilisateur");
                javafx.application.Platform.exit();
            }
        });
    }

    private void loadDashboard() {
        loadView("/fxml/DashboardView.fxml", "Tableau de Bord");
    }

    private void loadView(String fxmlPath, String viewName) {
        try {
            logger.debug("Chargement de la vue: {}", viewName);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            mainBorderPane.setCenter(view);
            updateStatus("Vue chargée: " + viewName);
        } catch (IOException e) {
            logger.error("Erreur lors du chargement de la vue: {}", viewName, e);
            showError("Erreur", "Impossible de charger la vue: " + viewName);
        }
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private void updateUserLabel() {
        if (userLabel != null) {
            String user = AuthManager.getCurrentUser();
            userLabel.setText("Connecté : " + (user != null ? user : "invité"));
        }
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Voulez-vous vous déconnecter ?");
        alert.setContentText("Vous retournerez à la page de connexion.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                AuthManager.logout();
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
                    Parent loginRoot = loader.load();
                    Stage stage = MainApp.getPrimaryStage();
                    if (stage == null && mainBorderPane != null && mainBorderPane.getScene() != null) {
                        stage = (Stage) mainBorderPane.getScene().getWindow();
                    }
                    if (stage != null) {
                        Scene scene = new Scene(loginRoot, 500, 350);
                        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                        stage.setTitle("IPAM Manager - Connexion");
                        stage.setScene(scene);
                        stage.setMinWidth(420);
                        stage.setMinHeight(300);
                        stage.show();
                    }
                } catch (Exception e) {
                    logger.error("Erreur lors de la déconnexion", e);
                    showError("Erreur", "Impossible de charger la page de connexion");
                }
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
