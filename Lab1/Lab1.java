import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.FileNotFoundException;

public class Lab1 {

    // Question 1
    static double poissonRandom(double lambda){
        return - (1D / lambda) * Math.log(1D - Math.random());
    }

    public static void q1() throws FileNotFoundException{
        PrintStream output = new PrintStream(new FileOutputStream(new File("q1output.txt")));
        double cumulative = 0D;
        double[] randoms = new double[1000];
        for (int i = 0; i < 1000; i++){
            randoms[i] = poissonRandom(75);
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

    public static void main(String[] args) throws FileNotFoundException{
        q1();
    }
}