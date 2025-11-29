package pt.unl.fct.pds.project2.model;

import pt.unl.fct.pds.project2.model.Node;
import java.util.Arrays;


public class Circuit {
    int id;
    Node[] nodes;
    int minBandwidth;
    
    public Circuit() {}
    public Circuit(
                    int id,
                    Node[] nodes,
                    int minBandwidth
                  ) {
        this.id = id;
        this.nodes = Arrays.copyOf(nodes, nodes.length);
        this.minBandwidth = minBandwidth;
    }

    public int getId() {return id;}
    public Node[] getNodes() {return nodes;}
    public int getMinBandwidth() {return minBandwidth;}

    
    public void setId(int id) {this.id = id;}
    public void setNodes(Node[] nodes) {this.nodes = Arrays.copyOf(nodes, nodes.length);}
    public void setMinBandwidth() {this.minBandwidth = minBandwidth;}
}
