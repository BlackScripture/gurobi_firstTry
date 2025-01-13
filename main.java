import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.*;

public class main {

    public static void main(String[] args) {

        /*
        List<String> list = new ArrayList<>();
        list.add("test");
        System.out.println(list.size());

         */

        //übergebe Auswahlparameter
        Scanner scanner = new Scanner(System.in);

        System.out.println("Zunächst werden die Eingabeparameter abgefragt.");

        System.out.print("Zur Auswahl stehen folgende Fälle: \n[1] testStuffClass\n[2] OneSolverNxN\n[3] RowSolverNxN\nÜbergebe einen Wert zwischen 1 und 3 zur Auswahl:");
        int caseToRun = scanner.nextInt();

        System.out.print("Gib einen Wert für die Dimensionierung n ein: ");
        int dimension = scanner.nextInt();

        System.out.print("Gib einen Wert für die Anzahl möglicher auftretender Farben ein (Wert zwischen 2 und " + ((dimension*(dimension-1))*2) + "): ");
        int numOfDifColors = scanner.nextInt();

        int fehlerToleranz = 0;
        if ( caseToRun >= 3) {
            System.out.print("Gib einen Wert für die Fehlertoleranz ein: ");
            fehlerToleranz = scanner.nextInt();
        }

        System.out.print("Gib einen Wert für das Zeitlimit in Sekunden zur Lösungsfindung ein:");
        double timeLimit = scanner.nextInt();

        System.out.print("Gib einen Wert für den Seed ein:");
        long seed = scanner.nextInt();

        scanner.close();

        long start = System.nanoTime();

        if (caseToRun == 1){
            testStuffClass methodToRun = new testStuffClass();
            methodToRun.testStuff(dimension, numOfDifColors, timeLimit, seed);
        }else if (caseToRun == 2){
            OneSolverNxN methodToRun = new OneSolverNxN();
            methodToRun.oneSolver(dimension, numOfDifColors, timeLimit, seed);
        }else if (caseToRun == 3){

            System.out.println("Dimensionierung: " + dimension + "x" + dimension);
            System.out.println("Anzahl möglicher verschiedener Farben: " + numOfDifColors);

            // Erstelle ein Objekt der puzzleManager-Klasse
            PuzzleManager puzzlemanager = new PuzzleManager();

            PuzzleAndPieces puzzleandpieces = puzzlemanager.createPuzzleAndPieces(dimension, dimension, seed);

            PuzzleField[] fields = puzzleandpieces.puzzle;
            PuzzlePiece[] pieces = puzzleandpieces.pieces;

            RowSolverNxN methodToRun = new RowSolverNxN();
            List<List<String>> previousPuzzles = new ArrayList<>();
            methodToRun.rowSolver(fields, pieces, dimension, timeLimit, fehlerToleranz, previousPuzzles);
        }

        long end = System.nanoTime();
        long elapsedTime = end - start;
        double elapsedTimeInSecond = (double) elapsedTime / 1_000_000_000;
        System.out.println("Runtime:" + elapsedTimeInSecond + " seconds");

    }
}
