import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Images {
    /**
     * Receives a csv file in mnist standard format, and returns a 2D array which represents it exactly
     */
    public static int[][] parse_csv(String file_path) {
        List<int[]> listMatrix = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
            String line = br.readLine();
            while (line != null) {
                String[] stringVals = line.split(",");
                int[] intVals = Arrays.stream(stringVals)
                        .mapToInt(Integer::parseInt)
                        .toArray();

                listMatrix.add(intVals);
                line = br.readLine();
            }
        } catch (IOException e) {
            return null;
        }

        int[][] arrayMatrix = new int[listMatrix.size()][listMatrix.get(0).length];

        for (int i=0; i<listMatrix.size(); i++) {
            arrayMatrix[i] = listMatrix.get(i);
        }

        return arrayMatrix;
    }
}
