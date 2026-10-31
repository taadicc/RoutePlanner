package net.unibl.etf.pj2.transport.util;

import net.unibl.etf.pj2.transport.data.Polazak; // IZMJENA: Uvozimo Polazak
import net.unibl.etf.pj2.transport.routing.Route;
import net.unibl.etf.pj2.transport.routing.RouteSegment;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * Servis za rad sa racunima
 * Omogucava snimanje racuna i ucitavanje ukupnog broja prodatih karata i prihoda
 */
public class TicketService {

    private static final String RACUNI_FOLDER = "racuni";

    /**
     * Ucitava ukupan broj prodatih karata i ukupan prihod iz sacuvanih racuna
     * @return Objekat tipa TicketSummary koji sadrzi broj karata i ukupan prihod
     */
    public static TicketSummary loadSummary() {
        long totalTickets = 0;
        int totalPrice = 0;

        File folder = new File(RACUNI_FOLDER);
        if (!folder.exists() || !folder.isDirectory()) {
            return new TicketSummary(0, 0);
        }

        try {
            totalTickets = Files.list(Paths.get(folder.getPath()))
                    .filter(Files::isRegularFile)
                    .count();

            totalPrice = Files.list(Paths.get(folder.getPath()))
                    .filter(Files::isRegularFile)
                    .mapToInt(path -> {
                        try (Stream<String> fileLines = Files.lines(path)) {
                            return fileLines.filter(line -> line.startsWith("UKUPNA CIJENA"))
                                    .mapToInt(line -> {
                                        try {
                                            return Integer.parseInt(line.split(":")[1].trim().split(" ")[0]);
                                        } catch (Exception e) {
                                            return 0;
                                        }
                                    })
                                    .sum();
                        } catch (IOException e) {
                            System.err.println("Greška pri čitanju fajla: " + path.getFileName());
                            return 0;
                        }
                    })
                    .sum();

        } catch (IOException e) {
            System.err.println("Greška pri učitavanju rezimea prodaje: " + e.getMessage());
        }

        return new TicketSummary(totalTickets, totalPrice);
    }

    /**
     * Kreira i cuva racun za datu rutu u tekstualni fajl
     * @param route Ruta za koju se kupuje karta
     * @return true ako je racun uspjesno sacuvan, inace false.
     */
    public static boolean saveTicket(Route route) {
        File dir = new File(RACUNI_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDate.now().toString() + "_" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));
        String fileName = String.format("%s/racun_%s_%s_%s.txt",
                RACUNI_FOLDER, route.getStartCityId(), route.getEndCityId(), timestamp);

        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.println("========== RAČUN ==========");
            writer.println("Datum: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy.")));
            writer.println("Vrijeme: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            writer.println("Relacija: " + route.getStartCityId() + " → " + route.getEndCityId());
            writer.println("Ukupno vrijeme: " + formatDuration(route.getTotalTimeMinutes()));
            writer.println("Presjedanja: " + route.getTransferCount());
            writer.println();
            int i = 1;
            for (RouteSegment seg : route.getSegments()) {
                Polazak polazak = seg.getPolazak(); // IZMJENA: Koristimo getPolazak()
                writer.printf("%d. %s  %s → %s%n", i++, polazak.getTransportType().toUpperCase(), // IZMJENA: getTransportType()
                        polazak.getFromStationId(), polazak.getToStationId());
                writer.println("   Polazak: " + polazak.getDepartureTime());
                writer.println("   Dolazak: " + polazak.getArrivalTime());
                writer.println("   Cijena: " + (int)polazak.getPrice() + " KM"); // IZMJENA: Cijena je double, prebacujemo u int
                writer.println("   Trajanje: " + formatDuration((int)polazak.getDuration().toMinutes()));
                writer.println();
            }
            writer.println("=====================================");
            writer.println("UKUPNA CIJENA: " + route.getTotalPrice() + " KM");
            return true;
        } catch (IOException e) {
            System.err.println("Greška prilikom čuvanja računa: " + e.getMessage());
            return false;
        }
    }

    /**
     * Pomoćna metoda za formatiranje vremena
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

    /**
     * Pomocna klasa za prenos podataka o prodaji
     */
    public static class TicketSummary {
        /** Ukupan broj prodatih karata. */
        public final long totalTickets;
        /** Ukupan prihod od prodaje karata. */
        public final int totalPrice;
        /**
         * Konstruktor za kreiranje objekta rezimea
         * @param totalTickets Ukupan broj prodatih karata
         * @param totalPrice Ukupan prihod od prodaje
         */
        public TicketSummary(long totalTickets, int totalPrice) {
            this.totalTickets = totalTickets;
            this.totalPrice = totalPrice;
        }
    }
}