package net.unibl.etf.pj2.transport.data;


import java.util.Objects;

/**
 * Predstavlja grad u transportnoj mrezi
 * Sadrzi ID grada, koordinate (x,y) unutar mreze, te ID-eve autobuske stanice i zeljeznicke stanice
 */

public class City {
    private final int x;
    private final int y;
    private final String busStationId; // npr. A_0_0
    private final String trainStationId; // npr. Z_0_0
    private final String cityId; // npr. G_0_0

    /**
     * Konstruktor za kreiranje objekta grada
     *
     * @param x              X koordinata grada u mrezi
     * @param y              Y koordinata grada u mrezi
     * @param busStationId   ID autobuske stanice
     * @param trainStationId ID zeljeznicke stanice
     * @param cityId         Jedinstveni identifikator grada
     */
    public City(int x, int y, String busStationId, String trainStationId, String cityId) {
        this.x = x;
        this.y = y;
        this.busStationId = busStationId;
        this.trainStationId = trainStationId;
        this.cityId = cityId;
    }

    /**
     * Vraca X koordinatu grada
     *
     * @return X koordinata
     */
    public int getX() {
        return x;
    }

    /**
     * Vraca Y koordinatu grada
     *
     * @return Y koordinata
     */
    public int getY() {
        return y;
    }

    /**
     * Vraca ID autobuske stanice
     *
     * @return ID autobuske stanice
     */
    public String getBusStationId() {
        return busStationId;
    }

    /**
     * Vraca ID zeljeznicke stanice
     *
     * @return ID zeljeznicke stanice
     */
    public String getTrainStationId() {
        return trainStationId;
    }

    /**
     * Vraća jedinstveni ID grada
     *
     * @return ID grada
     */
    public String getCityId() {
        return cityId;
    }

    /**
     * Provjerava da li su dva objekta klase City jednaka na osnovu njihovih koordinata
     * @param o Objekat sa kojim se poredi
     * @return true ako su koordinate jednake, inace false
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof City)) return false;
        City city = (City) o;
        return x == city.x && y == city.y;
    }

    /**
     * Generise hash kod za objekat City na osnovu njegovih koordinata
     * @return Hash kod
     */

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}