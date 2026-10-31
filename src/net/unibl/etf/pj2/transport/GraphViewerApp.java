package net.unibl.etf.pj2.transport;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.unibl.etf.pj2.transport.data.City;
import net.unibl.etf.pj2.transport.data.TransportDataGenerator;
import net.unibl.etf.pj2.transport.data.TransportDataService;
import net.unibl.etf.pj2.transport.routing.OptimizationCriteria;
import net.unibl.etf.pj2.transport.routing.Route;
import net.unibl.etf.pj2.transport.routing.RouteFinder;
import net.unibl.etf.pj2.transport.routing.RouteSegment;
import net.unibl.etf.pj2.transport.util.GraphBuilder;
import net.unibl.etf.pj2.transport.util.RouteDialogManager;
import net.unibl.etf.pj2.transport.util.TicketService;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import java.util.List;

/**
 * Glavna klasa aplikacije koja pokrece graficki korisnicki interfejs za vizualizaciju transportne mreze i ruta
 * Omogucava odabir polazista i odredista, pretragu rute po razlicitim kriterijumima i vizualizaciju rezultata na grafu
 */
public class GraphViewerApp extends Application {
        private int rows;
        private int cols;

        private String selectedStartCity = null;
        private String selectedEndCity = null;

        private ComboBox<String> startComboBox;
        private ComboBox<String> endComboBox;

        private Label totalTicketsLabel;
        private Label totalPriceLabel;
        private Graph graph;
        private RouteFinder routeFinder;
        private TransportDataService dataService;
        private FxViewer viewer;

        /**
         * Glavni konstruktor za klasu GraphViewerApp.
         */
        public GraphViewerApp() {
        }
        /**
         * Pocetna metoda za JavaFX aplikaciju
         * Prikazuje dijalog za unos dimenzija mreze i pokrece glavni prozor
         * @param primaryStage Glavni prozor aplikacije
         */
        @Override
        public void start(Stage primaryStage) {
                if (!showDimensionDialog()) {
                        System.exit(0);
                        return;
                }
                generateTransportData();
                showTransportNetwork(primaryStage);
        }

        /**
         * Prikazuje prozor za unos dimenzija transportne mreze
         * @return true ako korisnik potvrdi unos, false ako ga otkaze
         */
        private boolean showDimensionDialog() {
                Stage dialog = new Stage();
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.setTitle("Dimenzije transportne mreže");
                dialog.setResizable(false);

                VBox layout = new VBox(15);
                layout.setPadding(new Insets(20));
                layout.setAlignment(Pos.CENTER);

                Label titleLabel = new Label("Unesite dimenzije transportne mreže");
                titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

                HBox rowBox = new HBox(10);
                rowBox.setAlignment(Pos.CENTER);
                Label rowLabel = new Label("Broj redova (n):");
                Spinner<Integer> rowSpinner = new Spinner<>(2, 200, 10);
                rowSpinner.setEditable(true);
                rowBox.getChildren().addAll(rowLabel, rowSpinner);

                HBox colBox = new HBox(10);
                colBox.setAlignment(Pos.CENTER);
                Label colLabel = new Label("Broj kolona (m):");
                Spinner<Integer> colSpinner = new Spinner<>(2, 200, 10);
                colSpinner.setEditable(true);
                colBox.getChildren().addAll(colLabel, colSpinner);

                HBox buttonBox = new HBox(10);
                buttonBox.setAlignment(Pos.CENTER);
                Button okButton = new Button("Generiši mrežu");
                Button cancelButton = new Button("Otkaži");
                buttonBox.getChildren().addAll(okButton, cancelButton);

                layout.getChildren().addAll(titleLabel, rowBox, colBox, buttonBox);

                Scene scene = new Scene(layout, 350, 200);
                dialog.setScene(scene);

                final boolean[] confirmed = {false};
                okButton.setOnAction(e -> {
                        rows = rowSpinner.getValue();
                        cols = colSpinner.getValue();
                        confirmed[0] = true;
                        dialog.close();
                });
                cancelButton.setOnAction(e -> dialog.close());

                dialog.showAndWait();
                return confirmed[0];
        }

        /**
         * Generise podatke o transportnoj mrezi i cuva ih u JSON fajl
         */
        private void generateTransportData() {
                TransportDataGenerator generator = new TransportDataGenerator(rows, cols);
                TransportDataGenerator.TransportData data = generator.generateData();
                generator.saveToJson(data, "transport_data.json");
        }

