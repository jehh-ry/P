package client;

import java.io.*;
import java.net.Socket;

public class Client {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public Client(String address, int port) throws IOException {
        socket = new Socket(address, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    // Envoi d'un fichier
    public void sendFile(String username, String filePath) throws IOException {
        File file = new File(filePath);  // Utilise uniquement le chemin absolu local
        FileInputStream fileIn = new FileInputStream(file);
    
        out.writeUTF("UPLOAD");
        out.writeUTF(username);  
        out.writeUTF(file.getName());  // Envoie uniquement le nom du fichier, pas un chemin combiné
        out.writeLong(file.length());
    
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fileIn.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        fileIn.close();
    }
    
    
    
    // Lister les fichiers disponibles pour un utilisateur
    public String[] listFiles(String username) throws IOException {
        out.writeUTF("LIST");
        out.writeUTF(username);

        int fileCount = in.readInt();
        String[] files = new String[fileCount];
        for (int i = 0; i < fileCount; i++) {
            files[i] = in.readUTF();
        }
        return files;
    }

    // Télécharger un fichier
    public void downloadFile(String username, String fileName, String savePath) throws IOException {
        // Créer le répertoire de téléchargement (pas un répertoire avec le nom du fichier)
        File userDir = new File(savePath);  // Par exemple, "downloads/username"
        if (!userDir.exists()) {
            userDir.mkdirs();  // Créer le répertoire si nécessaire
        }
    
        // Créer le fichier de destination sans ajouter le répertoire du nom du fichier
        File destinationFile = new File(userDir, fileName);  // Utilisez le nom du fichier uniquement
    
        // Envoi de la demande de téléchargement
        out.writeUTF("DOWNLOAD");
        out.writeUTF(username);  // Nom d'utilisateur
        out.writeUTF(fileName);  // Nom du fichier à télécharger
    
        // Réception du fichier depuis le serveur
        long fileSize = in.readLong();  // Lire la taille du fichier
        try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (fileSize > 0 && (bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
        }
    }
    
    
    
    // Delete file
public void deleteFile(String username, String fileName) throws IOException {
    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
    dos.writeUTF("DELETE");
    dos.writeUTF(username);
    dos.writeUTF(fileName);
    dos.flush();
}

private void displayFiles(DataInputStream dis) throws IOException {
    String response = dis.readUTF();  
    if (response.startsWith("Fichiers disponibles:")) {
        System.out.println(response);  
        while (dis.available() > 0) {
            String fileName = dis.readUTF();  
            System.out.println(" - " + fileName);  // Affiche uniquement le nom du fichier
        }
    } else {
        System.out.println(response);  
    }
}
 
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();  // Fermer la connexion socket
        }
    }
    
}
