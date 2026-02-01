package com.ipam.controller;

import com.ipam.dao.AuditLogDAO;
import com.ipam.model.AuditLog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur pour l'historique d'audit
 */
public class AuditLogController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogController.class);

    @FXML
    private TableView<AuditLog> auditTable;

    @FXML
    private TableColumn<AuditLog, String> timestampColumn;

    @FXML
    private TableColumn<AuditLog, String> actionColumn;

    @FXML
    private TableColumn<AuditLog, String> entityTypeColumn;

    @FXML
    private TableColumn<AuditLog, Long> entityIdColumn;

    @FXML
    private TableColumn<AuditLog, String> detailsColumn;

    @FXML
    private TableColumn<AuditLog, String> usernameColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> actionFilterComboBox;

    @FXML
    private ComboBox<String> entityFilterComboBox;

    @FXML
    private ComboBox<Integer> limitComboBox;

    @FXML
    private Button exportPdfButton;

    @FXML
    private Button exportExcelButton;

    private final ObservableList<AuditLog> auditList = FXCollections.observableArrayList();
    private final AuditLogDAO auditLogDAO;

    public AuditLogController() {
        this.auditLogDAO = new AuditLogDAO();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initialisation de la vue d'historique d'audit");

        // Initialiser les colonnes
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("formattedTimestamp"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        entityTypeColumn.setCellValueFactory(new PropertyValueFactory<>("entityType"));
        entityIdColumn.setCellValueFactory(new PropertyValueFactory<>("entityId"));
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        // Styliser la colonne action
        actionColumn.setCellFactory(column -> new TableCell<AuditLog, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "CREATE" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        case "UPDATE" -> setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                        case "DELETE" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        case "ASSIGN", "RESERVE" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        default -> setStyle("");
                    }
                }
            }
        });

        auditTable.setItems(auditList);

        // Initialiser les ComboBox
        actionFilterComboBox.setItems(FXCollections.observableArrayList(
            "Tous", "CREATE", "UPDATE", "DELETE", "ASSIGN", "RELEASE", "RESERVE"
        ));
        actionFilterComboBox.setValue("Tous");

        entityFilterComboBox.setItems(FXCollections.observableArrayList(
            "Tous", "SUBNET", "IP", "RESERVATION"
        ));
        entityFilterComboBox.setValue("Tous");

        limitComboBox.setItems(FXCollections.observableArrayList(
            100, 500, 1000, 5000
        ));
        limitComboBox.setValue(1000);

        // Charger les logs
        loadAuditLogs();
    }

    private void loadAuditLogs() {
        new Thread(() -> {
            try {
                int limit = limitComboBox.getValue();
                List<AuditLog> logs = auditLogDAO.findAll(limit);
                Platform.runLater(() -> {
                    auditList.clear();
                    auditList.addAll(logs);
                });
            } catch (SQLException e) {
                logger.error("Erreur lors du chargement des logs", e);
                Platform.runLater(() -> showError("Erreur", "Impossible de charger l'historique"));
            }
        }).start();
    }

    @FXML
    private void handleFilter() {
        String actionFilter = actionFilterComboBox.getValue();
        String entityFilter = entityFilterComboBox.getValue();
        int limit = limitComboBox.getValue();

        new Thread(() -> {
            try {
                List<AuditLog> logs;

                if (!"Tous".equals(actionFilter)) {
                    logs = auditLogDAO.findByAction(actionFilter, limit);
                } else if (!"Tous".equals(entityFilter)) {
                    logs = auditLogDAO.findByEntityType(entityFilter, limit);
                } else {
                    logs = auditLogDAO.findAll(limit);
                }

                Platform.runLater(() -> {
                    auditList.clear();
                    auditList.addAll(logs);
                });
            } catch (SQLException e) {
                logger.error("Erreur lors du filtrage", e);
            }
        }).start();
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText();
        if (searchTerm.isEmpty()) {
            loadAuditLogs();
            return;
        }

        new Thread(() -> {
            try {
                int limit = limitComboBox.getValue();
                List<AuditLog> results = auditLogDAO.search(searchTerm, limit);
                Platform.runLater(() -> {
                    auditList.clear();
                    auditList.addAll(results);
                });
            } catch (SQLException e) {
                logger.error("Erreur lors de la recherche", e);
            }
        }).start();
    }

    @FXML
    private void handleRefresh() {
        loadAuditLogs();
    }

    @FXML
    private void handleExportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter l'historique en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("audit_log.pdf");
        java.io.File file = chooser.showSaveDialog(auditTable.getScene().getWindow());
        if (file != null) {
            try {
                com.ipam.util.ExportUtil.exportTableViewToPdf(auditTable, file, "Historique d'Audit");
                showInfo("Export", "PDF généré: " + file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Erreur export PDF", e);
                showError("Export", "Échec de l'export PDF: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter l'historique en Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        chooser.setInitialFileName("audit_log.xlsx");
        java.io.File file = chooser.showSaveDialog(auditTable.getScene().getWindow());
        if (file != null) {
            try {
                com.ipam.util.ExportUtil.exportTableViewToExcel(auditTable, file, "Historique d'Audit");
                showInfo("Export", "Excel généré: " + file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Erreur export Excel", e);
                showError("Export", "Échec de l'export Excel: " + e.getMessage());
            }
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
