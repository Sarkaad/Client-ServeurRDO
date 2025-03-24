import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.*;
import java.net.Socket;
import java.io.IOException;
import java.util.Scanner;

// Connection avec le Serveur
public class MainClient {
	public static void main(String[] args) throws IOException {
        /*//se connecte au serveur local sur le port 5000
		Socket socket = new Socket("localhost", 5000);
        System.out.println("Connecté au server !");*/

		try(Socket socket = new Socket( "localhost", 5000 );
			BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
			PrintWriter out = new PrintWriter( socket.getOutputStream(), true);
			Scanner sc = new Scanner(System.in)) {

			String token = null;
			boolean isRegistered = false;

			//Boucle pour entre REGISTER
			while ( !isRegistered){
				System.out.println("Veuillez vous connecter en entrant la comamnde qu'il faut  : ");
				String firstCommand = sc.nextLine().trim();
				if (firstCommand.equals("REGISTER")){
					out.println(firstCommand);
					String response = in.readLine();
					//System.out.println("Réponse du serveur : " + response);

					//On veut une réponse sour la forme REGISTER + Jetont
					String[] parts = response.split("\\|");
					if (parts.length >= 2 && parts[0].equals("REGISTERED")){
						token = parts[1];
						isRegistered = true;
						System.out.println("Vous etes enregistré avac le jeton : " + token);
					} else {
						System.out.println("Erreur de connexion");
					}
				} else {
					System.out.println("Commande invalide.");
				}

			}

			//Permettre a l'user d'entrer d'autres d'autres commandes après le REGISTER
			boolean exit = false;
			while (!exit){
				System.out.println("Veuillez entrer une commande : ");
				String command = sc.nextLine().trim();
				if (command.equals("exit")){
					//out.println(command);
					exit = true;
				} else {
					out.println(command);
					String responseServer = in.readLine();
					System.out.println("Réponse du serveur : " + responseServer);
				}

			}

			System.out.println("Fermeture de la connexion");

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

// ----------------------------------------

