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

        String basePath = selectedFile.getAbsolutePath().replace(".csrrg", "_partitioned").replace("og_files", "own_java");
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

    private void showCenteredMessage(String message, String title, int messageType) {
        JOptionPane pane = new JOptionPane(message, messageType);
        JDialog dialog = pane.createDialog(this, title);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}

