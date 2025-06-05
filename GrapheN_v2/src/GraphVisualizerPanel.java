import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class GraphVisualizerPanel extends JPanel {
    JLabel fileLabel;
    JButton submit = new JButton();
    File selectedFile;
    public GraphVisualizerPanel(MainFrame parent) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Graph Visualizer"));

        // Przycisk wyboru pliku
        JButton fileChooserButton = new JButton("Select Input File");
        fileChooserButton.setFocusable(false);
        fileLabel = new JLabel("No file selected");
        fileChooserButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                fileLabel.setText(selectedFile.getName());
                submit.setEnabled(true);
            }
        });

        submit = new JButton();
        submit.setText("Submit");
        submit.setFocusable(false);
        submit.setEnabled(false);
        submit.addActionListener(e -> visualizeGraph(parent));

        add(fileChooserButton);
        add(fileLabel);
        add(submit);
    }

    private void visualizeGraph(MainFrame parent){
        try {
            String extension = "";
            Graph graf = new Graph();
            String name = selectedFile.getAbsolutePath();
            int lastDotIndex = name.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < name.length() - 1) {
                extension = name.substring(lastDotIndex + 1);
            }
            if(extension.equals("csrrg")){
                if(selectedFile.getAbsolutePath().contains("og_files")){
                    graf = Graph.loadCSRRGGraph(selectedFile.getAbsolutePath());
                }else{
                    showCenteredMessage(
                            "Plik .csrrg musi być w folderze 'og_files'!",
                            "Błąd",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

            }else if(extension.equals("txt")){
                if(selectedFile.getAbsolutePath().contains("own_c")){
                    graf = Graph.loadCGraphTXT(selectedFile.getAbsolutePath());
                }else if(selectedFile.getAbsolutePath().contains("supplied_c")){
                    graf = Graph.loadTXTGraph(selectedFile.getAbsolutePath());
                }
            }else if(extension.equals("bin")){
                if(selectedFile.getAbsolutePath().contains("own_c")){
                    graf = Graph.loadCGraphBIN(selectedFile.getAbsolutePath());
                }/*else if(selectedFile.getAbsolutePath().contains("supplied_c")){
                    graf = Graph.loadTXTGraph(selectedFile.getAbsolutePath());
                }*/
            } else {
                showCenteredMessage(
                        "Nieobsługiwane rozszerzenie pliku: " + extension + "\n" +
                                "Obsługiwane rozszerzenia: .txt, .csrrg",
                        "Błąd",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (graf == null || graf.getWierzcholki() == null || graf.getWierzcholki().isEmpty()) {
                showCenteredMessage(
                        "Nie udało się wczytać grafu z pliku!",
                        "Błąd",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            parent.updateVisualizationPanel(new GraphPanel(graf.getWierzcholki(), parent));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void showCenteredMessage(String message, String title, int messageType) {
        JOptionPane pane = new JOptionPane(message, messageType);
        JDialog dialog = pane.createDialog(this, title);
        dialog.setLocationRelativeTo(null); // Wyśrodkuj na ekranie
        dialog.setVisible(true);
    }
}
