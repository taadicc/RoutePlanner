package net.unibl.etf.pj2.transport.data;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;


/**
 * Apstraktna klasa koja predstavlja jedan polazak izmedju dvije stanice
 * Sadrzi osnovne podatke o polasku kao sto su polazna i odredisna stanica, cijena, trajanje, vrijeme polaska i dolaska, te minimalno vrijeme cekanja
 * Predstavlja osnovu za konkretne klase prevoza - AutobuskiPolazak i ZeljeznickiPolazak
 */
public abstract class Polazak {
    protected String fromStationId;
    protected String toStationId;
    protected LocalTime departureTime;
    protected LocalTime arrivalTime;
    protected double price;
    protected long minWaitingTime;


    /**
     * Konstruktor za apstraktni polazak
     *
     * @param fromStationId  ID pocetne stanice
     * @param toStationId    ID odredisne stanice
     * @param departureTime  Vrijeme polaska
     * @param arrivalTime    Vrijeme dolaska
     * @param price          Cijena karte
     * @param minWaitingTime Minimalno vrijeme cekanja na presjedanju
     */
    public Polazak(String fromStationId, String toStationId, LocalTime departureTime, LocalTime arrivalTime, double price, long minWaitingTime) {
        this.fromStationId = fromStationId;
        this.toStationId = toStationId;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
        this.minWaitingTime = minWaitingTime;
    }

    /**
     * Apstraktna metoda koja vraca tip prevoza (autobus ili voz)
     *
     * @return String koji predstavlja tip prevoza
     */
    public abstract String getTransportType();

    /**
     * Vraca ID polazne stanice
     * @return ID polazne stanice
     */
    public String getFromStationId() {
        return fromStationId;
    }

    /**
     * Vraca ID odredisne stanice
     * @return ID odredisne stanice
     */
    public String getToStationId() {
        return toStationId;
    }

    /**
     * Vraca vrijeme polaska
     * @return Vrijeme polaska
     */
    public LocalTime getDepartureTime() {
        return departureTime;
    }

    /**
     * Vraca vrijeme dolaska
     * @return Vrijeme dolaska
     */
    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    /**
     * Vraca cijenu karte
     * @return Cijena karte
     */
    public double getPrice() {
        return price;
    }

    /**
     * Vraca minimalno vrijeme cekanja
     * @return Minimalno vrijeme cekanja u minutama
     */
    public long getMinWaitingTime() {
        return minWaitingTime;
    }

    /**
     * Izracunava i vraca trajanje putovanja
     * Uzima u obzir putovanja koja prelaze ponoc
     * @return Trajanje putovanja kao objekat {@link Duration}
     */

    public Duration getDuration() {
        if (arrivalTime == null || departureTime == null) return Duration.ZERO;
        Duration duration = Duration.between(departureTime, arrivalTime);
        if (duration.isNegative()) {
            return duration.plusDays(1);
        }
        return duration;
    }

    /**
     * Poredi dva objekta klase Polazak
     * Smatraju se jednakim ako imaju istu polaznu i odredisnu stanicu te isto vrijeme polaska
     * @param o Objekat sa kojim se poredi
     * @return true ako su objekti jednaki, inace false
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Polazak polazak = (Polazak) o;
        return Objects.equals(fromStationId, polazak.fromStationId) &&
                Objects.equals(toStationId, polazak.toStationId) &&
                Objects.equals(departureTime, polazak.departureTime);
    }

    /**
     * Generise hash kod za objekat Polazak na osnovu polazne stanice, odredisne stanice i vremena polaska
     * @return Hash kod
     */

    @Override
    public int hashCode() {
        return Objects.hash(fromStationId, toStationId, departureTime);
    }
}