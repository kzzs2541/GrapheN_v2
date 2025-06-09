import java.util.*;
import java.io.*;

public class GraphPartitioner
{
    private Graph originalGraph;
    private int targetPartitions;
    private double balanceFactor;
    private Random random = new Random();

    public GraphPartitioner(Graph graph, int targetPartitions, double balanceFactor)
    {
        this.originalGraph = graph;
        this.targetPartitions = targetPartitions;
        this.balanceFactor = balanceFactor;
    }

    public Map<Integer, Integer> partition() throws IOException
    {

        Graph coarseGraph = coarsenGraph(originalGraph);


        Map<Integer, Integer> partition = balancedInitialPartition(coarseGraph);


        partition = uncoarsenAndRefine(originalGraph, coarseGraph, partition);


        partition = enforceBalanceAndConnectivity(originalGraph, partition);

        return partition;
    }

//redukowanie grafu
    private Graph coarsenGraph(Graph graph)
    {
        Graph coarseGraph = new Graph();
        Map<Integer, Integer> matching = new HashMap<>();
        Set<Integer> matched = new HashSet<>();
        List<Integer> vertices = new ArrayList<>(graph.getVertices());
        Collections.shuffle(vertices);

        // losowe dopasowanie
        for (int i = 0; i < vertices.size(); i++)
        {
            int v = vertices.get(i);
            if (matched.contains(v)) continue;

            //szukanie niedopasowanych sąsiadów
            List<Vertices> neighbors = graph.getNeighbors(v);
            List<Vertices> unmatchedNeighbors = new ArrayList<>();
            for (Vertices neighbor : neighbors) {
                if (!matched.contains(neighbor.getId()))
                {
                    unmatchedNeighbors.add(neighbor);
                }
            }

            if (!unmatchedNeighbors.isEmpty())
            {
                //losowe wybranie wolnego sąsiada
                Vertices u = unmatchedNeighbors.get(random.nextInt(unmatchedNeighbors.size()));
                int coarseId = Math.min(v, u.getId());

                //tworzenie superwierzchołka
                Vertices vVertex = graph.getWierzcholki().get(v);
                Vertices uVertex = graph.getWierzcholki().get(u.getId());
                Vertices coarseVertex = new Vertices(
                        coarseId,
                        (vVertex.getX() + uVertex.getX()) / 2,
                        (vVertex.getY() + uVertex.getY()) / 2,
                        0
                );

                coarseGraph.addVertex(coarseId, coarseVertex);
                matching.put(v, coarseId);
                matching.put(u.getId(), coarseId);
                matched.add(v);
                matched.add(u.getId());
            } else
            {
                // dodanie niedopasowanych wierzchołków
                coarseGraph.addVertex(v, new Vertices(v, graph.getWierzcholki().get(v).getX(),
                        graph.getWierzcholki().get(v).getY(), 0));
                matching.put(v, v);
            }
        }

        //dodanie krawedzi miedzy superwierzcholkami
        for (Vertices v : graph.getWierzcholki().values())
        {
            for (Vertices neighbor : v.getNeighbors())
            {
                int coarseV = matching.get(v.getId());
                int coarseU = matching.get(neighbor.getId());
                if (coarseV != coarseU)
                {
                    coarseGraph.addEdge(coarseV, coarseU);
                }
            }
        }

        return coarseGraph;
    }

    //poczatkowy podzial
    private Map<Integer, Integer> balancedInitialPartition(Graph graph)
    {
        Map<Integer, Integer> partition = new HashMap<>();
        List<Integer> vertices = new ArrayList<>(graph.getVertices());
        Collections.shuffle(vertices);

        int avgSize = (int) Math.ceil((double) vertices.size() / targetPartitions);
        int maxSize = (int) (avgSize * (1 + balanceFactor));
        int[] partitionSizes = new int[targetPartitions];

        for (int v : vertices)
        {
            //znajdowanie najmniejszej partycji ktora nie przekracza limitu
            int chosenPart = 0;
            int minSize = Integer.MAX_VALUE;
            for (int p = 0; p < targetPartitions; p++)
            {
                if (partitionSizes[p] < minSize && partitionSizes[p] < maxSize)
                {
                    minSize = partitionSizes[p];
                    chosenPart = p;
                }
            }

            partition.put(v, chosenPart);
            partitionSizes[chosenPart]++;
        }

        return partition;
    }

    // uncoarsening i poczatkowe poprawki
    private Map<Integer, Integer> uncoarsenAndRefine(Graph fineGraph, Graph coarseGraph, Map<Integer, Integer> coarsePartition)
    {
        Map<Integer, Integer> finePartition = new HashMap<>();

        //odbudowanie struktury grafu
        for (Vertices fineVertex : fineGraph.getWierzcholki().values())
        {
            //znajdowanie odpowiadających podwierzchołkow
            int coarseId = findCorrespondingCoarseVertex(fineVertex, coarseGraph);
            if (coarsePartition.containsKey(coarseId))
            {
                finePartition.put(fineVertex.getId(), coarsePartition.get(coarseId));
            } else
            {
                //jezeli nie znaleziono dopasowania, przypisuje do losowej partycji
                //nie powinno sie zdarzac w teorii ale zapobiega bledom w duzych grafach
                finePartition.put(fineVertex.getId(), random.nextInt(targetPartitions));
            }
        }

        return finePartition;
    }

