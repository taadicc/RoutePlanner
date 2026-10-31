package net.unibl.etf.pj2.transport.data;

import com.google.gson.*;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;

/**
 * Servis za ucitavanje i upravljanje podacima o gradovima i polascima iz JSON fajla
 * Sluzi kao fasada za pristup podacima o transportnoj mrezi
 */
public class TransportDataService {
    private final List<City> cities = new ArrayList<>();
    private final Map<String, City> cityMap = new HashMap<>(); // "G_x_y" -> City
    private final List<Polazak> departures = new ArrayList<>();

    /**
     * Konstruktor koji ucitava podatke iz JSON fajla
     *
     * @param jsonFile Putanja do JSON fajla sa podacima o transportu
     */
    public TransportDataService(String jsonFile) {
        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            // parsiranje stanica
            JsonArray stations = root.getAsJsonArray("stations");
            if (stations != null) {
                for (JsonElement el : stations) {
                    JsonObject s = el.getAsJsonObject();
                    String cityId = s.get("city").getAsString();
                    String busId  = s.get("busStation").getAsString();
                    String trainId= s.get("trainStation").getAsString();

                    String[] parts = cityId.split("_");
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);

                    City city = new City(x, y, busId, trainId, cityId);
                    cities.add(city);
                    cityMap.put(cityId, city);
                }
            }

            // parsiranje polazaka
            JsonArray depArray = root.getAsJsonArray("departures");
            if (depArray != null) {
                for (JsonElement el : depArray) {
                    JsonObject d = el.getAsJsonObject();
                    String type = d.get("type").getAsString();
                    String from = d.get("from").getAsString();
                    String to   = d.get("to").getAsString();
                    LocalTime time = LocalTime.parse(d.get("departureTime").getAsString());
                    int duration = d.get("duration").getAsInt();
                    int price = d.get("price").getAsInt();
                    int minTransfer = d.get("minTransferTime").getAsInt();

                    LocalTime arrivalTime = time.plusMinutes(duration);
                    if ("autobus".equals(type)) {
                        departures.add(new AutobuskiPolazak(from, to, time, arrivalTime, price, minTransfer));
                    } else if ("voz".equals(type)) {
                        departures.add(new ZeljeznickiPolazak(from, to, time, arrivalTime, price, minTransfer));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Pronalazi objekat City na osnovu ID-ja stanice
     *
     * @param stationId ID stanice (npr. A_0_0 ili Z_0_0)
     * @return Objekat {@link City} ili null ako nije pronadjen
     */
    public City getCityByStation(String stationId) {
        if (stationId == null) return null;
        if (stationId.startsWith("A_") || stationId.startsWith("Z_")) {
            String suffix = stationId.substring(2);
            String cityId = "G_" + suffix;
            return getCityById(cityId);
        }
        if (stationId.startsWith("G_")) {
            return getCityById(stationId);
        }
        return null;
    }

    /**
     * Vraca listu svih gradova
     *
     * @return Lista objekata {@link City}.
     */
    public List<City> getAllCities() { return cities; }

    /**
     * Vraca listu svih polazaka
     *
     * @return Lista objekata {@link Polazak}.
     */
    public List<Polazak> getAllDepartures() { return departures; }

    /**
     * Pronalazi objekat City na osnovu ID-ja grada
     *
     * @param cityId ID grada (npr. G_0_0)
     * @return Objekat {@link City} ili null ako nije pronadjen
     */
    public City getCityById(String cityId) { return cityMap.get(cityId); }
}