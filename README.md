# IPAM Manager - Gestionnaire d'Adressage IP

## 📝 Description

IPAM Manager est une application desktop complète de gestion d'adressage IP (IP Address Management) développée en Java avec JavaFX. Elle permet de gérer efficacement des sous-réseaux, d'attribuer des adresses IP, de détecter les conflits et de maintenir un historique complet des opérations.

## ✨ Fonctionnalités

### 🌐 Gestion des Sous-réseaux
- Création de sous-réseaux avec notation CIDR
- Calcul automatique : première IP, dernière IP, broadcast, nombre d'hôtes
- Génération automatique de toutes les adresses IP utilisables
- Configuration de passerelle et serveurs DNS
- Support des VLANs
- Statistiques d'utilisation en temps réel
- Détection de chevauchement de sous-réseaux

### 📌 Gestion des Adresses IP
- Attribution manuelle ou automatique d'adresses IP
- Libération et réservation d'IPs
- Association avec adresses MAC
- Détection de conflits d'adresses MAC
- Statuts multiples : Disponible, Assignée, Réservée, Bloquée
- Filtrage par sous-réseau
- Recherche rapide multi-critères

### 📊 Tableau de Bord
- Vue d'ensemble des statistiques globales
- Nombre total de sous-réseaux et d'IPs
- Taux d'utilisation global avec code couleur
- Graphique circulaire de répartition des statuts
- Alertes visuelles pour les réseaux saturés

### 📜 Historique et Audit
- Journal complet de toutes les opérations
- Traçabilité utilisateur et horodatage
- Filtrage par action, type d'entité et date
- Recherche dans l'historique
- Export possible

### 🔍 Recherche et Filtres
- Recherche globale dans tous les modules
- Filtres avancés par critères multiples
- Résultats en temps réel

## 🛠️ Technologies Utilisées

- **Java 17** - Langage de programmation
- **JavaFX 21** - Interface graphique
- **SQLite** - Base de données embarquée
- **Maven** - Gestion de dépendances
- **SLF4J / Logback** - Logging
- **Apache Commons Net** - Utilitaires réseau

## 📋 Prérequis

- Java Development Kit (JDK) 17 ou supérieur
- Maven 3.6 ou supérieur (optionnel si utilisation de Maven Wrapper)

## 🚀 Installation et Exécution

### Option 1 : Avec Maven

```bash
# Cloner ou extraire le projet
cd ipam-manager

# Compiler le projet
mvn clean compile

# Exécuter l'application
mvn javafx:run
```

### Option 2 : Créer un JAR exécutable

```bash
# Créer le package
mvn clean package

# Le JAR sera dans target/ipam-manager-1.0.0.jar
# Exécuter avec:
java -jar target/ipam-manager-1.0.0.jar
```

### Option 3 : Utilisation avec IDE

1. Importer le projet dans IntelliJ IDEA, Eclipse ou NetBeans
2. S'assurer que le JDK 17+ est configuré
3. Exécuter la classe principale `com.ipam.MainApp`

## 📁 Structure du Projet

```
ipam-manager/
├── src/main/java/com/ipam/
│   ├── model/              # Entités (Subnet, IPAddress, etc.)
│   ├── dao/                # Accès aux données
│   ├── service/            # Logique métier
│   ├── controller/         # Contrôleurs JavaFX
│   ├── util/               # Utilitaires (IPCalculator, DatabaseManager)
│   └── MainApp.java        # Point d'entrée
├── src/main/resources/
│   ├── fxml/               # Fichiers FXML des vues
│   ├── css/                # Feuilles de style
│   └── logback.xml         # Configuration logging
└── pom.xml                 # Configuration Maven
```

## 💾 Base de Données

L'application utilise SQLite avec une base de données stockée dans :
- **Linux/Mac** : `~/.ipam/ipam.db`
- **Windows** : `C:\Users\<username>\.ipam\ipam.db`

### Schéma de Base de Données

**Table `subnets`**
- id, network_address, subnet_mask, cidr, description
- vlan_id, gateway, dns_servers
- created_date, modified_date

**Table `ip_addresses`**
- id, ip_address, subnet_id, status
- assigned_to, mac_address, description
- assigned_date, created_date

