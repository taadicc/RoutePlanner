package net.unibl.etf.pj2.transport.routing;

import net.unibl.etf.pj2.transport.data.Polazak;

import java.time.Duration;

/**
 * Predstavlja jedan segment putovanja, odnosno jedan polazak unutar rute
 * Sadrzi objekat polaska i informacije o vremenu cekanja na stanici
 */
public class RouteSegment {
    private final Polazak polazak;
    private final Duration waitTime; // trajanje cekanja prije polaska

    /**
     * Konstruktor za kreiranje segmenta rute
     *
     * @param polazak  Objekat polaska (AutobuskiPolazak ili ZeljeznickiPolazak)
     * @param waitTime Vrijeme cekanja prije ovog polaska
     */
    public RouteSegment(Polazak polazak, Duration waitTime) {
        this.polazak = polazak;
        this.waitTime = waitTime == null ? Duration.ZERO : waitTime;
    }

    /**
     * Vraca objekat polaska
     *
     * @return Objekat {@link Polazak}
     */
    public Polazak getPolazak() {
        return polazak;
    }

    /**
     * Vraca vrijeme cekanja prije ovog segmenta
     *
     * @return Vrijeme cekanja kao objekat {@link Duration}
     */
    public Duration getWaitTime() {
        return waitTime;
    }

    /**
     * Izracunava i vraca ukupno trajanje segmenta (putovanje + cekanje) u minutama
     *
     * @return Ukupno trajanje segmenta u minutama
     */
    public int getTotalTimeMinutes() {
        return (int) waitTime.toMinutes() + (int) polazak.getDuration().toMinutes();
    }

    /**
     * Generise string reprezentaciju segmenta rute
     *
     * @return Tekstualni prikaz segmenta
     */
    @Override
    public String toString() {
        return String.format("%s %s->%s @ %s (trajanje %d min, čekanje %d min)",
                polazak.getTransportType(),
                polazak.getFromStationId(),
                polazak.getToStationId(),
                polazak.getDepartureTime(),
                polazak.getDuration().toMinutes(),
                waitTime.toMinutes());
    }
}