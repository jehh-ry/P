package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import common.Constants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.io.File;


public class ClientGUI extends JFrame {
    private JTextField usernameField;
    private JTextField selectedFileField;
    private JTextField serverIpField;
    private JButton browseButton;
    private JButton sendButton;
    private JButton listFilesButton;
    private JButton downloadButton;
    private JButton deleteButton;
    private JComboBox<String> serverSelector;
    private JList<String> fileList;
    private DefaultListModel<String> listModel;
    private Client client;

    public ClientGUI() {
        setTitle("Transfert de Fichiers");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel headerLabel = new JLabel("Transfert de Fichiers", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(headerLabel, BorderLayout.NORTH);

        JPanel configPanel = new JPanel(new GridLayout(3, 1));

        JPanel serverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JLabel serverLabel = new JLabel("Serveur: ");
        serverIpField = new JTextField(15);
        serverIpField.setText("localhost");
        serverIpField.setEditable(false);
        serverSelector = new JComboBox<>(new String[]{"localhost", "Entrer IP"});
        serverSelector.addActionListener((ActionEvent e) -> {
            serverIpField.setEditable(serverSelector.getSelectedItem().equals("Entrer IP"));
            serverIpField.setText(serverIpField.isEditable() ? "" : "localhost");
        });
        serverPanel.add(serverLabel);
        serverPanel.add(serverSelector);
        serverPanel.add(serverIpField);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JLabel usernameLabel = new JLabel("Nom d'utilisateur: ");
        usernameField = new JTextField(15);
        userPanel.add(usernameLabel);
        userPanel.add(usernameField);

        JPanel filePanel = new JPanel(new BorderLayout(10, 10));
        filePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        selectedFileField = new JTextField();
        selectedFileField.setEditable(false);
        browseButton = new JButton("Parcourir...");
        filePanel.add(new JLabel("Fichier: "), BorderLayout.WEST);
        filePanel.add(selectedFileField, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);

        configPanel.add(serverPanel);
        configPanel.add(userPanel);
        configPanel.add(filePanel);
        add(configPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(fileList);
        centerPanel.add(new JLabel("Fichiers disponibles sur le serveur:"), BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sendButton = new JButton("Envoyer");
        listFilesButton = new JButton("Afficher mes fichiers");
        downloadButton = new JButton("Télécharger");
        deleteButton = new JButton("Supprimer");

        footerPanel.add(listFilesButton);
        footerPanel.add(downloadButton);
        footerPanel.add(deleteButton);
        footerPanel.add(sendButton);
        add(footerPanel, BorderLayout.SOUTH);

        browseButton.addActionListener(e -> handleBrowseFile());
        sendButton.addActionListener(e -> handleFileSend());
        listFilesButton.addActionListener(e -> handleListFiles());
        downloadButton.addActionListener(e -> handleFileDownload());
        deleteButton.addActionListener(e -> handleFileDelete());
    }

    
    private void handleBrowseFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void handleFileSend() {
        String filePath = selectedFileField.getText();
        String serverIp = serverIpField.getText();
        String username = usernameField.getText().trim();

        if (username.isEmpty() || filePath.isEmpty() || serverIp.isEmpty()) {
            showErrorMessage("Remplissez tous les champs!");
            return;
        }

        try {
            client = new Client(serverIp, Constants.PORT);
            client.sendFile(username, filePath);
            showSuccessMessage("Fichier envoyé avec succès!");
            client.close();
        } catch (IOException ex) {
            showErrorMessage("Erreur: " + ex.getMessage());
        }
    }

    private void handleListFiles() {
        String serverIp = serverIpField.getText();
        String username = usernameField.getText().trim();
    
        if (username.isEmpty() || serverIp.isEmpty()) {
            showErrorMessage("Remplissez tous les champs!");
            return;
        }
    
        try {
            client = new Client(serverIp, Constants.PORT);
            listModel.clear();
    
            String[] files = client.listFiles(username);
    
            // Affiche uniquement le nom du fichier
            for (String file : files) {
                if (file.contains("/")) {
                    file = file.substring(file.lastIndexOf("/") + 1);
                }
                listModel.addElement(file);
            }
            client.close();
        } catch (IOException ex) {
            showErrorMessage("Erreur: " + ex.getMessage());
        }
    }
    
    private void handleFileDownload() {
        String selectedFile = fileList.getSelectedValue();
        if (selectedFile == null) {
            showErrorMessage("Veuillez sélectionner un fichier à télécharger!");
            return;
        }
    
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            // Obtenez le répertoire de destination sans ajouter de sous-répertoire
            String savePath = fileChooser.getSelectedFile().getAbsolutePath();
            
            // Vérifiez si le répertoire de destination ne contient pas déjà le nom du fichier
            File destinationFile = new File(savePath, selectedFile);
            
            try {
                client = new Client(serverIpField.getText(), Constants.PORT);
                client.downloadFile(usernameField.getText().trim(), selectedFile, destinationFile.getAbsolutePath());
                showSuccessMessage("Fichier téléchargé avec succès!");
                client.close();
            } catch (IOException ex) {
                showErrorMessage("Erreur: " + ex.getMessage());
            }
        }
    }
    

    private void handleFileDelete() {
        String selectedFile = fileList.getSelectedValue();
        if (selectedFile == null) {
            showErrorMessage("Veuillez sélectionner un fichier à supprimer!");
            return;
        }
    
        try {
            client = new Client(serverIpField.getText(), Constants.PORT);
            client.deleteFile(usernameField.getText().trim(), selectedFile);
    
            JOptionPane.showMessageDialog(this, "Fichier supprimé avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
            client.close();
            handleListFiles(); // Met à jour la liste des fichiers après suppression
        } catch (IOException ex) {
            showErrorMessage("Erreur: " + ex.getMessage());
        }
    }
    

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Succès", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}  
