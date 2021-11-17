public class Lab2 {
    static double exponentialRandom(double lambda){
        // Convert a [0, 1) continuous random distribution to an exponential distribution
        return - (1D / lambda) * Math.log(1D - Math.random());
    }
    public static void main(String[] args) {
        if (args.length != 7){
            System.out.println("Usage: " + args[0] + " N A R L D S");
        }
        int N = Integer.parseInt(args[1]);
        double A = Double.parseDouble(args[2]);
        double R = Double.parseDouble(args[3]);
        int L = Integer.parseInt(args[4]);
        double Tprop = Double.parseDouble(args[5]) / Double.parseDouble(args[6]);
    }
}