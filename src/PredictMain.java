import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class PredictMain {

    private static void exit(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            exit("Usage: predict <tree_filename> <testset_filename>");
        }

        String treeFileName = args[0];
        String testSetFileName = args[1];

        DecisionTree t = null;
        try {
            ObjectInputStream treeObjStream = new ObjectInputStream(new FileInputStream(treeFileName));
            Object obj = treeObjStream.readObject();
            if (obj instanceof DecisionTree) {
                t = (DecisionTree) obj;
            } else {
                throw new ClassNotFoundException("");
            }
        } catch (IOException e) {
            exit("Error opening file " + treeFileName);
        } catch (ClassNotFoundException e) {
            exit("Error reading file " + treeFileName);
        }

        int[][] images_matrix = Images.parse_csv(testSetFileName);
        if (images_matrix == null) {
            exit("Error opening file " + testSetFileName);
        }

        int[][] answers_matrix;
        if (t.getVersion() == 1) {
            answers_matrix = Questions.convertToAnswersMatrixVer1(images_matrix);
        } else {
            answers_matrix = Questions.convertToAnswersMatrixVer2(images_matrix, Questions.BEST_CONFIG);
        }

        TreePredictor.runPrediction(t, answers_matrix);
    }
}
