import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.stream.DoubleStream;

public class DecisionTree implements Serializable {
    private int T;
    private int version;
    int[][] matrix;
    private Node root;

    /**
     * Represents a decision tree
     * @param matrix: an dataset in the format of an answers matrix,
     *                which means that it's columns are the answers to the questions
     * @param indexes - a list containing all the images that reach the root node,
     *                 Usually all of them (indexes = {0, ..., matrix.length - 1}
     */
    DecisionTree(int[][] matrix, int[] indexes) {
        this.matrix = matrix;
        this.root = new LeafNode(this, null, indexes, 0);
    }

    /**
     * Makes a copy of the tree.
     * Copies only the data relevant for prediction,
     * without the learning part
     */
    DecisionTree(DecisionTree other) {
        matrix = null;
        root = other.root.copy();
    }

    DecisionTree(DecisionTree other, int T, int version) {
        this(other);
        this.T = T;
        this.version = version;
    }

    public void setRoot(Node newRoot) {
        root = newRoot;
    }

    public Node getRoot() {
        return root;
    }

    public int getT() {
        return T;
    }

    public int getVersion() {
        return version;
    }
}

abstract class Node implements Serializable {
    protected DecisionTree tree;
    protected InternalNode parent;
    protected int[] indexes;


    /**
     * A basic node representation
     * @param tree - the original tree object
     * @param parent - the parent node
     * @param indexes - a list containing all the images that reach this node,
     *                represented by their index in tree.matrix
     */
    protected Node(DecisionTree tree, InternalNode parent, int[] indexes) {
        this.tree = tree;
        this.parent = parent;
        this.indexes = indexes;
    }

    /**
     * All copy constructors ignore this values, since they
     * are relevant only to the tree building algorithm,
     * and copying is only for prediction
     */
    protected Node(Node other){
        tree = null;
        parent = null;
        indexes = null;
    }

    protected abstract Node copy();

    protected Node(){}  // to allow copy constructors of deriving classes


    /**
     * Returns two lists, each of which contains the Ni values for each of the digits,
     * one for the left child node - Ni(La), and one for the right - Ni(Lb),
     * based on the given question id, and of course - the images that reached this node (this.indexes)
     */
    public double[][] getSplitNiArrays(int question) {
        double[] leftNiArray = new double[10];
        double[] rightNiArray = new double[10];

        for(int im=0; im<indexes.length; im++){
            int index = indexes[im];  // the actual index in tree.matrix
            if (tree.matrix[index][question] == 0) {
                leftNiArray[tree.matrix[index][0]]++;  // left is false
            } else {
                rightNiArray[tree.matrix[index][0]]++;  // right is true
            }
        }

        return new double[][] {leftNiArray, rightNiArray};
    }

}

class InternalNode extends Node {
    private int question;
    private Node left;
    private Node right;

    /**
     * An internal node, which is initialized with a question, and creates it's own child nodes
     * based on that question (and the indexes)
     * @param question - a question id, (from 1 to 785 in ver1 - column 0 is for the label)
     */
    protected InternalNode(DecisionTree tree, InternalNode parent, int[] indexes, int question) {
        super(tree, parent, indexes);
        this.question = question;
        left = null;
        right = null;

        apply(question);
    }

    protected InternalNode(InternalNode other) {
        super(other);
        question = other.question;
        left = other.left.copy();
        right = other.right.copy();
    }

    protected Node copy() {
        return new InternalNode(this);
    }

    public void swapChild(Node oldChild, Node newChild){
        if(this.left == oldChild){
            left = newChild;
        } else if (this.right == oldChild) {
            right = newChild;
        } else {
            throw new NoSuchElementException("Cannot swap child, since oldChild isn't a child of this node");
        }
    }

    public int getQuestion() {
        return question;
    }

    public Node getLeft() {
        return left;
    }

    public Node getRight() {
        return right;
    }

    /**
     * Returns the most common digit in the left and right child nodes, based on their Ni(L) values
     */
    private int[] getMostCommonDigits(double[] leftNiArray, double[] rightNiArray) {
        int leftDigit = -1;
        double leftLargest = -1;
        int rightDigit = -1;
        double rightLargest = -1;

        for(int i=0; i<10; i++) {
            if (leftLargest < leftNiArray[i]) {
                leftDigit = i;
                leftLargest = leftNiArray[i];
            }
            if (rightLargest < rightNiArray[i]) {
                rightDigit = i;
                rightLargest = rightNiArray[i];
            }
        }
        return new int[] {leftDigit, rightDigit};
    }

    /**
     * Splits the indexes of this node (of the images that reach it)
     * to left and right, based on the given question
     */
    public int[][] getSplitIndexes(int question, int leftNL, int rightNL) {
        int[] leftIndexes = new int[leftNL];
        int leftCounter = 0;
        int[] rightIndexes = new int[rightNL];
        int rightCounter = 0;

        for(int im=0; im<indexes.length; im++){
            int index = indexes[im];  // the actual index in tree.matrix
            if (tree.matrix[index][question] == 0) {
                leftIndexes[leftCounter++] = index;  // left is false
            } else {
                rightIndexes[rightCounter++] = index;  // right is true
            }
        }

        return new int[][] {leftIndexes, rightIndexes};
    }

