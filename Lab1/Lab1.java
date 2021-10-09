import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.SortedSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;
import java.io.File;
import java.io.FileNotFoundException;

abstract class Event {
    double time;
    Event(double time){
        this.time = time;
    }
    //process event with unlimited buffer size
    abstract void doEvent(int[] queueSize, List<Integer> observedPacketsInBuffer, List<Boolean> observedIdle);
    //process event with buffer size K
    abstract void doEventK(SortedSet<Event> allEvents, int[] queueSize, List<Integer> observedPacketsInBuffer, int[] lostPackets, double[] lastDepartureTime, int K);
}

class ObserverEvent extends Event {
    ObserverEvent(double time){
        super(time);
    }
    void doEvent(int[] queueSize, List<Integer> observedPacketsInBuffer, List<Boolean> observedIdle){
        observedPacketsInBuffer.add(queueSize[0]);
        observedIdle.add(queueSize[0] == 0);
    }

    void doEventK(SortedSet<Event> allEvents, int[] queueSize, List<Integer> observedPacketsInBuffer, int[] lostPackets, double[] lastDepartureTime, int K) {
        observedPacketsInBuffer.add(queueSize[0]);
    }
}

class ArrivalEvent extends Event {
    double lengthRate;
    double transmissionSpeed;
    double totalTime;

    ArrivalEvent(double time){
        super(time);
    }

    //Overloaded constructor to store values necessary to create Departure Events 
    ArrivalEvent(double time, double lr, double ts, double tt){
        super(time);
        lengthRate = lr;
        transmissionSpeed = ts;
        totalTime = tt;
    }

    void doEvent(int[] queueSize, List<Integer> observedPacketsInBuffer, List<Boolean> observedIdle){
        queueSize[0]++;
    }

    void doEventK(SortedSet<Event> allEvents, int[] queueSize, List<Integer> observedPacketsInBuffer, int[] lostPackets, double[] lastDepartureTime, int K) {
        if(queueSize[0] < K) { 
            queueSize[0]++;
            //generate packet's departure event
            lastDepartureTime[0] = Math.max(lastDepartureTime[0], time) + Lab1.exponentialRandom(lengthRate)/transmissionSpeed;
            if (lastDepartureTime[0] < totalTime){
                DepartureEvent departureEvent = new DepartureEvent(lastDepartureTime[0]);
                allEvents.add(departureEvent);
            }
        } else { //packet must be dropped
            lostPackets[0]++;
        }

    }
}

class DepartureEvent extends Event {
    DepartureEvent(double time){
        super(time);
    }
    void doEvent(int[] queueSize, List<Integer> observedPacketsInBuffer, List<Boolean> observedIdle){
        queueSize[0]--;
    }
    void doEventK(SortedSet<Event> allEvents, int[] queueSize, List<Integer> observedPacketsInBuffer, int[] lostPackets, double[] lastDepartureTime, int K) {
        queueSize[0]--;
    }
}

class EventComparator implements Comparator<Event> {
    public int compare(Event e1, Event e2){
        if (e1.time < e2.time){
            return -1;
        }
        return 1;
    }
}

class ResultSet {
    double avgPackets;
    double pIdle;
    double pLoss;
    ResultSet(double avgPackets, double pIdle){
        this.avgPackets = avgPackets;
        this.pIdle = pIdle;
    }
    ResultSet(double avgPackets, double pLoss, double pIdle){
        this.avgPackets = avgPackets;
        this.pLoss = pLoss;
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

    static ResultSet runSim(double totalTime, double arrivalRate, double lengthRate, double transmissionSpeed, double observerRate){
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
            observers.add(observerEvent);
            allEvents.add(observerEvent);
            observerTime += exponentialRandom(observerRate);
        }
        int[] queueSize = new int[1];
        List<Integer> observedPacketsInBuffer = new ArrayList<>();
        List<Boolean> observedIdle = new ArrayList<>();
        for (Event event : allEvents){
            event.doEvent(queueSize, observedPacketsInBuffer, observedIdle);
        }
        double totalPackets = 0;
        double totalIdle = 0;
        for (int i = 0; i < observedPacketsInBuffer.size(); i++){
            totalPackets += (double)observedPacketsInBuffer.get(i);
        }
        for (int i = 0; i < observedIdle.size(); i++){
            totalIdle += observedIdle.get(i) ? 1D : 0D;
        }
        return new ResultSet(totalPackets / (double)observedPacketsInBuffer.size(), totalIdle / (double)observedIdle.size());
    }

