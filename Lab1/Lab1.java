import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.SortedSet;
import java.io.File;
import java.io.FileNotFoundException;

abstract class Event {
    double time;
    Event(double time){
        this.time = time;
    }
    abstract void doEvent(int[] queueSize, List<Integer> observedPacketsInBuffer, List<Boolean> observedIdle);
}

class ObserverEvent extends Event {
    ObserverEvent(double time){
        super(time);
    }
    void doEvent(int[] queueSize, List<Integer> observedPacketsInBuffer, List<Boolean> observedIdle){
        observedPacketsInBuffer.add(queueSize[0]);
        observedIdle.add(queueSize[0] == 0);
    }
}

class ArrivalEvent extends Event {
    ArrivalEvent(double time){
        super(time);
    }
    void doEvent(int[] queueSize, List<Integer> observedPacketsInBuffer, List<Boolean> observedIdle){
        queueSize[0]++;
    }
}

class DepartureEvent extends Event {
    DepartureEvent(double time){
        super(time);
    }
    void doEvent(int[] queueSize, List<Integer> observedPacketsInBuffer, List<Boolean> observedIdle){
        queueSize[0]--;
    }
}

class EventComparator implements Comparator<Event> {
    public int compare(Event e1, Event e2){
        return e1.time - e2.time;
    }
}

public class Lab1 {

    // Question 1

    // Generate an exponential random value with the given lambda parameter
    static double exponentialRandom(double lambda){
        // Convert a [0, 1) continuous random distribution to an exponential distribution
        return - (1D / lambda) * Math.log(1D - Math.random());
    }

    public static void q1() throws FileNotFoundException{
        PrintStream output = new PrintStream(new FileOutputStream(new File("q1output.txt")));
        double cumulative = 0D;
        double[] randoms = new double[1000];
        for (int i = 0; i < 1000; i++){
            randoms[i] = exponentialRandom(75);
            output.println(randoms[i]);
            cumulative += randoms[i];
        }
        double mean = cumulative / 1000D;
        output.println("Mean: " + mean);
        double variance = 0;
        for (int i = 0; i < 1000; i++){
            variance += Math.pow(randoms[i] - mean, 2);
        }
        variance /= 999D;
        output.println("Variance: " + variance);
    }

    // Queston 2

    static void runSim(double totalTime, double arrivalRate, double lengthRate, double transmissionSpeed, double observerRate){
        SortedSet<Event> allEvents = new TreeSet<>(new EventComparator());
        List<ArrivalEvent> arrivals = new ArrayList<>();
        List<DepartureEvent> departures = new ArrayList<>();
        List<ObserverEvent> observers = new ArrayList<>();
        double arrivalTime = exponentialRandom(arrivalRate);
        while(arrivalTime < totalTime){
            ArrivalEvent arrivalEvent = new ArrivalEvent(arrivalTime);
            arrivals.add(arrivalEvent);
            allEvents.add(arrivalEvent);
            arrivalTime += exponentialRandom(arrivalRate);
        }
        double lastDeparture = 0;
        for (int i = 0; i < arrivals.size(); i++){
            double transmitTime = exponentialRandom(lengthRate) / transmissionSpeed;
            lastDeparture = Math.max(lastDeparture, arrivals.get(i).time) + transmitTime;
            if (lastDeparture < totalTime){
                DepartureEvent departureEvent = new DepartureEvent(lastDeparture);
                departures.add(departureEvent);
                allEvents.add(departureEvent);
            }
            else {
                break;
            }
        }
        double observerTime = exponentialRandom(observerRate);
        while(observerTime < totalTime){
            ObserverEvent observerEvent = new ObserverEvent(observerTime);
            arrivals.add(observerEvent);
            allEvents.add(observerEvent);
            observerTime += exponentialRandom(observerRate);
        }
        int[] queueSize = new int[1];
        List<Integer> observedPacketsInBuffer = new ArrayList<>();
        List<Boolean> observedIdle = new ArrayList<>();
        for (Event event : allEvents){
            event.doEvent(queueSize, observedPacketsInBuffer, observedIdle);
        }
    }

    public static void main(String[] args) throws FileNotFoundException{
        q1();
    }
}