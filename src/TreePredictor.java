import java.math.RoundingMode;
import java.text.DecimalFormat;

public class TreePredictor {
    /**
     * Returns predictions by the tree of the data matrix
     * @param tree a decision tree built for the set of questions C0
     * @param answers_matrix the answers matrix for a set of questions C0
     * @return an array of predictions
     */

    private static int[] makePredictionsArray(DecisionTree tree, int[][] answers_matrix) {
        int[] predictions = new int[answers_matrix.length];
        for(int im=0; im<answers_matrix.length; im++) {
            Node curNode = tree.getRoot();
            while(!(curNode instanceof LeafNode)) {
                InternalNode curInternal = (InternalNode) curNode;
                if(answers_matrix[im][curInternal.getQuestion()] == 0) {
                    curNode = curInternal.getLeft();
                } else {
                    curNode = curInternal.getRight();
                }
            }

            predictions[im] = ((LeafNode) curNode).getDigit();
        }

        return predictions;
    }

    /**
     * Returns the success percentage (as a fracture) of the given tree on a dataset
     */
    public static double getSuccessRate(DecisionTree tree, int[][] answers_matrix) {
        int[] predictions = makePredictionsArray(tree, answers_matrix);
        double success = 0;

        for(int im=0; im<answers_matrix.length; im++) {
            int digit = answers_matrix[im][0];
            if (predictions[im] == digit) {
                success++;
            }
        }

        success = success / (double) predictions.length;
        return success;
    }


    public static void runPrediction(DecisionTree tree, int[][] images_matrix) {
        int[] predictions = makePredictionsArray(tree, images_matrix);
        for(int i=0; i<predictions.length; i++) {
            System.out.println(predictions[i]);
        }
    }


    public static double getConfusionMatrix(DecisionTree tree, int[][] images_matrix) {
        int[] predictions = makePredictionsArray(tree, images_matrix);

        double[] instances = new double[10];
        double[][] confusion_matrix = new double[10][10];

        for(int i=0; i<images_matrix.length; i++) {
            int actualDigit = images_matrix[i][0];

            // count instances of digits in the dataset
            instances[actualDigit]++;

            // count for each digit its prediction
            confusion_matrix[actualDigit][predictions[i]]++;
        }


        DecimalFormat df = new DecimalFormat("#.###");
        df.setRoundingMode(RoundingMode.CEILING);
        for(int i=0; i<10; i++) {
            for(int j=0; j<10; j++) {
                confusion_matrix[i][j] /= instances[i];
                System.out.print(df.format(confusion_matrix[i][j]));
                System.out.print("\t");
            }
            System.out.println();

        }




        System.out.println();
        System.out.println(1.0 - getSuccessRate(tree, images_matrix));
        return getSuccessRate(tree, images_matrix);
    }



}