    // Question 5 + 6

    static ResultSet runSimK(double totalTime, double arrivalRate, double lengthRate, double transmissionSpeed, double observerRate, int K){
        //data structure that stores all event types and sorts them based on time 
        //sorting occurs whenever an event is added
        TreeSet<Event> allEvents = new TreeSet<Event>(new EventComparator());
        int numArrivalEvents = 0;

        //generate arrival events
        double arrivalTime = exponentialRandom(arrivalRate);
        while(arrivalTime < totalTime){
            ArrivalEvent arrivalEvent = new ArrivalEvent(arrivalTime, lengthRate, transmissionSpeed, totalTime);
            allEvents.add(arrivalEvent);
            numArrivalEvents++;
            arrivalTime += exponentialRandom(arrivalRate);
        }

        //generate observer events
        double observerTime = exponentialRandom(observerRate);
        while(observerTime < totalTime){
            ObserverEvent observerEvent = new ObserverEvent(observerTime);
            allEvents.add(observerEvent);
            observerTime += exponentialRandom(observerRate);
        }

        int[] queueSize = new int[1];
        int[] lostPackets = new int[1];
        double[] lastDepartureTime = new double[1];
        List<Integer> observedPacketsInBuffer = new ArrayList<>();

        //run all arrival, departure, and observer events
        //while loop is used since allEvents is modified during the loop (adding departure events)
        while(!allEvents.isEmpty()) {
            Event event = allEvents.pollFirst();
            event.doEventK(allEvents, queueSize, observedPacketsInBuffer, lostPackets, lastDepartureTime, K);
        }

        //calculate E[n] - average number of packets in buffer
        double totalPackets = 0;
        for (int i = 0; i < observedPacketsInBuffer.size(); i++){
            totalPackets += (double)observedPacketsInBuffer.get(i);
        }
        return new ResultSet(totalPackets/(double)observedPacketsInBuffer.size(), lostPackets[0]/(double)numArrivalEvents, 0);
    }

    static void stabilizeT(double rho){
        int multiplier = 1;
        ResultSet prevResults = null;
        while (true){
            System.out.println(multiplier);
            ResultSet results = runSim(1000 * multiplier, rho * SPEED / LENGTH, 1D / LENGTH, SPEED, 5D * rho * SPEED/LENGTH);
            if (prevResults != null){
                double dPackets = Math.abs((results.avgPackets - prevResults.avgPackets) / prevResults.avgPackets);
                double dPIdle = prevResults.pIdle == 0 ? 0 : Math.abs((results.pIdle - prevResults.pIdle) / prevResults.pIdle);
                if (prevResults != null && dPackets < 0.05 && dPIdle < 0.05){
                    break;
                }
            }
            multiplier++;
            prevResults = results;
        }
        System.out.println("Results for rho=" + rho + " stabilize at T=" + 1000 * (multiplier - 1));
    }

    static void stabilizeTK(double rho, int bufferSize){
        int multiplier = 1;
        ResultSet prevResults = null;
        while (true){
            ResultSet results = runSimK(1000 * multiplier, rho * SPEED / LENGTH, 1D / LENGTH, SPEED, 5D * rho * SPEED/LENGTH, bufferSize);
            if (prevResults != null){
                double dPackets = prevResults.avgPackets == 0 && results.avgPackets < 0.01 ? 0 : Math.abs((results.avgPackets - prevResults.avgPackets) / prevResults.avgPackets);
                double dPIdle = prevResults.pIdle == 0 && results.pIdle < 0.01 ? 0 : Math.abs((results.pIdle - prevResults.pIdle) / prevResults.pIdle);
                double dPLoss = prevResults.pLoss == 0 && results.pLoss < 0.01 ? 0 : Math.abs((results.pLoss - prevResults.pLoss) / prevResults.pLoss);
                if (prevResults != null && dPackets < 0.05 && dPIdle < 0.05 && dPLoss < 0.05){
                    break;
                }
            }
            multiplier++;
            prevResults = results;
        }
        System.out.println("Results for rho=" + rho + ", buffersize=" + bufferSize + " stabilize at T=" + 1000 * (multiplier - 1));
    }