        /**
         * Prikazuje glavni prozor sa transportnom mrezom
         * @param primaryStage Glavni prozor
         */

        private void showTransportNetwork(Stage primaryStage) {
                try {
                        dataService = new TransportDataService("transport_data.json");
                        routeFinder = new RouteFinder(dataService);

                        // Graf i viewer
                        graph = GraphBuilder.build(rows, cols, dataService);
                        viewer = new FxViewer(graph, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
                        viewer.disableAutoLayout();
                        FxViewPanel viewPanel = (FxViewPanel) viewer.addDefaultView(false);
                        setupGraphClickListener(viewPanel);

                        // Layout
                        BorderPane root = new BorderPane();
                        root.setTop(createInfoPanel());
                        root.setLeft(createParameterForm());
                        root.setCenter(viewPanel);

                        Scene scene = new Scene(root, 1400, 800);
                        primaryStage.setTitle("Transportna mreža - " + rows + "x" + cols + " gradova");
                        primaryStage.setScene(scene);
                        primaryStage.setMaximized(true);
                        primaryStage.show();

                        updateSalesSummary();

                        primaryStage.setOnCloseRequest(e -> {
                                if (viewer != null) viewer.close();
                        });
                } catch (Exception e) {
                        showErrorDialog("Greška pri učitavanju podataka", e.getMessage());
                        e.printStackTrace();
                }
        }

        /**
         * Kreira panel sa informacijama o mrezi i statistikom prodaje
         * @return VBox sa informacijama
         */

        private VBox createInfoPanel() {
                VBox info = new VBox(5);
                info.setPadding(new Insets(10));
                info.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #3498db; -fx-border-radius: 5;");
                Label title = new Label("Transportna mreža - " + rows + "x" + cols + " (" + (rows * cols) + " gradova)");
                title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                // Dodajte labele za prikaz statistike
                totalTicketsLabel = new Label();
                totalPriceLabel = new Label();

                info.getChildren().addAll(title, totalTicketsLabel, totalPriceLabel);
                return info;
        }

        /**
         * Ucitava i azurira labele sa ukupnim brojem podataka karata i prihodom
         */

        private void updateSalesSummary() {
                TicketService.TicketSummary summary = TicketService.loadSummary();
                totalTicketsLabel.setText("Ukupan broj prodatih karata: " + summary.totalTickets);
                totalPriceLabel.setText("Ukupan prihod od prodaje: " + summary.totalPrice + " KM");
        }

        /**
         * Kreira formu za unos parametara pretrage rute
         * @return
         */

        private VBox createParameterForm() {
                VBox form = new VBox(15);
                form.setPadding(new Insets(20));
                form.setMaxWidth(250);
                form.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-color: white;");

                Label title = new Label("Planiranje rute");
                title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

                startComboBox = new ComboBox<>();
                startComboBox.setMaxWidth(Double.MAX_VALUE);
                for (City c : dataService.getAllCities()) startComboBox.getItems().add(c.getCityId());

                endComboBox = new ComboBox<>();
                endComboBox.setMaxWidth(Double.MAX_VALUE);
                for (City c : dataService.getAllCities()) endComboBox.getItems().add(c.getCityId());

                ToggleGroup tg = new ToggleGroup();
                RadioButton time = new RadioButton("Vrijeme");
                time.setToggleGroup(tg);
                time.setSelected(true);
                RadioButton price = new RadioButton("Cijena");
                price.setToggleGroup(tg);
                RadioButton transfers = new RadioButton("Presjedanja");
                transfers.setToggleGroup(tg);

                Button search = new Button("Pronađi rutu");
                search.setMaxWidth(Double.MAX_VALUE);
                search.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                search.setOnAction(e -> {
                        String startCity = startComboBox.getValue();
                        String endCity = endComboBox.getValue();
                        OptimizationCriteria criteria = OptimizationCriteria.TIME;
                        if (price.isSelected()) criteria = OptimizationCriteria.PRICE;
                        else if (transfers.isSelected()) criteria = OptimizationCriteria.TRANSFERS;
                        performRouteSearch(startCity, endCity, criteria);
                });

                Button resetButton = new Button("Reset");
                resetButton.setMaxWidth(Double.MAX_VALUE);
                resetButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                resetButton.setOnAction(e -> resetSelection());

                Button buyButton = new Button("Kupovina karte");
                buyButton.setMaxWidth(Double.MAX_VALUE);
                buyButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
                buyButton.setOnAction(e -> {
                        String startCity = startComboBox.getValue();
                        String endCity = endComboBox.getValue();
                        OptimizationCriteria criteria = OptimizationCriteria.TIME;
                        if (price.isSelected()) criteria = OptimizationCriteria.PRICE;
                        else if (transfers.isSelected()) criteria = OptimizationCriteria.TRANSFERS;

                        Route bestRoute = routeFinder.findOptimalRoute(startCity, endCity, criteria);
                        if (bestRoute != null) {
                                if (TicketService.saveTicket(bestRoute)) {
                                        updateSalesSummary();
                                        showSuccessDialog("Karta kupljena!", "Račun je sačuvan.");
                                } else {
                                        showErrorDialog("Greška pri kupovini", "Račun nije sačuvan.");
                                }
                        } else {
                                showErrorDialog("Greška", "Ruta nije pronađena.");
                        }
                });

                form.getChildren().addAll(
                        title,
                        new Label("Polazište:"), startComboBox,
                        new Label("Odredište:"), endComboBox,
                        new Label("Kriterijum:"),
                        new VBox(5, time, price, transfers),
                        search,
                        buyButton,
                        resetButton
                );
                return form;
        }

        /**
         * Resetuje odabrane gradove i stilizaciju grafa
         */

        private void resetSelection() {
                selectedStartCity = null;
                selectedEndCity = null;
                startComboBox.getSelectionModel().clearSelection();
                endComboBox.getSelectionModel().clearSelection();
                clearGraphStyling();
        }

        /**
         * Pokrece pretragu rute na osnovu odabranih parametara i prikazuje rezultate
         * @param startCity Polazni grad
         * @param endCity Odredisni grad
         * @param criteria Kriterijum optimizacije
         */

        private void performRouteSearch(String startCity, String endCity, OptimizationCriteria criteria) {
                if (startCity == null || endCity == null) {
                        showErrorDialog("Neispravna selekcija", "Molimo odaberite polazišnu i odredišnu tačku.");
                        return;
                }

                clearGraphStyling();
                List<Route> top = routeFinder.findTop5Routes(startCity, endCity, criteria);
                if (top == null || top.isEmpty()) {
                        showErrorDialog("Nema rezultata", "Nije pronađena ruta između " + startCity + " i " + endCity);
                        return;
                }

                selectedStartCity = startCity;
                selectedEndCity = endCity;
                highlightBestRoute(top.get(0));

                RouteDialogManager.showRoutesDialog(top, criteria, () -> {
                        if (TicketService.saveTicket(top.get(0))) {
                                updateSalesSummary();
                                showSuccessDialog("Karta kupljena!", "Račun je sačuvan.");
                        } else {
                                showErrorDialog("Greška pri kupovini", "Račun nije sačuvan.");
                        }
                });
        }

        /**
         * Prikazuje standardni informacioni dijalog
         * @param title Naslov
         * @param content Poruka koja se prikazuje
         */

        private void showSuccessDialog(String title, String content) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
        }

