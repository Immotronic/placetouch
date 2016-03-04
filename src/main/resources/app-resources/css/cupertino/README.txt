/////////////////////////////////
//                             //
// Cupertino CSS customization //
//                             //
/////////////////////////////////

* Etape (1)

À partir du downloader du site JQUERY UI 
	(http://jqueryui.com/download/)
	
	- Toutes les options sont sélectionnées.
	- Thème sélectionné doit être cupertino
	- le "Theme Folder Name" garde la valeur "cupertino"
	
* Etape (2)

Une fois téléchargé, le fichier jquery-ui-1.10.3.custom.css a été modifier comme suit:

	- Recherche des occurrences de "Lucida Grande"
		--> Il s'avère qu'il y en a 2: lignes 789 et 799
			(définition des style ".ui-widget" et ".ui-widget button"
	- Il faut remplacer les définitions "font-family"  et "font-size" présentent par
		
		font-family: Helvetica,Arial,sans-serif;
		font-size: 1.0em;
		
