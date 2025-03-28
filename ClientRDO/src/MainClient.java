import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.*;
import java.net.Socket;
import java.io.IOException;
import java.util.Scanner;
//import pour décodage/encodage
import java.util.Base64;
import java.nio.charset.StandardCharsets;

// Connection avec le Serveur
public class MainClient {
	public static void main(String[] args) throws IOException {

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
					if (command.startsWith("READ")){
						StringBuilder fileContent = new StringBuilder();
						while (true){
							String fileResponse = in.readLine();
							if (fileResponse == null) {
								System.out.println("Connexion interrompue");
								break;
							}
							// On attend un format du type : "FILE|<nom_du_fichier>|<offset>|<isLast>|<fragment>
							String[] parts = fileResponse.split("\\|", 5);
							if (parts.length >= 5 && parts[0].equals("FILE")){
								System.out.println("Reception du fragment offset = " + parts[2] + ", isLast = " + parts[3]);
								System.out.println("Message reçu du serveur : " + fileResponse);

								//Décodage du fragment
								byte[] decodedBytes = Base64.getDecoder().decode(parts[4]);
								String fragment = new String(decodedBytes, StandardCharsets.UTF_8);
								fileContent.append(fragment);
								//Si isLast vaut 1 alors c'est le dernier fragment
								if (parts[3].equals("1")){
									break;
								}
							} else {
								System.out.println("Réponse inattendue : " + fileResponse);
								break;
							}
						}
						System.out.println("Contenu complet du fichier recu : " );
						System.out.println(fileContent.toString());
					} else {
						String responseServer = in.readLine();
						System.out.println("Réponse du serveur : " + responseServer);
					}

				}

			}

			System.out.println("Fermeture de la connexion");

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

// ----------------------------------------

