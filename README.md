# Order Report Refactoring (Legacy)

## Installation

### Prérequis
- Java 17 (ou 11, selon votre choix)
- Maven 3.8+

### Installer les dépendances
```bash
mvn install
```

---

## Exécution

### Exécuter le code refactoré

```bash
mvn exec:java "-Dexec.mainClass=OrderReportApp"
```

> Remarque : le code refactoré doit lire les mêmes CSV dans `legacy/data/` et produire **exactement** le même texte que le legacy.

### Exécuter les tests

```bash
mvn test
```

### Validation non-régression (Golden Master)

Le test Golden Master :

1. exécute le legacy
2. capture sa sortie
3. écrit la référence dans `legacy/expected/report.txt` (si absente)
4. exécute le refactor
5. compare les sorties strictement

Commande :

```bash
mvn -Dtest=GoldenMasterTest test
```

---

## Choix de Refactoring

### Problèmes identifiés dans le legacy

1. **God Method / responsabilités mélangées**

   * Impact : I/O, parsing, règles métier, agrégation, formatage et export sont couplés → maintenance et tests difficiles.

2. **Typage faible (Map<String, Object>) + casts**

   * Impact : erreurs runtime possibles, compréhension difficile, refactor risqué.

3. **Parsing CSV fragile et incohérent**

   * Impact : `split(",")`, lecture via plusieurs APIs, erreurs silencieuses → données ignorées sans trace.

4. **Magic numbers + règles cachées**

   * Impact : règles métier implicites (bonus matin/week-end, paliers shipping/discount) difficiles à isoler/tester.

5. **Side effects cachés**

   * Impact : une même méthode retourne un résultat et fait des écritures (stdout, fichier JSON).

### Solutions apportées

1. **Séparation des couches**

   * `io/` : lecture fichiers, écriture éventuelle
   * `domain/` : modèles (Customer, Product, Order, Promotion, Totals)
   * `service/` : règles (discount, tax, shipping, currency)
   * `report/` : rendu texte (format strictement identique)
   * Justification : réduit le couplage, augmente la testabilité, change minimal côté comportement.

2. **Modèles typés**

   * Remplacement progressif des `Map` par des classes immuables (ou records).
   * Justification : reduction d'erreurs, limite les casts, clarifie les champs obligatoires/optionnels.

3. **Isolation des règles métier**

   * Extraction de fonctions pures :

      * `computeLineTotal(...)` (promo + morning bonus)
      * `computeVolumeDiscount(...)`
      * `computeLoyaltyDiscount(...)`
      * `computeTax(...)`
      * `computeShipping(...)`
   * Justification : test unitaire simple + moins de régression.

4. **Tests**

- 1 Golden Master (non-régression globale)
- Tests unitaires ciblés sur :
    - calcul des remises
    - calcul des taxes
    - frais de port
    - bonus matin
    - promotions (PERCENTAGE / FIXED)

Objectif : sécuriser les règles à paliers et les branches conditionnelles.

### Architecture choisie

* **Pipeline** :

   1. Load + parse (`legacy/data/*.csv`)
   2. Build aggregates (totaux par client)
   3. Compute pricing (discount/tax/shipping/handling/currency)
   4. Render report (format identique)

* Dépendances :
   * Standard library + (optionnel) parser CSV léger
   * JUnit 5 pour tests

### Exemples concrets

**Exemple 1 : Extraction du calcul des remises**

* Problème : paliers dispersés + écrasement de valeurs.
* Solution : une fonction `computeVolumeDiscount(subtotal, level, dayOfWeek)` qui reproduit strictement la logique existante (bugs inclus).

**Exemple 2 : Golden Master**

* Problème : risque de “corriger” involontairement un bug legacy.
* Solution : comparaison caractère par caractère avec la sortie legacy.

---

## Limites et améliorations futures

### Ce qui n'a pas été fait (par manque de temps)

* [ ] Terminer la séparation des responsabilités (ex: ReportGenerator qui fait encore à la fois des calculs métier et du formattage)
* [ ] Remplacer toutes les règles “hardcodées” par une configuration (YAML/JSON)
* [ ] Améliorer le parsing CSV (quotes, virgules, encodage) sans changer le comportement
* [ ] Ajouter des tests d’intégration avec datasets variés
* [ ] Améliorer la gestion des erreurs (logs), qui sont souvent ignorées

### Compromis assumés

* Conservation volontaire de comportements discutables (bugs legacy) pour respecter la non-régression.
* Logging limité pour ne pas polluer stdout et casser le Golden Master.

### Pistes d'amélioration

* Externaliser les règles de pricing (discount/shipping/tax) vers une config versionnée.
* Remplacer les taux de change hardcodés par une source contrôlée (si le métier l’exige).
* Ajouter une “report model” intermédiaire pour découpler encore plus le rendu.

---

## Commandes utiles (récap)

```bash
# Tous les tests
mvn test

# Golden master seulement
mvn -Dtest=GoldenMasterTest test

# Run app refactorée
mvn exec:java "-Dexec.mainClass=OrderReportApp"
```