    //poprawki balansu i spojnosci
    private Map<Integer, Integer> enforceBalanceAndConnectivity(Graph graph, Map<Integer, Integer> partition) {

        partition = ensureConnectivity(graph, partition);


        partition = balancePartitions(graph, partition);

        return partition;
    }

    private Map<Integer, Integer> ensureConnectivity(Graph graph, Map<Integer, Integer> partition)
    {
        Map<Integer, List<Integer>> partitions = new HashMap<>();
        partition.forEach((v, p) -> partitions.computeIfAbsent(p, k -> new ArrayList<>()).add(v));

        for (int p = 0; p < targetPartitions; p++) {
            List<Integer> vertices = partitions.get(p);
            if (vertices == null) continue;

            //znajdowanie polaczanych elementow
            List<Set<Integer>> components = findConnectedComponents(graph, vertices);

            if (components.size() > 1) {
                Set<Integer> mainComponent = components.get(0);
                for (int i = 1; i < components.size(); i++)
                {
                    for (int v : components.get(i))
                    {
                        // znajdowanie sasiednich partycji
                        Map<Integer, Integer> adjacentPartitions = new HashMap<>();
                        for (Vertices neighbor : graph.getNeighbors(v))
                        {
                            int neighborPart = partition.get(neighbor.getId());
                            adjacentPartitions.put(neighborPart,
                                    adjacentPartitions.getOrDefault(neighborPart, 0) + 1);
                        }

                        //dopasowanie do najbardziej pasujacej partycji
                        if (!adjacentPartitions.isEmpty())
                        {
                            int newPart = Collections.max(adjacentPartitions.entrySet(),
                                    Map.Entry.comparingByValue()).getKey();
                            partition.put(v, newPart);
                        } else
                        {
                            //losowe przypisanie w przypadku brakow sasiednich partycji
                            partition.put(v, random.nextInt(targetPartitions));
                        }
                    }
                }
            }
        }

        return partition;
    }

    private Map<Integer, Integer> balancePartitions(Graph graph, Map<Integer, Integer> partition)
    {
        int totalVertices = graph.getVertexCount();
        int avgSize = (int) Math.ceil((double) totalVertices / targetPartitions);
        int maxSize = (int) (avgSize * (1 + balanceFactor));
        int minSize = (int) (avgSize * (1 - balanceFactor));

        //zliczanie obecnych rozmiarow
        int[] partitionSizes = new int[targetPartitions];
        partition.values().forEach(p -> partitionSizes[p]++);


        boolean balanced = false;
        while (!balanced)
        {
            balanced = true;

            //znajdowanie najwiekszej i najmniejszej partycji
            int largestPart = 0, smallestPart = 0;
            for (int p = 1; p < targetPartitions; p++)
            {
                if (partitionSizes[p] > partitionSizes[largestPart]) largestPart = p;
                if (partitionSizes[p] < partitionSizes[smallestPart]) smallestPart = p;
            }

            // sprawdzanie limitow
            if (partitionSizes[largestPart] > maxSize ||
                    partitionSizes[smallestPart] < minSize) {
                balanced = false;

                //znajdowanie wierzcholkow granicznych ktore mozna przesunac
                List<Integer> borderVertices = new ArrayList<>();
                for (int v : partition.keySet())
                {
                    if (partition.get(v) == largestPart)
                    {
                        for (Vertices neighbor : graph.getNeighbors(v))
                        {
                            if (partition.get(neighbor.getId()) != largestPart)
                            {
                                borderVertices.add(v);
                                break;
                            }
                        }
                    }
                }

                // przesuwanie wierzcholkow
                if (!borderVertices.isEmpty())
                {
                    Collections.shuffle(borderVertices);
                    int verticesToMove = Math.min(borderVertices.size(), (partitionSizes[largestPart] - avgSize) / 2);

                    for (int i = 0; i < verticesToMove; i++)
                    {
                        int v = borderVertices.get(i);
                        partition.put(v, smallestPart);
                        partitionSizes[largestPart]--;
                        partitionSizes[smallestPart]++;
                    }
                }
            }
        }

        return partition;
    }


    private int findCorrespondingCoarseVertex(Vertices fineVertex, Graph coarseGraph)
    {
        //znajdowanie najblizszego superwierzcholka
        double minDist = Double.MAX_VALUE;
        int closest = -1;

        for (Vertices coarseVertex : coarseGraph.getWierzcholki().values())
        {
            double dist = Math.sqrt(Math.pow(fineVertex.getX() - coarseVertex.getX(), 2) + Math.pow(fineVertex.getY() - coarseVertex.getY(), 2));
            if (dist < minDist)
            {
                minDist = dist;
                closest = coarseVertex.getId();
            }
        }

        return closest;
    }

