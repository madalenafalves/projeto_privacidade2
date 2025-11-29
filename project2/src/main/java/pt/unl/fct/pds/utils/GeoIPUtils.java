package pt.unl.fct.pds.project2.utils;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;

import java.io.File;
import java.net.InetAddress;

public class GeoIPUtils {

    private static DatabaseReader reader;

    static {
        try {
            File database = new File("src/main/resources/GeoLite2-Country.mmdb");
            reader = new DatabaseReader.Builder(database).build();
        } catch (Exception e) {
            System.err.println("GeoIP ERROR: Unable to load GeoLite2-Country.mmdb");
            reader = null;
        }
    }

    /**
     * Retorna o código do país (ISO2, ex: US, DE, PT)
     */
    public static String getCountryFromIP(String ip) {

        if (reader == null) return "??";

        try {
            InetAddress ipAddr = InetAddress.getByName(ip);
            CountryResponse response = reader.country(ipAddr);

            String country = response.getCountry().getIsoCode();
            return country != null ? country : "??";

        } catch (Exception e) {
            return "??";
        }
    }
}
