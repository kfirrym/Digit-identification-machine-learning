import java.util.HashMap;
import java.util.Map;

public class Questions {
    private static final int MAX_BYTE_VAL = 255;
    public static final String ROWS_NUM = "ROWS_NUM";
    public static final String COLS_NUM = "COLS_NUM";
    public static final String BLOCKS_NUM_SQR = "BLOCKS_NUM_SQR";
    public static final String ROWS_LEVELS = "ROWS_LEVELS";
    public static final String COLS_LEVELS = "COLS_LEVELS";
    public static final String BLOCKS_LEVELS = "BLOCKS_LEVELS";

    public static Map<String, Integer> BEST_CONFIG = new HashMap<String, Integer>() {{
        put(Questions.ROWS_NUM, 14);
        put(Questions.ROWS_LEVELS, 15);
        put(Questions.COLS_NUM, 14);
        put(Questions.COLS_LEVELS, 15);
        put(Questions.BLOCKS_NUM_SQR, 28);
        put(Questions.BLOCKS_LEVELS, 1);
    }};


    /**
     * Converts the the given raw data (from the csv) to binary answers for Ver1 questions,
     * in which every column represents an answer to a question (except for 0 for label).
     * It does it completely in place - since there is a question for each pixel
     */
    public static int[][] convertToAnswersMatrixVer1(int[][] data) {
        for(int i=0; i<data.length; i++) {
            for(int j=1; j<data[0].length; j++) {
                data[i][j] = data[i][j] > 128 ? 1 : 0;
            }
        }

        return data;
    }

    /**
     * Returns a row of binary answers for the entries in blackValues x levelsOfBlack.
     * Each question is about keeping beneath a level (threshold) of black levels in that specific item,
     * which can be a row/column/block, and the levels being evenly spaced between 0 and maxBlack
     */
    private static int[] makeAnswersFromBlackValues(int[] blackValues, int maxBlack, int levelsOfBlack) {
        int numOfRows = blackValues.length;
        int[] answers = new int[levelsOfBlack * numOfRows];
        int unit = maxBlack / levelsOfBlack;

        for(int row=0; row<blackValues.length; row++) {
            for(int lvl=0; lvl<levelsOfBlack; lvl++) {  // skipping last level - always true that <= maxBlack
                int blackLevel = unit*lvl;

                //TODO: mayble check range, not just <=
                if(blackValues[row] <= blackLevel) {
                    answers[row*levelsOfBlack + lvl] = 1;
                } else {
                    answers[row*levelsOfBlack + lvl] = 0;
                }
            }
        }

        return answers;

    }

    /**
     * Returns a row of binary answers for a given pixelMap on its rows.
     * @param numOfRows: how many rows should the pixelMap be divided to.
     * @param levelsOfBlack: see at makeAnswersFromBlackValue
     */
    private static int[] askRowQuestion(int[][] pixelMap, int numOfRows, int levelsOfBlack) {
        int dim = pixelMap.length;
        int rowsInLayer = dim/numOfRows;

        int[] blackInRows = new int[numOfRows];
        for(int i=0; i<dim; i++) {
            int rowNum = i / rowsInLayer;
            for(int j=0; j<dim; j++) {
                blackInRows[rowNum] += pixelMap[i][j];
            }
        }

        int maxBlack = rowsInLayer * dim * MAX_BYTE_VAL;
        return makeAnswersFromBlackValues(blackInRows, maxBlack, levelsOfBlack);

    }

    /**
     * Returns a row of binary answers for a given pixelMap on its columns.
     * @param numOfCols: how many columns should the pixelMap be divided to.
     * @param levelsOfBlack: see at makeAnswersFromBlackValue
     */
    private static int[] askColumnQuestion(int[][] pixelMap, int numOfCols, int levelsOfBlack) {
        int dim = pixelMap.length;
        int colsInLayer = dim/numOfCols;

        int[] blackInCols = new int[numOfCols];
            for(int j=0; j<dim; j++) {
            int colNum = j / colsInLayer;
                for(int i=0; i<dim; i++) {
                    blackInCols[colNum] += pixelMap[i][j];
                }
        }

        int maxBlack = colsInLayer * dim * MAX_BYTE_VAL;
        return makeAnswersFromBlackValues(blackInCols, maxBlack, levelsOfBlack);
    }

