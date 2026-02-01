package com.ipam.controller;

import com.ipam.MainApp;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ipam.util.AuthManager;

/**
 * Contrôleur pour la page de connexion simple
 */
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private CheckBox showPasswordCheck;
    @FXML private Button loginButton;
    @FXML private Label messageLabel;

    @FXML
    private void initialize() {
        // Enter triggers login
        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        passwordVisibleField.setOnAction(e -> handleLogin());

        // Sync visible/hidden password fields
        passwordVisibleField.textProperty().addListener((obs, oldV, newV) -> {
            if (showPasswordCheck.isSelected()) {
                passwordField.setText(newV);
            }
        });
        passwordField.textProperty().addListener((obs, oldV, newV) -> {
            if (!showPasswordCheck.isSelected()) {
                passwordVisibleField.setText(newV);
            }
        });
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = (showPasswordCheck != null && showPasswordCheck.isSelected())
                ? (passwordVisibleField.getText() == null ? "" : passwordVisibleField.getText().trim())
                : (passwordField.getText() == null ? "" : passwordField.getText().trim());

        // Identifiants par défaut: "admin" / "admin"
        if ("admin".equalsIgnoreCase(username) && "admin".equals(password)) {
            logger.info("Authentification réussie pour l'utilisateur {}", username);
            AuthManager.login(username);
            loadMainView();
        } else {
            messageLabel.setText("Identifiants invalides");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        boolean show = showPasswordCheck.isSelected();
        if (show) {
            passwordVisibleField.setText(passwordField.getText());
        } else {
            passwordField.setText(passwordVisibleField.getText());
        }
        passwordVisibleField.setVisible(show);
        passwordVisibleField.setManaged(show);
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
    }

    private void loadMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();

            Stage stage = MainApp.getPrimaryStage();
            if (stage == null) {
                // Fallback: obtenir le stage via le bouton
                stage = (Stage) loginButton.getScene().getWindow();
            }

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setTitle("IPAM Manager - Gestion d'Adressage IP");
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
            stage.show();
        } catch (Exception e) {
            logger.error("Erreur lors du chargement de la vue principale", e);
            messageLabel.setText("Erreur de chargement de l'application");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }
}
