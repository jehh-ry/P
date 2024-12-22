package server;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ServerHandler implements Runnable {
    private Socket clientSocket;
        private DataOutputStream out;
    
        public ServerHandler(Socket socket) {
            this.clientSocket = socket;
        }
    
    
    public void run() {
        try (
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
        ) {
            String command = dis.readUTF();
            
            switch (command) {
                case "UPLOAD":
                    handleUpload(dis, dos);
                break;
                case "LIST":
                    handleListFiles(dis, dos);
                break;
                case "DOWNLOAD":
                    handleDownload(dis, dos);
                                break;
                                default:
                                    dos.writeUTF("Commande inconnue.");
                                            }
                        } catch (IOException e) {
                            System.err.println("Erreur dans la gestion du client : " + e.getMessage());
                            }
                    }
                                                                        
                                                                    
                    public void handleDownload(DataInputStream dis, DataOutputStream dos) throws IOException {
                        String username = dis.readUTF(); // Nom d'utilisateur
                        String filename = dis.readUTF(); // Nom du fichier (sans répertoire supplémentaire)
                        
                        // Créer le répertoire utilisateur
                        File userDir = new File("uploads" + File.separator + username);
                        if (!userDir.exists()) {
                            dos.writeUTF("Erreur: Répertoire utilisateur introuvable.");
                            return;
                        }
                    
                        // Créer le fichier à télécharger
                        File fileToDownload = new File(userDir, filename);  // On ne concatène pas ici un autre nom de répertoire
                        
                        // Vérification de l'existence du fichier
                        if (fileToDownload.exists()) {
                            dos.writeUTF("Fichier trouvé, envoi en cours...");
                            
                            // Envoyer le fichier
                            sendFile(fileToDownload, dos);
                        } else {
                            dos.writeUTF("Erreur: Fichier introuvable.");
                        }
                    }
                    
                    private void sendFile(File file, DataOutputStream dos) throws IOException {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            
                            dos.writeLong(file.length());  // Envoyer la taille du fichier
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                dos.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                    
                    
                    
                    private void handleListFiles(DataInputStream dis, DataOutputStream dos) throws IOException {
                        String username = dis.readUTF();  
                    
                        File userDir = new File("uploads", username);  
                        if (!userDir.exists() || !userDir.isDirectory()) {
                            dos.writeUTF("Erreur: Répertoire utilisateur introuvable.");
                            return;
                        }
                    
                        File[] files = userDir.listFiles();
                        if (files != null && files.length > 0) {
                            dos.writeUTF("Fichiers disponibles:");
                            for (File file : files) {
                                if (file.isFile()) {
                                    dos.writeUTF(file.getName());  // Envoie seulement le nom du fichier
                                }
                            }
                        } else {
                            dos.writeUTF("Aucun fichier disponible.");
                        }
                    }
                    
    
            private void handleUpload(DataInputStream dis, DataOutputStream dos) throws IOException {
                String username = dis.readUTF();
                String fileName = dis.readUTF();  
            
                // Correction du chemin pour éviter la duplication
                File userDir = new File("uploads", username);  
                if (!userDir.exists()) {
                    userDir.mkdirs();  
                }
            
                File fileToUpload = new File(userDir, fileName);  // Évite d'ajouter "username" deux fois
                try (FileOutputStream fos = new FileOutputStream(fileToUpload)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = dis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                dos.writeUTF("Fichier téléchargé avec succès.");
            }
            
            
            
            private void handleDelete(DataInputStream dis, DataOutputStream dos) throws IOException {
                String username = dis.readUTF();
                String fileName = dis.readUTF();  // Nom complet du fichier avec extension
            
                // Correction du chemin pour éviter la duplication
                File userDir = new File("uploads", username);  // Répertoire de l'utilisateur
                if (!userDir.exists() || !userDir.isDirectory()) {
                    dos.writeUTF("Erreur: Répertoire utilisateur introuvable.");
                    return;
                }
            
                // Chemins corrects des parties
                File part1 = new File(userDir, fileName + ".part1");
                File part2 = new File(userDir, fileName + ".part2");
                File part3 = new File(userDir, fileName + ".part3");
            
                boolean part1Deleted = part1.exists() && part1.delete();
                boolean part2Deleted = part2.exists() && part2.delete();
                boolean part3Deleted = part3.exists() && part3.delete();
            
                // Affichage pour débogage
                System.out.println("Suppression partie 1: " + part1Deleted + " (" + part1.getAbsolutePath() + ")");
                System.out.println("Suppression partie 2: " + part2Deleted + " (" + part2.getAbsolutePath() + ")");
                System.out.println("Suppression partie 3: " + part3Deleted + " (" + part3.getAbsolutePath() + ")");
            
                if (part1Deleted && part2Deleted && part3Deleted) {
                    dos.writeUTF("Fichier supprimé avec succès.");
                } else {
                    dos.writeUTF("Erreur: Certaines parties du fichier n'ont pas pu être supprimées.");
                }
            }
            
                    
                    
                @SuppressWarnings("unused")
        private void receiveFile(DataInputStream in) throws IOException {
            String username = in.readUTF();
            String fileName = in.readUTF();
            long fileSize = in.readLong();
            
            
            File userDir = new File("uploads/" + username);
            if (!userDir.exists() && !userDir.mkdirs()) {
            throw new IOException("Impossible de créer le répertoire utilisateur : " + userDir.getPath());
        }
    
    
            try (FileOutputStream fileOut = new FileOutputStream(new File(userDir, fileName))) {
                byte[] buffer = new byte[4096];
                long remaining = fileSize;
                int bytesRead;
    
                while ((bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                    fileOut.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }
            System.out.println("Fichier reçu : " + fileName + " pour l'utilisateur " + username);
        }
    
    
        @SuppressWarnings("unused")
        private void sendFileList(DataOutputStream out, String username) throws IOException {
        File userDir = new File("uploads" + File.separator + username);
        if (!userDir.exists()) userDir.mkdirs();
    
        File[] files = userDir.listFiles();
        if (files != null) {
            out.writeInt(files.length);
    
            Set<String> fileNames = new HashSet<>();
            for (File file : files) {
                // Vérifier si le fichier est une partie d'un fichier plus grand
                if (file.getName().contains(".part")) {
                    String baseFileName = file.getName().substring(0, file.getName().lastIndexOf(".part"));
                    fileNames.add(baseFileName); // Ajouter le nom principal sans l'extension .part
                } else {
                    fileNames.add(file.getName()); // Ajouter directement si ce n'est pas une partie
                }
            }
    
            for (String fileName : fileNames) {
                out.writeUTF(fileName);
            }
        } else {
            out.writeInt(0);
        }
    }
    
        // @SuppressWarnings("unused")
        // private void sendFile(DataOutputStream out, String username, String fileName) throws IOException {
        //     String part1 = fileName + ".part1";
        //     String part2 = fileName + ".part2";
        //     String part3 = fileName + ".part3";
        
        //     // Vérification de la présence de toutes les parties
        //     if (checkFileExists(username, part1) && checkFileExists(username, part2) && checkFileExists(username, part3)) {
        //         sendPartFile(out, username, part1);
        //         sendPartFile(out, username, part2);
        //         sendPartFile(out, username, part3);
        //     } else {
        //         out.writeUTF("Une ou plusieurs parties du fichier sont manquantes.");
        //     }
        // }
        
        // Vérification si le fichier ou la partie existe
        private boolean checkFileExists(String username, String partFile) {
            File file = new File("uploads" + File.separator + username + File.separator + partFile);
            return file.exists();
        }
        
        // Envoi des parties
        private void sendPartFile(DataOutputStream out, String username, String partFile) throws IOException {
            File file = new File("uploads" + File.separator + username + File.separator + partFile);
            if (file.exists()) {
                out.writeLong(file.length());
                try (FileInputStream fileIn = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buffer)) > 0) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
        
        // @SuppressWarnings("unused")
        // private void saveFile(DataInputStream in, String username, String fileName) throws IOException {
        //     // Créer le répertoire de l'utilisateur s'il n'existe pas
        //     File userDir = new File("uploads" + File.separator + username);
        //     if (!userDir.exists()) {
        //         userDir.mkdirs();  // Créer le répertoire si nécessaire
        //     }
        
        //     // Création du fichier dans le répertoire utilisateur
        //     File file = new File(userDir, fileName);
        //     try (FileOutputStream fileOut = new FileOutputStream(file)) {
        //         byte[] buffer = new byte[4096];
        //         int bytesRead;
        //         while ((bytesRead = in.read(buffer)) != -1) {
        //             fileOut.write(buffer, 0, bytesRead);
        //         }
        //     }
        // }
        
        public void sendFile(String username, String filePath) throws IOException {
            // Créer le répertoire de l'utilisateur si nécessaire
            File userDir = new File("uploads" + File.separator + username);
            if (!userDir.exists()) {
                userDir.mkdirs();  // Créer le répertoire si nécessaire
            }
            
            // Créer un fichier de sortie pour l'upload
            File file = new File(filePath);
            FileInputStream fileIn = new FileInputStream(file);
            
            out.writeUTF("UPLOAD");
            out.writeUTF(username);  // Utiliser le nom d'utilisateur pour créer le répertoire
            out.writeUTF(file.getName());  // Assurez-vous d'envoyer uniquement le nom du fichier
            out.writeLong(file.length());
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            
            fileIn.close();
        }
        
}
