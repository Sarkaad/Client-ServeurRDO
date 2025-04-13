import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import utils.TokenGenerator;
//Liste des imports pour la lecture des deux fichier .txt au démarrage du serveur
import utils.TokenGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;
//Import pour encodage
import java.util.Base64;
import java.nio.charset.StandardCharsets;
//import pour les telechargements de fichier
import java.nio.file.Files;
import java.nio.file.Paths;



//Déploiement Serveur + attente de connexion avec le client
public class MainServer {

    //Exemple de liste de fichier qu'on prend depuis le fichier de configuration
    static ArrayList<String> filesList = new ArrayList<>();
    
	public static void main(String[] args)   {
        //Fonction pour charger les fichiers
        chargerConfig();

        try {
            //le serveur écoute sur le port 5000
            ServerSocket serverSocket = new ServerSocket(5000);
            System.out.println("Serveur démarré, en attente des clients...");

            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté : " + clientSocket.getInetAddress());

                //Création de thread pour chaque client
                new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                        boolean isRegistered = false;
                        String token = null;

                        //Attente du message REGISTER
                        String messageClient;

                        while ((messageClient = in.readLine()) != null){

                            System.out.println("Message recu : " + messageClient);
                            if(!isRegistered) {
                                if (messageClient.startsWith("REGISTER")){
                                    token = TokenGenerator.generateToken();
                                    out.println("REGISTERED|" + token + "|" );
                                    System.out.println("Jeton envoyé : " + token);
                                    isRegistered = true;
                                }else {
                                    out.println("ERROR ! Vous devez vous enregistrer avant tout !");
                                }

                            } else {
                                //Une fois ici on considère que le client est bien enregistré, on peut donc s'occuper des autres commandes
                                //Commande LS
                                if (messageClient.startsWith("LS")){
                                    //Nous voulons recevoir la commande ls + le token
                                    String[] parts = messageClient.split("\\|");
                                    if(parts.length >= 2 && parts[1].equals(token)) {
                                        //implémentation de la réponse LS. la réponse est composé du nombre de fichiers et de leur noms.
                                        StringBuilder response = new StringBuilder("LS|" + filesList.size());
                                        for (String file : filesList){
                                            response.append("|").append(file);
                                        }
                                        response.append("|");
                                        out.println(response.toString());
                                        System.out.println("LS sent : " + response.toString());
                                    } else {
                                        out.println("LS|UNAUTHORIZED");
                                    }
                                } else if (messageClient.startsWith("READ")) {
                                    //out.println("ERROR ! Commande inconnue");
                                    //nous voulons recevoir de la part du client READ + JETON + NOM DU FICHIER
                                    String[] parts = messageClient.split("\\|");
                                    if (parts.length >= 3 && parts[1].equals(token) ) {
                                        String fileName = parts[2];
                                        //Pour le bon fonctionnement du read redirect nous allons commencer ici par vérifier si le fichier que nous voulons lire existe localement
                                        java.nio.file.Path localPath = java.nio.file.Paths.get("config",fileName);
                                        if (java.nio.file.Files.exists(localPath)) {

                                            try {
                                                //lecture du fichier en UTF-8
                                                byte[] fileBytes = java.nio.file.Files.readAllBytes(localPath);
                                                String fileContent = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);

                                                int fragmentSize = 500;
                                                int totalLength = fileContent.length();
                                                int numFragments = (totalLength + fragmentSize - 1 ) / fragmentSize;


                                                //Affichage de l'envoi par fragement
                                                System.out.println("Contenu total (" + totalLength + " Caractères ), découpé en " + numFragments + " fragments.");

                                                //Envoi du contenu par fragments
                                                for (int i = 0; i < numFragments; i++) {
                                                    int start = i * fragmentSize;
                                                    int end = Math.min(start + fragmentSize, totalLength);
                                                    String fragment = fileContent.substring(start, end);
                                                    int isLast = (i == numFragments - 1) ? 1 : 0;
                                                    //On encode le fichier en base 64
                                                    String encodedFragment = Base64.getEncoder().encodeToString(fragment.getBytes(StandardCharsets.UTF_8));
                                                    out.println("FILE|" + fileName + "|"  + i + "|" + isLast + "|" + encodedFragment);
                                                    System.out.println("Fragment " + i + " envoyé (offset = " + start + ", isLast = " + isLast + ")");
                                                }
                                            } catch (IOException e) {
                                                out.println("ERROR ! Fichier introuvable");
                                                System.out.println("Erreur lors de la lecture du fichier " + fileName);
                                            }

                                        } else {
                                            // Le fichier n'existe pas localement : Vérifier dans la Files_list pour une redirection
                                            boolean redirectFound = false;
                                            for (String entry : filesList) {
                                                // On suppose que chaque entrée est sous la forme "nomFichier ip:port"
                                                String[] entryParts = entry.split(" ");
                                                // entryParts[0] correspond au nom du fichier
                                                if (entryParts[0].equals(fileName) && entryParts.length >= 2) {
                                                    //On decompose l'adresse de redirection
                                                    String[] addrParts = entryParts[1].split(":");
                                                    if (addrParts.length == 2) {
                                                        String remoteIP = addrParts[0];
                                                        String remotePort = addrParts[1];
                                                        // Ici nous créons un nouveau token qui sera utilisé pour la connexion au serveur distant
                                                        String newToken = TokenGenerator.generateToken();
                                                        out.println("READ-REDIRECT|" + remoteIP + "|" + remotePort + "|" + newToken + "|");
                                                        System.out.println("REDIRECT envoyé : READ-REDIRECT|" + remoteIP + "|" + remotePort + "|" + newToken + "|");
                                                        redirectFound = true;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (!redirectFound) {
                                                out.println("READ|ERROR|Fichier non trouvé|");
                                                System.out.println("Fichier " + fileName + " non trouvé localement et pas de redirection définie.");
                                            }
                                        }


                                    } else {
                                        out.println("READ|ERROR|Format incorrect ou token invalide|");
                                    }
                                } else {
                                    out.println("ERROR ! Commande inconnue");
                                }

                            }

                        }

                    }catch(IOException e){
                        e.printStackTrace();
                    }


                }).start();
            }


        }catch(IOException e) {
            e.printStackTrace();
        }



	}
    //------------------------

    //Listes pour stocker les configurations
    static ArrayList<String> peersList = new ArrayList<>();
    //static ArrayList<String> filesList = new ArrayList<>();

    //Méthode pour charger les configurations au démarrage
    public static void chargerConfig(){
        try {
            //lecture du fichier Peers_list
            Scanner scannerPeers = new Scanner(new java.io.File("config/Peers_list.txt"));
            while (scannerPeers.hasNextLine()){
                String peer = scannerPeers.nextLine().trim();
                if(!peer.isEmpty()) peersList.add(peer);
            }
            scannerPeers.close();

            //lecture des fichiers disponible
             Scanner scanner = new Scanner(new File("config/Files_list.txt"));
             while (scanner.hasNextLine()){
                 String file = scanner.nextLine().trim();
                 if(!file.isEmpty()) filesList.add(file);
             }
             scanner.close();

             //Affichage clair pour vérifier
            System.out.println("Peers chargés : " + peersList);
            System.out.println("Files disponible : " + filesList);

        }catch(FileNotFoundException e){
            System.out.println("Erreur : fichier de configuration introuvable");
        }
    }

}



