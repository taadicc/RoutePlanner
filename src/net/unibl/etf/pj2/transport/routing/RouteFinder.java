package net.unibl.etf.pj2.transport.routing;

import net.unibl.etf.pj2.transport.data.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import static net.unibl.etf.pj2.transport.routing.Route.mapStationToCity;

/**
 * Sluzi za pronalazenje optimalnih ruta izmedju gradova
 * Koristi Dijkstrin algoritam za pretragu najkrace rute na osnovu zadatih kriterijuma
 */

public class RouteFinder {

    private final TransportDataService dataService;
    private final Map<String, List<Polazak>> departuresByStation;
    private final Map<String, String> stationToCityMap;
    //private static final LocalTime START_TIME = LocalTime.of(6, 0);
    private static final int MAX_WAIT_HOURS = 24;
    private static final int TRANSFER_PENALTY_MINUTES = 15;

    /**
     * Konstruktor za kreiranje pronalazaca rute
     *
     * @param dataService Servis koji pruza podatke o polascima
     */
    public RouteFinder(TransportDataService dataService) {
        this.dataService = dataService;
        this.departuresByStation = buildDepartureIndex();
        this.stationToCityMap = buildStationToCityMap();
        System.out.println("RouteFinder inicijalizovan sa " + departuresByStation.size() + " stanica");
    }

    /**
     * Racuna maksimalan dozvoljen broj presjedanja na osnovu geografske udaljenosti gradova
     *
     * @param startCityId ID polaznog grada
     * @param endCityId ID odredisnog grada
     * @return Maksimalan broj presjedanja
     */
    private int calcMaxTransfersByCities(String startCityId, String endCityId) {
        City s = dataService.getCityById(startCityId);
        City t = dataService.getCityById(endCityId);
        if (s == null || t == null) return 20;
        int md = Math.abs(s.getX() - t.getX()) + Math.abs(s.getY() - t.getY());
        return Math.min(30, md + 5);
    }

    /**
     * Izgrađuje indeks polazaka, mapirajuci ID stanice na listu polazaka iz te stanice.
     *
     * @return Mapa sa polascima indeksiranim po polaznoj stanici
     */
    private Map<String, List<Polazak>> buildDepartureIndex() {
        Map<String, List<Polazak>> index = new HashMap<>();
        for (Polazak dep : dataService.getAllDepartures()) {
            index.computeIfAbsent(dep.getFromStationId(), k -> new ArrayList<>()).add(dep);
        }
        for (List<Polazak> deps : index.values()) {
            deps.sort(Comparator.comparing(Polazak::getDepartureTime));
        }
        return index;
    }


    /**
     * Izgrađuje mapu koja povezuje ID stanice sa ID-jem grada
     *
     * @return Mapa sa ID-jevima stanica kao kljucevima i ID-jevima gradova kao vrijednostima
     */
    private Map<String, String> buildStationToCityMap() {
        Map<String, String> map = new HashMap<>();
        for (City city : dataService.getAllCities()) {
            map.put(city.getBusStationId(), city.getCityId());
            map.put(city.getTrainStationId(), city.getCityId());
        }
        return map;
    }

    /**
     * Pronalazi najbolju rutu po zadatom kriterijumu
     *
     * @param startCityId ID polaznog grada
     * @param endCityId ID odredisnog grada
     * @param criteria Kriterijum optimizacije (VRIJEME, CIJENA, PRESJEDANJA)
     * @return Najbolja ruta ili null ako ruta ne postoji
     */
    public Route findOptimalRoute(String startCityId, String endCityId, OptimizationCriteria criteria) {
        List<Route> allRoutes = findTop5Routes(startCityId, endCityId, criteria);
        if (allRoutes.isEmpty()) return null;

        return allRoutes.stream()
                .min(getComparator(criteria))
                .orElse(null);
    }

