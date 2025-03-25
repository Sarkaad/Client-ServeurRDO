import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

//
import utils.TokenGenerator;

//Liste des imports pour la lecture des deux fichier .txt au démarrage du serveur
import utils.TokenGenerator;

import java.util.ArrayList;
import java.util.Scanner;



//Déploiement Serveur + attente de connexion avec le client
public class MainServer {

    //Exemple de liste de fichier qu'on prend depuis le fichier de configuration
    static ArrayList<String> filesList = new ArrayList<>();
    
	public static void main(String[] args)   {
        chargerConfig();
        
        //Ajouts de fichier manuellement 
        filesList.add("notes.txt");
        filesList.add("notes2.pdf");
        
        
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
                                if (messageClient.startsWith("LS")){
                                    //Nous voulons receoit la commande ls + le token
                                    String[] parts = messageClient.split("\\|");
                                    if(parts.length >= 2 && parts[1].equals(token)) {
                                        //implémentation de la réponse LS
                                        StringBuilder response = new StringBuilder("LS|" + filesList.size());
                                        for (String file : filesList){
                                            response.append("|").append(file);
                                        }
                                        response.append("|");
                                        out.println(response.toString());
                                        System.out.println("LS sent : " + response.toString());
                                    } else {
                                        out.println("ERROR ! Format incorrect");
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
    //---------------------------

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



