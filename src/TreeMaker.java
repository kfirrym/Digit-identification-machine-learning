import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class TreeMaker {
    /**
     * binary check for a number being a power of 2 (or 0)
     */
    private static boolean isPowerOf2(int n) {
        return (n & (n - 1)) == 0;
    }

    public static int[][] splitValidationSet(int indexes, double P) {
        List<Integer> allIndexes = IntStream.rangeClosed(0, indexes - 1).boxed().collect(Collectors.toList());
        Collections.shuffle(allIndexes);

        int validationLength = (int) (P * (double) indexes);
        List<Integer> validation = allIndexes.subList(0, validationLength);
        List<Integer> training = allIndexes.subList(validationLength, indexes);

        Collections.sort(validation);
        Collections.sort(training);

        int[][] sets = new int[2][];
        sets[0] = validation.stream().mapToInt(i->i).toArray();
        sets[1] = training.stream().mapToInt(i->i).toArray();

        return sets;
    }

    /**
     * Get a subset of the given matrix, based on a list of indexes
     */
    private static int[][] fromIndexes(int[][] matrix, int[] indexes) {
        int[][] result = new int[indexes.length][];

        for(int im=0; im<indexes.length; im++) {
            result[im] = matrix[indexes[im]];
        }
        return result;
    }


    /**
     * Run the tree building algorithm
     * @param buildTree: an initialized DecisionTree object
     * @param version: 1/2
     * @param singleTree: whether to return just the last tree (with max(T))
     * @return a list of trees with increasing sizes, one for every T value,
     * which were produced during the algorithm run
     */
    private static List<DecisionTree> runAlgorithm(DecisionTree buildTree, int L, int version, boolean singleTree) {
        int maxT = (int) Math.pow(2, L);
        List<DecisionTree> trees = new ArrayList<>(singleTree ? 1 : L+1);  // list of potential trees (or just the one)
        List<IGStruct> leafsIGs = new ArrayList<>(maxT);  // a list of all the leafs in the tree with their max IGS
        LeafNode root = (LeafNode) buildTree.getRoot();
        leafsIGs.add(root.getBestIG());  // initialized with the root

        // main algorithm: in each iteration, swap the best leaf with an internal node
        for(int i=1; i<=maxT; i++) {
            IGStruct bestIG = new IGStruct(null, -1, -1);

            // find leaf with best information gain
            for(IGStruct l: leafsIGs){
                bestIG.setIfBigger(l);
            }

            leafsIGs.remove(bestIG);  // this is okay because of the overridden equals() in IGStruct

            // swap with an internal node containing the best question
            LeafNode chosenLeaf = bestIG.getLeaf();
            InternalNode newInternal = chosenLeaf.apply(bestIG.getQuestion());
            LeafNode left = (LeafNode) newInternal.getLeft();
            LeafNode right = (LeafNode) newInternal.getRight();

            // add the new leafs to the list
            leafsIGs.add(left.getBestIG());
            leafsIGs.add(right.getBestIG());


            // copy the tree if it's a proper T value (power of 2)
            if (isPowerOf2(i) && !singleTree) {
                trees.add(new DecisionTree(buildTree, i, version));
            }
        }

        if (singleTree) {
            trees.add(new DecisionTree(buildTree, maxT, version));
        }

        return trees;
    }


    public static DecisionTree makeTree(int version, double P, int L, int[][] images_matrix, Map<String, Integer> ver2Config) {
        int[][] answers_matrix;
        if(version == 1){
            answers_matrix = Questions.convertToAnswersMatrixVer1(images_matrix);
        } else {
            answers_matrix = Questions.convertToAnswersMatrixVer2(images_matrix, ver2Config);
        }

        int[][] indexes = splitValidationSet(answers_matrix.length, P);
        int[] validationIndexes = indexes[0];
        int[] trainingIndexes = indexes[1];

        // run the algorithm the first time - to generate a tree for every T value
        DecisionTree validationTree = new DecisionTree(answers_matrix, trainingIndexes);
        List<DecisionTree> trees = runAlgorithm(validationTree, L, version, false);


        // choose the tree with the best success rate using the validation set on the generated trees
        int[][] validationSet = fromIndexes(answers_matrix, validationIndexes);
        double bestRate = -1;
        DecisionTree bestTree = null;
        for(DecisionTree t: trees) {
            double curRate = TreePredictor.getSuccessRate(t, validationSet);

            if(bestRate < curRate) {
                bestRate = curRate;
                bestTree = t;
            }
        }
        validationSet = null;  // free up some memory

        // running algorithm on both the validation and training set with the best sized tree
        int[] allIndexes = splitValidationSet(answers_matrix.length, 0)[1];
        DecisionTree finalTree = new DecisionTree(answers_matrix, allIndexes);
        finalTree = runAlgorithm(finalTree, L, version, true).get(0);  // only one tree in the list


        int error = (int) ((1.0 - bestRate) * 100);
        System.out.println("num: " + String.valueOf(answers_matrix.length));
        System.out.println("error: " + String.valueOf(error));
        System.out.println("size: " + finalTree.getT());
        return finalTree;
    }
}