# Procédure nouvelle release

Cette procédure explicite quelles étapes réaliser pour diffuser une nouvelle versiion du File Format Checker en opérationel.
Elle n'a pas vocation à se trouver dans le GitHub (ce fichier fait parti du gitIgnore).

pré-requis: 
- code, les fichiers de configuration et/ou les tables NVS mis-à-jour
- JDK installée
- git et maven installés 

1. Contrôle des tables NVS avec le [script nvs-tables-download](https://gitlab.ifremer.fr/amrit/development/nvs-tables-download).

2. Création d'une branche sur le Gitlab avec la nomenclature `vx.y.z`. Pour cette branche:
    - modifier le fichier file_checker_exec/pom.xml avec la nouvelle version du FileChecker en ligne 7: 
    `<version>x.y.z</version>`
    - Initier un Merge Request et indiquer les modifications depuis la version précédente (N.B. ce peut être fait après l'étape 5)

3. Créer un dépôt local de cette branche gitlab 
   `git clone vx.y.z https://gitlab.ifremer.fr/amrit/development/format-checker.git`
   - mettre à jour les tables NVS sous /file_checker_spec/NVS
   - si nécessaire, mettre à jour les autres fichiers 
   - ne pas oublier de faire un `git add file` si le fichier est nouveau et un `git commit -m "texte du commit"` sur les fichiers modifiés
   - reporter les modifications du dépôt local vers la branche GitLab: `git push origin vx.y.z` 
        (N.B.: cette dernière étape peut-être faite après les étapes 4 et 5)
  puis
    - Depuis /file_checker_exec : execution des test "end-to-end" internes `mvn verify` ou `mvnw verify` si depuis Windows
    - Depuis /file_checker_exec : Génerer l'executable *.jar `mvn clean package` ou `mvnw clean package` si depuis Windows
    - le .jar généré est situé sous /file_checker_exec/target/
    - déposer le .jar généré dans le texte de la Merge Request créée

4. Comparaison ancienne version et nouvelle version sur un ensemble de fichiers (typiquement les fichiers mis-à-jour au GDAC sur les 6 derniers jours) en utilisant le script [compare_version.py](https://gitlab.ifremer.fr/amrit/development/format-checker-gdac-audit). Le Readme indique un exemple pour sélectionner l'ensemble de fichier. Cette non-régression exhaustive peut être assez longue (par exemple, 6 jours de soumission, cela représente 29000 fichiers et prend environ 7 heures). On peut raccourcir bien évidemment la période visée au besoin.

5. Valider les nouveautés de la version (nouvelle entrée table importante, nouvelle fonction)

6. Tester sur coriolis_dev (/home/coriolis_dev/val/binlx/co03/co0308/co030801/co03080102/) : 
    - Copier le nouveau .jar et remplacer le file_checker_spec dans /exe
    - Modifier le ArgoFileChecker.csh pour faire référence au nouvel executable.
    - Modifier le /tst/TU_YL_20241114.csh pour le nom des fichiers logs
    - lancer le script /tst/TU_YL_20241114.csh
    - Répéter les deux étapes précédentes pour changer le nom du DAC (aoml, coriolis et incois sont disponibles en test)
    - Controler la bonne execution du File Checker dans le fichier execution_summary_x.y.z.log et éventuellement dans les erreurs dans les fichiers présents sous /home/coriolis_dev/val/spool/co03/co0308/co030801/co03080102/{DAC_NAME}/output. Pour le DAC Coriolis, 18 fichiers sont acceptés sur les 64.

7. Accepter et merger la Merge Request sur le Gitlab.

8. Créer le tag de version `x.y.z` sur le gitlab ifremer.

9. Copie de l'executable `.jar` et de file_checker_spec sur coriolis_exp /home/coriolis_exp/binlx/co03/co0308/co030801/co03080102/
	- modification du fichier ArgoFileChecker.csh sur coriolis_exp pour faire référence à la nouvelle version.

10. Modifier le document de suivi coriolis sur le google drive coriolis-doc/modifications -> indiquer les modifications, évolutions, heure locale.

11. Sur le [repo github](https://github.com/OneArgo/ArgoFormatChecker) public: Créer une branche, faire une PR bien documentée, et fusionner la PR.

12. Faire une release en ajoutant le .jar en pièce jointe.

13. Envoyer un message à [Michael Frost](Michael.Frost@nrlmry.navy.mil) en donnant le lien de la nouvelle release et en decrivant en quelques mots ce qu'apporte la nouvelle version, et mettre en copie l'AVTT chair (https://github.com/OneArgo/ArgoVocabs/blob/master/README.md#i-the-argo-vocabulary-task-team).

14. Faire une news ou amender une news (pour les versions mineures) sur https://www.argodatamgt.org/news/. Par exemple https://www.argodatamgt.org/news/20260421-ArgoFileFormatCheckerV3.html

15. Faire un message à Argo-dm pour informer tous les DACs de cette nouvelle version.


