# Perf2Gerber

Perf2Gerber est un outil de CAO léger conçu pour transformer des prototypes sur plaques d'essai (perfboards) en circuits imprimés (PCB) professionnels. Il offre un éditeur visuel intuitif basé sur une grille au pas standard de 2.54 mm et exporte directement les fichiers de fabrication industriels.

## Fonctionnalités

- **Éditeur visuel 2.54mm** : Routage rapide de pistes avec gestion personnalisée de l'épaisseur.
- **Double face** : Support des couches de cuivre Top (Rouge) et Bottom (Bleu) avec option de vue inversée (Back View).
- **Sérigraphie** : Ajout de textes personnalisables (taille, rotation) et librement déplaçables.
- **Ergonomie optimisée** : Outils dynamiques ("Spring-loaded" par maintien de touche), gomme sélective et historique complet (Undo/Redo).
- **Raccourcis persistants** : Configuration des touches sauvegardée nativement pour les futures sessions.

## Fichiers d'exportation (Standard Gerber RS-274X)

L'exportation génère une archive `.zip` en un clic, contenant la suite standard de fichiers requise par n'importe quel fabricant de circuits imprimés :

- `board.GML` : Contour physique de la carte (Edge Cuts).
- `board.DRL` : Fichier de perçage au format Excellon (trous traversants).
- `board.GTL` et `board.GBL` : Couches de routage en cuivre (Top et Bottom).
- `board.GTS` et `board.GBS` : Masques de soudure (Solder Mask / vernis d'épargne). Indiquent à l'usine où exposer le cuivre pour la soudure.
- `board.GTO` et `board.GBO` : Couches de sérigraphie (Silkscreen Top et Bottom) contenant vos textes et indications.

## Utilisation de l'éditeur

1. **Nouveau projet** Au lancement, définissez le nom du projet et la taille "utile" de votre carte (en nombre de trous). Un dossier de travail dédié (`.json`) est automatiquement créé pour vos sauvegardes.

2. **Dessin et Routage (Wire Tool)** Sélectionnez votre couche (Top ou Bottom) et utilisez l'outil Wire pour tracer vos pistes.
   - *Tracé simple :* Cliquez sur une pastille de départ, puis sur une pastille d'arrivée pour terminer le segment.
   - *Tracé en continu :* Maintenez enfoncée la touche de raccourci de l'outil Wire pour tracer une ligne ininterrompue qui suit vos clics. Relâchez la touche pour couper la piste.

3. **Gomme de précision (Erase Tool)** L'outil gomme permet une édition non destructive. Cliquez sur une pastille active pour la désactiver, ou cliquez précisément sur un segment de cuivre pour le supprimer sans effacer toute la piste.

4. **Sérigraphie (Text Tool & Pointer)** Sélectionnez l'outil Texte et cliquez sur la grille pour ajouter des annotations. Vous pouvez définir la taille exacte (en mm) et l'angle de rotation. Basculez ensuite sur l'outil Pointeur pour attraper vos textes et les glisser-déposer librement sur la carte.

5. **Exportation** Allez dans `File > Export to Gerber...`. L'archive prête pour la production industrielle est générée immédiatement dans le dossier racine de votre projet.