package net.unibl.etf.pj2.transport.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.unibl.etf.pj2.transport.data.Polazak;
import net.unibl.etf.pj2.transport.routing.*;

import java.util.List;

import static net.unibl.etf.pj2.transport.routing.Route.mapStationToCity;

/**
 * Klasa zaduzena za upravljanje prozorima za prikaz detalja rute i top 5 ruta
 * Omogucava tabelarni prikaz segmenata rute i interakciju sa korisnikom
 */
public class RouteDialogManager {

    /**
     * Glavni konstruktor za klasu RouteDialogManager
     */
    public RouteDialogManager() {
        // Prazan konstruktor
    }

    /**
     * Pomocna unutrasnja klasa koja sluzi kao model za redove u tabeli prikaza rute
     */
    public static class RouteTableRow {
        private final String polazak, dolazak, tip, cijena, vrijemePolaska, vrijemeDolaska, trajanje;

        /**
         * Konstruktor za kreiranje reda u tabeli rute
         *
         * @param polazak        Polazni grad/stanica
         * @param dolazak        Odredisni grad/stanica
         * @param tip            Tip prevoza (Autobus, voz)
         * @param cijena         Cijena karte za segment rute
         * @param vrijemePolaska Vrijeme polaska za segment rute
         * @param vrijemeDolaska Vrijeme dolaska za segment rute
         * @param trajanje       Trajanje putovanja za segment rute
         */
        public RouteTableRow(String polazak, String dolazak, String tip, String cijena,
                             String vrijemePolaska, String vrijemeDolaska, String trajanje) {
            this.polazak = polazak;
            this.dolazak = dolazak;
            this.tip = tip;
            this.cijena = cijena;
            this.vrijemePolaska = vrijemePolaska;
            this.vrijemeDolaska = vrijemeDolaska;
            this.trajanje = trajanje;
        }

        /**
         * Vraca polazni grad ili stanicu
         *
         * @return Polazni grad/stanica
         */
        public String getPolazak() { return polazak; }

        /**
         * Vraca odredisni grad ili stanicu
         *
         * @return Odredisni grad/stanica
         */
        public String getDolazak() { return dolazak; }

        /**
         * Vraca tip prevoza
         *
         * @return Tip prevoza
         */
        public String getTip() { return tip; }

        /**
         * Vraca cijenu segmenta rute
         *
         * @return Cijena segmenta rute
         */
        public String getCijena() { return cijena; }

        /**
         * Vraca vrijeme polaska
         *
         * @return Vrijeme polaska
         */
        public String getVrijemePolaska() { return vrijemePolaska; }

        /**
         * Vraca vrijeme dolaska
         *
         * @return Vrijeme dolaska
         */
        public String getVrijemeDolaska() { return vrijemeDolaska; }

        /**
         * Vraca trajanje putovanja za segment rute
         *
         * @return Trajanje putovanja
         */
        public String getTrajanje() { return trajanje; }
    }

