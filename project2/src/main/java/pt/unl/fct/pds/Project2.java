package pt.unl.fct.pds;

import pt.unl.fct.pds.project2.model.Node;
import pt.unl.fct.pds.project2.model.Circuit;
import pt.unl.fct.pds.project2.utils.ConsensusParser;

import java.util.*;

public class Project2 {

    public static void main(String[] args) {
        System.out.println("Welcome to the Circuit Simulator!");

        // Carregar nodes do consensus
        ConsensusParser parser = new ConsensusParser("consensus.txt");
        Node[] allNodes = parser.parseConsensus();

        int numCircuits = 1000;
        double alpha = 0.5;
        double beta = 0.2;

        // Contadores e listas
        Map<Node,Integer> guardCountsOld = new HashMap<>();
        Map<Node,Integer> middleCountsOld = new HashMap<>();
        Map<Node,Integer> exitCountsOld = new HashMap<>();
        Map<Node,Integer> globalCountsOld = new HashMap<>();
        List<Integer> bandwidthsOld = new ArrayList<>();

        Map<Node,Integer> guardCountsNew = new HashMap<>();
        Map<Node,Integer> middleCountsNew = new HashMap<>();
        Map<Node,Integer> exitCountsNew = new HashMap<>();
        Map<Node,Integer> globalCountsNew = new HashMap<>();
        List<Integer> bandwidthsNew = new ArrayList<>();

        // Simulações
        for (int i = 0; i < numCircuits; i++) {
            Circuit circuitOld = selectPathCurrent(allNodes);
            Circuit circuitNew = selectPathNew(allNodes, alpha, beta);

            updateCounts(circuitOld, globalCountsOld, guardCountsOld, middleCountsOld, exitCountsOld, bandwidthsOld);
            updateCounts(circuitNew, globalCountsNew, guardCountsNew, middleCountsNew, exitCountsNew, bandwidthsNew);
        }

        // Resultados
        System.out.println("\n=== Entropy ===");
        System.out.printf("Current Algorithm: Global=%.4f, Guard=%.4f, Middle=%.4f, Exit=%.4f\n",
                calculateEntropy(globalCountsOld, numCircuits*3),
                calculateEntropy(guardCountsOld, numCircuits),
                calculateEntropy(middleCountsOld, numCircuits),
                calculateEntropy(exitCountsOld, numCircuits));
        System.out.printf("New Algorithm: Global=%.4f, Guard=%.4f, Middle=%.4f, Exit=%.4f\n",
                calculateEntropy(globalCountsNew, numCircuits*3),
                calculateEntropy(guardCountsNew, numCircuits),
                calculateEntropy(middleCountsNew, numCircuits),
                calculateEntropy(exitCountsNew, numCircuits));

        System.out.println("\n=== Bandwidth stats ===");
        System.out.println("Current Algorithm: " + summarizeBandwidth(bandwidthsOld));
        System.out.println("New Algorithm: " + summarizeBandwidth(bandwidthsNew));
    }

    // ================= Funções auxiliares =================
    public static void updateCounts(Circuit circuit, Map<Node,Integer> global,
                                    Map<Node,Integer> guard, Map<Node,Integer> middle,
                                    Map<Node,Integer> exit, List<Integer> bandwidths) {
        Node[] nodes = circuit.getNodes();
        increment(global, nodes[0]);
        increment(global, nodes[1]);
        increment(global, nodes[2]);
        increment(guard, nodes[0]);
        increment(middle, nodes[1]);
        increment(exit, nodes[2]);
        bandwidths.add(calculateMinBandwidth(nodes[0], nodes[1], nodes[2]));
    }

    public static void increment(Map<Node,Integer> map, Node node) {
        map.put(node, map.getOrDefault(node, 0) + 1);
    }

    public static double calculateEntropy(Map<Node,Integer> counts, int totalSelections) {
        double entropy = 0.0;
        for (int count : counts.values()) {
            double p = (double) count / totalSelections;
            entropy -= p * (Math.log(p)/Math.log(2));
        }
        return entropy;
    }

    public static int calculateMinBandwidth(Node guard, Node middle, Node exit) {
        return Math.min(guard.getBandwidth(), Math.min(middle.getBandwidth(), exit.getBandwidth()));
    }

    public static String summarizeBandwidth(List<Integer> bandwidths) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        double sum = 0;
        for (int bw : bandwidths) {
            sum += bw;
            if (bw < min) min = bw;
            if (bw > max) max = bw;
        }
        double avg = sum / bandwidths.size();
        return String.format("min=%d, max=%d, avg=%.2f", min, max, avg);
    }

    // ================= Algoritmos =================
    public static Circuit selectPathCurrent(Node[] nodes) {
        Node guard = sampleRandom(nodes);
        Node middle = sampleRandom(nodes);
        Node exit = sampleRandom(nodes);
        return new Circuit(0, new Node[]{guard, middle, exit}, calculateMinBandwidth(guard, middle, exit));
    }

    public static Circuit selectPathNew(Node[] nodes, double alpha, double beta) {
        Node exit = sampleRandom(nodes);
        Node guard = sampleWeightedGuard(nodes, exit, alpha);
        Node middle = sampleWeightedMiddle(nodes, guard, exit, beta);
        return new Circuit(0, new Node[]{guard, middle, exit}, calculateMinBandwidth(guard, middle, exit));
    }

    public static Node sampleRandom(Node[] nodes) {
        Random rand = new Random();
        return nodes[rand.nextInt(nodes.length)];
    }

    // ================= Ponderação geográfica =================
    public static Node sampleWeightedGuard(Node[] nodes, Node exit, double alpha) {
        // Pesos baseados em bandwidth
        List<Double> weights = new ArrayList<>();
        for (Node n : nodes) {
            double w = n.getBandwidth();
            if (!n.getCountry().equals(exit.getCountry())) {
                w *= (1 + alpha); // aumenta peso se país diferente
            }
            weights.add(w);
        }
        return weightedSample(nodes, weights);
    }

    public static Node sampleWeightedMiddle(Node[] nodes, Node guard, Node exit, double beta) {
        List<Double> weights = new ArrayList<>();
        for (Node n : nodes) {
            double w = n.getBandwidth();
            int c = 1;
            boolean diffGuard = !n.getCountry().equals(guard.getCountry());
            boolean diffExit = !n.getCountry().equals(exit.getCountry());
            if (diffGuard && diffExit) c = 3;
            else if (diffGuard || diffExit) c = 2;
            w *= (1 + beta * c);
            weights.add(w);
        }
        return weightedSample(nodes, weights);
    }

    // ================= Amostragem ponderada =================
    public static Node weightedSample(Node[] nodes, List<Double> weights) {
        double sum = weights.stream().mapToDouble(d -> d).sum();
        double r = Math.random() * sum;
        double acc = 0;
        for (int i = 0; i < nodes.length; i++) {
            acc += weights.get(i);
            if (r <= acc) return nodes[i];
        }
        return nodes[nodes.length - 1]; // fallback
    }
}
