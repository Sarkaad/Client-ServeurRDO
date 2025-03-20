import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.*;
import java.net.Socket;
import java.io.IOException;

// Connection avec le Serveur
public class MainClient {
	public static void main(String[] args) throws IOException {
        /*//se connecte au serveur local sur le port 5000
		Socket socket = new Socket("localhost", 5000);
        System.out.println("Connecté au server !");*/

		try {
			Socket socket = new Socket("localhost", 5000);
			BufferedReader in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

			//Envoi du REGISTER avec IP locale ( méthode simple )
			out.println("REGISTER|127.0.0.1|");
			System.out.println("Demande REGISTER envoyée au serveur.");

			//Attente réponse serveur
			String response = in.readLine();
			System.out.println("Réponse recue : " + response);

			//Fermeture de la connexion après le test rapide
			socket.close();

		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}

// ----------------------------------------

