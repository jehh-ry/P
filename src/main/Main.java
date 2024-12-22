import server.Server;
import client.ClientGUI;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Lancer le serveur dans un thread
        Thread serverThread = new Thread(() -> {
            try {
                Server.main(null);
            } catch (IOException e) {
                System.err.println("Erreur lors du démarrage du serveur : " + e.getMessage());
            }
        });
        serverThread.start();

        // Démarrer l'interface client
        javax.swing.SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}
