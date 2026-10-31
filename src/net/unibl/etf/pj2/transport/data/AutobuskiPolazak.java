package net.unibl.etf.pj2.transport.data;

import java.time.LocalTime;

/**
 * Konkretna klasa koja predstavlja polazak autobusom
 * Nasljedjuje od apstraktne klase {@link Polazak}
 */
public class AutobuskiPolazak extends Polazak {

    /**
     * Konstruktor za polazak autobusom
     *
     * @param fromStationId  ID polazne stanice
     * @param toStationId    ID odredisne stanice
     * @param departureTime  Vrijeme polaska
     * @param arrivalTime    Vrijeme dolaska
     * @param price          Cijena karte
     * @param minWaitingTime Minimalno vrijeme cekanja na presjedanju
     */
    public AutobuskiPolazak(String fromStationId, String toStationId, LocalTime departureTime, LocalTime arrivalTime, double price, long minWaitingTime) {
        super(fromStationId, toStationId, departureTime, arrivalTime, price, minWaitingTime);
    }

    /**
     * Vraca tip prevoza, u ovom slucaju "autobus"
     *
     * @return String "autobus"
     */
    @Override
    public String getTransportType() {
        return "autobus";
    }
}