import java.util.ArrayList;
import java.util.List;

public class Vertices {
    private int id;
    private int x;
    private int y;
    private int color;
    private List<Vertices> neighbors;

    public Vertices(int id, int x, int y, int color){
        this.id = id;
        this.x = x;
        this.y = y;
        this.color = color;
        this.neighbors = new ArrayList<>();
    }

    public int getId(){
        return id;
    }
    public int getX(){
        return x;
    }
    public int getY() {
        return y;
    }
    public int getColor(){return color;}
    public void setColor(int color){this.color = color;}
    public List<Vertices> getNeighbors(){
        return neighbors;
    }

    public void addNeighbor(Vertices neighbor){
        if(!neighbors.contains(neighbor)){
            neighbors.add(neighbor);
        }
    }

    @Override
    public String toString() {
        return "Wierzcholek " + id + " (" + x + ", " + y + ")";
    }
}
