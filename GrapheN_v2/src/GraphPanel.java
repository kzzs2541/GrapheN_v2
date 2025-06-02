import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

public class GraphPanel extends JPanel {
    private static final int VERTEX_RADIUS = 20;
    private static final int EDGE_THICKNESS = 2;
    private final Map<Integer, Vertices> wierzcholki;
    private double scale = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private int lastMouseX, lastMouseY;
    private List<JCheckBox> partCheckboxes = new ArrayList<>();
    private Set<Integer> visibleParts = new HashSet<>();
    private Color[] partColors = {Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW,
            Color.PINK, Color.CYAN, Color.ORANGE, Color.MAGENTA};

    public GraphPanel(Map<Integer, Vertices> wierzcholki, MainFrame parent) {
        this.wierzcholki = wierzcholki;
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        // Panel z checkboxami
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(Color.LIGHT_GRAY);

        // Znajdź wszystkie unikalne kolory podgrafów
        Set<Integer> allParts = new HashSet<>();
        for (Vertices v : wierzcholki.values()) {
            allParts.add(v.getColor());
        }

        // Stwórz checkboxy dla każdego podgrafu
        for (int part : allParts) {
            JCheckBox checkBox = new JCheckBox("Podgraf " + (part + 1), true);
            checkBox.setForeground(partColors[part % partColors.length]);
            checkBox.addActionListener(e -> {
                if (checkBox.isSelected()) {
                    visibleParts.add(part);
                } else {
                    visibleParts.remove(part);
                }
                repaint();
            });
            partCheckboxes.add(checkBox);
            controlPanel.add(checkBox);
            visibleParts.add(part); // Domyślnie wszystkie widoczne
        }

        parent.add(controlPanel, BorderLayout.NORTH);

        // Nasłuchiwanie zoomu
        addMouseWheelListener(e -> {
            double zoomFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
            scale = Math.max(0.5, Math.min(1.5, scale * zoomFactor));
            repaint();
        });

        // Nasłuchiwanie przeciągania
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;
                translateX += dx;
                translateY += dy;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform transform = new AffineTransform();
        transform.translate(translateX, translateY);
        transform.scale(scale, scale);
        g2d.setTransform(transform);

        // Rysuj tylko widoczne krawędzie
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(EDGE_THICKNESS));
        for (Vertices w : wierzcholki.values()) {
            if (!visibleParts.contains(w.getColor())) continue;

            Point p1 = new Point(w.getX() * 50, w.getY() * 50);
            for (Vertices sasiad : w.getNeighbors()) {
                if (visibleParts.contains(sasiad.getColor())) {
                    Point p2 = new Point(sasiad.getX() * 50, sasiad.getY() * 50);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        // Rysuj tylko widoczne wierzchołki
        for (Vertices w : wierzcholki.values()) {
            if (!visibleParts.contains(w.getColor())) continue;

            g2d.setColor(partColors[w.getColor() % partColors.length]);
            g2d.fillOval(w.getX() * 50 - VERTEX_RADIUS, w.getY() * 50 - VERTEX_RADIUS,
                    2 * VERTEX_RADIUS, 2 * VERTEX_RADIUS);

            g2d.setColor(Color.BLACK);
            g2d.drawOval(w.getX() * 50 - VERTEX_RADIUS, w.getY() * 50 - VERTEX_RADIUS,
                    2 * VERTEX_RADIUS, 2 * VERTEX_RADIUS);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String label = String.valueOf(w.getId());
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(label,
                    w.getX() * 50 - fm.stringWidth(label) / 2,
                    w.getY() * 50 + fm.getAscent() / 4);
        }
    }
}
