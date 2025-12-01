package pt.unl.fct.pds.project2.utils;

import pt.unl.fct.pds.project2.model.Node;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ConsensusParser {

    private String filename;
    private static DatabaseReader geoIpReader;

    // Inicializa o GeoIPReader de forma consistente
    static {
        try {
            File database = new File(ConsensusParser.class.getClassLoader()
                    .getResource("GeoLite2-Country.mmdb").getFile());
            geoIpReader = new DatabaseReader.Builder(database).build();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Fail loading GeoLite2-Country.mmdb");
        }
    }

    public ConsensusParser() {}

    public ConsensusParser(String filename) {
        this.filename = filename;
    }

    public String getFilename() { return filename; }

    public void setFilename(String filename) { this.filename = filename; }

    public Node[] parseConsensus() {
        ArrayList<Node> nodes = new ArrayList<>();
        Node currentNode = null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (BufferedReader br = new BufferedReader(new FileReader(this.filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("r ")) {
                    String[] parts = line.split(" ");
                    if (parts.length < 8) continue; // Ignorar linhas inválidas

                    String nickname = parts[1];
                    String fingerprint = parts[2];
                    String pubTimeStr = parts[4];
                    LocalDateTime timePublished;
                    try {
                        timePublished = LocalDateTime.parse(pubTimeStr, formatter);
                    } catch (Exception e) {
                        // fallback para ISO padrão
                        try {
                            timePublished = LocalDateTime.parse(pubTimeStr);
                        } catch (Exception ex) {
                            timePublished = LocalDateTime.now();
                        }
                    }

                    String ip = parts[5];
                    int orPort = safeParseInt(parts[6]);
                    int dirPort = safeParseInt(parts[7]);

                    currentNode = new Node();
                    currentNode.setNickname(nickname);
                    currentNode.setFingerprint(fingerprint);
                    currentNode.setTimePublished(timePublished);
                    currentNode.setIpAddress(ip);
                    currentNode.setOrPort(orPort);
                    currentNode.setDirPort(dirPort);
                    nodes.add(currentNode);

                } else if (line.startsWith("s ") && currentNode != null) {
                    String[] flags = line.substring(2).trim().split(" ");
                    currentNode.setFlags(flags);

                } else if (line.startsWith("v ") && currentNode != null) {
                    currentNode.setVersion(line.substring(2).trim());

                } else if (line.startsWith("w ") && currentNode != null) {
                    try {
                        String[] parts = line.split("=");
                        currentNode.setBandwidth(safeParseInt(parts[1]));
                    } catch (Exception e) {
                        currentNode.setBandwidth(0);
                    }

                } else if (line.startsWith("p ") && currentNode != null) {
                    currentNode.setExitPolicy(line.substring(2).trim());
                }
            }

            // Preencher country via GeoIP
            for (Node node : nodes) {
                node.setCountry(getCountry(node.getIpAddress()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return nodes.toArray(new Node[0]);
    }

    private int safeParseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getCountry(String ip) {
        try {
            InetAddress inet = InetAddress.getByName(ip);
            CountryResponse response = geoIpReader.country(inet);
            if (response.getCountry() != null && response.getCountry().getIsoCode() != null) {
                return response.getCountry().getIsoCode();
            }
        } catch (IOException | GeoIp2Exception e) {
            // Ignorar e retornar "Unknown"
        }
        return "Unknown";
    }
}
