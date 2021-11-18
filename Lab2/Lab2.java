import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Double;

class Node {
    List<Double> frames;// Queue of frames
    double R;
    int collisionCounter = 0;
    double successfulTransmissions = 0;
    double attemptedTransmissions = 0;

    // Generate an exponential random value with the given lambda parameter
    private double exponentialRandom(double lambda){
        // Convert a [0, 1) continuous random distribution to an exponential distribution
        return - (1D / lambda) * Math.log(1D - Math.random());
    }

    private double exponentialBackoff() {
        Random rand = new Random();
        return ((double)rand.nextInt((int)Math.pow(2, collisionCounter)))*512D/R;
    }

    public Node(int arrivalRate, double T_sim, double channelSpeed) {
        R = channelSpeed;
        frames = new ArrayList<>();
        // Generate queue frames using Poisson function
        double arrivalTime = exponentialRandom(arrivalRate);
        while(arrivalTime < T_sim){
            frames.add(arrivalTime);
            arrivalTime += exponentialRandom(arrivalRate);
        }
    }

    // returns the next time a frame transmission will occur
    public double nextTrans() {
        return frames.get(0);
    }

    // returns true if all frames have been sent or dropped
    public boolean isDone() {
        return frames.size() == 0;
    }

    public double getSuccessfulTransmissions() {
        return successfulTransmissions;
    }

    public double getAttemptedTransmissions() {
        return attemptedTransmissions;
    }

    // this method is called if the frame transmitted without collision
    public void success() {
        successfulTransmissions += 1D;
        attemptedTransmissions += 1D;
        collisionCounter = 0;
        frames.remove(0);
    }

    // this method will change the arrival time of every frame to be >= newArrivalTime
    public void wait(double newArrivalTime) {
        for(int i = 0; i < frames.size(); i++) {
           if(frames.get(i) > newArrivalTime) {
               break;
           }
           frames.set(i, newArrivalTime);
        }
    }

    // called when a collision occurs
    // will increment appropriate variables and then wait using exponential backoff
    public void collision(double collisionTime) {
        attemptedTransmissions += 1D;
        collisionCounter++;
        if(collisionCounter > 10) {
            collisionCounter = 0;
            frames.remove(0);
        } else {
            wait(collisionTime + exponentialBackoff());
        }
    }
}

public class Lab2 {
    public static void runPersistentSim(int N, int A, double R, double L, double D, double S, double T_sim) {
        List<Node> nodes = new ArrayList<>();
        double T_prop = D/S;// propogation delay
        double T_trans = L/R;// transmission delay
        double successfulTransmissions = 0;
        double attemptedTransmissions = 0;

        // Create the N nodes
        for(int i = 0; i < N; i++) {
            nodes.add(new Node(A, T_sim, R));
        }

        // run the actual simulation
        while(true) {
            int transNode = -1;
            double transNodeTime = T_sim;
            boolean collisionDetected = false;
            int furthestCollision = 0;// distance to furthest colliding node away from sender (see Piazza @213)

            // find node that will start transmitting next
            for(int i = 0; i < N; i++) {
               if(!nodes.get(i).isDone() && nodes.get(i).nextTrans() < transNodeTime) {
                  transNode = i;
                  transNodeTime = nodes.get(i).nextTrans();
               }
            }

            // exit the while loop if all frames have been sent or dropped
            // or the end of the simulation has been reached (whichever comes first)
            if(transNode == -1) {
                break;
            }

            // check other nodes for collisions
            for(int i = 0; i < N; i++) {
                if(i != transNode && !nodes.get(i).isDone() && nodes.get(i).nextTrans() <= transNodeTime + Math.abs(transNode-i)*T_prop) {
                    //System.out.println("COLLISION at " + i + ": " + nodes.get(i).nextTrans());
                    collisionDetected = true;
                    furthestCollision = Math.max(Math.abs(transNode-i), furthestCollision);
                    nodes.get(i).collision(transNodeTime + Math.abs(transNode-i)*T_prop);// handle collision and wait
                }
            }

            // handle collision for transmitting node
            if(collisionDetected) {
                nodes.get(transNode).collision(transNodeTime + furthestCollision*T_prop);// handle collision and wait
            } else {
                nodes.get(transNode).success();
                // update all waiting nodes
                for(int i = 0; i < N; i++) {
                    nodes.get(i).wait(transNodeTime + Math.abs(transNode-i)*T_prop + T_trans);
                }
            }
        }

        //int frames = 0;

        // Calculate total successful and attempted transmissions
        for(int i = 0; i < N; i++) {
            successfulTransmissions += nodes.get(i).getSuccessfulTransmissions();
            attemptedTransmissions += nodes.get(i).getAttemptedTransmissions();
        }

        // Print results
        double efficiency = successfulTransmissions/attemptedTransmissions;
        double throughput = successfulTransmissions*(L/(T_sim*1000000D));
        System.out.println("Efficiency = " + efficiency + ", Throughput = " + throughput);
    }

    // Generate results for question 1 (persistent CSMA/CD protocl) 
    // This method will call runPersistentSim for each value of N and A
    // specified in the lab manual
    public static void question1(double R, double L, double D, double S, double T_sim) {
        for(int N = 20; N < 101; N+=20) {
            System.out.print("Results for persistent CSMA/CD protocol where N = " + N + ", A = 7: ");
            runPersistentSim(N, 7, R, L, D, S, T_sim);
            System.out.print("Results for persistent CSMA/CD protocol where N = " + N + ", A = 10: ");
            runPersistentSim(N, 10, R, L, D, S, T_sim);
            System.out.print("Results for persistent CSMA/CD protocol where N = " + N + ", A = 20: ");
            runPersistentSim(N, 20, R, L, D, S, T_sim);
        }
    }

    public static void main(String[] args) {
        double R = 1000000D;// speed of LAN bus (in bits/s)
        double L = 1500D; // size of frame (in bits)
        double D = 10D; // distance between adjacent nodes (in metres)
        double S = 200000000D; // propogation speed (in m/s)
        double T_sim = 1000D; // simulation time (in seconds)
        question1(R, L, D, S, T_sim);
    }
}
