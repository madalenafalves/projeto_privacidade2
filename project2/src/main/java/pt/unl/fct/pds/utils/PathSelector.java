package pt.unl.fct.pds.project2.utils;

import pt.unl.fct.pds.project2.model.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PathSelector {

    private Node[] consensus;

    public PathSelector(Node[] consensus) {
        this.consensus = consensus;
    }

    /**
     * Função principal que seleciona o circuito completo
     */
    public Node[] selectPath() {
        Node exit = selectExit();
        Node guard = selectGuard(exit);
        Node middle = selectMiddle(guard, exit);
        return new Node[]{guard, middle, exit};
    }

    /**
     * Seleciona o Exit Node
     * Regras: flag Fast + exit policy adequada
     */
    private Node selectExit() {
        List<Node> candidates = new ArrayList<>();
        for (Node node : consensus) {
            if (hasFlag(node, "Fast") && suitableExit(node)) {
                candidates.add(node);
            }
        }
        return weightedSample(candidates);
    }

    /**
     * Seleciona o Guard Node
     * Regras: flag Guard, remover nodes da mesma família ou mesmo /16 subnet do Exit
     */
    private Node selectGuard(Node exit) {
        List<Node> candidates = new ArrayList<>();
        for (Node node : consensus) {
            if (hasFlag(node, "Guard") &&
                !sameSubnet16(node.getIpAddress(), exit.getIpAddress()) &&
                !sameFamily(node, exit)) {
                candidates.add(node);
            }
        }
        return weightedSample(candidates);
    }

    /**
     * Seleciona o Middle Node
     * Regras: flag Fast, remover nodes da mesma família ou mesmo /16 subnet do Guard ou Exit
     */
    private Node selectMiddle(Node guard, Node exit) {
        List<Node> candidates = new ArrayList<>();
        for (Node node : consensus) {
            if (hasFlag(node, "Fast") &&
                !sameSubnet16(node.getIpAddress(), guard.getIpAddress()) &&
                !sameSubnet16(node.getIpAddress(), exit.getIpAddress()) &&
                !sameFamily(node, guard) &&
                !sameFamily(node, exit)) {
                candidates.add(node);
            }
        }
        return weightedSample(candidates);
    }

    /** ---------------- Funções auxiliares ---------------- **/

    private boolean hasFlag(Node node, String flag) {
        if (node.getFlags() == null) return false;
        for (String f : node.getFlags()) {
            if (f.equals(flag)) return true;
        }
        return false;
    }

    private boolean suitableExit(Node node) {
        // Para simplificação, vamos assumir que todas as policies são adequadas
        return true;
    }

    private boolean sameFamily(Node a, Node b) {
        // Família = mesma primeira parte do fingerprint (exemplo simplificado: 8 primeiros chars)
        if (a.getFingerprint() == null || b.getFingerprint() == null) return false;
        return a.getFingerprint().substring(0, 8).equals(b.getFingerprint().substring(0, 8));
    }

    private boolean sameSubnet16(String ip1, String ip2) {
        // Apenas IPv4 por agora
        String[] parts1 = ip1.split("\\.");
        String[] parts2 = ip2.split("\\.");
        return parts1[0].equals(parts2[0]) && parts1[1].equals(parts2[1]);
    }

    private Node weightedSample(List<Node> candidates) {
        if (candidates.isEmpty()) return null;

        // Calcular soma das bandwidths
        int total = 0;
        for (Node node : candidates) {
            total += node.getBandwidth();
        }

        // Amostragem ponderada por Wi / Wt
        Random rand = new Random();
        int r = rand.nextInt(total);
        int cumulative = 0;
        for (Node node : candidates) {
            cumulative += node.getBandwidth();
            if (r < cumulative) return node;
        }

        // fallback
        return candidates.get(candidates.size() - 1);
    }
}
