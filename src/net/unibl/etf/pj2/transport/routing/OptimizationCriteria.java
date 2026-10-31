package net.unibl.etf.pj2.transport.routing;

/**
 * Kriterijumi optimizacije za pretragu rute.
 */
public enum OptimizationCriteria {
    /** Putanja se optimizuje prema najkraćem vremenu putovanja. */
    TIME,
    /** Putanja se optimizuje prema najnižoj cijeni. */
    PRICE,
    /** Putanja se optimizuje prema najmanjem broju presjedanja. */
    TRANSFERS
}