**Table `reservations`**
- id, ip_address_id, reserved_by
- reason, expiration_date, created_date

**Table `audit_logs`**
- id, action, entity_type, entity_id
- details, username, timestamp

## 📖 Guide d'Utilisation

### Créer un Sous-réseau

1. Aller dans **Gestion > Sous-réseaux**
2. Remplir le formulaire :
   - Adresse Réseau : `192.168.1.0`
   - CIDR : `24`
   - Description : `Réseau bureaux`
   - Passerelle : `192.168.1.1` (optionnel)
3. Cliquer sur **Créer**
4. Toutes les IPs du sous-réseau seront générées automatiquement

### Assigner une Adresse IP

1. Aller dans **Gestion > Adresses IP**
2. Sélectionner un sous-réseau dans le filtre
3. Sélectionner une IP disponible (statut vert)
4. Remplir les informations :
   - Assigné à : `PC-Bureau-01`
   - Adresse MAC : `00:11:22:33:44:55`
   - Description : Informations complémentaires
5. Cliquer sur **Assigner**

### Réserver une IP

1. Sélectionner une IP disponible
2. Remplir la description (obligatoire pour réservation)
3. Cliquer sur **Réserver**

### Consulter l'Historique

1. Aller dans **Outils > Historique d'Audit**
2. Utiliser les filtres pour affiner la recherche
3. Toutes les opérations sont enregistrées avec horodatage

## 🎨 Fonctionnalités Avancées

### Calculs Automatiques

L'application calcule automatiquement :
- Adresse réseau
- Première IP utilisable
- Dernière IP utilisable
- Adresse de broadcast
- Nombre total d'hôtes
- Masque de sous-réseau

### Validations

- Format d'adresse IP (IPv4)
- Format d'adresse MAC
- CIDR valide (0-32)
- Détection de chevauchement de sous-réseaux
- Détection de conflits d'adresses MAC
- Vérification de disponibilité des IPs

### Code Couleur

**Sous-réseaux** (taux d'utilisation) :
- 🟢 Vert : < 50%
- 🔵 Bleu : 50-74%
- 🟠 Orange : 75-89%
- 🔴 Rouge : ≥ 90%

**Adresses IP** (statut) :
- 🟢 Disponible
- 🔵 Assignée
- 🟠 Réservée
- 🔴 Bloquée

## 🔧 Configuration

### Modifier le Niveau de Logging

Éditer `src/main/resources/logback.xml` :

```xml
<logger name="com.ipam" level="DEBUG">  <!-- Changer en INFO, WARN, ERROR -->
```

### Personnaliser les Styles

Éditer `src/main/resources/css/styles.css` pour modifier les couleurs et styles.

## 🐛 Dépannage

### Erreur "JavaFX runtime components are missing"

**Solution** : S'assurer que JavaFX est bien inclus dans les dépendances Maven

### Base de données verrouillée

**Solution** : Fermer toutes les instances de l'application

### Erreur de permissions

**Solution** : S'assurer que l'utilisateur a les droits d'écriture dans `~/.ipam/`

## 📝 Logs

Les logs sont stockés dans :
- **Linux/Mac** : `~/.ipam/logs/ipam-manager.log`
- **Windows** : `C:\Users\<username>\.ipam\logs\ipam-manager.log`

## 🤝 Contribution

Ce projet est un projet éducatif/professionnel. Pour toute suggestion :

1. Créer une issue décrivant le problème ou la fonctionnalité
2. Proposer une solution ou amélioration

## 📄 Licence

Ce projet est fourni à des fins éducatives et professionnelles.

## 👤 Auteur

Développé pour le projet de gestion d'adressage IP.

## 🔮 Améliorations Futures

- Export PDF des rapports
- Export CSV des données
- Import de configurations existantes
- Scan réseau avec Nmap
- Support IPv6
- Mode client-serveur
- Authentification multi-utilisateurs
- Notifications par email
- Dashboard plus détaillé
- API REST pour intégration

---

## 📞 Support

Pour toute question ou problème, consulter les logs dans `~/.ipam/logs/` 
ou activer le mode DEBUG dans logback.xml.

**Version** : 1.0.0  
**Date** : Janvier 2025
