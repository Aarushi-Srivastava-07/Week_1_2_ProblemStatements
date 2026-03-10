import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ParkingLot {
    private enum SpotStatus { EMPTY, OCCUPIED, DELETED }

    private static class Spot {
        SpotStatus status = SpotStatus.EMPTY;
        String license;
        long entryTime;
    }

    private final Spot[] spots;
    private final int totalSpots;
    private final Map<String, Integer> licenseToSpot = new ConcurrentHashMap<>();
    private final AtomicInteger occupancy = new AtomicInteger(0);
    private final AtomicLong totalProbes = new AtomicLong(0);
    private final AtomicInteger parkCount = new AtomicInteger(0);
    private final int[] hourlyOccupancy = new int[24];
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ParkingLot(int totalSpots) {
        this.totalSpots = totalSpots;
        this.spots = new Spot[totalSpots];
        for (int i = 0; i < totalSpots; i++) spots[i] = new Spot();
        scheduler.scheduleAtFixedRate(this::recordHourlyOccupancy, 1, 1, TimeUnit.HOURS);
    }

    private int hash(String license) {
        return Math.abs(license.hashCode()) % totalSpots;
    }

    public String parkVehicle(String license) {
        int index = hash(license);
        int probes = 0;
        while (probes < totalSpots) {
            SpotStatus status = spots[index].status;
            if (status == SpotStatus.EMPTY || status == SpotStatus.DELETED) {
                spots[index].status = SpotStatus.OCCUPIED;
                spots[index].license = license;
                spots[index].entryTime = System.currentTimeMillis();
                licenseToSpot.put(license, index);
                occupancy.incrementAndGet();
                totalProbes.addAndGet(probes + 1);
                parkCount.incrementAndGet();
                return String.format("Assigned spot #%d (%d probe%s)", index, probes + 1, probes == 0 ? "" : "s");
            }
            probes++;
            index = (index + 1) % totalSpots;
        }
        return "Parking full";
    }

    public String exitVehicle(String license) {
        Integer spotIndex = licenseToSpot.remove(license);
        if (spotIndex == null) return "Vehicle not found";
        Spot spot = spots[spotIndex];
        long durationMillis = System.currentTimeMillis() - spot.entryTime;
        long hours = durationMillis / (1000 * 60 * 60);
        long minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60);
        double fee = hours * 5.0 + (minutes > 0 ? 5.0 : 0); // $5 per hour, round up
        spot.status = SpotStatus.DELETED;
        spot.license = null;
        occupancy.decrementAndGet();
        return String.format("Spot #%d freed, Duration: %dh %dm, Fee: $%.2f", spotIndex, hours, minutes, fee);
    }

    private void recordHourlyOccupancy() {
        int occ = occupancy.get();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        hourlyOccupancy[hour] = Math.max(hourlyOccupancy[hour], occ);
    }

    public String getStatistics() {
        double avgProbes = parkCount.get() == 0 ? 0 : (double) totalProbes.get() / parkCount.get();
        int occ = occupancy.get();
        double occupancyPercent = (occ * 100.0) / totalSpots;
        int peakHour = 0;
        for (int i = 1; i < 24; i++) if (hourlyOccupancy[i] > hourlyOccupancy[peakHour]) peakHour = i;
        String peakPeriod = peakHour + "-" + (peakHour + 1) + (peakHour == 0 ? " AM" : peakHour < 12 ? " AM" : peakHour == 12 ? " PM" : " PM");
        return String.format("Occupancy: %.1f%%, Avg Probes: %.2f, Peak Hour: %s", occupancyPercent, avgProbes, peakPeriod);
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {
        ParkingLot lot = new ParkingLot(500);
        System.out.println(lot.parkVehicle("ABC-1234"));
        System.out.println(lot.parkVehicle("ABC-1235"));
        System.out.println(lot.parkVehicle("XYZ-9999"));
        Thread.sleep(2000);
        System.out.println(lot.exitVehicle("ABC-1234"));
        System.out.println(lot.getStatistics());
        lot.shutdown();
    }
}