    /**
     * Returns a row of binary answers for a given pixelMap on its blocks.
     * @param blocksInRow: how many blocks should be in a row, which makes it the square root
     *                     of the total number of blocks in the pixel map
     * @param levelsOfBlack: see at makeAnswersFromBlackValue
     */
    private static int[] askBlockQuestion(int[][] pixelMap, int blocksInRow, int levelsOfBlack) {
        int dim = pixelMap.length;
        int pixelsInBlock = dim / blocksInRow;
        int numOfBlocks = (int) Math.pow(blocksInRow, 2);

        int[] blackInBlock = new int[numOfBlocks];

        // do outer loop with block-size jumps
        for(int i=0; i<dim; i+=pixelsInBlock) {
            for(int j=0; j<dim; j+=pixelsInBlock) {

                // do inner loop, over block-size
                for(int r=0; r<pixelsInBlock; r++) {
                    for(int c=0; c<pixelsInBlock; c++) {
                        int blockId = (i/pixelsInBlock * blocksInRow) + (j/pixelsInBlock);
                        blackInBlock[blockId] += pixelMap[i+r][j+c];
                    }

                }
            }
        }

        int maxBlack = pixelsInBlock * dim * MAX_BYTE_VAL;
        return makeAnswersFromBlackValues(blackInBlock, maxBlack, levelsOfBlack);
    }

    /**
     * Get a 2D representation of the pixels in the image (a pixel map) from an image entry
     */
    private static int[][] make2D(int[] image, int dim) {
        int[][] res = new int[dim][dim];
        for(int i=0; i<image.length-1; i++) {  // shifted due to the label at index 0
            int pix = image[i+1];  // here too
            res[i/dim][i%dim] = pix;
        }

        return res;
    }

    private static int[] joinArrays(int[] a, int[] b, int[] c, int[] d) {
        int[] res = new int[a.length + b.length + c.length + d.length];

        int i=0;
        for(; i<a.length; i++) {
            res[i] = a[i];
        }

        int j=0;
        for(; j<b.length; j++) {
            res[i+j] = b[j];
        }

        int k=0;
        for(; k<c.length; k++) {
            res[i+j+k] = c[k];
        }

        int m=0;
        for(; m<d.length; m++) {
            res[i+j+k+m] = d[m];
        }

        return res;
    }


    /**
     * Converts the the given raw data (from the csv) to binary answers for Ver2 questions.
     * in which every column represents an answer to a question (except for 0 for label)
     * It uses a configuration, which defines how many rows/columns/blocks should the image be divided to,
     * And for each of them how many questions should be created - which are for different levels (threshold)
     * of black of the sum of their pixels
     */
    public static int[][] convertToAnswersMatrixVer2(int[][] data, Map<String, Integer> config) {
        int dim = (int) Math.sqrt(data[0].length - 1);  // 28

        for(int im=0; im<data.length; im++) {
            int[][] pixelMap = make2D(data[im], dim);

            if(config.get(ROWS_NUM) > dim || config.get(COLS_NUM) > dim || config.get(BLOCKS_NUM_SQR) > dim) {
                throw new Error("Num of row/cols/blocks can't exceed: " + String.valueOf(dim));
            }
            if(dim % config.get(ROWS_NUM) != 0 || dim % config.get(COLS_NUM) != 0 || dim % config.get(BLOCKS_NUM_SQR) != 0) {
                throw new Error("num of rows/cols/blocks has to be a divisor of: " + String.valueOf(dim));
            }

            int[] rowAnswers = askRowQuestion(pixelMap, config.get(ROWS_NUM), config.get(ROWS_LEVELS));
            int[] colAnswers = askColumnQuestion(pixelMap, config.get(COLS_NUM), config.get(COLS_LEVELS));
            int[] blockAnswers = askBlockQuestion(pixelMap, config.get(BLOCKS_NUM_SQR), config.get(BLOCKS_LEVELS));

            data[im] = joinArrays(new int[]{data[im][0]}, rowAnswers, colAnswers, blockAnswers);
        }
        return data;

    }
}


