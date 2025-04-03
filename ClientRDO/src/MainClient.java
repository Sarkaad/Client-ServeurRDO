import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.*;
import java.net.Socket;
import java.io.IOException;
// Rajouté pour la nouvelle méthode de connexion au serveur
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
//import pour décodage/encodage
import java.util.Base64;
import java.nio.charset.StandardCharsets;

// Connection avec le Serveur
public class MainClient {

	//Demander a l'utilisateur de saisir la touche E pour se connecter avec le serveur
	//La connexion se fait par l'entrée de l'adresse IP et le port du serveur
	public static Socket connect() {
		Scanner sc = new Scanner(System.in);
		Socket socket = null;

		//On demande a l'user de taper la touche E pour ouvrir le système de connexion au serveur
		//Et si l'user ne tape pas la touche E, alors on annule la connexion et on retourne null
		System.out.println("Taper E pour établir une connexion a un serveur : ");
		String input = sc.nextLine().trim();
		if (!input.equalsIgnoreCase("E")) {
			System.out.println("Commande non reconnue, connexion annulée.");
			return null;
		}

		//Une fois que l'user aura entrer la touche E, on peut maintenant lui demander d'entrer L'IP et le port
		// Pour l'adresse IP :
		System.out.println("Veuillez entrer l'adresse IP du serveur auquel vous voulez vous connecter : ");
		String ip = sc.nextLine().trim(); // Cette ligne permet de supprimer les espaces superflus au début et a la fin de l'entrée de l'user

		/*Ici on fait une validation de l'adresse IP entrée par l'user. Pour cela je me sert d'un regex qui permet entre autre
		de limiter ce que entre l'user.*/
		String ipv4Pattern = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)(\\.|$)){4}$";
		if (!ip.matches(ipv4Pattern)) {
			System.out.println("Adresse IP introuvable/Invalide");
			return null;
		}

		// Pour le Port :
		System.out.println("Veuillez entrer le port de ce serveur : ");
		String portStr = sc.nextLine().trim();
		int port;
		try {
			port = Integer.parseInt(portStr);
			if (port < 1 || port > 65535) {
				System.out.println("Port invalide (doit etre entre 1 et 65535)");
				return null;
			}
		} catch (NumberFormatException e){
			System.out.println("Port invalide");
			return null;
		}

		//Ici je tente d'établir la connexion et j'ajoute un ensemble de Log pour permettre au client de savoir ce qu'il se passe
		try {
			socket = new Socket(ip,port);
			System.out.println("Connexion établie avec le serveur " + ip + ":" + port);
		}catch (UnknownHostException e) {
			System.out.println("Hote inconnu.");
		} catch (IOException e) {
			System.out.println("Erreur lors de la connexion au serveur.");
		}
		return socket;
	}



	public static void main(String[] args) throws IOException {
		//Ici on fait l'appel direct de la fonction Connect
		Socket socket = connect();
		if (socket == null) {
			System.out.println("La connexion n'a pas pu etre établie.");
			return;
		}

		try(//Socket socket = new Socket( "localhost", 5000 );
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

			//Permettre a l'user d'entrer d'autres commandes après le REGISTER
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

