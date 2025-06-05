import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

public class Graph {
    private Map<Integer, Vertices> graph;

    public Graph(){
        this.graph = new HashMap<>();
    }

    public void addVertex(int id, Vertices vertex) {
        if (!graph.containsKey(id)) {
            graph.put(id, vertex);
        }
    }

    public void addEdge(int id1, int id2) {
        Vertices w1 = graph.get(id1);
        Vertices w2 = graph.get(id2);
        if (w1 != null && w2 != null) {
            w1.addNeighbor(w2);
        }
    }

    public static Graph loadCSRRGGraph(String filePath) throws IOException {
        Graph graph = new Graph();
        BufferedReader br = new BufferedReader(new FileReader(filePath));

        String maxVertices = br.readLine();

        String[] indexesStr = br.readLine().split(";");
        int[] indexes = Arrays.stream(indexesStr).mapToInt(Integer::parseInt).toArray();
        int numOfVertices = indexes.length;
        String[] rowStartsStr = br.readLine().split(";");
        int[] rowStarts = Arrays.stream(rowStartsStr).mapToInt(Integer::parseInt).toArray();
        int numOfRows = rowStarts.length;
        String[] groupsStr = br.readLine().split(";");
        int[] groups = Arrays.stream(groupsStr).mapToInt(Integer::parseInt).toArray();
        String[] groupStartsStr = br.readLine().split(";");
        int[] groupStarts = Arrays.stream(groupStartsStr).mapToInt(Integer::parseInt).toArray();
        int numOfGroups = groupStarts.length;
        int id = 0;
        for(int i = 0; i < numOfRows; i++){
            int start = rowStarts[i];
            int end = (i + 1 < numOfRows) ? rowStarts[i+1] : numOfVertices;
            for(int j = start; j < end; j++){
                Vertices vertex = new Vertices(id, indexes[id], i, 0);
                graph.addVertex(id, vertex);
                id++;
            }
        }

        for(int i = 0; i < numOfGroups; i++){
            int start = groupStarts[i];
            int end = (i + 1 < numOfGroups) ? groupStarts[i + 1] : groups.length;
            for(int j = start + 1; j < end; j++){
                graph.addEdge(groups[start], groups[j]);
            }
        }
        return graph;
    }

    public static Graph loadTXTGraph(String filePath) throws IOException{
        Graph graph = new Graph();
        BufferedReader br = new BufferedReader(new FileReader(filePath));

        br.readLine();
        String wierzcholki = br.readLine();
        int liczbaWierzcholkow = 0;
        Pattern pattern = Pattern.compile("Liczba wierzcholkow: (\\d+)");
        Matcher matcher = pattern.matcher(wierzcholki);
        if (matcher.find()) {
            liczbaWierzcholkow = Integer.parseInt(matcher.group(1));
            //parsowanie ilosci wierzcholkow - na tej podstawie zostanie utworzona poczatkowa lista wierzcholkow w formie jak najbardziej kwadratowej
        }
        int rowLength = (int) Math.round(Math.sqrt(liczbaWierzcholkow));
        for(int i = 0; i < liczbaWierzcholkow; i++){
            Vertices vertex = new Vertices(i, i % rowLength, i / rowLength, 0);
            graph.addVertex(i, vertex);
        }

        br.readLine();
        br.readLine();
        br.readLine();

        for(int i = 0; i < liczbaWierzcholkow; i++){
            String line = br.readLine();
            String cleanLine = line.replace("[", "").replace("]", "").replace(" ", "").replace(".",";");

            String[] elements = cleanLine.split(";");

            for(int j = 0; j < liczbaWierzcholkow; j++){
                if(elements[j].equals("1")) {
                    graph.addEdge(i, j);
                }
            }
        }

        br.readLine();
        br.readLine();
        br.readLine();
        for(int i = 0; i < liczbaWierzcholkow; i++){
            String[] pair = br.readLine().split(" - ");
            int[] intPair = Arrays.stream(pair).mapToInt(Integer::parseInt).toArray();

            Vertices w = graph.graph.get(intPair[0]);
            w.setColor(intPair[1]);
        }

        return graph;
    }

    public static Graph loadCGraphTXT(String filepath) throws IOException{
        Graph graph = new Graph();
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        int row = 0;
        int id = 0;
        int color = 0;
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                color++;
                continue;
            }


            // Sprawdzamy czy to linia z macierzą (zawiera "[")
            if (line.startsWith("[")) {
                line = line.replaceAll("[\\[\\]]", "").trim();
                String[] parts = line.split("\\s*\\.\\s*");

                for (int col = 0; col < parts.length; col++) {
                    int value = Integer.parseInt(parts[col]);
                    if (value == 1) {
                        // Traktujemy '1' jako wierzchołek na pozycji (row, col)
                        graph.addVertex(id, new Vertices(id, col, row, 0));
                        id++;
                    }
                }
                row++;
            } else {
                // 2. Parsowanie list połączeń
                String[] connection = line.split("\\s*-\\s*");
                if (connection.length == 2) {
                    int from = Integer.parseInt(connection[0]);
                    int to = Integer.parseInt(connection[1]);
                    if (to != -1) {  // Ignorujemy połączenia z "-1"
                        graph.addEdge(from, to);
                        Vertices w = graph.graph.get(to);
                        w.setColor(color);
                    }
                    Vertices w1 = graph.graph.get(from);
                    w1.setColor(color);
                }
            }
        }
        return graph;
    }

    public static Graph loadCGraphBIN(String filepath) throws IOException{
        Graph graph = new Graph();
        DataInputStream dis = new DataInputStream(new FileInputStream(filepath));
        int id = 0;
        int color = 0;

        int width, height;
        width = Integer.reverseBytes(dis.readInt());
        height = Integer.reverseBytes(dis.readInt());

        int pos;

        for(int i = 0; i < height; i++){
            for(int j = 0; j < width; j++){
                pos = Integer.reverseBytes(dis.readInt());
                if(pos == 1){
                    graph.addVertex(id, new Vertices(id, j, i, 0));
                    id++;
                }
            }
        }
        int numOfSubgraphs = Integer.reverseBytes(dis.readInt());

        for(int i = 0; i < numOfSubgraphs; i++){
            int numOfPairs = Integer.reverseBytes(dis.readInt());
            for(int j = 0; j < numOfPairs; j++){
                int v1 = Integer.reverseBytes(dis.readInt());
                int v2 = Integer.reverseBytes(dis.readInt());
                if (v2 != -1) {
                    graph.addEdge(v1, v2);
                    Vertices w = graph.graph.get(v2);
                    w.setColor(color);
                }
                Vertices w1 = graph.graph.get(v1);
                w1.setColor(color);
            }
            color++;
        }
        dis.close();
        return graph;
    }

    public Map<Integer, Vertices> getWierzcholki() {
        return graph;
    }
}



