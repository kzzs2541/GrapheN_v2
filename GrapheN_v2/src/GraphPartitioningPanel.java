import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

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
        Graph graf = Graph.loadTXTGraph(selectedFile.getAbsolutePath());


        String inputFile = fileLabel.getText();
        if(!splitCountField.getText().isEmpty()){
            n = Integer.parseInt(splitCountField.getText());
        }else{
            n = 2;
        }
        if(!marginErrorField.getText().isEmpty()){
            margin = Integer.parseInt(marginErrorField.getText());
        }else{
            margin = 10;
        }
        if(optionBin.isSelected()){
            outputType = "bin";
        }else{
            outputType = "txt";
        }

        System.out.println("plik wejsciowy: " + selectedFile.getAbsolutePath());
        System.out.println("ilosc podzialow: " + n);
        System.out.println("margines bledu: " + margin);
        System.out.println("typ pliku wyjsciowego: " + outputType);

        for (Vertices w : graf.getWierzcholki().values()) {
            System.out.println(w + " -> Sąsiedzi: " + w.getNeighbors());
        }
    }
}