        /**
         * Resetuje stilizaciju svih cvorova i ivica grafa
         */

        private void clearGraphStyling() {
                if (graph == null) return;
                graph.nodes().forEach(node -> node.removeAttribute("ui.class"));
                graph.edges().forEach(edge -> edge.removeAttribute("ui.class"));
        }

        /**
         * Izdvaja i stilizuje najbolju rutu na grafu
         * @param route Ruta koja se istice
         */

        private void highlightBestRoute(Route route) {
                if (graph == null || route == null) return;

                Platform.runLater(() -> {
                        clearGraphStyling();

                        // sve ivice nevidljive
                        graph.edges().forEach(edge -> edge.setAttribute("ui.class", "invisible"));

                        // bojenje ivica rute i povecanje njihove vidljivosti
                        String previousCityId = route.getStartCityId();
                        for (RouteSegment segment : route.getSegments()) {
                                String currentCityId = Route.mapStationToCity(segment.getPolazak().getToStationId()); // IZMJENA: segment.getDeparture() se mijenja u segment.getPolazak()

                                Edge edge = graph.getEdge(previousCityId + "->" + currentCityId);
                                if (edge == null) {
                                        edge = graph.getEdge(currentCityId + "->" + previousCityId);
                                }

                                if (edge != null) {
                                        edge.setAttribute("ui.class", "route");
                                }

                                previousCityId = currentCityId;
                        }

                        // bojenje cvorova na ruti
                        for (String cityId : route.getCityPath()) {
                                Node node = graph.getNode(cityId);
                                if (node != null) {
                                        if (cityId.equals(route.getStartCityId())) {
                                                node.setAttribute("ui.class", "start");
                                        } else if (cityId.equals(route.getEndCityId())) {
                                                node.setAttribute("ui.class", "end");
                                        } else {
                                                node.setAttribute("ui.class", "station");
                                        }
                                }
                        }
                });
        }

