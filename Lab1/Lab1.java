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

/**
 * Event indicating an observer recording queue state
 */
class ObserverEvent extends Event {
    ObserverEvent(double time){
        super(time);
    }
    void doEvent(int[] queueSize, List<Integer> observedPacketsInBuffer, List<Boolean> observedIdle){
        // Record packets in queue
        observedPacketsInBuffer.add(queueSize[0]);
        // Record whether queue is idle
        observedIdle.add(queueSize[0] == 0);
    }

    void doEventK(SortedSet<Event> allEvents, int[] queueSize, List<Integer> observedPacketsInBuffer, int[] lostPackets, double[] lastDepartureTime, int K) {
        observedPacketsInBuffer.add(queueSize[0]);
    }
}

/**
 * Event indicating a packet entering the queue
 */
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
        // Add packet to queue
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

/**
 * Event indicating a packet leaving the queue
 */
class DepartureEvent extends Event {
    DepartureEvent(double time){
        super(time);
    }
    void doEvent(int[] queueSize, List<Integer> observedPacketsInBuffer, List<Boolean> observedIdle){
        // Remove packet from queue
        queueSize[0]--;
    }
    void doEventK(SortedSet<Event> allEvents, int[] queueSize, List<Integer> observedPacketsInBuffer, int[] lostPackets, double[] lastDepartureTime, int K) {
        queueSize[0]--;
    }
}

/**
 * Allows us to sort events by execution order
 */
class EventComparator implements Comparator<Event> {
    public int compare(Event e1, Event e2){
        if (e1.time < e2.time){
            return -1;
        }
        return 1;
    }
}

