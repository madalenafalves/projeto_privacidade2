package pt.unl.fct.pds.project2.utils;

import pt.unl.fct.pds.project2.model.Node;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ConsensusParser {

    private String filename;

    public ConsensusParser() {}
    public ConsensusParser(String filename) { this.filename = filename; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Node[] parseConsensus() {

        if (filename == null) {
            System.err.println("ConsensusParser ERROR: filename is null");
            return new Node[0];
        }

        ArrayList<Node> nodes = new ArrayList<>();

        // Variáveis temporárias para um Node
        String nickname = null;
        String fingerprint = null;
        LocalDateTime timePublished = null;
        String ip = null;
        int orPort = 0;
        int dirPort = 0;
        String[] flags = null;
        String version = null;
        int bandwidth = 0;
        String exitPolicy = null;
        String country = null;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;

            while ((line = br.readLine()) != null) {

                // ► Nova entrada 'r'
                if (line.startsWith("r ")) {

                    // Se já temos um node a ser construído → Guardamos
                    if (nickname != null) {
                        nodes.add(new Node(
                                nickname,
                                fingerprint,
                                timePublished,
                                ip,
                                orPort,
                                dirPort,
                                flags != null ? flags : new String[0],
                                version,
                                bandwidth,
                                country,
                                exitPolicy
                        ));
                    }

                    // Reset para o novo Node
                    String[] parts = line.split(" ");

                    nickname = parts[1];
                    fingerprint = parts[2];

                    // Publication time está no formato YYYY-MM-DD HH:MM:SS
                    String dateTimeString = parts[4] + " " + parts[5];
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    timePublished = LocalDateTime.parse(dateTimeString, formatter);

                    ip = parts[6];
                    orPort = Integer.parseInt(parts[7]);
                    dirPort = Integer.parseInt(parts[8]);

                    // Ainda não conhecemos:
                    flags = null;
                    version = null;
                    bandwidth = 0;
                    exitPolicy = null;

                    // GeoIP
                    country = GeoIPUtils.getCountryFromIP(ip);
                }

                // ► Flags
                else if (line.startsWith("s ")) {
                    flags = line.substring(2).trim().split(" ");
                }

                // ► Version
                else if (line.startsWith("v ")) {
                    version = line.substring(2).trim();
                }

                // ► Bandwidth
                else if (line.startsWith("w ")) {
                    try {
                        String bw = line.split("Bandwidth=")[1];
                        bandwidth = Integer.parseInt(bw.split(" ")[0]);
                    } catch (Exception e) {
                        bandwidth = 0;
                    }
                }

                // ► Exit policy
                else if (line.startsWith("p ")) {
                    exitPolicy = line.substring(2).trim();
                }

                // ► Ignorar IPv6
                else if (line.startsWith("a ")) {
                    continue;
                }

                // Outras linhas ignoradas
            }

            // Quando o ficheiro acaba → guardar o último Node
            if (nickname != null) {
                nodes.add(new Node(
                        nickname,
                        fingerprint,
                        timePublished,
                        ip,
                        orPort,
                        dirPort,
                        flags != null ? flags : new String[0],
                        version,
                        bandwidth,
                        country,
                        exitPolicy
                ));
            }

        } catch (Exception e) {
            System.err.println("Error parsing consensus: " + e.getMessage());
            e.printStackTrace();
        }

        return nodes.toArray(new Node[0]);
    }
}
