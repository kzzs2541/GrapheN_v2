import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public MainFrame() {
        super("GrapheN");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLayout(new BorderLayout());

        // Lewy panel (menu wyborów)
        JPanel menu = createMenuPanel();
        add(menu, BorderLayout.WEST);

        // Prawy panel (wizualizacja)
        //JPanel visualizationPanel = createVisualizationPanel();
        //add(visualizationPanel, BorderLayout.CENTER);
    }

    private JPanel createMenuPanel() {
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel do dzielenia grafu
        GraphPartitioningPanel splitterPanel = new GraphPartitioningPanel();
        menuPanel.add(splitterPanel);
        menuPanel.add(Box.createVerticalStrut(20));

        // Panel do wizualizacji
        GraphVisualizerPanel visualizerPanel = new GraphVisualizerPanel(this);
        menuPanel.add(visualizerPanel);

        return menuPanel;
    }

    public JPanel createVisualizationPanel() {
        JPanel graphPanel = new JPanel();
        graphPanel.setBackground(Color.WHITE);
        graphPanel.setBorder(BorderFactory.createTitledBorder("Visualization Area"));
        return graphPanel;
        //na razie jest to placeholder, po wczytaniu grafu zostanie zastapione nowym panelem
    }
}

