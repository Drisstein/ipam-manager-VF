# 🚀 GUIDE DE DÉMARRAGE RAPIDE - IPAM Manager

## ⚡ Démarrage Ultra-Rapide (5 minutes)

### Prérequis
- ✅ Java 17 ou supérieur installé
- ✅ Maven 3.6+ (ou utiliser les wrappers fournis)

### Méthode 1 : Script de Lancement (Recommandé)

#### Sur Linux/Mac :
```bash
cd ipam-manager
chmod +x run.sh
./run.sh
```

#### Sur Windows :
```batch
cd ipam-manager
run.bat
```

### Méthode 2 : Commandes Maven

```bash
cd ipam-manager

# Compiler
mvn clean compile

# Lancer
mvn javafx:run
```

### Méthode 3 : IDE (IntelliJ / Eclipse / NetBeans)

1. Importer le projet Maven
2. Attendre la synchronisation des dépendances
3. Lancer `com.ipam.MainApp`

---

## 📖 Premiers Pas dans l'Application

### 1️⃣ Créer votre Premier Sous-réseau

1. Cliquer sur **"Sous-réseaux"** dans le menu
2. Remplir le formulaire :
   - **Adresse Réseau** : `192.168.1.0`
   - **CIDR** : `24`
   - **Description** : `Réseau Test`
   - **Passerelle** : `192.168.1.1`
3. Cliquer sur **"Créer"**

✅ **Résultat** : 254 adresses IP sont automatiquement générées !

### 2️⃣ Assigner une Adresse IP

1. Aller dans **"Adresses IP"**
2. Sélectionner le sous-réseau `192.168.1.0/24`
3. Cliquer sur une IP verte (disponible)
4. Remplir :
   - **Assigné à** : `PC-Bureau-01`
   - **MAC** : `00:11:22:33:44:55`
   - **Description** : `Poste de travail`
5. Cliquer sur **"Assigner"**

✅ **Résultat** : L'IP passe en bleu (assignée) !

### 3️⃣ Consulter le Tableau de Bord

1. Cliquer sur **"Tableau de Bord"**
2. Voir les statistiques :
   - 1 sous-réseau
   - 254 IPs totales
   - 1 IP utilisée
   - Graphique de répartition

### 4️⃣ Vérifier l'Historique

1. Aller dans **"Historique"**
2. Voir toutes les opérations effectuées
3. Filtrer par action si besoin

---

## 🎯 Cas d'Utilisation Courants

### Scénario 1 : Nouvelle Entreprise

**Besoin** : Organiser 3 réseaux (bureaux, serveurs, invités)

```
1. Créer sous-réseau "Bureaux"
   - 192.168.10.0/24
   - Gateway: 192.168.10.1
   - DNS: 8.8.8.8, 8.8.4.4

2. Créer sous-réseau "Serveurs"
   - 192.168.20.0/24
   - Gateway: 192.168.20.1
   
3. Créer sous-réseau "Invités"
   - 192.168.30.0/25
   - Gateway: 192.168.30.1
```

### Scénario 2 : Attribution Systématique

**Besoin** : Documenter tous les équipements

```
Pour chaque équipement :
1. Sélectionner le sous-réseau approprié
2. Assigner IP + MAC
3. Description : Type d'équipement + Emplacement
```

### Scénario 3 : Réservation pour Infrastructure

**Besoin** : Réserver IPs pour imprimantes/serveurs

```
1. Sélectionner l'IP souhaitée
2. Cliquer "Réserver"
3. Description : "Imprimante RDC" ou "Serveur Web"
```

---

## 🔧 Résolution de Problèmes

### ❌ "JavaFX runtime components are missing"

**Solution** :
```bash
mvn clean install
mvn javafx:run
```

### ❌ "Cannot find java command"

**Solution** :
```bash
# Vérifier Java
java -version

# Si pas installé
# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# Mac
brew install openjdk@17

# Windows
# Télécharger depuis https://adoptium.net/
```

### ❌ L'application ne démarre pas

**Vérifications** :
1. Java 17+ installé : `java -version`
2. Pas d'autre instance en cours
3. Consulter les logs : `~/.ipam/logs/ipam-manager.log`

---

## 📚 Ressources

### Documentation Complète
- **README.md** : Documentation principale
- **ANALYSE_PROJET.md** : Analyse technique détaillée

### Fichiers Clés
- **pom.xml** : Configuration Maven
- **src/main/java/com/ipam/** : Code source
- **src/main/resources/** : Vues et styles

### Support
- Logs : `~/.ipam/logs/`
- Base de données : `~/.ipam/ipam.db`

---

## ✨ Fonctionnalités à Découvrir

### Recherche Rapide
Utilisez la barre de recherche dans chaque module pour trouver rapidement :
- Sous-réseaux par adresse ou description
- IPs par adresse, équipement ou MAC

### Statistiques en Temps Réel
Le tableau de bord se met à jour automatiquement avec :
- Taux d'utilisation
- Répartition des statuts
- Code couleur selon saturation

### Historique Complet
Chaque action est tracée :
- Qui a fait quoi et quand
- Filtres avancés
- Export possible

---

## 🎓 Conseils d'Utilisation

### Bonnes Pratiques

✅ **Descriptions claires** : Toujours décrire les sous-réseaux et équipements
✅ **Adresses MAC** : Les renseigner pour détecter les doublons
✅ **Réservations** : Utiliser pour équipements fixes (imprimantes, serveurs)
✅ **Historique** : Consulter régulièrement pour traçabilité

### À Éviter

❌ **Pas de description** : Rend la gestion confuse
❌ **Ignorer les alertes** : Rouge = réseau saturé, agir !
❌ **Libérer sans vérifier** : Toujours confirmer avant libération

---

## 🚀 Aller Plus Loin

### Extensions Futures
- Export CSV/PDF des rapports
- Import de configurations existantes
- Scan réseau automatique
- Support IPv6

### Personnalisation
- Modifier `styles.css` pour changer l'apparence
- Ajuster `logback.xml` pour le niveau de logging

---

## 📞 Contact & Feedback

Pour toute question, suggestion ou bug :
1. Consulter les logs dans `~/.ipam/logs/`
2. Vérifier la documentation
3. Activer le mode DEBUG si nécessaire

---

**Version** : 1.0.0  
**Date** : Janvier 2025  
**Statut** : ✅ Production Ready

🎉 **Bon usage de IPAM Manager !**
