# Perf2Gerber

Perf2Gerber est un outil de CAO (Conception Assistée par Ordinateur) léger, intuitif et fait sur mesure pour transformer vos prototypes sur plaques d'essai (perfboards/stripboards) en véritables circuits imprimés professionnels (PCB).

Que vous soyez en train de concevoir une nouvelle pédale d'effet pour guitare, un module de synthétiseur matériel, ou n'importe quel circuit électronique sur une grille au pas de 2.54 mm, Perf2Gerber vous permet de dessiner vos connexions visuellement et d'exporter instantanément les fichiers de fabrication industriels.

## Fonctionnalités principales

- Éditeur visuel intuitif : Dessinez vos pistes de cuivre directement sur une grille perforée virtuelle.
- Routage double face : Support des couches de cuivre supérieures (Top - Rouge) et inférieures (Bottom - Bleu).
- Gomme "chirurgicale" : Effacez des segments de piste précis ou déconnectez des pastilles sans détruire tout votre routage.
- Interpolation et snapping : Le logiciel comprend vos intentions, fusionne les pistes et active les pastilles traversées automatiquement.
- Largeur de piste ajustable : Choisissez l'épaisseur de vos pistes selon vos besoins en courant.
- Sauvegarde JSON : Enregistrez vos projets et reprenez-les plus tard.
- Exportation "1-click" pour usine : Génère automatiquement une archive `.zip` contenant la "Sainte Trinité" des fichiers Gerber (RS-274X) et Excellon, prête à être envoyée aux fabricants.

## Compatibilité de fabrication (JLCPCB, NextPCB, PCBWay...)

Le bouton **Export to Gerber...** génère une archive contenant toutes les couches nécessaires pour une production industrielle standard :

- `board.GML` : Contour de la carte (Edge Cuts) avec un décalage de sécurité automatique.
- `board.DRL` : Fichier de perçage (Excellon / trous traversants).
- `board.GTL` et `board.GBL` : Couches de cuivre (Top et Bottom).
- `board.GTS` et `board.GBS` : Masques de soudure (Solder Mask / vernis épargne).

## Captures d'écran

![Éditeur Perf2Gerber](lien_vers_image_de_votre_interface.png)

![Rendu Gerber Viewer](lien_vers_image_du_rendu_NextPCB.png)

## Installation et exécution

Ce projet est développé en Java avec JavaFX pour l'interface graphique.

### Prérequis

- Java Development Kit (JDK) 11 ou supérieur
- JavaFX SDK (si non inclus dans votre JDK)

### Comment lancer l'application

1. Clonez ce dépôt :

```bash
git clone https://github.com/votre-nom-utilisateur/Perf2Gerber.git
```

2. Ouvrez le projet dans votre IDE favori (IntelliJ IDEA, Eclipse, VS Code).
3. Assurez-vous que les bibliothèques JavaFX sont bien configurées dans votre environnement.
4. Lancez la classe principale `App.java`.

## Utilisation

1. **Nouveau projet**  
   Au lancement, définissez la taille "utile" de votre carte (en nombre de trous).

2. **Dessin**  
   Sélectionnez votre couche (Top/Bottom) et cliquez sur les pastilles pour tirer vos pistes. Le logiciel liera automatiquement vos traits.

3. **Ligne droite**  
   Maintenez la touche `Command` (ou `Ctrl`) pour dessiner en mode continu.

4. **Effacement**  
   Sélectionnez l'outil gomme. Cliquez sur une pastille dorée pour la désactiver, ou sur une ligne de cuivre pour la couper.

5. **Exportation**  
   Allez dans `File > Export to Gerber...`, choisissez l'emplacement de sauvegarde, et envoyez le fichier `.zip` généré à votre fabricant de PCB.

## Licence

(Ajoutez ici le type de licence que vous souhaitez utiliser, par exemple MIT, GPL, etc.)