package pt.unl.fct.pds.project2.utils;

import pt.unl.fct.pds.project2.model.Node;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import java.net.InetAddress;

public class ConsensusParser {

    private String filename;
    private static DatabaseReader geoIpReader;

    static {
        try {
            File database = new File(ConsensusParser.class.getClassLoader()
                    .getResource("GeoLite2-Country.mmdb").getFile());
            geoIpReader = new DatabaseReader.Builder(database).build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ConsensusParser() {}

    public ConsensusParser(String filename) { this.filename = filename; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Node[] parseConsensus() {
        ArrayList<Node> nodes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(this.filename))) {
            String line;
            Node currentNode = null;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("r ")) {
                    // Ex: r Nickname Fingerprint Digest PublicationTime IP ORPort DirPort
                    String[] parts = line.split(" ");
                    String nickname = parts[1];
                    String fingerprint = parts[2];
                    String pubTimeStr = parts[4];
                    LocalDateTime timePublished = LocalDateTime.parse(pubTimeStr, DateTimeFormatter.ISO_DATE_TIME);
                    String ip = parts[5];
                    int orPort = Integer.parseInt(parts[6]);
                    int dirPort = Integer.parseInt(parts[7]);

                    currentNode = new Node();
                    currentNode.setNickname(nickname);
                    currentNode.setFingerprint(fingerprint);
                    currentNode.setTimePublished(timePublished);
                    currentNode.setIpAddress(ip);
                    currentNode.setOrPort(orPort);
                    currentNode.setDirPort(dirPort);
                    nodes.add(currentNode);

                } else if (line.startsWith("s ") && currentNode != null) {
                    String[] flags = line.substring(2).split(" ");
                    currentNode.setFlags(flags);

                } else if (line.startsWith("v ") && currentNode != null) {
                    currentNode.setVersion(line.substring(2));

                } else if (line.startsWith("w ") && currentNode != null) {
                    // Ex: w Bandwidth=12345
                    String[] parts = line.split("=");
                    int bandwidth = Integer.parseInt(parts[1]);
                    currentNode.setBandwidth(bandwidth);

                } else if (line.startsWith("p ") && currentNode != null) {
                    currentNode.setExitPolicy(line.substring(2));
                }
            }

            // Preencher país via GeoIP
            for (Node node : nodes) {
                node.setCountry(getCountry(node.getIpAddress()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return nodes.toArray(new Node[0]);
    }

    private String getCountry(String ip) {
        try {
            InetAddress inet = InetAddress.getByName(ip);
            CountryResponse response = geoIpReader.country(inet);
            return response.getCountry().getIsoCode();
        } catch (IOException | GeoIp2Exception e) {
            e.printStackTrace();
        }
        return "Unknown";
    }
}
