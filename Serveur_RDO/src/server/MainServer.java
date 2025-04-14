package server;

import Configuration.ConfigManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    public static void main(String[] args) {
        ConfigManager config = new ConfigManager();

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Serveur démarré, en attente des clients...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté : " + clientSocket.getInetAddress());
                Thread clientThread = new Thread(new ClientHandler(clientSocket, config));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


