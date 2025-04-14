package Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class ConfigManager {
    private ArrayList<String> peersList = new ArrayList<>();
    private ArrayList<String> filesList = new ArrayList<>();

    public ConfigManager() {
        loadConfig();
    }

    private void loadConfig() {
        try {
            // Lecture du fichier Peers_list.txt
            Scanner scannerPeers = new Scanner(new File("config/Peers_list.txt"));
            while (scannerPeers.hasNextLine()){
                String peer = scannerPeers.nextLine().trim();
                if (!peer.isEmpty()) {
                    peersList.add(peer);
                }
            }
            scannerPeers.close();

            // Lecture du fichier Files_list.txt
            Scanner scannerFiles = new Scanner(new File("config/Files_list.txt"));
            while (scannerFiles.hasNextLine()){
                String file = scannerFiles.nextLine().trim();
                if (!file.isEmpty()) {
                    filesList.add(file);
                }
            }
            scannerFiles.close();

            System.out.println("Peers chargés : " + peersList);
            System.out.println("Files disponibles : " + filesList);
        } catch (FileNotFoundException e) {
            System.out.println("Erreur : fichier de configuration introuvable");
        }
    }

    public ArrayList<String> getPeersList() {
        return peersList;
    }

    public ArrayList<String> getFilesList() {
        return filesList;
    }
}
