package com.ipam.controller;

import com.ipam.model.Subnet;
import com.ipam.service.SubnetService;
import com.ipam.util.IPCalculator;
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
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la gestion des sous-réseaux
 */
public class SubnetController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SubnetController.class);

    @FXML
    private TableView<Subnet> subnetTable;

    @FXML
    private TableColumn<Subnet, String> networkColumn;

    @FXML
    private TableColumn<Subnet, Integer> cidrColumn;

    @FXML
    private TableColumn<Subnet, String> descriptionColumn;

    @FXML
    private TableColumn<Subnet, String> gatewayColumn;

    @FXML
    private TableColumn<Subnet, Integer> totalHostsColumn;

    @FXML
    private TableColumn<Subnet, Integer> usedHostsColumn;

    @FXML
    private TableColumn<Subnet, String> usageColumn;

    @FXML
    private TextField searchField;

    @FXML
    private TextField networkAddressField;

    @FXML
    private ComboBox<Integer> cidrComboBox;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField vlanIdField;

    @FXML
    private TextField gatewayField;

    @FXML
    private TextField dnsServersField;

    @FXML
    private Label firstIpLabel;

    @FXML
    private Label lastIpLabel;

    @FXML
    private Label broadcastLabel;

    @FXML
    private Label totalIpsLabel;

    @FXML
    private Button exportPdfButton;

    @FXML
    private Button exportExcelButton;

    private final ObservableList<Subnet> subnetList = FXCollections.observableArrayList();
    private final SubnetService subnetService;
    private Subnet selectedSubnet;

    public SubnetController() {
        this.subnetService = new SubnetService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initialisation de la vue des sous-réseaux");
        
        // Initialiser les colonnes
        networkColumn.setCellValueFactory(new PropertyValueFactory<>("networkCidr"));
        cidrColumn.setCellValueFactory(new PropertyValueFactory<>("cidr"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        gatewayColumn.setCellValueFactory(new PropertyValueFactory<>("gateway"));
        totalHostsColumn.setCellValueFactory(new PropertyValueFactory<>("totalHosts"));
        usedHostsColumn.setCellValueFactory(new PropertyValueFactory<>("usedHosts"));
        
        // Colonne personnalisée pour l'utilisation
        usageColumn.setCellValueFactory(cellData -> {
            Subnet subnet = cellData.getValue();
            String usage = String.format("%.1f%%", subnet.getUsagePercentage());
            return new javafx.beans.property.SimpleStringProperty(usage);
        });

        // Styliser la colonne usage selon le pourcentage
        usageColumn.setCellFactory(column -> new TableCell<Subnet, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Subnet subnet = getTableRow().getItem();
                    if (subnet != null) {
                        double percentage = subnet.getUsagePercentage();
                        if (percentage >= 90) {
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        } else if (percentage >= 75) {
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #27ae60;");
                        }
                    }
                }
            }
        });

        subnetTable.setItems(subnetList);

        // Listener sur la sélection
        subnetTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    selectedSubnet = newValue;
                    fillFormWithSubnet(newValue);
                }
            }
        );

        // Initialiser le ComboBox CIDR
        cidrComboBox.setItems(FXCollections.observableArrayList(
            8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30
        ));
        cidrComboBox.setValue(24); // Valeur par défaut

        // Listener pour calculer automatiquement les infos du réseau
        networkAddressField.textProperty().addListener((obs, old, newVal) -> calculateNetworkInfo());
        cidrComboBox.valueProperty().addListener((obs, old, newVal) -> calculateNetworkInfo());

        // Charger les données
        loadSubnets();
    }

    private void loadSubnets() {
        new Thread(() -> {
            try {
                List<Subnet> subnets = subnetService.getAllSubnets();
                Platform.runLater(() -> {
                    subnetList.clear();
                    subnetList.addAll(subnets);
                });
            } catch (SQLException e) {
                logger.error("Erreur lors du chargement des sous-réseaux", e);
                Platform.runLater(() -> showError("Erreur", "Impossible de charger les sous-réseaux"));
            }
        }).start();
    }

    @FXML
    private void handleCreate() {
        try {
            validateForm();

            Subnet subnet = buildSubnetFromForm();
            
            // Vérifier les chevauchements
            if (subnetService.checkOverlap(subnet.getNetworkAddress(), subnet.getCidr())) {
                showWarning("Attention", "Ce sous-réseau chevauche un sous-réseau existant. Voulez-vous continuer?");
                return;
            }

            subnetService.createSubnet(subnet);
            showSuccess("Succès", "Sous-réseau créé avec succès");
            clearForm();
            loadSubnets();

        } catch (IllegalArgumentException e) {
            showError("Validation", e.getMessage());
        } catch (SQLException e) {
            logger.error("Erreur lors de la création du sous-réseau", e);
            showError("Erreur", "Impossible de créer le sous-réseau: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedSubnet == null) {
            showWarning("Attention", "Veuillez sélectionner un sous-réseau à modifier");
            return;
        }

        try {
            validateForm();

            selectedSubnet.setDescription(descriptionField.getText());
            selectedSubnet.setGateway(gatewayField.getText());
            selectedSubnet.setDnsServers(dnsServersField.getText());
            
            String vlanText = vlanIdField.getText();
            if (!vlanText.isEmpty()) {
                selectedSubnet.setVlanId(Integer.parseInt(vlanText));
            }

            subnetService.updateSubnet(selectedSubnet);
            showSuccess("Succès", "Sous-réseau mis à jour avec succès");
            clearForm();
            loadSubnets();

        } catch (IllegalArgumentException e) {
            showError("Validation", e.getMessage());
        } catch (SQLException e) {
            logger.error("Erreur lors de la mise à jour du sous-réseau", e);
            showError("Erreur", "Impossible de mettre à jour le sous-réseau");
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedSubnet == null) {
            showWarning("Attention", "Veuillez sélectionner un sous-réseau à supprimer");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer le sous-réseau ?");
        confirmAlert.setContentText(
            String.format("Êtes-vous sûr de vouloir supprimer le sous-réseau %s ?\n" +
                          "Toutes les adresses IP associées seront également supprimées.",
                          selectedSubnet.getNetworkCidr())
        );

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                subnetService.deleteSubnet(selectedSubnet.getId());
                showSuccess("Succès", "Sous-réseau supprimé avec succès");
                clearForm();
                loadSubnets();
            } catch (SQLException e) {
                logger.error("Erreur lors de la suppression du sous-réseau", e);
                showError("Erreur", "Impossible de supprimer le sous-réseau");
            }
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText();
        if (searchTerm.isEmpty()) {
            loadSubnets();
            return;
        }

        new Thread(() -> {
            try {
                List<Subnet> results = subnetService.searchSubnets(searchTerm);
                Platform.runLater(() -> {
                    subnetList.clear();
                    subnetList.addAll(results);
                });
            } catch (SQLException e) {
                logger.error("Erreur lors de la recherche", e);
                Platform.runLater(() -> showError("Erreur", "Erreur lors de la recherche"));
            }
        }).start();
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    @FXML
    private void handleExportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les sous-réseaux en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("subnets.pdf");
        java.io.File file = chooser.showSaveDialog(subnetTable.getScene().getWindow());
        if (file != null) {
            try {
                com.ipam.util.ExportUtil.exportTableViewToPdf(subnetTable, file, "Sous-réseaux");
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
        chooser.setTitle("Exporter les sous-réseaux en Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        chooser.setInitialFileName("subnets.xlsx");
        java.io.File file = chooser.showSaveDialog(subnetTable.getScene().getWindow());
        if (file != null) {
            try {
                com.ipam.util.ExportUtil.exportTableViewToExcel(subnetTable, file, "Sous-réseaux");
                showSuccess("Export", "Excel généré: " + file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Erreur export Excel", e);
                showError("Export", "Échec de l'export Excel: " + e.getMessage());
            }
        }
    }

    private void calculateNetworkInfo() {
        String ip = networkAddressField.getText();
        Integer cidr = cidrComboBox.getValue();

        if (ip == null || ip.isEmpty() || cidr == null) {
            firstIpLabel.setText("-");
            lastIpLabel.setText("-");
            broadcastLabel.setText("-");
            totalIpsLabel.setText("-");
            return;
        }

        if (!IPCalculator.isValidIP(ip)) {
            firstIpLabel.setText("IP invalide");
            return;
        }

        try {
            String networkAddress = IPCalculator.getNetworkAddress(ip, cidr);
            String firstIp = IPCalculator.getFirstUsableIp(networkAddress, cidr);
            String lastIp = IPCalculator.getLastUsableIp(networkAddress, cidr);
            String broadcast = IPCalculator.getBroadcastAddress(networkAddress, cidr);
            int totalHosts = IPCalculator.getTotalHosts(cidr);

            firstIpLabel.setText(firstIp);
            lastIpLabel.setText(lastIp);
            broadcastLabel.setText(broadcast);
            totalIpsLabel.setText(String.valueOf(totalHosts));
        } catch (Exception e) {
            firstIpLabel.setText("Erreur");
            lastIpLabel.setText("Erreur");
            broadcastLabel.setText("Erreur");
            totalIpsLabel.setText("Erreur");
        }
    }

    private void fillFormWithSubnet(Subnet subnet) {
        networkAddressField.setText(subnet.getNetworkAddress());
        cidrComboBox.setValue(subnet.getCidr());
        descriptionField.setText(subnet.getDescription());
        gatewayField.setText(subnet.getGateway());
        dnsServersField.setText(subnet.getDnsServers());
        
        if (subnet.getVlanId() != null) {
            vlanIdField.setText(subnet.getVlanId().toString());
        }

        calculateNetworkInfo();
    }

    private Subnet buildSubnetFromForm() {
        String networkAddress = IPCalculator.getNetworkAddress(
            networkAddressField.getText(), cidrComboBox.getValue());

        Subnet subnet = new Subnet(networkAddress, cidrComboBox.getValue(), descriptionField.getText());
        subnet.setGateway(gatewayField.getText());
        subnet.setDnsServers(dnsServersField.getText());
        
        String vlanText = vlanIdField.getText();
        if (!vlanText.isEmpty()) {
            subnet.setVlanId(Integer.parseInt(vlanText));
        }

        return subnet;
    }

    private void validateForm() {
        if (networkAddressField.getText().isEmpty()) {
            throw new IllegalArgumentException("L'adresse réseau est obligatoire");
        }

        if (!IPCalculator.isValidIP(networkAddressField.getText())) {
            throw new IllegalArgumentException("Adresse réseau invalide");
        }

        if (cidrComboBox.getValue() == null) {
            throw new IllegalArgumentException("Le CIDR est obligatoire");
        }

        if (descriptionField.getText().isEmpty()) {
            throw new IllegalArgumentException("La description est obligatoire");
        }
    }

    private void clearForm() {
        networkAddressField.clear();
        cidrComboBox.setValue(24);
        descriptionField.clear();
        vlanIdField.clear();
        gatewayField.clear();
        dnsServersField.clear();
        selectedSubnet = null;
        subnetTable.getSelectionModel().clearSelection();
        calculateNetworkInfo();
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
}
