import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class LearnTreeMain {

    private static void exit(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    public static void main(String[] args) {
        if(args.length < 5) {
            exit("Usage: learntree <1/2> <P> <L> <trainingset_filename> <outputtree_filename>");
        }

        int version = -1;
        double P = -1;
        int L = -1;
        String trainingSetFile = args[3];
        String outputTreeFile = args[4];
        try {
            version = Integer.parseInt(args[0]);
            if (version != 1 && version != 2) {
                throw new NumberFormatException("");
            }
        } catch (NumberFormatException e) {
            exit("Version should be 1/2");
        }

        try {
            P = Integer.parseInt(args[1]);
            if (P <= 0 || 100 <= P) {
                throw new NumberFormatException("");
            }
            P /= 100;
        } catch (NumberFormatException e) {
            exit("P should be between 0 and 100");
        }


        try {
            L = Integer.parseInt(args[2]);
            if (L <= 0) {
                throw new NumberFormatException("");
            }
        } catch (NumberFormatException e) {
            exit("L should be a positive integer");
        }

        int[][] images_matrix = Images.parse_csv(trainingSetFile);
        if (images_matrix == null) {
            exit("Error opening file " + trainingSetFile);
        }

        DecisionTree t = TreeMaker.makeTree(version, P, L, images_matrix, Questions.BEST_CONFIG);

        try {
            ObjectOutputStream treeObjStream = new ObjectOutputStream(new FileOutputStream(outputTreeFile));
            treeObjStream.writeObject(t);
            treeObjStream.close();
        } catch (IOException e) {
            exit("Error opening file " + outputTreeFile);
        }
    }
}
