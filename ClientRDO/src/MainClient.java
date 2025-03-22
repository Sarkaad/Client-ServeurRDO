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

			//supposons que nous aillons déjà recu le jeton via REGISTER et que je le stock dans token
			String token = "token";

			//Envoie de la commande LS avec le jeton
			out.println("LS|" + token + "|");
			System.out.println("Commande LS envoyée au serveur.");

			//Lecture et affichage de la réponse du serveur
			String lsresponse = in.readLine();
			System.out.println("lsresponse : " + lsresponse);

			//Fermeture de la connexion après le test rapide
			socket.close();

		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}

// ----------------------------------------

