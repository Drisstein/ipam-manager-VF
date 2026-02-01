#!/bin/bash

echo "======================================"
echo "  IPAM Manager - Démarrage"
echo "======================================"
echo ""

# Vérifier si Java est installé
if ! command -v java &> /dev/null; then
    echo "❌ Java n'est pas installé ou n'est pas dans le PATH"
    echo "   Veuillez installer Java 17 ou supérieur"
    exit 1
fi

# Vérifier la version de Java
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17 ou supérieur est requis"
    echo "   Version actuelle: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java version: $(java -version 2>&1 | head -n 1)"
echo ""

# Vérifier si Maven est installé
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven n'est pas installé"
    echo "   Veuillez installer Maven 3.6 ou supérieur"
    exit 1
fi

echo "✅ Maven version: $(mvn -version | head -n 1)"
echo ""

# Compiler si nécessaire
if [ ! -d "target" ] || [ ! -f "target/classes/com/ipam/MainApp.class" ]; then
    echo "📦 Compilation du projet..."
    mvn clean compile
    if [ $? -ne 0 ]; then
        echo "❌ Erreur lors de la compilation"
        exit 1
    fi
    echo "✅ Compilation réussie"
    echo ""
fi

# Lancer l'application
echo "🚀 Lancement de IPAM Manager..."
echo ""
mvn javafx:run
