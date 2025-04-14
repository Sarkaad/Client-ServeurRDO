package server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import utils.TokenGenerator;
import Configuration.ConfigManager;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ConfigManager config;

    public ClientHandler(Socket socket, ConfigManager config) {
        this.clientSocket = socket;
        this.config = config;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            boolean isRegistered = false;
            String token = null;
            String messageClient;

            while ((messageClient = in.readLine()) != null) {
                System.out.println("Message reçu : " + messageClient);

                if (!isRegistered) {
                    if (messageClient.startsWith("REGISTER")) {
                        token = TokenGenerator.generateToken();
                        out.println("REGISTERED|" + token + "|");
                        System.out.println("Jeton envoyé : " + token);
                        isRegistered = true;
                    } else {
                        out.println("ERROR ! Vous devez vous enregistrer avant tout !");
                    }
                } else {
                    processCommand(messageClient, token, out, in);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processCommand(String message, String token, PrintWriter out, BufferedReader in) throws IOException {
        if (message.startsWith("LS")) {
            String[] parts = message.split("\\|");
            if (parts.length >= 2 && parts[1].equals(token)) {
                ArrayList<String> filesList = config.getFilesList();
                StringBuilder response = new StringBuilder("LS|" + filesList.size());
                for (String file : filesList) {
                    response.append("|").append(file);
                }
                response.append("|");
                out.println(response.toString());
                System.out.println("LS envoyé : " + response.toString());
            } else {
                out.println("LS|UNAUTHORIZED");
            }
        } else if (message.startsWith("READ")) {
            String[] parts = message.split("\\|");
            if (parts.length >= 3 && parts[1].equals(token)) {
                String fileName = parts[2];
                java.nio.file.Path localPath = Paths.get("config", fileName);
                if (Files.exists(localPath)) {
                    sendFileContent(fileName, localPath, out);
                } else {
                    boolean redirectFound = redirectFile(fileName, out);
                    if (!redirectFound) {
                        out.println("READ|ERROR|Fichier non trouvé|");
                        System.out.println("Fichier " + fileName + " non trouvé localement et pas de redirection définie.");
                    }
                }
            } else {
                out.println("READ|ERROR|Format incorrect ou token invalide|");
            }
        } else if (message.startsWith("WRITE")) {
            String[] parts = message.split("\\|");
            if (parts.length >= 3 && parts[1].equals(token)) {
                String fileName = parts[2];
                out.println("WRITE|BEGIN");
                System.out.println("WRITE BEGIN pour le fichier : " + fileName);
                StringBuilder fileContent = new StringBuilder();
                while (true) {
                    String fragmentMsg = in.readLine();
                    if (fragmentMsg == null) {
                        System.out.println("Connexion interrompue durant la réception du fichier.");
                        break;
                    }
                    String[] fragParts = fragmentMsg.split("\\|", 5);
                    if (fragParts.length >= 5 && fragParts[0].equals("FILE") && fragParts[1].equals(fileName)) {
                        System.out.println("Réception du fragment offset = " + fragParts[2] + ", isLast = " + fragParts[3]);
                        try {
                            byte[] decodedBytes = Base64.getDecoder().decode(fragParts[4]);
                            String fragment = new String(decodedBytes, StandardCharsets.UTF_8);
                            fileContent.append(fragment);
                        } catch (IllegalArgumentException ex) {
                            System.out.println("Erreur lors du décodage du fragment, utilisation du fragment brut.");
                            fileContent.append(fragParts[4]);
                        }
                        if (fragParts[3].equals("1")) {
                            break;
                        }
                    } else {
                        System.out.println("Réponse inattendue durant WRITE : " + fragmentMsg);
                        break;
                    }
                }

                try {
                    Files.createDirectories(Paths.get("config"));
                    Files.write(Paths.get("config", fileName), fileContent.toString().getBytes(StandardCharsets.UTF_8));
                    if (!config.getFilesList().contains(fileName)) {
                        config.getFilesList().add(fileName);
                    }
                    out.println("WRITE|OK|Fichier enregistré");
                    System.out.println("Fichier " + fileName + " enregistré dans 'config'.");
                } catch (IOException e) {
                    out.println("WRITE|ERROR|Echec d'enregistrement");
                    System.out.println("Erreur d'enregistrement du fichier " + fileName);
                }
            } else {
                out.println("WRITE|ERROR|Format incorrect ou token invalide");
            }
        } else {
            out.println("ERROR ! Commande inconnue");
        }
    }

    private void sendFileContent(String fileName, java.nio.file.Path path, PrintWriter out) throws IOException {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
            int fragmentSize = 500;
            int totalLength = fileContent.length();
            int numFragments = (totalLength + fragmentSize - 1) / fragmentSize;

            System.out.println("Contenu total (" + totalLength + " caractères), découpé en " + numFragments + " fragments.");

            for (int i = 0; i < numFragments; i++) {
                int start = i * fragmentSize;
                int end = Math.min(start + fragmentSize, totalLength);
                String fragment = fileContent.substring(start, end);
                int isLast = (i == numFragments - 1) ? 1 : 0;
                String encodedFragment = Base64.getEncoder().encodeToString(fragment.getBytes(StandardCharsets.UTF_8));
                out.println("FILE|" + fileName + "|" + i + "|" + isLast + "|" + encodedFragment);
                System.out.println("Fragment " + i + " envoyé (offset = " + start + ", isLast = " + isLast + ")");
            }
        } catch (IOException e) {
            out.println("ERROR ! Fichier introuvable");
            System.out.println("Erreur lors de la lecture du fichier " + fileName);
        }
    }

    private boolean redirectFile(String fileName, PrintWriter out) {
        for (String entry : config.getFilesList()) {
            String[] entryParts = entry.split(" ");
            if (entryParts[0].equals(fileName) && entryParts.length >= 2) {
                String[] addrParts = entryParts[1].split(":");
                if (addrParts.length == 2) {
                    String remoteIP = addrParts[0];
                    String remotePort = addrParts[1];
                    String newToken = TokenGenerator.generateToken();
                    out.println("READ-REDIRECT|" + remoteIP + "|" + remotePort + "|" + newToken + "|");
                    System.out.println("REDIRECT envoyé : READ-REDIRECT|" + remoteIP + "|" + remotePort + "|" + newToken + "|");
                    return true;
                }
            }
        }
        return false;
    }
}
