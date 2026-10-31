package net.unibl.etf.pj2.transport.data;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Klasa za generisanje podataka o transportnoj mrezi i polascima u JSON formatu
 * Modifikovana je da podrzava matricu gradova n x m
 */
public class TransportDataGenerator {
    private final int rows; // n
    private final int cols; // m
    private static final int DEPARTURES_PER_STATION = 5;
    private static final Random random = new Random();

    /**
     * Konstruktor za kreiranje generatora podataka
     *
     * @param rows Broj redova u mrezi gradova
     * @param cols Broj kolona u mrezi gradova
     */
    public TransportDataGenerator(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    /**
     * Prazna klasa za strukturu podataka o stanici za JSON serijalizaciju
     */
    public static class Station {
        public String city;        // G_x_y
        public String busStation;  // A_x_y
        public String trainStation;// Z_x_y
    }

    /**
     * Prazna klasa za strukturu podataka o polasku za JSON serijalizaciju
     */
    public static class DepartureJson {
        public String type;           // "autobus" ili "voz"
        public String from;           // A_x_y ili Z_x_y
        public String to;             // A_x_y ili Z_x_y
        public String departureTime;  // HH:mm
        public int duration;          // u minutama
        public int price;
        public int minTransferTime;   // u minutama
    }

    /**
     * Prazna klasa za strukturu podataka transporta za JSON serijalizaciju
     */
    public static class TransportData {
        public String[][] countryMap;
        public List<Station> stations;
        public List<DepartureJson> departures;
    }

    /**
     * Generise kompletne podatke o transportnoj mrezi
     *
     * @return Objekat {@link TransportData} sa generisanim podacima
     */
    public TransportData generateData() {
        TransportData data = new TransportData();
        data.countryMap = generateCountryMap();
        data.stations = generateStations();
        data.departures = generateDepartures(data.stations);
        return data;
    }

    /**
     * Generise i cuva podatke o transportnoj mrezi u JSON fajl
     *
     * @param rows     Broj redova (n)
     * @param cols     Broj kolona (m)
     * @param filename Naziv izlaznog JSON fajla
     */
    public static void generate(int rows, int cols, String filename) {
        TransportDataGenerator generator = new TransportDataGenerator(rows, cols);
        TransportData data = generator.generateData();
        generator.saveToJson(data, filename);
    }

    /**
     * Generise matricu sa ID-jevima gradova
     *
     * @return Dvostruki niz stringova sa ID-jevima gradova
     */
    private String[][] generateCountryMap() {
        String[][] countryMap = new String[rows][cols];
        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < cols; y++) {
                countryMap[x][y] = "G_" + x + "_" + y;
            }
        }
        return countryMap;
    }

    /**
     * Generise listu stanica sa njihovim ID-jevima
     *
     * @return Lista objekata {@link Station}
     */
    private List<Station> generateStations() {
        List<Station> stations = new ArrayList<>();
        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < cols; y++) {
                Station station = new Station();
                station.city = "G_" + x + "_" + y;
                station.busStation = "A_" + x + "_" + y;
                station.trainStation = "Z_" + x + "_" + y;
                stations.add(station);
            }
        }
        return stations;
    }

    /**
     * Generise listu polazaka na osnovu liste stanica
     *
     * @param stations Lista stanica
     * @return Lista objekata {@link DepartureJson}
     */
    private List<DepartureJson> generateDepartures(List<Station> stations) {
        List<DepartureJson> departures = new ArrayList<>();

        for (Station station : stations) {
            int x = Integer.parseInt(station.city.split("_")[1]);
            int y = Integer.parseInt(station.city.split("_")[2]);

            generateDeparturesForStation(departures, "autobus", station.busStation, x, y);
            generateDeparturesForStation(departures, "voz", station.trainStation, x, y);
        }

        System.out.println("Generisano ukupno polazaka: " + departures.size());
        return departures;
    }

    /**
     * Generise polaske za jednu stanicu
     *
     * @param departures    Lista polazaka u koju se dodaju novi polasci
     * @param type          Tip prevoza
     * @param fromStation   ID polazne stanice
     * @param x             X koordinata polazne stanice
     * @param y             Y koordinata polazne stanice
     */
    private void generateDeparturesForStation(List<DepartureJson> departures,
                                              String type, String fromStation, int x, int y) {
        List<int[]> neighbors = getNeighborCoords(x, y);

        if (neighbors.isEmpty()) {
            DepartureJson loopback = new DepartureJson();
            loopback.type = type;
            loopback.from = fromStation;
            String alternativePrefix = type.equals("autobus") ? "Z_" : "A_";
            loopback.to = alternativePrefix + x + "_" + y;
            loopback.departureTime = generateRandomTime();
            loopback.duration = 5 + random.nextInt(11);
            loopback.price = 50 + random.nextInt(51);
            loopback.minTransferTime = 5 + random.nextInt(16);
            departures.add(loopback);
            return;
        }

        for (int i = 0; i < DEPARTURES_PER_STATION; i++) {
            int[] targetCoords = neighbors.get(random.nextInt(neighbors.size()));
            DepartureJson departure = new DepartureJson();
            departure.type = type;
            departure.from = fromStation;
            String targetPrefix = random.nextBoolean() ? "A_" : "Z_";
            departure.to = targetPrefix + targetCoords[0] + "_" + targetCoords[1];
            departure.departureTime = generateRandomTime();
            departure.duration = generateTravelDuration(x, y, targetCoords[0], targetCoords[1]);
            departure.price = generatePrice(departure.duration, type);
            departure.minTransferTime = 5 + random.nextInt(21);
            departures.add(departure);
        }

        if (random.nextDouble() < 0.3) {
            generateLongDistanceDepartures(departures, type, fromStation, x, y);
        }
    }

    /**
     * Generise polaske ka udaljenijim gradovima
     *
     * @param departures    Lista polazaka u koju se dodaju novi polasci
     * @param type          Tip prevoza
     * @param fromStation   ID polazne stanice
     * @param x             X koordinata polazne stanice
     * @param y             Y koordinata polazne stanice
     */
    private void generateLongDistanceDepartures(List<DepartureJson> departures,
                                                String type, String fromStation, int x, int y) {
        int longDistanceDepartures = 1 + random.nextInt(2);

        for (int i = 0; i < longDistanceDepartures; i++) {
            int targetX, targetY;
            int attempts = 0;
            do {
                targetX = random.nextInt(rows);
                targetY = random.nextInt(cols);
                attempts++;
            } while ((targetX == x && targetY == y) && attempts < 10);

            if (attempts >= 10) continue;

            DepartureJson departure = new DepartureJson();
            departure.type = type;
            departure.from = fromStation;
            String targetPrefix = type.equals("autobus") ? "A_" : "Z_";
            departure.to = targetPrefix + targetX + "_" + targetY;
            departure.departureTime = generateRandomTime();
            departure.duration = generateTravelDuration(x, y, targetX, targetY);
            departure.price = generatePrice(departure.duration, type);
            departure.minTransferTime = 10 + random.nextInt(21);
            departures.add(departure);
        }
    }

    /**
     * Generise random vrijeme u formatu HH:mm
     *
     * @return Random vrijeme kao string
     */
    private String generateRandomTime() {
        int hour = random.nextInt(24);
        int minute = random.nextInt(4) * 15;
        return String.format("%02d:%02d", hour, minute);
    }

    /**
     * Generise trajanje putovanja na osnovu udaljenosti izmedju dva grada
     *
     * @param x1 X koordinata prvog grada
     * @param y1 Y koordinata prvog grada
     * @param x2 X koordinata drugog grada
     * @param y2 Y koordinata drugog grada
     * @return Trajanje putovanja u minutama
     */
    private int generateTravelDuration(int x1, int y1, int x2, int y2) {
        int distance = Math.abs(x1 - x2) + Math.abs(y1 - y2);
        int baseDuration = 20 + (distance * 15);
        int variation = random.nextInt(31) - 15;
        return Math.max(10, baseDuration + variation);
    }

    /**
     * Generise cijenu karte na osnovu trajanja i tipa prevoza
     *
     * @param duration Trajanje putovanja u minutama
     * @param type     Tip prevoza ("autobus" ili "voz")
     * @return Cijena karte
     */
    private int generatePrice(int duration, String type) {
        int basePrice = 100 + (duration * 2);
        if (type.equals("voz")) {
            basePrice = (int) (basePrice * 1.3);
        }
        double variation = 0.8 + (random.nextDouble() * 0.4);
        return Math.max(50, (int) (basePrice * variation));
    }

    /**
     * Pomocna metoda koja pronalazi koordinate susjednih gradova
     *
     * @param x X koordinata
     * @param y Y koordinata
     * @return Lista nizova sa koordinatama susjeda
     */
    private List<int[]> getNeighborCoords(int x, int y) {
        List<int[]> list = new ArrayList<>();
        int[][] directions = { {-1,0}, {1,0}, {0,-1}, {0,1} };
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (nx >= 0 && nx < rows && ny >= 0 && ny < cols) {
                list.add(new int[]{nx, ny});
            }
        }
        return list;
    }

    /**
     * Cuva generisane podatke u JSON fajl
     *
     * @param data     Objekat {@link TransportData} koji sadrzi podatke za cuvanje
     * @param filename Naziv fajla u koji se podaci cuvaju
     */
    public void saveToJson(TransportData data, String filename) {
        try (FileWriter file = new FileWriter(filename)) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"countryMap\": [\n");
            for (int i = 0; i < rows; i++) {
                json.append("    [");
                for (int j = 0; j < cols; j++) {
                    json.append("\"").append(data.countryMap[i][j]).append("\"");
                    if (j < cols - 1) json.append(", ");
                }
                json.append("]");
                if (i < rows - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ],\n");
            json.append("  \"stations\": [\n");
            for (int i = 0; i < data.stations.size(); i++) {
                Station s = data.stations.get(i);
                json.append("    {\"city\": \"").append(s.city)
                        .append("\", \"busStation\": \"").append(s.busStation)
                        .append("\", \"trainStation\": \"").append(s.trainStation)
                        .append("\"}");
                if (i < data.stations.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ],\n");
            json.append("  \"departures\": [\n");
            for (int i = 0; i < data.departures.size(); i++) {
                DepartureJson d = data.departures.get(i);
                json.append("    {\"type\": \"").append(d.type)
                        .append("\", \"from\": \"").append(d.from)
                        .append("\", \"to\": \"").append(d.to)
                        .append("\", \"departureTime\": \"").append(d.departureTime)
                        .append("\", \"duration\": ").append(d.duration)
                        .append(", \"price\": ").append(d.price)
                        .append(", \"minTransferTime\": ").append(d.minTransferTime)
                        .append("}");
                if (i < data.departures.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]\n");
            json.append("}");
            file.write(json.toString());
            System.out.println("Podaci sačuvani u " + filename);
        } catch (IOException e) {
            System.err.println("Greška pri čuvanju podataka: " + e.getMessage());
            e.printStackTrace();
        }
    }
}