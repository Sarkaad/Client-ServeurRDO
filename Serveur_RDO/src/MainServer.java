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
	public static void main(String[] args)   {
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

                        //Attente du message REGISTER
                        String messagleClient = in.readLine();
                        System.out.println("Message recu : " + messagleClient);

                        if (messagleClient.startsWith("REGISTER")){
                            String token = TokenGenerator.generateToken();
                            out.println("REGISTERED|" + token + "|" );
                            System.out.println("Jeton envoyé : " + token);
                        }

                        // fermeture de la connexion
                        clientSocket.close();

                    }catch(IOException e){
                        e.printStackTrace();
                    }


                }).start();
            }

            /*// Attente de la connnexion d'un seul client pour l'instant
            Socket clientSocket = serverSocket.accept();

            //Fermeture simplifiée pour ce premier test
            clientSocket.close();
            serverSocket.close();*/
        }catch(IOException e) {
            e.printStackTrace();
        }

        chargerConfig();

	}
    //---------------------------

    //Listes pour stocker les configurations
    static ArrayList<String> peersList = new ArrayList<>();
    static ArrayList<String> filesList = new ArrayList<>();

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



