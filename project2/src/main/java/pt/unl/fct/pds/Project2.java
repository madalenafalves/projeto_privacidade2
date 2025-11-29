package pt.unl.fct.pds;

import pt.unl.fct.pds.project2.model.Node;
import pt.unl.fct.pds.project2.model.Circuit;
import pt.unl.fct.pds.project2.utils.ConsensusParser;
import pt.unl.fct.pds.project2.utils.PathSelector;

/**
 * Application for Tor Path Selection alternatives.
 *
 */
public class Project2 
{
    public static void main(String[] args) 
    {
        System.out.println("Welcome to the Circuit Simulator!");

        // 1️⃣ Criar o parser e ler o ficheiro de consenso
        String consensusFile = "caminho/para/consensus.txt"; // substitui pelo caminho real
        ConsensusParser parser = new ConsensusParser(consensusFile);
        Node[] nodes = parser.parseConsensus();

        // 2️⃣ Criar o PathSelector com os nodes parseados
        PathSelector selector = new PathSelector(nodes);

        // 3️⃣ Selecionar um circuito completo (Guard, Middle, Exit)
        Node[] circuit = selector.selectPath();

        // 4️⃣ Mostrar os nodes selecionados
        if (circuit != null) {
            System.out.println("Guard: " + circuit[0].getNickname());
            System.out.println("Middle: " + circuit[1].getNickname());
            System.out.println("Exit: " + circuit[2].getNickname());
        } else {
            System.out.println("Não foi possível criar um circuito.");
        }
    }
}
