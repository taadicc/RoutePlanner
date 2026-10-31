package net.unibl.etf.pj2.transport.util;

import net.unibl.etf.pj2.transport.data.City;
import net.unibl.etf.pj2.transport.data.Polazak;
import net.unibl.etf.pj2.transport.data.TransportDataService;
import net.unibl.etf.pj2.transport.routing.Route;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

/**
 * Pomocna klasa za izgradnju i vizualizaciju transportne mreze pomocu biblioteke GraphStream
 *
 * Ova klasa preuzima podatke o gradovima i polascima i na osnovu njih kreira graf
 *
 */


public class GraphBuilder {

    /**
     * Konstruktor za klasu GraphBuilder.
     */
    public GraphBuilder() {
    }
    /**
     * Gradovi se predstavljaju kao cvorovi, a veze izmedju njih su polasci (kao grane)
     * @param rows Broj redova u matrici gradova
     * @param cols Broj kolona u matrici gradova
     * @param dataService Servis koji pruza podatke o gradovima i polascima
     * @return Objekat {@link Graph} koji predstavlja izgradjenu transportnu mrezu
     */

    public static Graph build(int rows, int cols, TransportDataService dataService) {
        Graph graph = new SingleGraph("Transportna mreža");
        String css = ""
                + "node { size: 6px; fill-color: #3498db; } "
                + "node.start { size: 12px; fill-color: #e74c3c; stroke-mode: plain; stroke-width: 2px; } "
                + "node.end { size: 12px; fill-color: #2ecc71; stroke-mode: plain; stroke-width: 2px; } "
                + "node.station { size: 8px; fill-color: #f39c12; stroke-mode: plain; stroke-width: 2px; } "
                + "edge { size: 1px; fill-color: #bdc3c7; } "
                + "edge.route { size: 2px; fill-color: #e91e63; stroke-mode: plain; stroke-color: #e91e63; } "
                + "edge.invisible { size: 0px; }";

        graph.setAttribute("ui.stylesheet", css);
        graph.setAttribute("ui.antialias");
        graph.setAttribute("ui.quality");

        double scaleX = 800.0 / (rows > 1 ? rows - 1 : 1);
        double scaleY = 600.0 / (cols > 1 ? cols - 1 : 1);

        for (City city : dataService.getAllCities()) {
            String cityId = city.getCityId();
            org.graphstream.graph.Node node = graph.addNode(cityId);
            double x = city.getX() * scaleX;
            double y = city.getY() * scaleY;
            node.setAttribute("xyz", x, y, 0);
        }

        for (Polazak polazak : dataService.getAllDepartures()) {
            String fromCityId = Route.mapStationToCity(polazak.getFromStationId());
            String toCityId = Route.mapStationToCity(polazak.getToStationId());

            if (fromCityId.equals(toCityId)) {
                continue;
            }

            String edgeId = fromCityId + "->" + toCityId;
            String reversedEdgeId = toCityId + "->" + fromCityId;

            if (graph.getEdge(edgeId) == null && graph.getEdge(reversedEdgeId) == null) {
                graph.addEdge(edgeId, fromCityId, toCityId);
            }
        }

        return graph;
    }
}