    /**
     * Prikazuje prozor sa detaljima najbolje rute i opcijom za prikaz top 5 ruta
     *
     * @param routes   Lista ruta, sa najboljom na prvom mjestu
     * @param criteria Kriterijum optimizacije koji je koriscen za pretragu
     * @param onBuy    Funkcionalni interfejs (callback) koji se izvrsava nakon kupovine karte
     */
    public static void showRoutesDialog(List<Route> routes, OptimizationCriteria criteria, Runnable onBuy) {
        if (routes.isEmpty()) return;
        Route bestRoute = routes.get(0);
        Stage routeStage = new Stage();
        routeStage.initModality(Modality.APPLICATION_MODAL);
        routeStage.setTitle("Detalji rute");

        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));

        Label headerLabel = new Label("Najbolja ruta (" + criteria + ")");
        Label summaryLabel = new Label(String.format("Vrijeme: %s | Cijena: %d KM | Presjedanja: %d",
                formatDuration(bestRoute.getTotalTimeMinutes()), bestRoute.getTotalPrice(), bestRoute.getTransferCount()));

        TableView<RouteTableRow> table = createRouteTable(bestRoute);

        Button showTop5Button = new Button("Prikaz Top 5");
        showTop5Button.setOnAction(e -> showTop5RoutesWindow(routes, criteria, onBuy));

        mainLayout.getChildren().addAll(headerLabel, summaryLabel, table, showTop5Button);

        Scene scene = new Scene(mainLayout, 900, 650);
        routeStage.setScene(scene);
        routeStage.show();
    }

    /**
     * Kreira i popunjava tabelu sa detaljima o segmentima date rute
     *
     * @param route Ruta za koju se kreira tabela
     * @return Popunjeni objekat {@link TableView}
     */
    private static TableView<RouteTableRow> createRouteTable(Route route) {
        TableView<RouteTableRow> table = new TableView<>();

        TableColumn<RouteTableRow, String> polazakCol = new TableColumn<>("Polazak");
        polazakCol.setCellValueFactory(new PropertyValueFactory<>("polazak"));

        TableColumn<RouteTableRow, String> dolazakCol = new TableColumn<>("Dolazak");
        dolazakCol.setCellValueFactory(new PropertyValueFactory<>("dolazak"));

        TableColumn<RouteTableRow, String> tipCol = new TableColumn<>("Tip");
        tipCol.setCellValueFactory(new PropertyValueFactory<>("tip"));

        TableColumn<RouteTableRow, String> cijenaCol = new TableColumn<>("Cijena");
        cijenaCol.setCellValueFactory(new PropertyValueFactory<>("cijena"));

        TableColumn<RouteTableRow, String> polaskaCol = new TableColumn<>("Vrijeme polaska");
        polaskaCol.setCellValueFactory(new PropertyValueFactory<>("vrijemePolaska"));

        TableColumn<RouteTableRow, String> dolaskaCol = new TableColumn<>("Vrijeme dolaska");
        dolaskaCol.setCellValueFactory(new PropertyValueFactory<>("vrijemeDolaska"));

        TableColumn<RouteTableRow, String> trajanjeCol = new TableColumn<>("Trajanje");
        trajanjeCol.setCellValueFactory(new PropertyValueFactory<>("trajanje"));

        table.getColumns().addAll(polazakCol, dolazakCol, tipCol, cijenaCol, polaskaCol, dolaskaCol, trajanjeCol);

        ObservableList<RouteTableRow> data = FXCollections.observableArrayList();

        for (RouteSegment segment : route.getSegments()) {
            Polazak polazak = segment.getPolazak();

            if (polazak != null) {
                String polaznaStanica = polazak.getFromStationId();
                String dolaznaStanica = polazak.getToStationId();
                String tip = polazak.getTransportType().equals("autobus") ? "Autobus" : "Voz";
                String cijena = (int) polazak.getPrice() + " KM";
                String vrijemePolaska = polazak.getDepartureTime().toString();
                String vrijemeDolaska = polazak.getArrivalTime().toString();
                String trajanje = formatDuration((int)polazak.getDuration().toMinutes());

                data.add(new RouteTableRow(polaznaStanica, dolaznaStanica, tip, cijena, vrijemePolaska, vrijemeDolaska, trajanje));
            }
        }
        table.setItems(data);
        return table;
    }

    /**
     * Prikazuje novi prozor sa listom top 5 ruta
     *
     * @param routes   Lista top 5 ruta
     * @param criteria Kriterijum optimizacije
     * @param onBuy    Funkcionalni interfejs (callback) koji se izvrsava nakon kupovine karte
     */
    private static void showTop5RoutesWindow(List<Route> routes, OptimizationCriteria criteria, Runnable onBuy) {
        Stage top5Stage = new Stage();
        top5Stage.initModality(Modality.APPLICATION_MODAL);
        top5Stage.setTitle("Top 5 ruta - " + criteria);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        for (int i = 0; i < Math.min(5, routes.size()); i++) {
            Route route = routes.get(i);

            VBox routeBox = new VBox(5);
            routeBox.setPadding(new Insets(10));
            routeBox.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5;");

            Label summaryLabel = new Label(String.format("%d. Vrijeme: %s | Cijena: %d KM | Presjedanja: %d",
                    i + 1, formatDuration(route.getTotalTimeMinutes()), route.getTotalPrice(), route.getTransferCount()));

            Label pathLabel = new Label("Putanje: " + route.getCityPathSignature());
            pathLabel.setWrapText(true);

            Button buyButton = new Button("Kupi kartu");
            buyButton.setOnAction(e -> {
                if (onBuy != null) {
                    onBuy.run();
                }
                top5Stage.close();
            });

            routeBox.getChildren().addAll(summaryLabel, pathLabel, buyButton);
            layout.getChildren().add(routeBox);
        }

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 600, 400);
        top5Stage.setScene(scene);
        top5Stage.show();
    }

    /**
     * Prikazuje standardni informacioni dijalog
     *
     * @param title   Naslov
     * @param message Poruka koja se prikazuje
     */
    private static void showSuccessDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Prikazuje standardni dijalog za greske
     *
     * @param title   Naslov dijaloga
     * @param message Poruka o gresci
     */
    private static void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Pomocna metoda za formatiranje trajanja iz minuta u format "X dan(a) Y h Z min"
     *
     * @param minutes Ukupno trajanje u minutama
     * @return Formatirani string trajanja
     */
    private static String formatDuration(int minutes) {
        int days = minutes / (24 * 60);
        int remainingMinutes = minutes % (24 * 60);
        int hours = remainingMinutes / 60;
        int remainingMinutes2 = remainingMinutes % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(" dan(a) ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append(" h ");
        }
        sb.append(remainingMinutes2).append(" min");
        return sb.toString().trim();
    }
}