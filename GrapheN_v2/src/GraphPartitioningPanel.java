import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;

public class GraphPartitioningPanel extends JPanel {
    public int n;
    public int margin;
    String outputType;
    File selectedFile;
    private JLabel fileLabel;
    private JTextField splitCountField;
    private JTextField marginErrorField;
    private JRadioButton optionTxt;
    private JRadioButton optionBin;
    private JButton submit;

    public GraphPartitioningPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Graph Splitter"));

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

        // Pola tekstowe
        splitCountField = new JTextField(10);
        marginErrorField = new JTextField(10);

        // RadioButtony do wyboru formatu
        optionTxt = new JRadioButton(".txt");
        optionTxt.setFocusable(false);
        optionBin = new JRadioButton(".bin");
        optionBin.setFocusable(false);
        ButtonGroup outputFormatGroup = new ButtonGroup();
        outputFormatGroup.add(optionTxt);
        outputFormatGroup.add(optionBin);

        submit = new JButton();
        submit.setText("Submit");
        submit.setFocusable(false);
        submit.setEnabled(false);
        submit.addActionListener(e -> {
            try {
                printOptions();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });


        // Dodanie komponentów
        add(fileChooserButton);
        add(fileLabel);
        add(new JLabel("Number of splits:"));
        add(splitCountField);
        add(new JLabel("Margin of error:"));
        add(marginErrorField);
        add(new JLabel("Output format:"));
        add(optionTxt);
        add(optionBin);
        add(submit);
    }

    private void printOptions() throws IOException {
        Graph graf = Graph.loadCSRRGGraph(selectedFile.getAbsolutePath());

        if (!splitCountField.getText().isEmpty()) {
            n = Integer.parseInt(splitCountField.getText());
        } else {
            n = 2;
        }
        if (!marginErrorField.getText().isEmpty()) {
            margin = Integer.parseInt(marginErrorField.getText());
        } else {
            margin = 10;
        }

        GraphPartitioner partitioner = new GraphPartitioner(graf, n, margin/100.0);
        Map<Integer, Integer> partition = partitioner.partition();

        String basePath = selectedFile.getAbsolutePath().replace(".csrrg", "_partitioned");
        String outputPath;

        if (optionBin.isSelected()) {
            outputPath = basePath + ".bin";
            partitioner.savePartitionedGraphBIN(graf, partition, outputPath);
        } else {
            outputPath = basePath + ".txt";
            partitioner.savePartitionedGraph(graf, partition, outputPath);
        }

        showCenteredMessage(
                "Graph partitioned into " + n + " parts\n" +
                        "Saved to: " + outputPath,
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveToCGraphTXT(Graph graph, File outputFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // 1. Zapisz macierz sąsiedztwa
            int maxX = 0, maxY = 0;
            for (Vertices v : graph.getWierzcholki().values()) {
                if (v.getX() > maxX) maxX = v.getX();
                if (v.getY() > maxY) maxY = v.getY();
            }

            // Utwórz macierz i wypełnij ją zerami
            int[][] matrix = new int[maxY + 1][maxX + 1];
            for (Vertices v : graph.getWierzcholki().values()) {
                matrix[v.getY()][v.getX()] = 1;
            }

            // Zapisz macierz
            for (int i = 0; i < matrix.length; i++) {
                writer.print("[");
                for (int j = 0; j < matrix[i].length; j++) {
                    writer.print(matrix[i][j]);
                    if (j < matrix[i].length - 1) writer.print(".");
                }
                writer.println("]");
            }

            // Dodaj puste linie między podgrafami
            writer.println();
            writer.println();

            // 2. Zapisz połączenia między wierzchołkami (podgrafy)
            Map<Integer, List<Vertices>> colorGroups = new HashMap<>();
            for (Vertices v : graph.getWierzcholki().values()) {
                colorGroups.computeIfAbsent(v.getColor(), k -> new ArrayList<>()).add(v);
            }

            for (Map.Entry<Integer, List<Vertices>> entry : colorGroups.entrySet()) {
                List<Vertices> group = entry.getValue();
                for (Vertices v : group) {
                    for (Vertices neighbor : v.getNeighbors()) {
                        writer.println(v.getId() + " - " + neighbor.getId());
                    }
                    // Dla wierzchołków bez sąsiadów
                    if (v.getNeighbors().isEmpty()) {
                        writer.println(v.getId() + " - -1");
                    }
                }
                writer.println(); // Pusta linia między podgrafami
            }
        }
    }

    private void saveToCGraphBIN(Graph graph, File outputFile) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(outputFile))) {
            // 1. Oblicz rozmiar macierzy
            int maxX = 0, maxY = 0;
            for (Vertices v : graph.getWierzcholki().values()) {
                if (v.getX() > maxX) maxX = v.getX();
                if (v.getY() > maxY) maxY = v.getY();
            }

            // Zapisz wymiary macierzy
            dos.writeInt(Integer.reverseBytes(maxX + 1));
            dos.writeInt(Integer.reverseBytes(maxY + 1));

            // Zapisz macierz (wierszami)
            int[][] matrix = new int[maxY + 1][maxX + 1];
            for (Vertices v : graph.getWierzcholki().values()) {
                matrix[v.getY()][v.getX()] = 1;
            }

            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    dos.writeInt(Integer.reverseBytes(matrix[i][j]));
                }
            }

            // 2. Zapisz połączenia (podgrafy)
            Map<Integer, List<Vertices>> colorGroups = new HashMap<>();
            for (Vertices v : graph.getWierzcholki().values()) {
                colorGroups.computeIfAbsent(v.getColor(), k -> new ArrayList<>()).add(v);
            }

            // Liczba podgrafów
            dos.writeInt(Integer.reverseBytes(colorGroups.size()));

            for (Map.Entry<Integer, List<Vertices>> entry : colorGroups.entrySet()) {
                List<Vertices> group = entry.getValue();
                // Liczba par w podgrafie
                int pairCount = group.stream().mapToInt(v -> Math.max(1, v.getNeighbors().size())).sum();
                dos.writeInt(Integer.reverseBytes(pairCount));

                for (Vertices v : group) {
                    if (v.getNeighbors().isEmpty()) {
                        // Wierzchołek bez sąsiadów
                        dos.writeInt(Integer.reverseBytes(v.getId()));
                        dos.writeInt(Integer.reverseBytes(-1));
                    } else {
                        // Wierzchołek z sąsiadami
                        for (Vertices neighbor : v.getNeighbors()) {
                            dos.writeInt(Integer.reverseBytes(v.getId()));
                            dos.writeInt(Integer.reverseBytes(neighbor.getId()));
                        }
                    }
                }
            }
        }
    }

    private void showCenteredMessage(String message, String title, int messageType) {
        JOptionPane pane = new JOptionPane(message, messageType);
        JDialog dialog = pane.createDialog(this, title);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}

