
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
import java.nio.file.Files;
import java.nio.file.Paths;


// Connection avec le Serveur
public class MainClient {

	// Demander à l'utilisateur de saisir la touche E pour se connecter avec le serveur
	// La connexion se fait par l'entrée de l'adresse IP et le port du serveur
	public static Socket connect() {
		Scanner sc = new Scanner(System.in);
		Socket socket = null;

		// On demande à l'user de taper la touche E pour ouvrir le système de connexion au serveur
		// Et si l'user ne tape pas la touche E, alors on annule la connexion et on retourne null
		System.out.println("Taper E pour établir une connexion a un serveur : ");
		String input = sc.nextLine().trim();
		if (!input.equalsIgnoreCase("E")) {
			System.out.println("Commande non reconnue, connexion annulée.");
			return null;
		}

		// Une fois que l'user aura entré la touche E, on lui demande d'entrer l'IP et le port
		// Pour l'adresse IP :
		System.out.println("Veuillez entrer l'adresse IP du serveur auquel vous voulez vous connecter : ");
		String ip = sc.nextLine().trim(); // Cette ligne permet de supprimer les espaces superflus

		/* Ici on fait une validation de l'adresse IP entrée par l'user grâce à un regex */
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
				System.out.println("Port invalide (doit être entre 1 et 65535)");
				return null;
			}
		} catch (NumberFormatException e) {
			System.out.println("Port invalide");
			return null;
		}

		// Ici on tente d'établir la connexion et on ajoute des logs pour informer le client
		try {
			socket = new Socket(ip, port);
			System.out.println("Connexion établie avec le serveur " + ip + ":" + port);
		} catch (UnknownHostException e) {
			System.out.println("Hôte inconnu.");
		} catch (IOException e) {
			System.out.println("Erreur lors de la connexion au serveur.");
		}
		return socket;
	}

	public static void main(String[] args) throws IOException {
		// Appel direct de la fonction connect
		Socket socket = connect();
		if (socket == null) {
			System.out.println("La connexion n'a pas pu être établie.");
			return;
		}

		BufferedReader in = null;
		PrintWriter out = null;
		Scanner sc = new Scanner(System.in);

		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);

			String token = null;
			boolean isRegistered = false;

			// Boucle pour entrer REGISTER
			while (!isRegistered) {
				System.out.println("Veuillez vous connecter en entrant la commande qu'il faut : ");
				String firstCommand = sc.nextLine().trim();
				if (firstCommand.equals("REGISTER")) {
					out.println(firstCommand);
					String response = in.readLine();

					// On attend une réponse de la forme REGISTERED|<jeton>
					String[] parts = response.split("\\|");
					if (parts.length >= 2 && parts[0].equals("REGISTERED")) {
						token = parts[1];
						isRegistered = true;
						System.out.println("Vous êtes enregistré avec le jeton : " + token);
					} else {
						System.out.println("Erreur de connexion");
					}
				} else {
					System.out.println("Commande invalide.");
				}
			}

			// Permettre à l'user d'entrer d'autres commandes après le REGISTER
			boolean exit = false;
			while (!exit) {
				System.out.println("Veuillez entrer une commande : ");
				String command = sc.nextLine().trim();
				/* Permet de fermer la connexion actuelle et de se connecter ailleurs via la touche E */
				if (command.equalsIgnoreCase("E")) {
					System.out.println("Reconnexion demandée...");
					socket.close();
					socket = connect();
					if (socket == null) {
						System.out.println("La reconnexion a échoué");
						exit = true;
					} else {
						in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						out = new PrintWriter(socket.getOutputStream(), true);
						System.out.println("Reconnexion établie !");
					}
					continue;
				}

				if (command.equals("exit")) {
					exit = true;
				} else {
					if (command.startsWith("READ")) {
						out.println(command);
						StringBuilder fileContent = new StringBuilder();
						boolean fragmentReceived = false;
						while (true) {
							String fileResponse = in.readLine();
							if (fileResponse == null) {
								System.out.println("Connexion interrompue");
								break;
							}
							// Format attendu : "FILE|<nom_du_fichier>|<offset>|<isLast>|<fragmentEncodé>"
							String[] partsResp = fileResponse.split("\\|", 5);
							if (partsResp.length >= 5 && partsResp[0].equals("FILE")) {
								fragmentReceived = true;
								System.out.println("Réception du fragment offset = " + partsResp[2] + ", isLast = " + partsResp[3]);
								System.out.println("Message reçu du serveur : " + fileResponse);

								byte[] decodedBytes = Base64.getDecoder().decode(partsResp[4]);
								String fragment = new String(decodedBytes, StandardCharsets.UTF_8);
								fileContent.append(fragment);
								if (partsResp[3].equals("1")) {
									break;
								}
							} else {
								System.out.println("Réponse inattendue : " + fileResponse);
								break;
							}
						}
						if (fragmentReceived) {
							System.out.println("Contenu complet du fichier reçu : ");
							System.out.println(fileContent.toString());
						}
					} else if (command.startsWith("WRITE")) {
						// Branche WRITE : on utilise directement le fichier indiqué dans la commande
						// Format attendu : WRITE|<token>|<nom_du_fichier>|
						String[] writeParts = command.split("\\|");
						if (writeParts.length < 3) {
							System.out.println("Commande WRITE invalide. Format attendu : WRITE|<token>|<nom_fichier>|");
							continue;
						}
						// On envoie directement la commande saisie par l'utilisateur
						out.println(command);
						String response = in.readLine();
						if (response == null) {
							System.out.println("Connexion interrompue durant la commande WRITE.");
							continue;
						}
						if (response.startsWith("WRITE|BEGIN")) {
							System.out.println("Le serveur autorise l'écriture. Préparation de l'envoi du fichier...");
							// On suppose que le fichier se trouve dans le dossier "src" et porte le même nom que writeParts[2]
							String localFileName = writeParts[2]; // ex : "document.txt"
							File selectedFile = new File("src", localFileName);

							if (!selectedFile.exists() || !selectedFile.isFile()) {
								System.out.println("Le fichier " + localFileName + " n'existe pas dans 'src'.");
								continue;
							}

							try {
								byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
								String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
								int fragmentSize = 500;
								int totalLength = fileContent.length();
								int numFragments = (totalLength + fragmentSize - 1) / fragmentSize;
								System.out.println("Fichier lu (" + totalLength + " caractères), découpé en " + numFragments + " fragments.");
								for (int i = 0; i < numFragments; i++) {
									int start = i * fragmentSize;
									int end = Math.min(start + fragmentSize, totalLength);
									String fragment = fileContent.substring(start, end);
									int isLast = (i == numFragments - 1) ? 1 : 0;
									String encodedFragment = Base64.getEncoder().encodeToString(fragment.getBytes(StandardCharsets.UTF_8));
									out.println("FILE|" + localFileName + "|" + i + "|" + isLast + "|" + encodedFragment);
									System.out.println("Fragment " + i + " envoyé (offset = " + start + ", isLast = " + isLast + ")");
								}
								String finalResponse = in.readLine();
								System.out.println("Réponse finale du serveur : " + finalResponse);
							} catch (IOException e) {
								System.out.println("Erreur lors de la lecture du fichier " + selectedFile.getAbsolutePath());
								e.printStackTrace();
							}
						} else {
							System.out.println("Le serveur n'a pas autorisé le transfert (" + response + ").");
						}
					} else {
						out.println(command);
						String responseServer = in.readLine();
						System.out.println("Réponse du serveur : " + responseServer);
					}
				}
			}

			System.out.println("Fermeture de la connexion");

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
				if (socket != null && !socket.isClosed())
					socket.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}




// ----------------------------------------