    static final double LENGTH = 2000D;
    static final double SPEED = 1000000D;

    static void runQ2Experiment(double totalTime) throws FileNotFoundException{
        System.out.println("Q2 results with infinite buffer:");
        PrintStream output = new PrintStream(new FileOutputStream(new File("q2results.csv")));
        for (double rho = 0.25; rho <= 0.95; rho += 0.1){
            ResultSet results = runSim(totalTime, rho * SPEED / LENGTH, 1D / LENGTH, SPEED, 5*rho*SPEED/LENGTH);
            System.out.printf("Results for rho = %.2f: avg packets %.2f, idle %.4f\n", rho, results.avgPackets, results.pIdle);
            output.printf("%.2f,%.2f,%.4f\n", rho, results.avgPackets, results.pIdle);
        }
    }

    //run simulator for M/M/1/K queue for each value of K (10, 25, 50)
    static void runQ6Experiment(double totalTime) throws FileNotFoundException{
        PrintStream output10 = new PrintStream(new FileOutputStream(new File("q6results-10.csv")));
        System.out.println("\nK = 10");
        for(double rho = 0.5; rho <= 1.51; rho += 0.1) {
            ResultSet results = runSimK(totalTime, rho*SPEED/LENGTH, 1D / LENGTH, SPEED, 5*rho*SPEED/LENGTH, 10);
            System.out.printf("Results for rho = %.2f: avg packets %.2f, packet loss probability %.4f\n", rho, results.avgPackets, results.pLoss);
            output10.printf("%.2f,%.2f,%.4f\n", rho, results.avgPackets, results.pLoss);
        }
        PrintStream output25 = new PrintStream(new FileOutputStream(new File("q6results-25.csv")));
        System.out.println("\nK = 25");
        for(double rho = 0.5; rho <= 1.51; rho += 0.1) {
            ResultSet results = runSimK(totalTime, rho*SPEED/LENGTH, 1D / LENGTH, SPEED, 5*rho*SPEED/LENGTH, 25);
            System.out.printf("Results for rho = %.2f: avg packets %.2f, packet loss probability %.4f\n", rho, results.avgPackets, results.pLoss);
            output25.printf("%.2f,%.2f,%.4f\n", rho, results.avgPackets, results.pLoss);
        }
        PrintStream output50 = new PrintStream(new FileOutputStream(new File("q6results-50.csv")));
        System.out.println("\nK = 50");
        for(double rho = 0.5; rho <= 1.51; rho += 0.1) {
            ResultSet results = runSimK(totalTime, rho*SPEED/LENGTH, 1D / LENGTH, SPEED, 5*rho*SPEED/LENGTH, 50);
            System.out.printf("Results for rho = %.2f: avg packets %.2f, packet loss probability %.4f\n", rho, results.avgPackets, results.pLoss);
            output50.printf("%.2f,%.2f,%.4f\n", rho, results.avgPackets, results.pLoss);
        }
    }

    // Set flag to true to run time stability experiments
    static final boolean STABILIZE = false;
    public static void main(String[] args) throws FileNotFoundException{
        q1();
        if (STABILIZE){
            for (double rho = 0.25; rho <= 0.95; rho += 0.1){
                stabilizeT(rho);
                stabilizeTK(rho, 10);
                stabilizeTK(rho, 25);
                stabilizeTK(rho, 50);
            }
        }
        runQ2Experiment(8000D);
        runQ6Experiment(8000D);
    }
}
