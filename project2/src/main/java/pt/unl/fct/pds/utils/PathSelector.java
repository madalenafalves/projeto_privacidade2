package pt.unl.fct.pds.project2.utils;

import pt.unl.fct.pds.project2.model.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PathSelector {

    private List<Node> relays;
    private Random random = new Random();

    public PathSelector(List<Node> relays) {
        this.relays = relays;
    }

    
    private boolean hasFlag(Node n, String flag) {
        for (String f : n.getFlags()) {
            if (f.equalsIgnoreCase(flag)) return true;
        }
        return false;
    }

    // Exit policy simplificada: aceita qualquer um com ExitPolicy != "reject"
    private boolean canExit(Node n) {
        return !n.getExitPolicy().toLowerCase().contains("reject");
    }

    private boolean same16Subnet(Node a, Node b) {
        // compara os primeiros dois octetos
        try {
            String[] A = a.getIpAddress().split("\\.");
            String[] B = b.getIpAddress().split("\\.");
            return A[0].equals(B[0]) && A[1].equals(B[1]);
        } catch (Exception e) {
            return false;
        }
    }


    private List<Node> filterExit() {
        List<Node> list = new ArrayList<>();
        for (Node n : relays) {
            if (hasFlag(n, "Fast") && canExit(n)) {
                list.add(n);
            }
        }
        return list;
    }

    private List<Node> filterGuard(Node exit) {
        List<Node> list = new ArrayList<>();
        for (Node n : relays) {
            if (hasFlag(n, "Guard") && !same16Subnet(n, exit)) {
                list.add(n);
            }
        }
        return list;
    }

    private List<Node> filterMiddle(Node guard, Node exit) {
        List<Node> list = new ArrayList<>();
        for (Node n : relays) {
            if (hasFlag(n, "Fast") &&
                    !same16Subnet(n, guard) &&
                    !same16Subnet(n, exit)) {
                list.add(n);
            }
        }
        return list;
    }


    private Node weightedRandomBandwidth(List<Node> list) {
        double total = 0;
        for (Node n : list) total += n.getBandwidth();

        double r = random.nextDouble() * total;
        double accum = 0;

        for (Node n : list) {
            accum += n.getBandwidth();
            if (accum >= r) return n;
        }
        return list.get(list.size() - 1);
    }

    private Node weightedRandomTemp(List<Node> list) {
        double total = 0;
        for (Node n : list) {
            total += n.tempWeight;
        }

        double r = random.nextDouble() * total;
        double accum = 0;

        for (Node n : list) {
            accum += n.tempWeight;
            if (accum >= r) return n;
        }
        return list.get(list.size() - 1);
    }


    public Node selectExit() {
        List<Node> exits = filterExit();
        return weightedRandomBandwidth(exits);
    }

    public Node selectGuard(Node exit) {
        List<Node> guards = filterGuard(exit);
        return weightedRandomBandwidth(guards);
    }

    public Node selectMiddle(Node guard, Node exit) {
        List<Node> mids = filterMiddle(guard, exit);
        return weightedRandomBandwidth(mids);
    }

    public Node[] selectPath() {
        Node exit = selectExit();
        Node guard = selectGuard(exit);
        Node middle = selectMiddle(guard, exit);
        return new Node[]{guard, middle, exit};
    }


    public Node selectGuardGeo(Node exit, double alpha) {
        List<Node> guards = filterGuard(exit);

        for (Node g : guards) {
            double w = g.getBandwidth();

            // aplica α se for país diferente
            if (!g.getCountry().equalsIgnoreCase(exit.getCountry())) {
                w *= (1 + alpha);
            }

            g.tempWeight = w;
        }

        return weightedRandomTemp(guards);
    }

    public Node selectMiddleGeo(Node guard, Node exit, double beta) {
        List<Node> mids = filterMiddle(guard, exit);

        for (Node m : mids) {
            double w = m.getBandwidth();
            String c = m.getCountry();

            int shared = 0;
            if (c.equalsIgnoreCase(guard.getCountry())) shared++;
            if (c.equalsIgnoreCase(exit.getCountry())) shared++;

            int factor;

            if (shared == 0) factor = 3;      // país único
            else if (shared == 1) factor = 2; // partilha com 1
            else factor = 1;                  // partilha com 2

            w *= (1 + beta * factor);

            m.tempWeight = w;
        }

        return weightedRandomTemp(mids);
    }

    public Node[] selectPathGeo(double alpha, double beta) {
        Node exit = selectExit();
        Node guard = selectGuardGeo(exit, alpha);
        Node middle = selectMiddleGeo(guard, exit, beta);
        return new Node[]{guard, middle, exit};
    }
}
