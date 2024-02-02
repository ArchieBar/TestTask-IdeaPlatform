package org.example;

import com.google.gson.Gson;
import org.example.model.Ticket;
import org.example.model.TicketsWrapper;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    private static final DateTimeFormatter dataFormat = DateTimeFormatter.ofPattern("dd.MM.yy");
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("H:mm");
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Пожалуйста, укажите путь к файлу tickets.json в качестве аргумента командной строки.");
            return;
        }

        String filePath = args[0];

        try {
            List<Ticket> ticketsVvoTlv = loadAndFilterTickets(filePath);
            Map<String, Duration> minFlightTime = calculateMinFlightTime(ticketsVvoTlv);
            double differencePrice = calculatePriceDifference(ticketsVvoTlv);

            printResults(minFlightTime, differencePrice);

        } catch (FileNotFoundException e) {
            System.out.println("Файл не найден!");
        }
    }

    private static List<Ticket> loadAndFilterTickets(String filePath) throws FileNotFoundException {
        FileReader reader = new FileReader(filePath);
        TicketsWrapper ticketsWrapper = gson.fromJson(reader, TicketsWrapper.class);

        return ticketsWrapper.getTickets().stream()
                .filter(ticket -> ticket.getOrigin().equalsIgnoreCase("VVO"))
                .filter(ticket -> ticket.getDestination().equalsIgnoreCase("TLV"))
                .collect(Collectors.toList());
    }

    private static Map<String, Duration> calculateMinFlightTime(List<Ticket> tickets) {
        Map<String, Duration> minFlightTime = new HashMap<>();
        Map<String, List<Ticket>> carrierMap = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getCarrier));

        for (String carrier : carrierMap.keySet()) {
            Duration intermediateMinFlightTime = null;
            for (Ticket ticket : carrierMap.get(carrier)) {
                LocalDateTime departureDateTime = getLocalDateTime(ticket.getDepartureDate(), ticket.getDepartureTime());
                LocalDateTime arrivalDateTime = getLocalDateTime(ticket.getArrivalDate(), ticket.getArrivalTime());

                if (arrivalDateTime.toLocalTime().isBefore(departureDateTime.toLocalTime())) {
                    arrivalDateTime = arrivalDateTime.plusDays(1);
                }

                Duration durationFlight = Duration.between(departureDateTime, arrivalDateTime);

                if (intermediateMinFlightTime == null || durationFlight.compareTo(intermediateMinFlightTime) < 0) {
                    intermediateMinFlightTime = durationFlight;
                }
            }
            minFlightTime.put(carrier, intermediateMinFlightTime);
        }

        return minFlightTime;
    }

    private static double calculatePriceDifference(List<Ticket> tickets) {
        List<Integer> priceTicketList = tickets.stream()
                .map(Ticket::getPrice)
                .sorted()
                .collect(Collectors.toList());

        double averageValue = priceTicketList.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElseThrow(() -> new RuntimeException("Не удалось вычислить среднее значение цены!"));

        double medianPrice;
        int size = priceTicketList.size();

        if (size % 2 == 0) {
            medianPrice = (priceTicketList.get(size / 2 - 1) + priceTicketList.get(size / 2)) / 2.0;
        } else {
            medianPrice = priceTicketList.get(size / 2);
        }

        return averageValue - medianPrice;
    }

    private static void printResults(Map<String, Duration> minFlightTime, double differencePrice) {
        System.out.println(differencePrice);
        for (String carrier : minFlightTime.keySet()) {
            String durationString = String.format(
                    "%02d:%02d",
                    minFlightTime.get(carrier).toHours(),
                    minFlightTime.get(carrier).toMinutesPart());
            System.out.println(carrier + " " + durationString);
        }
    }

    private static LocalDateTime getLocalDateTime(String dateString, String timeString) {
        LocalDate date = LocalDate.parse(dateString, dataFormat);
        LocalTime time = LocalTime.parse(timeString, timeFormat);

        return LocalDateTime.of(date, time);
    }
}