/**
 * Structure for storing experimental results
 */
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

    /**
     * Generate a list of 1000 exponential randoms and print them to file
     * 
     * @throws FileNotFoundException
     */
    public static void q1() throws FileNotFoundException{
        // Open output file
        PrintStream output = new PrintStream(new FileOutputStream(new File("q1output.txt")));
        double cumulative = 0D;
        double[] randoms = new double[1000];
        for (int i = 0; i < 1000; i++){
            // Generate random
            randoms[i] = exponentialRandom(75);
            output.println(randoms[i]);
            cumulative += randoms[i];
        }
        // Calculate mean
        double mean = cumulative / 1000D;
        output.println("Mean: " + mean);
        double variance = 0;
        // Iteratively calculate variance
        for (int i = 0; i < 1000; i++){
            variance += Math.pow(randoms[i] - mean, 2);
        }
        variance /= 999D;
        output.println("Variance: " + variance);
    }

    // Queston 2

    /**
     * Simulate a packet queue with the given parameters
     * 
     * @param totalTime
     * @param arrivalRate
     * @param lengthRate
     * @param transmissionSpeed
     * @param observerRate
     * @return ResultSet containing average # packets in queue and idle %
     */
    static ResultSet runSim(double totalTime, double arrivalRate, double lengthRate, double transmissionSpeed, double observerRate){
        // List of all events, sorted by execution order using custom comparator
        SortedSet<Event> allEvents = new TreeSet<>(new EventComparator());
        List<ArrivalEvent> arrivals = new ArrayList<>();
        List<DepartureEvent> departures = new ArrayList<>();
        List<ObserverEvent> observers = new ArrayList<>();
        // Generate first arrival time
        double arrivalTime = exponentialRandom(arrivalRate);
        // Keep generating arrival times until we exceed the experiment time
        while(arrivalTime < totalTime){
            // Create a new event
            ArrivalEvent arrivalEvent = new ArrivalEvent(arrivalTime);
            arrivals.add(arrivalEvent);
            // Add to sorted list of all events
            allEvents.add(arrivalEvent);
            // Generate the next arrival time
            arrivalTime += exponentialRandom(arrivalRate);
        }
        double lastDeparture = 0;
        // Generate a departure for each arriving packet
        for (int i = 0; i < arrivals.size(); i++){
            // Generate packet service time
            double transmitTime = exponentialRandom(lengthRate) / transmissionSpeed;
            // Packet takes transmitTime to service, starting from the last departure, or the time the packet arrived, whichever is later
            lastDeparture = Math.max(lastDeparture, arrivals.get(i).time) + transmitTime;
            if (lastDeparture < totalTime){
                // If the given time is within our range, create the event
                DepartureEvent departureEvent = new DepartureEvent(lastDeparture);
                departures.add(departureEvent);
                // Add to sorted list of all events
                allEvents.add(departureEvent);
            }
            else {
                break;
            }
        }
        // Generate the first observer time
        double observerTime = exponentialRandom(observerRate);
        // Keep generating observer times until we exceed the experiment time
        while(observerTime < totalTime){
            ObserverEvent observerEvent = new ObserverEvent(observerTime);
            observers.add(observerEvent);
            // Add to sorted list of all events
            allEvents.add(observerEvent);
            // Generate the next observer time
            observerTime += exponentialRandom(observerRate);
        }
        // This is really just an int, but creating as an array allows us to pass by reference
        int[] queueSize = new int[1];
        List<Integer> observedPacketsInBuffer = new ArrayList<>();
        List<Boolean> observedIdle = new ArrayList<>();
        // Service all events, in execution order
        for (Event event : allEvents){
            event.doEvent(queueSize, observedPacketsInBuffer, observedIdle);
        }
        double totalPackets = 0;
        double totalIdle = 0;
        // Calculate averages of total packets, idle time
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

    /**
     * Find a value of totalTime for which the experiment is stable, given a rho and infinite buffersize
     * 
     * @param rho
     */
    static void stabilizeT(double rho){
        int multiplier = 1;
        ResultSet prevResults = null;
        while (true){
            // Run sim at given time
            System.out.println(multiplier);
            ResultSet results = runSim(1000 * multiplier, rho * SPEED / LENGTH, 1D / LENGTH, SPEED, 5D * rho * SPEED/LENGTH);
            if (prevResults != null){
                // Compare current and previous results
                double dPackets = Math.abs((results.avgPackets - prevResults.avgPackets) / prevResults.avgPackets);
                double dPIdle = prevResults.pIdle == 0 ? 0 : Math.abs((results.pIdle - prevResults.pIdle) / prevResults.pIdle);
                if (prevResults != null && dPackets < 0.05 && dPIdle < 0.05){
                    // Results are withni 5%, we have found our target
                    break;
                }
            }
            multiplier++;
            prevResults = results;
        }
        System.out.println("Results for rho=" + rho + " stabilize at T=" + 1000 * (multiplier - 1));
    }

    /**
     * Find a value of totalTime for which the experiment is stable, given a rho and buffersize
     * 
     * @param rho
     * @param bufferSize
     */
    static void stabilizeTK(double rho, int bufferSize){
        int multiplier = 1;
        ResultSet prevResults = null;
        while (true){
            // Run sim at given time
            ResultSet results = runSimK(1000 * multiplier, rho * SPEED / LENGTH, 1D / LENGTH, SPEED, 5D * rho * SPEED/LENGTH, bufferSize);
            if (prevResults != null){
                // Compare current and previous results
                double dPackets = prevResults.avgPackets == 0 && results.avgPackets < 0.01 ? 0 : Math.abs((results.avgPackets - prevResults.avgPackets) / prevResults.avgPackets);
                double dPIdle = prevResults.pIdle == 0 && results.pIdle < 0.01 ? 0 : Math.abs((results.pIdle - prevResults.pIdle) / prevResults.pIdle);
                double dPLoss = prevResults.pLoss == 0 && results.pLoss < 0.01 ? 0 : Math.abs((results.pLoss - prevResults.pLoss) / prevResults.pLoss);
                if (prevResults != null && dPackets < 0.05 && dPIdle < 0.05 && dPLoss < 0.05){
                    // Results are withni 5%, we have found our target
                    break;
                }
            }
            multiplier++;
            prevResults = results;
        }
        System.out.println("Results for rho=" + rho + ", buffersize=" + bufferSize + " stabilize at T=" + 1000 * (multiplier - 1));
    }

    // Average packet length, in bits
    static final double LENGTH = 2000D;
    // Packet service speed, in bits/second
    static final double SPEED = 1000000D;

    /**
     * Generate data for question 2
     * 
     * @param totalTime
     * @throws FileNotFoundException
     */
    static void runQ2Experiment(double totalTime) throws FileNotFoundException{
        System.out.println("Q2 results with infinite buffer:");
        PrintStream output = new PrintStream(new FileOutputStream(new File("q2results.csv")));
        for (double rho = 0.25; rho <= 0.95; rho += 0.1){
            // Run sim at given rho value
            ResultSet results = runSim(totalTime, rho * SPEED / LENGTH, 1D / LENGTH, SPEED, 5*rho*SPEED/LENGTH);
            System.out.printf("Results for rho = %.2f: avg packets %.2f, idle %.4f\n", rho, results.avgPackets, results.pIdle);
            // Output to csv for easy chartifying
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
