package com.ipam.controller;

import com.ipam.service.IPAddressService;
import com.ipam.service.SubnetService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le tableau de bord
 */
public class DashboardController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML
    private Label totalSubnetsLabel;

    @FXML
    private Label totalIPsLabel;

    @FXML
    private Label usedIPsLabel;

    @FXML
    private Label availableIPsLabel;

    @FXML
    private Label usagePercentageLabel;

    @FXML
    private PieChart ipStatusChart;

    private final SubnetService subnetService;
    private final IPAddressService ipAddressService;

    public DashboardController() {
        this.subnetService = new SubnetService();
        this.ipAddressService = new IPAddressService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initialisation du tableau de bord");
        loadStatistics();
    }

    private void loadStatistics() {
        // Charger les statistiques dans un thread séparé
        new Thread(() -> {
            try {
                // Statistiques des sous-réseaux
                SubnetService.SubnetStatistics subnetStats = subnetService.getGlobalStatistics();
                
                // Statistiques des IPs
                IPAddressService.IPStatistics ipStats = ipAddressService.getStatistics();

                // Mettre à jour l'UI dans le thread JavaFX
                Platform.runLater(() -> {
                    updateSubnetStatistics(subnetStats);
                    updateIPStatistics(ipStats);
                    updateChart(ipStats);
                });

            } catch (SQLException e) {
                logger.error("Erreur lors du chargement des statistiques", e);
                Platform.runLater(() -> showError("Erreur lors du chargement des statistiques"));
            }
        }).start();
    }

    private void updateSubnetStatistics(SubnetService.SubnetStatistics stats) {
        totalSubnetsLabel.setText(String.valueOf(stats.getTotalSubnets()));
        totalIPsLabel.setText(String.valueOf(stats.getTotalIps()));
        usedIPsLabel.setText(String.valueOf(stats.getUsedIps()));
        availableIPsLabel.setText(String.valueOf(stats.getAvailableIps()));
        
        double percentage = stats.getUsagePercentage();
        usagePercentageLabel.setText(String.format("%.1f%%", percentage));
        
        // Couleur selon le taux d'utilisation
        String color = getColorForPercentage(percentage);
        usagePercentageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 24px;");
    }

    private void updateIPStatistics(IPAddressService.IPStatistics stats) {
        // Statistiques déjà affichées via les statistiques de subnet
        logger.debug("Statistiques IP: Available={}, Assigned={}, Reserved={}, Blocked={}", 
            stats.getAvailable(), stats.getAssigned(), stats.getReserved(), stats.getBlocked());
    }

    private void updateChart(IPAddressService.IPStatistics stats) {
        ipStatusChart.getData().clear();
        
        if (stats.getAvailable() > 0) {
            ipStatusChart.getData().add(new PieChart.Data("Disponibles", stats.getAvailable()));
        }
        
        if (stats.getAssigned() > 0) {
            ipStatusChart.getData().add(new PieChart.Data("Assignées", stats.getAssigned()));
        }
        
        if (stats.getReserved() > 0) {
            ipStatusChart.getData().add(new PieChart.Data("Réservées", stats.getReserved()));
        }
        
        if (stats.getBlocked() > 0) {
            ipStatusChart.getData().add(new PieChart.Data("Bloquées", stats.getBlocked()));
        }

        ipStatusChart.setLegendVisible(true);
        ipStatusChart.setLabelsVisible(true);
    }

    private String getColorForPercentage(double percentage) {
        if (percentage >= 90) {
            return "#e74c3c"; // Rouge
        } else if (percentage >= 75) {
            return "#f39c12"; // Orange
        } else if (percentage >= 50) {
            return "#3498db"; // Bleu
        } else {
            return "#27ae60"; // Vert
        }
    }

    @FXML
    private void handleRefresh() {
        logger.info("Actualisation du tableau de bord");
        loadStatistics();
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
