package excel.create;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Calendar;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class RecuperationDataHeure13102023 {

    private static long currentSourceSizeA = 0;
    private static long previousSourceSizeA = 0;
    
    public static void main(String[] args) {

        String sourceDirectoryPathA = "C:/Users/axel.mfumu-lunanga/OneDrive - TRIGO SAS/Desktop/SERV FTP/images_IV3/IV00003/0000";
        String sourceDirectoryPathB = "C://Users/axel.mfumu-lunanga/OneDrive - TRIGO SAS/Desktop/SERV FTP/images_IV3/IV00003/0000";
        String destinationDirectoryPath = "C:/Users/axel.mfumu-lunanga/OneDrive - TRIGO SAS/Documents/IV3 Poissy/Recupdonner";
        String globalfilename = "Suivi_global_ctrl";
        String ncfilename = "Suivi_ctrl_non_conforme";

        try {

            // Crée un objet ServerSocket qui écoute sur le port 2000
            ServerSocket serverSocket = new ServerSocket(2000);

            // Attend une connexion entrante et accepte-la
            Socket socket = serverSocket.accept();

            // Affiche un message pour indiquer que la connexion est établie
            System.out.println("Connected to PLC");

            // Crée un InputStreamReader pour lire les données depuis le socket
            InputStreamReader inputSr = new InputStreamReader(socket.getInputStream(), "UTF-8");
            int read = 0;

            // Appel de la fonction pour créer/mettre à jour le fichier
            createOrUpdateFile(globalfilename);
            createOrUpdateFile(ncfilename);

            // Boucle infinie pour lire les données tant que la connexion est active
            while (true) {

                // Utilisez un StringBuilder pour accumuler les données
                StringBuilder data = new StringBuilder();

                // Lit les caractères depuis le flux d'entrée jusqu'à ce qu'on atteigne la fin (0)
                while ((read = inputSr.read()) != 0) {

                    // Ajoute le caractère lu au StringBuilder
                    data.append((char) read);
                }

                // Si des données ont été lues
                if (data != null && !data.toString().equals("")) {

                    // Divise les données en paramètres en utilisant des espaces comme séparateurs
                    String[] parameters = data.toString().split("\\s+");
                       
                    // Obtenir la taille actuelle du fichier source
                    currentSourceSizeA = getSourceDirectorySize(sourceDirectoryPathA);
                    

                    // Obtient l'heure actuelle
                    String timestamp = getCurrentTimestamp();
                    String Date = getCurrentDate();
                    // Affiche chaque paramètre
                    for (int i = 1; i < parameters.length; i++) {
                        System.out.println(timestamp + " : " + parameters[i]);
                    }

                 

                    // Attend que la taille du fichier source change pour l'un des répertoires
                    while (currentSourceSizeA <= previousSourceSizeA) {
                        currentSourceSizeA = getSourceDirectorySize(sourceDirectoryPathA);
                        Thread.sleep(500); // Attendre 500 millisecondes avant de vérifier à nouveau
                        System.out.println("la");
                        System.out.println(currentSourceSizeA);
                        

                    }
                    previousSourceSizeA = getSourceDirectorySize(sourceDirectoryPathA);
                    // Appeler la fonction pour copier les trois derniers fichiers
                    int num_voitures = copierDerniersFichiers(sourceDirectoryPathA, sourceDirectoryPathB, destinationDirectoryPath);

                    // Appel de la fonction pour ajouter les paramètres au fichier
                    appendToFile(Date,timestamp, parameters, num_voitures, globalfilename);


                }

            }
        } catch (IOException e) {

            // Gère les erreurs de communication
            System.out.println("Communication error: " + e);
        } catch (InterruptedException e) {

            // Gère les erreurs d'interruption de thread
            System.out.println("Thread interrupted: " + e);
        }
    }

    private static int copierDerniersFichiers(String sourceDirectoryPathA, String sourceDirectoryPathB, String destinationDirectoryPath) {
        File sourceDirectoryA = new File(sourceDirectoryPathA);
        File sourceDirectoryB = new File(sourceDirectoryPathB);
        File destinationDirectory = new File(destinationDirectoryPath);

        // Initialisez nextFolderName à l'extérieur du bloc try
        String nextFolderName = null;
        int nextFolderNumber = 1; // Le numéro du prochain dossier "voiture n°X"


        // Vérifie si le répertoire source existe
        if (sourceDirectoryA.exists() && sourceDirectoryA.isDirectory()&& sourceDirectoryB.exists() && sourceDirectoryB.isDirectory()) {
            
            // Liste tous les fichiers du répertoire source
            File[] filesA = sourceDirectoryA.listFiles();
            File[] filesB = sourceDirectoryB.listFiles();

            // Trie les fichiers en fonction de leur date de modification (du plus récent au plus ancien)
            Arrays.sort(filesA, Comparator.comparingLong(File::lastModified).reversed());
            Arrays.sort(filesB, Comparator.comparingLong(File::lastModified).reversed());

            // Vérifie si le répertoire de destination existe, sinon, crée-le
            if (!destinationDirectory.exists()) {
                destinationDirectory.mkdirs();
            }

            // Liste tous les dossiers existants dans le répertoire de destination
            File[] existingFolders = destinationDirectory.listFiles(File::isDirectory);

            // Trie les dossiers en fonction de leur nom (ordre lexicographique inverse)
            Arrays.sort(existingFolders, Comparator.comparing(File::getName).reversed());
            
                    // Si des dossiers existent, déterminez le numéro du prochain dossier "voiture n°X"
            if (existingFolders.length > 0) {
                int maxFolderNumber = 0;
                for (File folder : existingFolders) {
                    String folderName = folder.getName();
                    if (folderName.startsWith("voiture n°")) {
                        try {
                            int folderNumber = Integer.parseInt(folderName.substring(10));
                            if (folderNumber > maxFolderNumber) {
                                maxFolderNumber = folderNumber;
                            }
                        } catch (NumberFormatException e) {
                        // En cas d'erreur de conversion, ignorez ce dossier
                        }
                    }   
                }
                // Incrémentation du numéro de dossier
                nextFolderNumber = maxFolderNumber + 1;
            }

            // Créez le prochain dossier "voiture n°X" dans le répertoire de destination
            nextFolderName = "voiture n°" + nextFolderNumber;
            File nextDestinationDirectory = new File(destinationDirectory, nextFolderName);
            nextDestinationDirectory.mkdirs();

            //créez les répertoires "Porte Gauche" et "Porte Droite" en fonction de la source
            String porteGauchePath = nextDestinationDirectory.getPath() + "/Porte Gauche";
            String porteDroitePath = nextDestinationDirectory.getPath() + "/Porte Droite";

            File porteGaucheDirectory = new File(porteGauchePath);
            File porteDroiteDirectory = new File(porteDroitePath);

            porteGaucheDirectory.mkdirs();
            porteDroiteDirectory.mkdirs();

            for (int i = 0; i < 2 && i < filesA.length; i++) {
                File file = filesA[i];
                try {
        
                    // Construit le chemin de destination en ajoutant le nom du fichier
                    String destinationFilePathA = porteGauchePath + "/" + file.getName();
        
                    // Copie le fichier vers le répertoire de destination approprié
                    Path sourcePath = file.toPath();
                    Path destinationPathA = new File(destinationFilePathA).toPath();
                    Files.copy(sourcePath, destinationPathA, StandardCopyOption.REPLACE_EXISTING);
        
                    System.out.println("Porte gauche : Fichier copié avec succès : " + file.getName());
        
                } catch (IOException e) {
                    System.out.println("Erreur lors de la copie du fichier : " + file.getName());
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < 2 && i < filesB.length; i++) {
                File file = filesB[i];
                try {
                    
                    String destinationFilePathB = porteDroitePath + "/" + file.getName();
                    Path sourcePath = file.toPath();
                    // Copie le fichier vers le répertoire de destination approprié
                    Path destinationPathB = new File(destinationFilePathB).toPath();
                    Files.copy(sourcePath, destinationPathB, StandardCopyOption.REPLACE_EXISTING);
        
                    System.out.println("Porte droite : Fichier copié avec succès : " + file.getName());
        
                } catch (IOException e) {
                    System.out.println("Erreur lors de la copie du fichier : " + file.getName());
                    e.printStackTrace();
                }
            }
            System.out.println(nextFolderName);
            System.out.println("________________________________________________________");
            
        } else {
            System.out.println("Le répertoire source n'existe pas.");
        }
        return nextFolderNumber;
    }

    // Fonction pour créer ou mettre à jour le fichier suivi_global 
    private static void createOrUpdateFile(String nom_fichier) {
        try {
            // Créez un nouveau classeur Excel
            Workbook workbook = new XSSFWorkbook();

            // Créez une feuille de calcul dans le classeur
            Sheet sheet = workbook.createSheet("Données");

            // Écrivez les en-têtes
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Date");
            headerRow.createCell(1).setCellValue("Heure");
            headerRow.createCell(2).setCellValue("Voiture");
            headerRow.createCell(3).setCellValue("Résultat IV3 gauche");
            headerRow.createCell(4).setCellValue("Résultat IV3 droite");
            // Enregistrez le classeur dans un fichier
            FileOutputStream fileOut = new FileOutputStream(nom_fichier + ".xlsx");
            workbook.write(fileOut);
            fileOut.close();

            // Fermez le classeur
            workbook.close();
        } catch (IOException e) {
            System.out.println("File creation/update error: " + e);
        }
    }

    // Fonction pour ajouter les paramètres au fichier suivi
    private static void appendToFile(String Date ,String timestamp, String[] parameters, int voitures, String nom_fichier) {
        try {
                // Ouvrez le classeur existant
                FileInputStream fileIn = new FileInputStream(nom_fichier + ".xlsx");
                Workbook workbook = WorkbookFactory.create(fileIn);
                fileIn.close();
        
                // Accédez à la feuille de calcul
                Sheet sheet = workbook.getSheetAt(0);
        
                // Trouvez la dernière ligne occupée
                int rowCount = sheet.getPhysicalNumberOfRows();
        
                // Créez une nouvelle ligne
                Row row = sheet.createRow(rowCount);
        
                // Remplissez les cellules
                row.createCell(0).setCellValue(Date);
                row.createCell(1).setCellValue(timestamp);
                row.createCell(2).setCellValue("Voiture n°" + voitures);
                row.createCell(3).setCellValue(parameters[1]);
        
                // Enregistrez le classeur dans le même fichier
                FileOutputStream fileOut = new FileOutputStream(nom_fichier + ".xlsx");
                workbook.write(fileOut);
                fileOut.close();
        
                // Fermez le classeur
                workbook.close();
            } catch (IOException e) {
                System.out.println("File append error: " + e);
            }
    }

    // Fonction pour obtenir l'heure actuelle au format "HH:mm:ss"
    private static String getCurrentTimestamp() {
        // Obtenez l'heure actuelle
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
    
        // Soustrayez 4 secondes à l'heure actuelle
        calendar.add(Calendar.SECOND, -4);
    
        // Formatez l'heure au format "HH:mm:ss"
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    //Fonction pour obtenir la date actuelle 
    private static String getCurrentDate(){
        SimpleDateFormat dateSdf = new SimpleDateFormat("dd-MM-yyyy",Locale.getDefault());
        String currentDate = dateSdf.format(new Date());
        return currentDate;
    }

    // Fonction pour obtenir la taille du répertoire source
    private static long getSourceDirectorySize(String sourceDirectoryPath) {
        File sourceDirectory = new File(sourceDirectoryPath);

        long totalSize = 0;

        if (sourceDirectory.exists() && sourceDirectory.isDirectory()) {
            File[] files = sourceDirectory.listFiles();

            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                }
            }
        }

        return totalSize;
    }
}