    /**
     * Set node question to the given one, and create child leafs with their corresponding
     * indexes and digits, based on the application of that question
     * @param question - the question id
     */
    public void apply(int question) {
        double[][] NiArrays = getSplitNiArrays(question);
        double[] leftNiArray = NiArrays[0];
        double[] rightNiArray = NiArrays[1];
        int leftNL = (int) DoubleStream.of(leftNiArray).sum();
        int rightNL = (int) DoubleStream.of(rightNiArray).sum();

        int[] commonDigits = getMostCommonDigits(leftNiArray, rightNiArray);
        int[][] indexes = getSplitIndexes(question, leftNL, rightNL);


        left = new LeafNode(tree, this, indexes[0], commonDigits[0]);
        right = new LeafNode(tree, this, indexes[1], commonDigits[1]);
    }

}

class LeafNode extends Node {
    private int digit;

    /**
     * A leaf, which holds the most common digit in the images that reach it (based in it's indexes)
     * @param digit - a digit from 0 to 9
     */
    protected LeafNode(DecisionTree tree, InternalNode parent, int[] indexes, int digit) {
        super(tree, parent, indexes);
        this.digit = digit;
    }

    protected LeafNode(LeafNode other) {
        super(other);
        digit = other.digit;
    }

    protected Node copy() {
        return new LeafNode(this);
    }

    public int getDigit() {
        return digit;
    }

    private double log2(double n)
    {
        return Math.log(n) / Math.log(2);
    }


    /**
     * Calculates current leaf's entropy - H(L) from the instructions
     * @return - the entropy
     */
    private double HL() {
        double[] NiArray = new double[10];
        for(int im=0; im<indexes.length; im++){
            NiArray[tree.matrix[indexes[im]][0]]++;
        }

        double NL = DoubleStream.of(NiArray).sum();
        return HL(NiArray, NL);
    }

    /**
     * Calculates custom leaf entropy - H(L) from the instructions
     * @param NiArray - an array of size 10, containing the number of instances each label got
     * @param NL - the sum of those instances
     * @return - the entropy
     */
    private double HL(double[] NiArray, double NL) {
        double entropy = 0;

        if(NL == 0) {
            return entropy;
        }

        for(int i=0; i<10; i++) {
            if (NiArray[i] != 0) {
                entropy += (NiArray[i] / NL) * log2(NL / NiArray[i]);
            }
        }

        return entropy;
    }


    /**
     * Calculates custom question's entropy - H(X) from the instructions
     * @param q - the question id
     * @return the entropy
     */
    private double HX(int q) {
        double[][] NiArrays = getSplitNiArrays(q);
        double[] leftNiArray = NiArrays[0];
        double[] rightNiArray = NiArrays[1];

        double leftNL = DoubleStream.of(leftNiArray).sum();
        double rightNL = DoubleStream.of(rightNiArray).sum();
        int NL = indexes.length;

        return (leftNL / NL) * HL(leftNiArray, leftNL) + (rightNL / NL) * HL(rightNiArray, rightNL);
    }

    /**
     * Iterates all questions, calculates the information gain for each of them,
     * and returns the best question along with it's info gain, already factored by N(L) of the leaf!
     *
     * This is probably the heaviest operation in the whole algorithm.
     */
    public IGStruct getBestIG(){
        double _HL = HL();
        if (_HL == 0) {
            return new IGStruct(this, 0, 1);  // choose question 1 for debug-ability
        }

        IGStruct bestIG = new IGStruct(null, -1, -1);  // this should be overwritten immediately
        // iterate all questions
        for(int q=1; q<tree.matrix[0].length; q++){  // start from 1 to skip labels column
            double currentIG = _HL - HX(q);

            bestIG.setIfBigger(this, currentIG, q);
        }

        return bestIG.factored();
    }

    /**
     * Transform the leaf into an internal node, set to the given question
     * @param question - the question id
     * @return - the new internal node
     */
    public InternalNode apply(int question){
        InternalNode newNode = new InternalNode(tree, parent, indexes, question);
        if (parent == null) {  // means this is the initial root leaf
            tree.setRoot(newNode);
        } else {
            parent.swapChild(this, newNode);
        }
        return newNode;
    }
}


class IGStruct {
    private LeafNode leaf;
    private double IG;
    private int question;

    IGStruct(LeafNode leaf, double ig, int question) {
        set(leaf, ig, question);
    }

    /**
     * Implemented to support List.remove()
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof IGStruct) {
            return this.equals((IGStruct) other);
        }
        return false;
    }

    public boolean equals(IGStruct other) {
        return (leaf == other.leaf && IG == other.IG && question == other.question);
    }

    public void set(LeafNode l, double ig, int q) {
        leaf = l;
        IG = ig;
        question = q;
    }

    public void setIfBigger(IGStruct other) {
        if (this.IG < other.getIG()) {
            this.set(other.leaf, other.IG, other.question);
        }
    }

    public void setIfBigger(LeafNode l, double ig, int question) {
        setIfBigger(new IGStruct(l, ig, question));
    }

    public void factorBy(double NL){
        IG *= NL;
    }

    /**
     * NOTICE: this function first sets the IG, then returns the same object
     */
    public IGStruct factored(double NL) {
        factorBy(NL);
        return this;
    }

    public IGStruct factored() {
        return factored(leaf.indexes.length);
    }

    public double getIG() {
        return IG;
    }

    public int getQuestion() {
        return question;
    }
     public LeafNode getLeaf(){
         return leaf;
     }
}
