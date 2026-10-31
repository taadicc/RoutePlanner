package net.unibl.etf.pj2.transport.routing;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Predstavlja kompletnu rutu izmedju dva grada
 * Sastoji se od niza segmenata (putovanja), te sadrži informacije o ukupnom vremenu, cijeni, presjedanjima i putanji
 */
public class Route {
    private final List<RouteSegment> segments;
    private final String startCityId;
    private final String endCityId;

    /**
     * Konstruktor za kreiranje objekta rute
     *
     * @param startCityId ID polaznog grada
     * @param endCityId   ID odredišnog grada
     */
    public Route(String startCityId, String endCityId) {
        this.startCityId = startCityId;
        this.endCityId = endCityId;
        this.segments = new LinkedList<>();
    }

    /**
     * Dodaje novi segment putovanja u rutu
     *
     * @param segment Segment rute koji se dodaje
     */
    public void addSegment(RouteSegment segment) {
        this.segments.add(segment);
    }

    /**
     * Vraća listu segmenata koji cine rutu
     *
     * @return Lista objekata {@link RouteSegment}.
     */
    public List<RouteSegment> getSegments() {
        return segments;
    }

    /**
     * Izracunava i vraca ukupno trajanje rute u minutama, ukljucujuci i vrijeme cekanja.
     *
     * @return Ukupno trajanje rute u minutama
     */
    public int getTotalTimeMinutes() {
        if (segments.isEmpty()) return 0;
        return (int) segments.stream().mapToLong(RouteSegment::getTotalTimeMinutes).sum();
    }

    /**
     * Izracunava i vraca ukupnu cijenu cijele rute
     *
     * @return Ukupna cijena rute
     */
    public int getTotalPrice() {
        return (int) segments.stream().mapToDouble(s -> s.getPolazak().getPrice()).sum();
    }

    /**
     * Izracunava i vraca broj presjedanja na ruti
     *
     * @return Broj presjedanja
     */
    public int getTransferCount() {
        return segments.size() - 1;
    }

    /**
     * Vraća ID polaznog grada
     *
     * @return ID polaznog grada
     */
    public String getStartCityId() {
        return startCityId;
    }

    /**
     * Vraća ID odredisnog grada
     *
     * @return ID odredisnog grada
     */
    public String getEndCityId() {
        return endCityId;
    }

    /**
     * Vraca listu ID-jeva gradova kroz koje ruta prolazi
     *
     * @return Lista stringova sa ID-jevima gradova
     */
    public List<String> getCityPath() {
        List<String> path = new LinkedList<>();
        if (!segments.isEmpty()) {
            path.add(mapStationToCity(segments.get(0).getPolazak().getFromStationId()));
            for (RouteSegment segment : segments) {
                path.add(mapStationToCity(segment.getPolazak().getToStationId()));
            }
        }
        return path;
    }

    /**
     * Vraća tekstualni prikaz putanje gradova
     *
     * @return String sa putanjom
     */
    public String getCityPathSignature() {
        return String.join("->", getCityPath());
    }

    /**
     * Statistička metoda za mapiranje ID-ja stanice u ID grada
     *
     * @param stationId ID stanice
     * @return ID grada
     */
    public static String mapStationToCity(String stationId) {
        if (stationId == null) return null;
        if (stationId.startsWith("A_") || stationId.startsWith("Z_")) {
            return "G_" + stationId.substring(2);
        }
        return stationId;
    }
}