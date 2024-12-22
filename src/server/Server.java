package server;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class Server {
    
    private ServerSocket serverSocket;

    public Server(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        System.out.println("Serveur démarré sur le port " + port);
    }

    public void start() throws IOException {
        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

       
        public void run() {
            try (
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                ) {
            String command = dis.readUTF();

            switch (command) {
                case "UPLOAD":
                    handleUpload(dis);
                    break;
                case "LIST":
                    handleList(dos);
                    break;
                case "DOWNLOAD":
                    handleDownload(dis, dos);
                    break;
                case "DELETE":  // Nouveau cas
                    handleDelete(dis, dos);
                    break;
                default:
                    dos.writeUTF("Commande inconnue.");
            }
            
            
            
                 } catch (IOException e) {
            System.err.println("Erreur dans la gestion du client : " + e.getMessage());;
                }
            
            }
   
            private void handleUpload(DataInputStream dis) throws IOException {
                String username = dis.readUTF();
                String filename = dis.readUTF();
                long fileSize = dis.readLong();
            
                File userDir = new File("uploads/" + username);
                if (!userDir.exists()) {
                    userDir.mkdirs();  // Crée le dossier utilisateur si inexistant
                }
                System.out.println("Répertoire utilisateur créé: " + userDir.getAbsolutePath());
            
                byte[] buffer = new byte[4096];
            
                for (int part = 1; part <= 3; part++) {
                    File partFile = new File(userDir, filename + ".part" + part);
                    try (FileOutputStream fos = new FileOutputStream(partFile)) {
                        long bytesToRead = fileSize / 3;
                        if (part == 3) { 
                            bytesToRead = fileSize - (fileSize / 3) * 2;  // Calcul pour la dernière partie
                        }
                        long totalBytesRead = 0;
                        int bytesRead;
                        while (totalBytesRead < bytesToRead && (bytesRead = dis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                        System.out.println("Partie " + part + " enregistrée: " + partFile.getAbsolutePath());
                    }
                }
                
            }
            
            

            private void handleList(DataOutputStream dos) throws IOException {
                File uploadsDir = new File("uploads");
                if (!uploadsDir.exists()) {
                    dos.writeInt(0);  // Aucun fichier trouvé
                    return;
                }
            
                File[] userDirs = uploadsDir.listFiles(File::isDirectory);
                List<String> uniqueFiles = new ArrayList<>();
            
                if (userDirs != null) {
                    for (File userDir : userDirs) {
                        File[] files = userDir.listFiles((dir, name) -> name.endsWith(".part1"));
                        if (files != null) {
                            for (File file : files) {
                                String baseFileName = file.getName().replace(".part1", "");
                                uniqueFiles.add(userDir.getName() + "/" + baseFileName);
                            }
                        }
                    }
                }
            
                dos.writeInt(uniqueFiles.size());
                for (String fileName : uniqueFiles) {
                    dos.writeUTF(fileName);
                }
            }
            
       
        

            private void handleDownload(DataInputStream dis, DataOutputStream dos) throws IOException {
                String username = dis.readUTF();  // Lecture du nom d'utilisateur
                String filename = dis.readUTF();  // Nom de base du fichier sans extension
            
                // Répertoire de l'utilisateur
                File userDir = new File("uploads/" + username);
                if (!userDir.exists()) {
                    dos.writeUTF("Erreur: Répertoire utilisateur introuvable.");
                    return;
                }
            
                // Fichiers partiels
                File part1 = new File(userDir, filename + ".part1");
                File part2 = new File(userDir, filename + ".part2");
                File part3 = new File(userDir, filename + ".part3");
            
                // Vérification si toutes les parties existent
                if (part1.exists() && part2.exists() && part3.exists()) {
                    dos.writeUTF("Fichier complet trouvé, envoi en cours...");
            
                    // Création d'un fichier temporaire pour assembler les parties
                    File tempFile = new File(userDir, filename);
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        appendFile(fos, part1);  // Ajouter la partie 1
                        appendFile(fos, part2);  // Ajouter la partie 2
                        appendFile(fos, part3);  // Ajouter la partie 3
                    }
            
                    // Envoi du fichier reconstitué
                    sendFile(tempFile, dos);
                    tempFile.delete();  // Suppression du fichier temporaire après envoi
                } else {
                    dos.writeUTF("Erreur: Fichier incomplet ou introuvable.");
                    System.out.println("Erreur: Une ou plusieurs parties du fichier sont manquantes.");
                }
            }
            
            // Méthode pour assembler un fichier à partir de ses parties
            private void appendFile(FileOutputStream fos, File part) throws IOException {
                try (FileInputStream fis = new FileInputStream(part)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);  // Écrire les données dans le fichier temporaire
                    }
                }
            }
            
            // Méthode pour envoyer le fichier reconstitué
            private void sendFile(File file, DataOutputStream dos) throws IOException {
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    dos.writeLong(file.length());  // Envoi de la taille du fichier
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);  // Envoi des données du fichier
                    }
                }
            }

            private void handleDelete(DataInputStream dis, DataOutputStream dos) throws IOException {
                String username = dis.readUTF();  // Lecture du nom d'utilisateur
                String fileName = dis.readUTF();  // Nom complet du fichier avec extension
            
                // Répertoire de l'utilisateur
                File userDir = new File("uploads", username);  // Correctement défini avec "uploads/username"
                if (!userDir.exists() || !userDir.isDirectory()) {
                    dos.writeUTF("Erreur: Répertoire utilisateur introuvable.");
                    return;
                }
            
                // Fichiers partiels
                File part1 = new File(userDir, fileName + ".part1");
                File part2 = new File(userDir, fileName + ".part2");
                File part3 = new File(userDir, fileName + ".part3");
            
                // Tentative de suppression des parties
                boolean part1Deleted = part1.exists() && part1.delete();
                boolean part2Deleted = part2.exists() && part2.delete();
                boolean part3Deleted = part3.exists() && part3.delete();
            
                // Affichage pour débogage des tentatives de suppression
                System.out.println("Suppression partie 1: " + part1Deleted + " (" + part1.getAbsolutePath() + ")");
                System.out.println("Suppression partie 2: " + part2Deleted + " (" + part2.getAbsolutePath() + ")");
                System.out.println("Suppression partie 3: " + part3Deleted + " (" + part3.getAbsolutePath() + ")");
            
                // Retour d'information sur le statut de la suppression
                if (part1Deleted && part2Deleted && part3Deleted) {
                    dos.writeUTF("Fichier supprimé avec succès.");
                } else {
                    dos.writeUTF("Erreur: Certaines parties du fichier n'ont pas pu être supprimées.");
                }
            }
            
    }    
           public static void main(String[] args) throws IOException {
        Server server = new Server(5000);
        server.start();
    }
    }

