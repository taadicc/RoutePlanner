package net.unibl.etf.pj2.transport.data;

import java.time.LocalTime;

/**
 * Konkretna klasa koja predstavlja polazak vozom
 * Nasljedjuje od apstraktne klase {@link Polazak}
 */
public class ZeljeznickiPolazak extends Polazak {
    /**
     * Konstruktor za polazak vozom
     *
     * @param fromStationId   ID polazne stanice
     * @param toStationId     ID odredisne stanice
     * @param departureTime   Vrijeme polaska
     * @param arrivalTime     Vrijeme dolaska
     * @param price           Cijena karte
     * @param minWaitingTime  Minimalno vrijeme cekanja na presjedanju
     */
    public ZeljeznickiPolazak(String fromStationId, String toStationId, LocalTime departureTime, LocalTime arrivalTime, double price, long minWaitingTime) {
        super(fromStationId, toStationId, departureTime, arrivalTime, price, minWaitingTime);
    }

    /**
     * Vraca tip prevoza, u ovom slucaju "voz"
     *
     * @return String "voz"
     */
    @Override
    public String getTransportType() {
        return "voz";
    }
}