    /**
     * Pronalazi do 5 najboljih ruta po zadatom kriterijumu
     *
     * @param startCityId ID polaznog grada
     * @param endCityId ID odredisnog grada
     * @param criteria Kriterijum optimizacije (VRIJEME, CIJENA, PRESJEDANJA)
     * @return Lista najboljih ruta
     */
    public List<Route> findTop5Routes(String startCityId, String endCityId, OptimizationCriteria criteria) {
        List<Route> topRoutes = new ArrayList<>();
        Map<String, List<Polazak>> tempDeparturesByStation = new HashMap<>();
        for (Map.Entry<String, List<Polazak>> entry : departuresByStation.entrySet()) {
            tempDeparturesByStation.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        for (int i = 0; i < 5; i++) {
            List<Route> candidates = findAllPossibleRoutes(startCityId, endCityId, criteria, tempDeparturesByStation);

            if (candidates.isEmpty()) break;

            Route bestRoute = candidates.stream().min(getComparator(criteria)).orElse(null);
            if (bestRoute == null) break;

            String signature = bestRoute.getCityPathSignature();
            boolean isUnique = topRoutes.stream().noneMatch(r -> r.getCityPathSignature().equals(signature));

            if (isUnique) {
                topRoutes.add(bestRoute);
            }

            for (RouteSegment segment : bestRoute.getSegments()) {
                String fromStationId = segment.getPolazak().getFromStationId();
                if (tempDeparturesByStation.containsKey(fromStationId)) {
                    tempDeparturesByStation.get(fromStationId).remove(segment.getPolazak());
                }
            }
        }

        return topRoutes.stream()
                .sorted(getComparator(criteria))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Pronalazi sve moguce rute između polaznog i odredisnog grada
     *
     * @param startCityId ID polaznog grada.
     * @param endCityId ID odredisnog grada.
     * @param criteria Kriterijum optimizacije
     * @param localDepartures Lokalna mapa polazaka za pretragu
     * @return Lista pronađenih ruta
     */
    private List<Route> findAllPossibleRoutes(String startCityId, String endCityId, OptimizationCriteria criteria, Map<String, List<Polazak>> localDepartures) {
        City startCity = dataService.getCityById(startCityId);
        City endCity = dataService.getCityById(endCityId);
        if (startCity == null || endCity == null) {
            System.out.println("Nepostojeći gradovi: " + startCityId + " ili " + endCityId);
            return new ArrayList<>();
        }

        List<Route> foundRoutes = new ArrayList<>();
        int maxTransfersAllowed = calcMaxTransfersByCities(startCityId, endCityId);

        Set<String> targetStations = new HashSet<>();
        targetStations.add(endCity.getBusStationId());
        targetStations.add(endCity.getTrainStationId());

        searchFromStation(startCity.getBusStationId(), targetStations, startCityId, endCityId, foundRoutes, criteria, maxTransfersAllowed, localDepartures);
        searchFromStation(startCity.getTrainStationId(), targetStations, startCityId, endCityId, foundRoutes, criteria, maxTransfersAllowed, localDepartures);

        return foundRoutes;
    }

    /**
     * Izvodi pretragu rute od zadate stanice koristeci Dijkstrin algoritam
     *
     * @param startStation Polazna stanica
     * @param targetStations Skup ciljnih stanica
     * @param startCityId ID polaznog grada
     * @param endCityId ID odredisnog grada
     * @param foundRoutes Lista u koju se dodaju pronađene rute
     * @param criteria Kriterijum optimizacije
     * @param maxTransfersAllowed Maksimalan broj dozvoljenih presjedanja
     * @param localDepartures Lokalna mapa polazaka
     */
    private void searchFromStation(String startStation, Set<String> targetStations, String startCityId, String endCityId, List<Route> foundRoutes, OptimizationCriteria criteria, int maxTransfersAllowed, Map<String, List<Polazak>> localDepartures) {
        PriorityQueue<SearchState> queue = new PriorityQueue<>(Comparator.comparingInt(s -> s.getEstimatedCost(criteria)));
        Map<String, Integer> bestCostToStation = new HashMap<>();
        //SearchState initial = new SearchState(startStation, LocalDateTime.now().toLocalDate().atTime(START_TIME), 0, new Route(startCityId, endCityId));
        SearchState initial = new SearchState(startStation, LocalDateTime.now(), 0, new Route(startCityId, endCityId));
        queue.offer(initial);
        bestCostToStation.put(startStation, 0);
        while (!queue.isEmpty()) {
            SearchState current = queue.poll();
            if (current.getEstimatedCost(criteria) > bestCostToStation.getOrDefault(current.currentStation, Integer.MAX_VALUE)) {
                continue;
            }

            if (current.route.getTransferCount() > maxTransfersAllowed) {
                continue;
            }

            if (targetStations.contains(current.currentStation) && !current.route.getSegments().isEmpty()) {

                foundRoutes.add(cloneRoute(current.route));
                continue;
            }
            expandState(current, queue, criteria, bestCostToStation, localDepartures);
        }
    }

    /**
     * Prosiruje trenutno stanje pretrage, dodajuci nove moguce polaske u red prioriteta
     *
     * @param current Trenutno stanje pretrage
     * @param queue Red prioriteta za pretragu
     * @param criteria Kriterijum optimizacije
     * @param bestCostToStation Mapa najboljih troskova do svake stanice
     * @param localDepartures Lokalna mapa polazaka
     */
    private void expandState(SearchState current, PriorityQueue<SearchState> queue, OptimizationCriteria criteria, Map<String, Integer> bestCostToStation, Map<String, List<Polazak>> localDepartures) {
        List<Polazak> departures = localDepartures.getOrDefault(current.currentStation, Collections.emptyList());
        for (Polazak polazak : departures) {
            Duration waitTime = calculateWaitTime(current.currentTime, polazak.getDepartureTime());
            if (waitTime.toMinutes() > MAX_WAIT_HOURS * 60) {
                continue;
            }
            LocalDateTime nextAvailableTime = current.currentTime.plus(waitTime).plus(polazak.getDuration());
            Route newRoute = cloneRoute(current.route);
            newRoute.addSegment(new RouteSegment(polazak, waitTime));
            int newElapsedTime = current.elapsedMinutes + (int) waitTime.toMinutes() + (int) polazak.getDuration().toMinutes();
            int newCost = 0;
            switch (criteria) {
                case PRICE:
                    newCost = newRoute.getTotalPrice();
                    break;
                case TRANSFERS:
                    newCost = newRoute.getTransferCount();
                    break;
                case TIME:
                default:
                    newCost = newElapsedTime;
                    break;
            }

            String nextStationId = polazak.getToStationId();
            if (newCost < bestCostToStation.getOrDefault(nextStationId, Integer.MAX_VALUE)) {
                bestCostToStation.put(nextStationId, newCost);
                SearchState newState = new SearchState(nextStationId, nextAvailableTime, newElapsedTime, newRoute);
                queue.offer(newState);
            }
        }
        String currentCity = stationToCityMap.get(current.currentStation);
        if (currentCity != null) {
            City city = dataService.getCityById(currentCity);
            if (city != null) {
                String alternativeStation = null;
                if (current.currentStation.equals(city.getBusStationId())) {
                    alternativeStation = city.getTrainStationId();
                } else if (current.currentStation.equals(city.getTrainStationId())) {
                    alternativeStation = city.getBusStationId();
                }
                if (alternativeStation != null) {
                    LocalDateTime newTime = current.currentTime.plusMinutes(TRANSFER_PENALTY_MINUTES);
                    int newElapsedTime = current.elapsedMinutes + TRANSFER_PENALTY_MINUTES;
                    int newCost = 0;
                    switch (criteria) {
                        case PRICE:
                            newCost = current.route.getTotalPrice();
                            break;
                        case TRANSFERS:
                            newCost = current.route.getTransferCount() + 1;
                            break;
                        case TIME:
                        default:
                            newCost = newElapsedTime;
                            break;
                    }
                    if (newCost < bestCostToStation.getOrDefault(alternativeStation, Integer.MAX_VALUE)) {
                        bestCostToStation.put(alternativeStation, newCost);
                        Route newRoute = cloneRoute(current.route);
                        SearchState transferState = new SearchState(alternativeStation, newTime, newElapsedTime, newRoute);
                        queue.offer(transferState);
                    }
                }
            }
        }
    }

    /**
     * Izracunava vrijeme cekanja izmedju dolaska i polaska
     *
     * @param currentTime Trenutno vrijeme dolaska na stanicu
     * @param departureTime Vrijeme polaska sa te stanice
     * @return Trajanje cekanja kao objekat {@link Duration}
     */
    private Duration calculateWaitTime(LocalDateTime currentTime, LocalTime departureTime) {
        LocalDateTime departureDateTime = currentTime.toLocalDate().atTime(departureTime);
        if (departureDateTime.isBefore(currentTime)) {
            departureDateTime = departureDateTime.plusDays(1);
        }
        return Duration.between(currentTime, departureDateTime);
    }

    /**
     * Pomocna klasa za reprezentaciju stanja tokom pretrage rute
     */
    private static class SearchState {
        final String currentStation;
        final LocalDateTime currentTime;
        final int elapsedMinutes;
        final Route route;
        /**
         * Konstruktor za stanje pretrage
         *
         * @param station ID trenutne stanice
         * @param time Trenutno vrijeme
         * @param elapsed Protekle minute od pocetka
         * @param route Trenutna ruta
         */

        SearchState(String station, LocalDateTime time, int elapsed, Route route) {
            this.currentStation = station;
            this.currentTime = time;
            this.elapsedMinutes = elapsed;
            this.route = route;
        }

        /**
         * Izracunava procijenjeni trosak do trenutnog stanja na osnovu kriterijuma
         *
         * @param criteria Kriterijum optimizacije
         * @return Procijenjeni trosak
         */
        int getEstimatedCost(OptimizationCriteria criteria) {
            switch (criteria) {
                case PRICE:
                    return route.getTotalPrice();
                case TRANSFERS:
                    return route.getTransferCount();
                case TIME:
                default:
                    return elapsedMinutes;
            }
        }
    }

    /**
     * Kreira duboku kopiju rute
     *
     * @param original Originalna ruta
     * @return Kopija rute
     */

    private Route cloneRoute(Route original) {
        Route clone = new Route(original.getStartCityId(), original.getEndCityId());
        for (RouteSegment segment : original.getSegments()) {
            clone.addSegment(new RouteSegment(segment.getPolazak(), segment.getWaitTime()));
        }
        return clone;
    }

    /**
     * Vraca komparator za sortiranje ruta na osnovu kriterijuma optimizacije
     *
     * @param criteria Kriterijum optimizacije
     * @return Objekat {@link Comparator}
     */
    private Comparator<Route> getComparator(OptimizationCriteria criteria) {
        switch (criteria) {
            case PRICE:
                return Comparator.comparingInt(Route::getTotalPrice).thenComparingInt(Route::getTotalTimeMinutes).thenComparingInt(Route::getTransferCount);
            case TRANSFERS:
                return Comparator.comparingInt(Route::getTransferCount).thenComparingInt(Route::getTotalTimeMinutes).thenComparingInt(Route::getTotalPrice);
            case TIME:
            default:
                return Comparator.comparingInt(Route::getTotalTimeMinutes).thenComparingInt(Route::getTransferCount).thenComparingInt(Route::getTotalPrice);
        }
    }
}