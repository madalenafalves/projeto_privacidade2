package pt.unl.fct.pds;

import pt.unl.fct.pds.project2.model.Node;
import pt.unl.fct.pds.project2.model.Circuit;
import pt.unl.fct.pds.project2.utils.ConsensusParser;
import pt.unl.fct.pds.project2.utils.PathSelector;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Project2 {

    public static void main(String[] args) {
        System.out.println("Welcome to the Circuit Simulator!");

        // ---------------- Carregar nodes do consensus ----------------
        ConsensusParser parser = new ConsensusParser("consensus.txt");
        Node[] allNodesArray = parser.parseConsensus();
        List<Node> allNodes = Arrays.asList(allNodesArray);

        PathSelector selector = new PathSelector(allNodes);

        // ---------------- Parâmetros da simulação ----------------
        int numCircuits = 1000;
        double alpha = 0.5;
        double beta = 0.2;

        // ---------------- Estruturas de métricas ----------------
        // Original
        Set<Node> guardSetOld = new HashSet<>();
        Set<Node> middleSetOld = new HashSet<>();
        Set<Node> exitSetOld = new HashSet<>();
        Set<Node> globalSetOld = new HashSet<>();
        Map<Node,Integer> guardCountsOld = new HashMap<>();
        Map<Node,Integer> middleCountsOld = new HashMap<>();
        Map<Node,Integer> exitCountsOld = new HashMap<>();
        Map<Node,Integer> globalCountsOld = new HashMap<>();
        List<Integer> bandwidthsOld = new ArrayList<>();

        // Geo-Aware
        Set<Node> guardSetNew = new HashSet<>();
        Set<Node> middleSetNew = new HashSet<>();
        Set<Node> exitSetNew = new HashSet<>();
        Set<Node> globalSetNew = new HashSet<>();
        Map<Node,Integer> guardCountsNew = new HashMap<>();
        Map<Node,Integer> middleCountsNew = new HashMap<>();
        Map<Node,Integer> exitCountsNew = new HashMap<>();
        Map<Node,Integer> globalCountsNew = new HashMap<>();
        List<Integer> bandwidthsNew = new ArrayList<>();

        // ---------------- Loop de simulação ----------------
        for (int i = 0; i < numCircuits; i++) {
            Circuit circuitOld = selectPathCurrent(selector);
            Circuit circuitNew = selectPathNew(selector, alpha, beta);

            updateMetrics(circuitOld, guardSetOld, middleSetOld, exitSetOld, globalSetOld,
                    guardCountsOld, middleCountsOld, exitCountsOld, globalCountsOld, bandwidthsOld);

            updateMetrics(circuitNew, guardSetNew, middleSetNew, exitSetNew, globalSetNew,
                    guardCountsNew, middleCountsNew, exitCountsNew, globalCountsNew, bandwidthsNew);
        }

        // ---------------- Resultados ----------------
        System.out.println("\n=== Node Diversity ===");
        System.out.printf("Original: Guard=%d, Middle=%d, Exit=%d, Global=%d\n",
                guardSetOld.size(), middleSetOld.size(), exitSetOld.size(), globalSetOld.size());
        System.out.printf("Geo-Aware: Guard=%d, Middle=%d, Exit=%d, Global=%d\n",
                guardSetNew.size(), middleSetNew.size(), exitSetNew.size(), globalSetNew.size());

        System.out.println("\n=== Entropy ===");
        System.out.printf("Original: Global=%.4f, Guard=%.4f, Middle=%.4f, Exit=%.4f\n",
                calculateEntropy(globalCountsOld, numCircuits*3),
                calculateEntropy(guardCountsOld, numCircuits),
                calculateEntropy(middleCountsOld, numCircuits),
                calculateEntropy(exitCountsOld, numCircuits));
        System.out.printf("Geo-Aware: Global=%.4f, Guard=%.4f, Middle=%.4f, Exit=%.4f\n",
                calculateEntropy(globalCountsNew, numCircuits*3),
                calculateEntropy(guardCountsNew, numCircuits),
                calculateEntropy(middleCountsNew, numCircuits),
                calculateEntropy(exitCountsNew, numCircuits));

        System.out.println("\n=== Bandwidth stats ===");
        System.out.println("Original: " + summarizeBandwidth(bandwidthsOld));
        System.out.println("Geo-Aware: " + summarizeBandwidth(bandwidthsNew));

        // ---------------- Exportar para CSV para gráficos ----------------
        exportBandwidthCSV("bandwidth_comparison.csv", bandwidthsOld, bandwidthsNew);
    }

    // ================= Funções auxiliares =================
    public static void updateMetrics(Circuit circuit,
                                     Set<Node> guardSet, Set<Node> middleSet, Set<Node> exitSet, Set<Node> globalSet,
                                     Map<Node,Integer> guardCounts, Map<Node,Integer> middleCounts,
                                     Map<Node,Integer> exitCounts, Map<Node,Integer> globalCounts,
                                     List<Integer> bandwidths) {
        Node[] nodes = circuit.getNodes();

        // Diversidade
        guardSet.add(nodes[0]);
        middleSet.add(nodes[1]);
        exitSet.add(nodes[2]);
        globalSet.add(nodes[0]);
        globalSet.add(nodes[1]);
        globalSet.add(nodes[2]);

        // Contagem para entropia
        increment(guardCounts, nodes[0]);
        increment(middleCounts, nodes[1]);
        increment(exitCounts, nodes[2]);
        increment(globalCounts, nodes[0]);
        increment(globalCounts, nodes[1]);
        increment(globalCounts, nodes[2]);

        // Bandwidth mínimo
        bandwidths.add(calculateMinBandwidth(nodes[0], nodes[1], nodes[2]));
    }

    public static void increment(Map<Node,Integer> map, Node node) {
        map.put(node, map.getOrDefault(node,0)+1);
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
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        double sum = 0;
        for (int bw : bandwidths) {
            sum += bw;
            if (bw < min) min = bw;
            if (bw > max) max = bw;
        }
        double avg = sum / bandwidths.size();
        return String.format("min=%d, max=%d, avg=%.2f", min, max, avg);
    }

    public static void exportBandwidthCSV(String filename, List<Integer> oldBW, List<Integer> newBW) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Circuit,Original,GeoAware\n");
            int size = Math.min(oldBW.size(), newBW.size());
            for (int i = 0; i < size; i++) {
                writer.write(String.format("%d,%d,%d\n", i+1, oldBW.get(i), newBW.get(i)));
            }
            System.out.println("Bandwidth CSV exported to: " + filename);
        } catch (IOException e) {
            System.err.println("Error writing CSV: " + e.getMessage());
        }
    }

    // ================= Algoritmos =================
    public static Circuit selectPathCurrent(PathSelector selector) {
        Node[] path = selector.selectPath();
        return new Circuit(0, path, calculateMinBandwidth(path[0], path[1], path[2]));
    }

    public static Circuit selectPathNew(PathSelector selector, double alpha, double beta) {
        Node[] path = selector.selectPathGeo(alpha, beta);
        return new Circuit(0, path, calculateMinBandwidth(path[0], path[1], path[2]));
    }
}