        /**
         * Postavlja listener za klikove misa na panelu grafa
         * @param viewPanel Panel za prikaz grafa
         */

        private void setupGraphClickListener(FxViewPanel viewPanel) {
                viewPanel.setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                                String clickedCityId = findNearestNode(viewPanel, event.getX(), event.getY());
                                if (clickedCityId != null) {
                                        handleCityClick(clickedCityId);
                                }
                        }
                });
        }

        /**
         * Pronalazi najblizi cvor na grafu u odnosu na koordinate klika
         * @param viewPanel Panel za prikaz grafa
         * @param clickX X koordinata klika
         * @param clickY Y koordinata klika
         * @return ID najblizeg cvora ili null ako nije pronadjen nijedan u blizini
         */

        private String findNearestNode(FxViewPanel viewPanel, double clickX, double clickY) {
                final double THRESHOLD = 25.0;
                String nearest = null;
                double minDist = Double.MAX_VALUE;

                for (org.graphstream.graph.Node node : graph) {
                        Object[] pos = (Object[]) node.getAttribute("xyz");
                        if (pos != null && pos.length >= 2) {
                                double nodeX = (Double) pos[0];
                                double nodeY = (Double) pos[1];
                                var screenPos = viewPanel.getCamera().transformGuToPx(nodeX, nodeY, 0);
                                double dist = Math.hypot(clickX - screenPos.x, clickY - screenPos.y);
                                if (dist < THRESHOLD && dist < minDist) {
                                        minDist = dist;
                                        nearest = node.getId();
                                }
                        }
                }
                return nearest;
        }

        /**
         * Upravlja klikovima na gradove
         * @param cityId ID grada na koji je korisnik kliknuo
         */

        private void handleCityClick(String cityId) {
                if (selectedStartCity == null) {
                        selectedStartCity = cityId;
                        startComboBox.setValue(cityId);
                        updateCityVisualSelection(cityId, "start");
                } else if (selectedEndCity == null && !cityId.equals(selectedStartCity)) {
                        selectedEndCity = cityId;
                        endComboBox.setValue(cityId);
                        updateCityVisualSelection(cityId, "end");
                } else if (cityId.equals(selectedStartCity)) {
                        selectedStartCity = null;
                        startComboBox.getSelectionModel().clearSelection();
                        updateCityVisualSelection(cityId, "reset");
                } else if (cityId.equals(selectedEndCity)) {
                        selectedEndCity = null;
                        endComboBox.getSelectionModel().clearSelection();
                        updateCityVisualSelection(cityId, "reset");
                }
        }

        /**
         * Azurira vizuelni izgled cvora na grafu u zavisnosti od odabira
         * @param cityId ID grada
         * @param type Tip vizuelne promjene (start, end, reset)
         */

        private void updateCityVisualSelection(String cityId, String type) {
                if (graph == null || cityId == null) return;
                var node = graph.getNode(cityId);
                if (node == null) return;
                switch (type.toLowerCase()) {
                        case "start": node.setAttribute("ui.class", "start"); break;
                        case "end": node.setAttribute("ui.class", "end"); break;
                        case "reset": node.removeAttribute("ui.class"); break;
                }
        }

        /**
         * Prikazuje standardni dijalog za greske
         * @param title Naslov
         * @param msg Poruka o gresci
         */

        private void showErrorDialog(String title, String msg) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle(title);
                a.setHeaderText(null);
                a.setContentText(msg);
                a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                a.showAndWait();
        }


        /**
         * Glavna main metoda koja pokrece aplikaciju
         * @param args Argumenti komandne linije
         */

        public static void main(String[] args) {
                System.setProperty("org.graphstream.ui", "javafx");
                launch(args);
        }
}