    private List<Set<Integer>> findConnectedComponents(Graph graph, List<Integer> vertices)
    {
        List<Set<Integer>> components = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();

        for (int v : vertices)
        {
            if (!visited.contains(v))
            {
                Set<Integer> component = new HashSet<>();
                Stack<Integer> stack = new Stack<>();
                stack.push(v);

                while (!stack.isEmpty())
                {
                    int current = stack.pop();
                    if (!visited.contains(current))
                    {
                        visited.add(current);
                        component.add(current);
                        for (Vertices neighbor : graph.getNeighbors(current))
                        {
                            if (vertices.contains(neighbor.getId()))
                            {
                                stack.push(neighbor.getId());
                            }
                        }
                    }
                }
                components.add(component);
            }
        }
        return components;
    }

    public void savePartitionedGraph(Graph graph, Map<Integer, Integer> partition, String filePath) throws IOException
    {
        // przypisanie kolorow do partycji
        for (Map.Entry<Integer, Vertices> entry : graph.getWierzcholki().entrySet()) {
            entry.getValue().setColor(partition.get(entry.getKey()));
        }

        //zapisywanie do pliku txt
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath)))
        {
            // macierz sasiedztwa
            int maxX = 0, maxY = 0;
            for (Vertices v : graph.getWierzcholki().values())
            {
                if (v.getX() > maxX) maxX = v.getX();
                if (v.getY() > maxY) maxY = v.getY();
            }

            int[][] matrix = new int[maxY + 1][maxX + 1];
            for (Vertices v : graph.getWierzcholki().values())
            {
                matrix[v.getY()][v.getX()] = 1;
            }

            for (int i = 0; i < matrix.length; i++)
            {
                writer.print("[");
                for (int j = 0; j < matrix[i].length; j++)
                {
                    writer.print(matrix[i][j]);
                    if (j < matrix[i].length - 1) writer.print(".");
                }
                writer.println("]");
            }

            writer.println();
            writer.println();

            //polaczenia i kolory
            Map<Integer, List<Vertices>> colorGroups = new HashMap<>();
            for (Vertices v : graph.getWierzcholki().values())
            {
                colorGroups.computeIfAbsent(v.getColor(), k -> new ArrayList<>()).add(v);
            }

            for (Map.Entry<Integer, List<Vertices>> entry : colorGroups.entrySet())
            {
                List<Vertices> group = entry.getValue();
                for (Vertices v : group)
                {
                    for (Vertices neighbor : v.getNeighbors())
                    {
                        writer.println(v.getId() + " - " + neighbor.getId());
                    }
                    if (v.getNeighbors().isEmpty())
                    {
                        writer.println(v.getId() + " - -1");
                    }
                }
                writer.println();
            }
        }
    }
    public void savePartitionedGraphBIN(Graph graph, Map<Integer, Integer> partition, String filePath) throws IOException {
        //przypisanie kolorow do partycji
        for (Map.Entry<Integer, Vertices> entry : graph.getWierzcholki().entrySet()) {
            entry.getValue().setColor(partition.get(entry.getKey()));
        }

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(filePath))) {
            //wymiary macierzy
            int maxX = 0, maxY = 0;
            for (Vertices v : graph.getWierzcholki().values()) {
                if (v.getX() > maxX) maxX = v.getX();
                if (v.getY() > maxY) maxY = v.getY();
            }


            dos.writeInt(Integer.reverseBytes(maxX + 1));
            dos.writeInt(Integer.reverseBytes(maxY + 1));

            //zapisywanie macierzy
            int[][] matrix = new int[maxY + 1][maxX + 1];
            for (Vertices v : graph.getWierzcholki().values()) {
                matrix[v.getY()][v.getX()] = 1;
            }

            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    dos.writeInt(Integer.reverseBytes(matrix[i][j]));
                }
            }

            // zapisywanie podgrafow
            Map<Integer, List<Vertices>> partitions = new HashMap<>();
            for (Vertices v : graph.getWierzcholki().values()) {
                partitions.computeIfAbsent(v.getColor(), k -> new ArrayList<>()).add(v);
            }

            // liczba podgrafow
            dos.writeInt(Integer.reverseBytes(partitions.size()));

            for (Map.Entry<Integer, List<Vertices>> entry : partitions.entrySet()) {
                List<Vertices> group = entry.getValue();

                //obliczenie liczby polaczen miedzy wierzcholkami
                int pairCount = 0;
                for (Vertices v : group) {
                    pairCount += Math.max(1, v.getNeighbors().size());
                }

                // naglowek partycji
                dos.writeInt(Integer.reverseBytes(pairCount));

                // lista krawedzi
                for (Vertices v : group) {
                    if (v.getNeighbors().isEmpty()) {
                        dos.writeInt(Integer.reverseBytes(v.getId()));
                        dos.writeInt(Integer.reverseBytes(-1));
                    } else {
                        //wszystkie krawedzie danego wierzcholka
                        for (Vertices neighbor : v.getNeighbors()) {
                            dos.writeInt(Integer.reverseBytes(v.getId()));
                            dos.writeInt(Integer.reverseBytes(neighbor.getId()));
                        }
                    }
                }
            }
        }
    }
}