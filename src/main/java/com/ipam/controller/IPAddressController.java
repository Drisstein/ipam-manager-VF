package com.ipam.controller;

import com.ipam.model.IPAddress;
import com.ipam.model.Subnet;
import com.ipam.service.IPAddressService;
import com.ipam.service.SubnetService;
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
 * Contrôleur pour la gestion des adresses IP
 */
public class IPAddressController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(IPAddressController.class);

    @FXML
    private TableView<IPAddress> ipTable;

    @FXML
    private TableColumn<IPAddress, String> ipColumn;

    @FXML
    private TableColumn<IPAddress, String> subnetColumn;

    @FXML
    private TableColumn<IPAddress, String> statusColumn;

    @FXML
    private TableColumn<IPAddress, String> assignedToColumn;

    @FXML
    private TableColumn<IPAddress, String> macColumn;

    @FXML
    private TableColumn<IPAddress, String> descriptionColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<Subnet> subnetComboBox;

    @FXML
    private TextField assignedToField;

    @FXML
    private TextField macAddressField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Button assignButton;
    @FXML
    private Button releaseButton;
    @FXML
    private Button reserveButton;
    @FXML
    private Button exportPdfButton;
    @FXML
    private Button exportExcelButton;

    private final ObservableList<IPAddress> ipList = FXCollections.observableArrayList();
    private final IPAddressService ipAddressService;
    private final SubnetService subnetService;
    private IPAddress selectedIP;

    public IPAddressController() {
        this.ipAddressService = new IPAddressService();
        this.subnetService = new SubnetService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initialisation de la vue des adresses IP");

        // Initialiser les colonnes
        ipColumn.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        subnetColumn.setCellValueFactory(new PropertyValueFactory<>("subnetName"));
        statusColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().getDisplayName())
        );
        assignedToColumn.setCellValueFactory(new PropertyValueFactory<>("assignedTo"));
        macColumn.setCellValueFactory(new PropertyValueFactory<>("macAddress"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Styliser la colonne statut
        statusColumn.setCellFactory(column -> new TableCell<IPAddress, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    IPAddress ip = getTableRow().getItem();
                    if (ip != null) {
                        switch (ip.getStatus()) {
                            case AVAILABLE -> setStyle("-fx-text-fill: #27ae60;");
                            case ASSIGNED -> setStyle("-fx-text-fill: #3498db;");
                            case RESERVED -> setStyle("-fx-text-fill: #f39c12;");
                            case BLOCKED -> setStyle("-fx-text-fill: #e74c3c;");
                        }
                    }
                }
            }
        });

        ipTable.setItems(ipList);

        // Listener sur la sélection
        ipTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    selectedIP = newValue;
                    fillFormWithIP(newValue);
                    updateButtonsState();
                }
            }
        );

        // Charger les données
        loadSubnets();
        loadAllIPs();

        updateButtonsState();
    }

    private void loadSubnets() {
        new Thread(() -> {
            try {
                List<Subnet> subnets = subnetService.getAllSubnets();
                Platform.runLater(() -> {
                    subnetComboBox.setItems(FXCollections.observableArrayList(subnets));
                });
            } catch (SQLException e) {
                logger.error("Erreur lors du chargement des sous-réseaux", e);
            }
        }).start();
    }

    private void loadAllIPs() {
        new Thread(() -> {
            try {
                List<IPAddress> ips = ipAddressService.getAllIPAddresses();
                Platform.runLater(() -> {
                    ipList.clear();
                    ipList.addAll(ips);
                });
            } catch (SQLException e) {
                logger.error("Erreur lors du chargement des IPs", e);
                Platform.runLater(() -> showError("Erreur", "Impossible de charger les adresses IP"));
            }
        }).start();
    }

    @FXML
    private void handleFilterBySubnet() {
        Subnet selectedSubnet = subnetComboBox.getValue();
        if (selectedSubnet == null) {
            loadAllIPs();
            return;
        }

        new Thread(() -> {
            try {
                // S'assurer que les IPs existent pour ce sous-réseau
                subnetService.ensureIpsGenerated(selectedSubnet.getId());
                List<IPAddress> ips = ipAddressService.getIPsBySubnet(selectedSubnet.getId());
                Platform.runLater(() -> {
                    ipList.clear();
                    ipList.addAll(ips);
                });
            } catch (SQLException e) {
                logger.error("Erreur lors du filtrage", e);
            }
        }).start();
    }

    @FXML
    private void handleAssign() {
        if (selectedIP == null) {
            showWarning("Attention", "Veuillez sélectionner une adresse IP");
            return;
        }

        if (!selectedIP.isAvailable()) {
            showWarning("Attention", "Cette IP n'est pas disponible");
            return;
        }

        try {
            String assignedTo = assignedToField.getText();
            String macAddress = macAddressField.getText();
            String description = descriptionArea.getText();

            if (assignedTo == null || assignedTo.trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom de l'équipement est obligatoire");
            }

            ipAddressService.assignIP(selectedIP.getId(), assignedTo, macAddress, description);
            showSuccess("Succès", "IP assignée avec succès");
            clearForm();
            loadAllIPs();
            updateButtonsState();

        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleRelease() {
        if (selectedIP == null) {
            showWarning("Attention", "Veuillez sélectionner une adresse IP");
            return;
        }

        if (selectedIP.isAvailable()) {
            showWarning("Attention", "Cette IP est déjà disponible");
            return;
        }

        try {
            ipAddressService.releaseIP(selectedIP.getId());
            showSuccess("Succès", "IP libérée avec succès");
            clearForm();
            loadAllIPs();
            updateButtonsState();

        } catch (SQLException e) {
            logger.error("Erreur lors de la libération de l'IP", e);
            showError("Erreur", "Impossible de libérer l'IP");
        }
    }

    @FXML
    private void handleReserve() {
        if (selectedIP == null) {
            showWarning("Attention", "Veuillez sélectionner une adresse IP");
            return;
        }

        if (!selectedIP.isAvailable()) {
            showWarning("Attention", "Cette IP n'est pas disponible");
            return;
        }

        try {
            String description = descriptionArea.getText();
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Une description est obligatoire pour la réservation");
            }

            ipAddressService.reserveIP(selectedIP.getId(), description);
            showSuccess("Succès", "IP réservée avec succès");
            clearForm();
            loadAllIPs();
            updateButtonsState();

        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText();
        if (searchTerm.isEmpty()) {
            loadAllIPs();
            return;
        }

        new Thread(() -> {
            try {
                List<IPAddress> results = ipAddressService.searchIPAddresses(searchTerm);
                Platform.runLater(() -> {
                    ipList.clear();
                    ipList.addAll(results);
                });
            } catch (SQLException e) {
                logger.error("Erreur lors de la recherche", e);
            }
        }).start();
    }

    @FXML
    private void handleExportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les adresses IP en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("ip_addresses.pdf");
        java.io.File file = chooser.showSaveDialog(ipTable.getScene().getWindow());
        if (file != null) {
            try {
                com.ipam.util.ExportUtil.exportTableViewToPdf(ipTable, file, "Adresses IP");
                showSuccess("Export", "PDF généré: " + file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Erreur export PDF", e);
                showError("Export", "Échec de l'export PDF: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les adresses IP en Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        chooser.setInitialFileName("ip_addresses.xlsx");
        java.io.File file = chooser.showSaveDialog(ipTable.getScene().getWindow());
        if (file != null) {
            try {
                com.ipam.util.ExportUtil.exportTableViewToExcel(ipTable, file, "Adresses IP");
                showSuccess("Export", "Excel généré: " + file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Erreur export Excel", e);
                showError("Export", "Échec de l'export Excel: " + e.getMessage());
            }
        }
    }

    private void fillFormWithIP(IPAddress ip) {
        assignedToField.setText(ip.getAssignedTo());
        macAddressField.setText(ip.getMacAddress());
        descriptionArea.setText(ip.getDescription());
    }

    private void clearForm() {
        assignedToField.clear();
        macAddressField.clear();
        descriptionArea.clear();
        selectedIP = null;
        ipTable.getSelectionModel().clearSelection();
        updateButtonsState();
    }

    private void showSuccess(String title, String message) {
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

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateButtonsState() {
        boolean hasSelection = selectedIP != null;
        boolean isAvailable = hasSelection && selectedIP.isAvailable();
        boolean isAssigned = hasSelection && !selectedIP.isAvailable();

        if (assignButton != null) assignButton.setDisable(!isAvailable);
        if (reserveButton != null) reserveButton.setDisable(!isAvailable);
        if (releaseButton != null) releaseButton.setDisable(!isAssigned);
    }
}
