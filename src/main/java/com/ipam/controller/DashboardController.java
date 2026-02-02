package com.ipam.controller;

import com.ipam.service.IPAddressService;
import com.ipam.service.SubnetService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
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

    @FXML
    private FlowPane networkOverviewContainer;

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
                    populateNetworkOverview();
                });

            } catch (SQLException e) {
                logger.error("Erreur lors du chargement des statistiques", e);
                Platform.runLater(() -> showError("Erreur lors du chargement des statistiques"));
            }
        }).start();
    }

    private void populateNetworkOverview() {
        try {
            var subnets = subnetService.getAllSubnets();
            networkOverviewContainer.getChildren().clear();
            for (var s : subnets) {
                networkOverviewContainer.getChildren().add(buildSubnetCard(s));
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de l'aperçu des réseaux", e);
        }
    }

    private VBox buildSubnetCard(com.ipam.model.Subnet s) {
        int total = s.getTotalHosts();
        int used = s.getUsedHosts();
        int available = Math.max(0, total - used);
        double pct = s.getUsagePercentage() / 100.0;
        int utilization = (int) s.getUsagePercentage();

        // 1. Header: Name + Status
        Label nameLabel = new Label(s.getDescription() != null && !s.getDescription().isEmpty() ? s.getDescription() : "Network");
        nameLabel.getStyleClass().add("network-name");
        
        Label statusLabel = new Label("Active");
        statusLabel.getStyleClass().add("network-status-badge");
        
        javafx.scene.layout.Region spacerHeader = new javafx.scene.layout.Region();
        HBox.setHgrow(spacerHeader, javafx.scene.layout.Priority.ALWAYS);
        
        HBox header = new HBox(nameLabel, spacerHeader, statusLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        // 2. CIDR
        Label cidrLabel = new Label(s.getNetworkCidr());
        cidrLabel.getStyleClass().add("network-cidr");

        // 3. Metrics: Allocated | Available
        // Allocated
        Label allocatedTitle = new Label("Allocated");
        allocatedTitle.getStyleClass().add("network-metric-label");
        Label allocatedValue = new Label(used + "/" + total);
        allocatedValue.getStyleClass().add("network-metric-value");
        VBox allocatedBox = new VBox(2, allocatedTitle, allocatedValue);

        javafx.scene.layout.Region spacerMetrics = new javafx.scene.layout.Region();
        HBox.setHgrow(spacerMetrics, javafx.scene.layout.Priority.ALWAYS);

        // Available
        Label availableTitle = new Label("Available");
        availableTitle.getStyleClass().add("network-metric-label");
        Label availableValue = new Label(String.valueOf(available));
        availableValue.getStyleClass().addAll("network-metric-value", "available");
        VBox availableBox = new VBox(2, availableTitle, availableValue);
        
        HBox metrics = new HBox(allocatedBox, spacerMetrics, availableBox);
        metrics.setPadding(new javafx.geometry.Insets(0, 0, 10, 0));

        // 4. Utilization Bar
        Label utilLabel = new Label("Utilization");
        utilLabel.getStyleClass().add("network-metric-label");
        
        Label utilValue = new Label(utilization + "%");
        // Using network-metric-label for the % text as per simple look, or create a specific one. 
        // In image it's small text on right.
        utilValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;"); 
        
        javafx.scene.layout.Region spacerUtil = new javafx.scene.layout.Region();
        HBox.setHgrow(spacerUtil, javafx.scene.layout.Priority.ALWAYS);
        HBox utilHeader = new HBox(utilLabel, spacerUtil, utilValue);

        ProgressBar bar = new ProgressBar(pct);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("network-progress-bar");
        
        // Color logic
        if (pct >= 0.9) {
            bar.setStyle("-fx-accent: #e74c3c;");
        } else if (pct >= 0.75) {
            bar.setStyle("-fx-accent: #f39c12;");
        } else {
            bar.setStyle("-fx-accent: #10b981;");
        }

        VBox progressBox = new VBox(5, utilHeader, bar);
        progressBox.setPadding(new javafx.geometry.Insets(0, 0, 15, 0));

        // 5. Button
        javafx.scene.control.Button detailsBtn = new javafx.scene.control.Button("View Details");
        detailsBtn.setMaxWidth(Double.MAX_VALUE);
        detailsBtn.getStyleClass().add("view-details-btn");

        // Main Container
        VBox card = new VBox(header, cidrLabel, metrics, progressBox, detailsBtn);
        card.getStyleClass().add("network-card");
        card.setPrefWidth(300);
        card.setMinWidth(300);
        
        return card;
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
