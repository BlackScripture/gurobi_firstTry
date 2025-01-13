import com.gurobi.gurobi.*;

import java.util.*;
import java.util.function.DoubleToIntFunction;

public class RowSolverNxN {

    public void rowSolver(PuzzleField[] fields, PuzzlePiece[] pieces, int dimension, double timeLimit, int fehlerToleranz, List<List<String>> previousPuzzles) {

        //erstelle unlösbares Puzzle indem eine ungerade Anzahl an Farbkanten erzeugt wird
/*
        if (pieces[0].edges[0][0] != 0){
            pieces[0].edges[0][0] = pieces[0].edges[0][0] + 1;
            pieces[0].edges[1][1] = pieces[0].edges[1][1] + 1;
            pieces[0].edges[2][2] = pieces[0].edges[2][2] + 1;
            pieces[0].edges[3][3] = pieces[0].edges[3][3] + 1;
        } else {
            pieces[0].edges[0][2] = pieces[0].edges[0][2] + 1;
            pieces[0].edges[1][3] = pieces[0].edges[1][3] + 1;
            pieces[0].edges[2][0] = pieces[0].edges[2][0] + 1;
            pieces[0].edges[3][1] = pieces[0].edges[3][1] + 1;
        }

 */

        // für Verarbeitung zu PuzzlePieceRow-Objekte
        PuzzlePiece[][] puzzleArray = new PuzzlePiece[dimension][dimension];
        int[][] rotations = new int[dimension][dimension];
        var currentRow = 0;
        var currentFieldInRow = 0;


        /////////////////////////
        /////////////////////////
        //Hier startet Gurobi Modell
        /////////////////////////
        /////////////////////////

        try {
            // Erstelle ein neues Gurobi-Modell
            GRBEnv env = new GRBEnv(true);
            env.set("logFile", "OneSolverNxN.log");
            env.start();
            GRBModel model = new GRBModel(env);

            //model.set(GRB.IntParam.Seed, 1);

            //Array zum speichern aller Variablen
            int variablenAnzahl = (dimension * dimension) * (dimension * dimension) * 4;
            GRBVar[] vars = new GRBVar[variablenAnzahl];

            //globaler Counter für Anzahl der Klauseln
            int clauseCounter = 0;

            // Erstelle Hashmap / Dient zur Abfrage des key (int Wert) um abfrage in vars[key].get.... zu machen bzw. Belegungen zu mappen
            Map<String, Integer> varMap = new HashMap<>();
            int varCount = 0;

            // Variablen definieren: jedes Feld mit jedem Puzzleteil in jeder Rotation
            // füge sie anschließend der Hashmap hinzu
            for (int field_index = 0; field_index < fields.length; field_index++) {            // field_index iteriert durch jedes Feld
                for (int piece_index = 0; piece_index < pieces.length; piece_index++) {     // piece_index iteriert durch jedes Puzzleteil
                    for (int rotation = 0; rotation < 4; rotation++) {                    // rotation iteriert durch jede Rotation eines Puzzleteils
                        String belegungString = "field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation;
                        // mappe nun auf jedes Feld jedes Puzzleteil in jeder Rotation
                        varMap.put(belegungString, varCount);
                        vars[varCount] = model.addVar(0, 1, 0, GRB.BINARY, belegungString); // Hier die Variablen erstellen
                        varCount++;
                    }
                }
            }

            System.out.println("aaaaaaaaaaaaaa" + previousPuzzles.size());
            // Verbiete vorherige Belegungen des Gesamtpuzzles
            for ( int placement = 0; placement < previousPuzzles.size(); placement++){
                GRBLinExpr expr = new GRBLinExpr();
                for ( int fieldPieceStringIndex = 0; fieldPieceStringIndex < previousPuzzles.get(placement).size(); fieldPieceStringIndex++){
                    String belegungString = previousPuzzles.get(placement).get(fieldPieceStringIndex);
                    int varIndex = varMap.get(belegungString);
                    expr.addTerm(1.0, vars[varIndex]);
                }
                double limit = previousPuzzles.get(placement).size() - 1;
                model.addConstr(expr, GRB.LESS_EQUAL, limit, ("clause_" + clauseCounter++));
            }


            // Jedes Feld bekommt genau 1 Puzzleteil zugewiesen
            for (int field_index = 0; field_index < fields.length; field_index++) {

                GRBLinExpr expr = new GRBLinExpr();
                for (int piece_index = 0; piece_index < pieces.length; piece_index++) {
                    for (int rotation = 0; rotation < 4; rotation++) {

                        String belegungString = "field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation;
                        int varIndex = varMap.get(belegungString);
                        expr.addTerm(1.0, vars[varIndex]);
                    }
                }
                model.addConstr(expr, GRB.EQUAL, 1.0, ("clause_" + clauseCounter++));
            }


            // Jedes Puzzleteil genau 1 mal verwendet
            for (int piece_index = 0; piece_index < pieces.length; piece_index++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int field_index = 0; field_index < fields.length; field_index++) {
                    for (int rotation = 0; rotation < 4; rotation++) {
                        String belegungString = "field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation;
                        int varIndex = varMap.get(belegungString);
                        expr.addTerm(1.0, vars[varIndex]);
                    }
                }
                model.addConstr(expr, GRB.EQUAL, 1.0, ("clause_" + clauseCounter++));
            }


            // Kantenfarbe 0 muss immer am Rand liegen
            // verbieten von =0 in Mitte
            for (int field_index = 0; field_index < fields.length; field_index++) {
                int x = fields[field_index].x;
                int y = fields[field_index].y;

                for (int piece_index = 0; piece_index < pieces.length; piece_index++) {
                    for (int rotation = 0; rotation < 4; rotation++) {

                        String belegungString = "field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation;
                        int varIndex = varMap.get(belegungString);

                        // Prüfe, ob das Puzzleteil eine 0-Kante hat und an einem Randfeld liegt
                        if (pieces[piece_index].edges[rotation][0] == 0 && x > 0) { // Oberkante
                            model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                        }
                        if (pieces[piece_index].edges[rotation][1] == 0 && y < ((int) Math.sqrt(fields.length)) - 1) { // rechte Kante
                            model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                        }
                        if (pieces[piece_index].edges[rotation][2] == 0 && x < ((int) Math.sqrt(fields.length)) - 1) { // Unterkante
                            model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                        }
                        if (pieces[piece_index].edges[rotation][3] == 0 && y > 0) { // linke Kante
                            model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                        }
                    }
                }
            }


            // Benachbarte Felder müssen gleiche Kantenfarben haben
            for (int field_index = 0; field_index < fields.length; field_index++) {
                int x = fields[field_index].x;
                int y = fields[field_index].y;

                // Nachbar rechts (x, y+1)
                if (y + 1 < ((int) Math.sqrt(fields.length))) { // Prüfe, ob das rechte Nachbarfeld im Raster liegt
                    for (int piece_index_1 = 0; piece_index_1 < pieces.length; piece_index_1++) {
                        for (int rotation_piece_1 = 0; rotation_piece_1 < 4; rotation_piece_1++) {
                            for (int piece_index_2 = 0; piece_index_2 < pieces.length; piece_index_2++) {
                                for (int rotation_piece_2 = 0; rotation_piece_2 < 4; rotation_piece_2++) {
                                    // Nur wenn zwei verschiedene Puzzleteile an benachbarten Feldern liegen
                                    if (piece_index_1 != piece_index_2) {

                                        if (pieces[piece_index_1].edges[rotation_piece_1][1] != pieces[piece_index_2].edges[rotation_piece_2][3] ||
                                                pieces[piece_index_1].symbols[rotation_piece_1][1] == pieces[piece_index_2].symbols[rotation_piece_2][3]) {

                                            // Die Kantenfarben des rechten und linken Puzzleteils müssen übereinstimmen
                                            int leftVar = varMap.get("field_" + field_index + "_piece_" + pieces[piece_index_1].piece_id + "_rotation_" + rotation_piece_1);
                                            int rightVar = varMap.get("field_" + (field_index + 1) + "_piece_" + pieces[piece_index_2].piece_id + "_rotation_" + rotation_piece_2);

                                            GRBLinExpr expr = new GRBLinExpr();

                                            expr.addTerm(1.0, vars[leftVar]);
                                            expr.addTerm(1.0, vars[rightVar]);

                                            model.addConstr(expr, GRB.LESS_EQUAL, 1, "clause_" + clauseCounter++);

                                        }
                                    }
                                }
                            }
                        }
                    }
                }


/*
                // Nachbar unten (x+1, y)
                if (x + 1 < ((int) Math.sqrt(fields.length))) { // Prüfe, ob das untere Nachbarfeld im Raster liegt
                    for (int piece_index_1 = 0; piece_index_1 < pieces.length; piece_index_1++) {
                        for (int rotation_piece_1 = 0; rotation_piece_1 < 4; rotation_piece_1++) {
                            for (int piexe_index_2 = 0; piexe_index_2 < pieces.length; piexe_index_2++) {
                                for (int rotation_piece_2 = 0; rotation_piece_2 < 4; rotation_piece_2++) {
                                    // Nur wenn zwei verschiedene Puzzleteile an benachbarten Feldern liegen
                                    if (piece_index_1 != piexe_index_2) {

                                        if (pieces[piece_index_1].edges[rotation_piece_1][2] != pieces[piexe_index_2].edges[rotation_piece_2][0] ||
                                                pieces[piece_index_1].symbols[rotation_piece_1][2] == pieces[piexe_index_2].symbols[rotation_piece_2][0]) {

                                            // Die Kantenfarben des rechten und linken Puzzleteils müssen übereinstimmen
                                            int topVar = varMap.get("field_" + field_index + "_piece_" + pieces[piece_index_1].piece_id + "_rotation_" + rotation_piece_1);
                                            int bottomVar = varMap.get("field_" + (field_index + ((int) Math.sqrt(fields.length))) + "_piece_" + pieces[piexe_index_2].piece_id + "_rotation_" + rotation_piece_2);

                                            GRBLinExpr expr = new GRBLinExpr();

                                            expr.addTerm(1.0, vars[topVar]);
                                            expr.addTerm(1.0, vars[bottomVar]);

                                            model.addConstr(expr, GRB.LESS_EQUAL, 1, "clause_" + clauseCounter++);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

 */


            }

/*
            // Zeitlimit setzen (in Sekunden)
            model.set(GRB.DoubleParam.TimeLimit, timeLimit);

 */

            // Optimieren
            model.optimize();

            var placement = new String[fields.length];
            var placementIndex = 0;

            var index = 0;
            var subString1 = "";
            var subString2 = "";
            var subString3 = "";
            var subString4 = "";
            var subString5 = "";
            var subString6 = "";
            var subString7 = "";
            var outputstring = "";
/*
            // bisher beste Lösung, wenn Zeitlimit erreicht
            if (model.get(GRB.IntAttr.Status) == GRB.Status.TIME_LIMIT) {
                System.out.println("Zeitlimit erreicht. Beste gefundene Lösung:");

                double gesamtGewicht = model.get(GRB.DoubleAttr.ObjVal);
                System.out.println("Gesamtgewicht verletzter Klauseln mit definierter Gewichtung: " + gesamtGewicht);

                System.out.println("Zuweisung der Puzzleteile zu den Feldern:");
                for (int field_index = 0; field_index < fields.length; field_index++) {
                    for (int piece_index = 0; piece_index < pieces.length; piece_index++) {
                        for (int rotation = 0; rotation < 4; rotation++) {
                            int belegungKey = varMap.get("field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation);
                            if (vars[belegungKey].get(GRB.DoubleAttr.X) == 1.0) { // Wenn die Variable positiv ist, ist das Puzzleteil zugewiesen

                                // Verarbeitung zu PuzzlePieceRow-Objekte
                                puzzleArray[currentRow][currentFieldInRow] = pieces[piece_index];
                                rotations[currentRow][currentFieldInRow] = rotation;
                                // Prüfe ob nächstes Feld noch in Reihe liegt
                                if (currentFieldInRow == ersterWert - 1) {
                                    currentRow++;
                                    currentFieldInRow = 0;
                                } else {
                                    currentFieldInRow++;
                                }


                                //Ausgabe
                                if (index <= fields.length) {
                                    subString1 = subString1 + "------------------";
                                    subString2 = subString2 + "/       " + pieces[piece_index].edges[rotation][0] + "        /";
                                    subString3 = subString3 + "/       " + pieces[piece_index].symbols[rotation][0] + "        /";
                                    if (pieces[piece_index].piece_id < 10) {
                                        subString4 = subString4 + "/  " + pieces[piece_index].edges[rotation][3] + " " + pieces[piece_index].symbols[rotation][3] + " (" + pieces[piece_index].piece_id + ") " + pieces[piece_index].symbols[rotation][1] + " " + pieces[piece_index].edges[rotation][1] + "   /";
                                    } else {
                                        subString4 = subString4 + "/  " + pieces[piece_index].edges[rotation][3] + " " + pieces[piece_index].symbols[rotation][3] + " (" + pieces[piece_index].piece_id + ") " + pieces[piece_index].symbols[rotation][1] + " " + pieces[piece_index].edges[rotation][1] + "  /";
                                    }
                                    subString5 = subString5 + "/       " + pieces[piece_index].symbols[rotation][2] + "        /";
                                    subString6 = subString6 + "/       " + pieces[piece_index].edges[rotation][2] + "        /";
                                    subString7 = subString7 + "------------------";
                                    index++;
                                    if (index % (int) Math.sqrt(fields.length) == 0) {
                                        outputstring = outputstring + "\n" + subString1 + "\n" + subString2 + "\n" + subString3 + "\n" + subString4 + "\n" + subString5 + "\n" + subString6 + "\n" + subString7;
                                        subString1 = "";
                                        subString2 = "";
                                        subString3 = "";
                                        subString4 = "";
                                        subString5 = "";
                                        subString6 = "";
                                        subString7 = "";
                                    }
                                }
                                //neu

                                System.out.println("Feld (" + fields[field_index].x + ", " + fields[field_index].y + ") -> Puzzleteil " + pieces[piece_index].piece_id + " Rotation " + rotation);
                                placement[placementIndex] = ("field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation);
                                placementIndex++;
                            }
                        }
                    }
                }
            }

 */

            //optimale Lösung ausgeben
            if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                System.out.println("Optimale Lösung gefunden:");

                double gesamtGewicht = model.get(GRB.DoubleAttr.ObjVal);
                System.out.println("Gesamtgewicht verletzter Klauseln mit definierter Gewichtung: " + gesamtGewicht);

                System.out.println("Zuweisung der Puzzleteile zu den Feldern:");
                for (int field_index = 0; field_index < fields.length; field_index++) {
                    for (int piece_index = 0; piece_index < pieces.length; piece_index++) {
                        for (int rotation = 0; rotation < 4; rotation++) {
                            int belegungKey = varMap.get("field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation);
                            if (vars[belegungKey].get(GRB.DoubleAttr.X) == 1.0) { // Wenn die Variable positiv ist, ist das Puzzleteil zugewiesen


                                // Verarbeitung zu PuzzlePieceRow-Objekte
                                puzzleArray[currentRow][currentFieldInRow] = pieces[piece_index];
                                rotations[currentRow][currentFieldInRow] = rotation;
                                // Prüfe ob nächstes Feld noch in Reihe liegt
                                if (currentFieldInRow == dimension - 1) {
                                    currentRow++;
                                    currentFieldInRow = 0;
                                } else {
                                    currentFieldInRow++;
                                }


                                //Ausgabe
                                if (index <= fields.length) {
                                    subString1 = subString1 + "------------------";
                                    subString2 = subString2 + "/       " + pieces[piece_index].edges[rotation][0] + "        /";
                                    subString3 = subString3 + "/       " + pieces[piece_index].symbols[rotation][0] + "        /";
                                    if (pieces[piece_index].piece_id < 10) {
                                        subString4 = subString4 + "/  " + pieces[piece_index].edges[rotation][3] + " " + pieces[piece_index].symbols[rotation][3] + " (" + pieces[piece_index].piece_id + ") " + pieces[piece_index].symbols[rotation][1] + " " + pieces[piece_index].edges[rotation][1] + "   /";
                                    } else {
                                        subString4 = subString4 + "/  " + pieces[piece_index].edges[rotation][3] + " " + pieces[piece_index].symbols[rotation][3] + " (" + pieces[piece_index].piece_id + ") " + pieces[piece_index].symbols[rotation][1] + " " + pieces[piece_index].edges[rotation][1] + "  /";
                                    }
                                    subString5 = subString5 + "/       " + pieces[piece_index].symbols[rotation][2] + "        /";
                                    subString6 = subString6 + "/       " + pieces[piece_index].edges[rotation][2] + "        /";
                                    subString7 = subString7 + "------------------";
                                    index++;
                                    if (index % (int) Math.sqrt(fields.length) == 0) {
                                        outputstring = outputstring + "\n" + subString1 + "\n" + subString2 + "\n" + subString3 + "\n" + subString4 + "\n" + subString5 + "\n" + subString6 + "\n" + subString7;
                                        subString1 = "";
                                        subString2 = "";
                                        subString3 = "";
                                        subString4 = "";
                                        subString5 = "";
                                        subString6 = "";
                                        subString7 = "";
                                    }
                                }
                                //neu

                                System.out.println("Feld (" + fields[field_index].x + ", " + fields[field_index].y + ") -> Puzzleteil " + pieces[piece_index].piece_id + " Rotation " + rotation);
                                placement[placementIndex] = ("field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation);
                                placementIndex++;
                            }
                        }
                    }
                }
            } else {
                System.out.println("Keine optimale Lösung gefunden.");
            }
            //neu
            System.out.println(outputstring);
            //neu

            if (placement[0] != null) {
                PlacementManager placementManager = new PlacementManager();

                //Auswertung der gefundenen Belegung
                //int dimension = (int) Math.sqrt(fields.length);
                int violations = placementManager.countViolations(fields, pieces, dimension, placement);
                System.out.println("Anzahl falscher Paare und außen != grau: " + violations);
            }

            // Modell und Umgebung freigeben
            model.dispose();
            env.dispose();


        } catch (GRBException e) {
            System.out.println("Fehler: " + e.getMessage());
        }

        /////////////////////////
        /////////////////////////
        //Hier endet Gurobi Modell
        /////////////////////////
        /////////////////////////
/*
        //Testausgabe PuzzlePieceRow nach Generierung der Arrays
        for ( int row = 0; row < ersterWert; row++){
            System.out.println();
            for ( int piece = 0; piece < ersterWert; piece++){
                System.out.print("Piece_" + puzzleArray[row][piece].piece_id + "_Rotation_" + rotations[row][piece] + "//");
            }
        }
        System.out.println();

 */


/*
     ////////////////////////////// wahrscheinlich löschen
        // Erstelle nun Liste mit PuzzlePieceRow-Objekten, um Permutationen zu generieren und dynamisch abzuspeichern
        List<List<PuzzlePieceRow>> puzzlePieceRows = new ArrayList<>();
        int initialIDs = 0;
        for ( int row = 0; row < ersterWert; row++){
            PuzzlePieceRow tmpRow = puzzlemanager.createPuzzlePieceRow(initialIDs, puzzleArray[initialIDs], rotations[initialIDs]);
            puzzlePieceRows.add(new ArrayList<>());
            puzzlePieceRows.get(initialIDs).add(tmpRow);
            initialIDs++;
        }

 */

        // Speichere die bisherigen placements im 1xn Format zwischen
        //Liste ist im Format: Reihe<verschiedenePlatzierungenInReihe<PlatzierungsstringFeldPieceRotation>>>
        List<List<PuzzlePieceRow>> puzzlePieceRows = new ArrayList<>();
        int initialIDs = 0;
        for (int row = 0; row < dimension; row++) {
            puzzlePieceRows.add(new ArrayList<>()); //initialisiere Reihen
            // Erstelle ein Objekt der puzzleManager-Klasse
            PuzzleManager puzzlemanager = new PuzzleManager();
            PuzzlePieceRow tmpRow = puzzlemanager.createPuzzlePieceRow(initialIDs, puzzleArray[initialIDs], rotations[initialIDs]);
            puzzlePieceRows.get(row).add(tmpRow); //initialisiere erste Belegung in jeder Reihe
            initialIDs++;
        }
/*
        //Testausgabe PuzzlePieceRow nach Generierung des PuzzlePieceRow-Objekts
        for ( int row = 0; row < ersterWert; row++){
            System.out.println();
            System.out.print("Piece_" + puzzlePieceRows.get(row).get(0).piece_id+ "_AnzahlPermutationen: " + puzzlePieceRows.get(row).size() + "//");
        }
        System.out.println();

 */

        // Speichere die bisherigen placements im 1xn Format zwischen
        //Liste ist im Format: Reihe<verschiedenePlatzierungenInReihe<PlatzierungsstringFeldPieceRotation>>>
        List<List<List<String>>> forbiddenPlacements = new ArrayList<>();
        for (int row = 0; row < dimension; row++) {
            forbiddenPlacements.add(new ArrayList<>()); //initialisiere Reihen
            forbiddenPlacements.get(row).add(new ArrayList<>()); //initialisiere erste verbotene Belegung in jeder Reihe
            for (int pieceOnField = 0; pieceOnField < dimension; pieceOnField++) {
                forbiddenPlacements.get(row).get(0).add("field_" + pieceOnField + "_piece_" + puzzleArray[row][pieceOnField].piece_id + "_rotation_" +
                        rotations[row][pieceOnField]);
            }
        }
/*
        //Testausgabe ob erste forbidden Strings richtig angelegt wurden
        for ( int row = 0; row < forbiddenPlacements.size(); row++){
            for ( int numForbidden = 0; numForbidden < forbiddenPlacements.get(row).size(); numForbidden++){
                for ( int pieceOnField = 0; pieceOnField < forbiddenPlacements.get(row).get(numForbidden).size(); pieceOnField++){
                    System.out.print(forbiddenPlacements.get(row).get(numForbidden).get(pieceOnField) + " //");
                }
                System.out.println();
            }
        }

 */
        /*
        System.out.println(forbiddenPlacements.get(3).size());

        for ( int row = 0; 0 < forbiddenPlacements.size(); row++){
            for ( int forbiddenIndex = 0; forbiddenIndex < forbiddenPlacements.get(row).size(); forbiddenIndex++){
                System.out.println("Forbidden:" + row + " " + forbiddenIndex);
                for ( int string = 0; string < forbiddenPlacements.get(row).get(forbiddenIndex).size(); string++){
                    System.out.println(forbiddenPlacements.get(row).get(forbiddenIndex).get(string));
                }
                System.out.println();
            }
        }

         */

        createRowPermutations(fields, pieces, dimension, timeLimit, 0, puzzlePieceRows, fehlerToleranz, forbiddenPlacements, previousPuzzles);

    }

    public void createRowPermutations(PuzzleField[] fields_old, PuzzlePiece[] pieces_old, int dimension, double timeLimit, int currentRow, List<List<PuzzlePieceRow>> puzzlePieceRows, int fehlerToleranz, List<List<List<String>>> forbiddenPlacements, List<List<String>> previousPuzzles) {

        //setze currentRow + 1 wenn model unsatisfiable
        //resette dann auch verbotene Belegungen

        int puzzlePieceRowID = dimension;


        // So lange potentiell noch Permutationen gefunden werden können, suche weiter
        while (currentRow < dimension) {

            // PuzzleTeile der gegenwärtigen Reihe
            PuzzlePiece[] pieces = puzzlePieceRows.get(currentRow).get(0).pieces;

            // PuzzleFelder für 1xn Reihe
            PuzzleManager puzzlemanager = new PuzzleManager();
            PuzzleField[] fields = puzzlemanager.create1xnField(dimension);
/*
        //Testausgabe ob Felder richtig generiert sind
        for ( int field = 0; field < dimension; field++){
            System.out.println("Feld_x_" + fields[field].x + "_y_" + fields[field].y);
        }

 */
/*
        //Testausgabe ob Kanten richtig übernommen wurden für 1xn Teil
        for (int edge = 0; edge < (pieces.length*2)+2; edge++){
            System.out.println("color_" + puzzlePieceRows.get(1).get(0).edges[edge] + "_symbol_" + puzzlePieceRows.get(1).get(0).symbols[edge]);
        }

 */

            /////////////////////////
            /////////////////////////
            //Hier startet Gurobi Modell
            /////////////////////////
            /////////////////////////

            try {
                // Erstelle ein neues Gurobi-Modell
                GRBEnv env = new GRBEnv(true);
                env.set("logFile", "RowPermutations.log");
                env.start();
                GRBModel model = new GRBModel(env);

                //Array zum speichern aller Variablen
                int variablenAnzahl = (dimension) * (dimension) * 4;
                GRBVar[] vars = new GRBVar[variablenAnzahl];

                //globaler Counter für Anzahl der Klauseln
                int clauseCounter = 0;

                // Erstelle Hashmap / Dient zur Abfrage des key (int Wert) um abfrage in vars[key].get.... zu machen bzw. Belegungen zu mappen
                Map<String, Integer> varMap = new HashMap<>();
                int varCount = 0;

                // Variablen definieren: jedes Feld mit jedem Puzzleteil in jeder Rotation
                // füge sie anschließend der Hashmap hinzu
                for (int field_index = 0; field_index < fields.length; field_index++) {            // field_index iteriert durch jedes Feld
                    for (int piece_index = 0; piece_index < pieces.length; piece_index++) {     // piece_index iteriert durch jedes Puzzleteil
                        for (int rotation = 0; rotation < 4; rotation++) {                    // rotation iteriert durch jede Rotation eines Puzzleteils
                            String belegungString = "field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation;
                            // mappe nun auf jedes Feld jedes Puzzleteil in jeder Rotation
                            varMap.put(belegungString, varCount);
                            vars[varCount] = model.addVar(0, 1, 0, GRB.BINARY, belegungString); // Hier die Variablen erstellen
                            varCount++;
                        }
                    }
                }

                // verbiete bisherige Lösungen
                for (int i = 0; i < forbiddenPlacements.get(currentRow).size(); i++) {

                    GRBLinExpr expr = new GRBLinExpr();
                    for (int varStringIndex = 0; varStringIndex < forbiddenPlacements.get(currentRow).get(i).size(); varStringIndex++) {
                        String belegungString = forbiddenPlacements.get(currentRow).get(i).get(varStringIndex);
                        int varIndex = varMap.get(belegungString);
                        expr.addTerm(1.0, vars[varIndex]);
                    }
                    double limit = dimension - 1;
                    model.addConstr(expr, GRB.LESS_EQUAL, limit, ("clause_" + clauseCounter++));
                }


                // Jedes Feld bekommt genau 1 Puzzleteil zugewiesen
                for (int field_index = 0; field_index < fields.length; field_index++) {

                    GRBLinExpr expr = new GRBLinExpr();
                    for (int piece_index = 0; piece_index < pieces.length; piece_index++) {
                        for (int rotation = 0; rotation < 4; rotation++) {

                            String belegungString = "field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation;
                            int varIndex = varMap.get(belegungString);
                            expr.addTerm(1.0, vars[varIndex]);
                        }
                    }
                    model.addConstr(expr, GRB.EQUAL, 1.0, ("clause_" + clauseCounter++));
                }


                // Jedes Puzzleteil genau 1 mal verwendet
                for (int piece_index = 0; piece_index < pieces.length; piece_index++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int field_index = 0; field_index < fields.length; field_index++) {
                        for (int rotation = 0; rotation < 4; rotation++) {
                            String belegungString = "field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation;
                            int varIndex = varMap.get(belegungString);
                            expr.addTerm(1.0, vars[varIndex]);
                        }
                    }
                    model.addConstr(expr, GRB.EQUAL, 1.0, ("clause_" + clauseCounter++));
                }

                // Kantenfarbe 0 muss immer am Rand liegen
                // verbieten von =0 in Mitte
                for (int field_index = 0; field_index < fields.length; field_index++) {
                    int y = fields[field_index].y;

                    for (int piece_index = 0; piece_index < pieces.length; piece_index++) {
                        for (int rotation = 0; rotation < 4; rotation++) {

                            String belegungString = "field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation;
                            int varIndex = varMap.get(belegungString);

                            // Prüfe, ob das Puzzleteil eine Farbkante hat und an einem Randfeld liegt
                            if (pieces[piece_index].edges[rotation][0] != 0 && currentRow == 0) { // Oberkante
                                model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                            }
                            if (pieces[piece_index].edges[rotation][1] != 0 && y == dimension - 1) { // rechte Kante
                                model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                            }
                            if (pieces[piece_index].edges[rotation][2] != 0 && currentRow == dimension - 1) { // Unterkante
                                model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                            }
                            if (pieces[piece_index].edges[rotation][3] != 0 && y == 0) { // linke Kante
                                model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                            }
                        }
                    }
                }

                // Benachbarte Felder müssen gleiche Kantenfarben haben
                for (int field_index = 0; field_index < fields.length; field_index++) {
                    int y = fields[field_index].y;

                    // Nachbar rechts (x, y+1)
                    if (y + 1 < fields.length) { // Prüfe, ob das rechte Nachbarfeld im Raster liegt
                        for (int piece_index_1 = 0; piece_index_1 < pieces.length; piece_index_1++) {
                            for (int rotation_piece_1 = 0; rotation_piece_1 < 4; rotation_piece_1++) {
                                for (int piece_index_2 = 0; piece_index_2 < pieces.length; piece_index_2++) {
                                    for (int rotation_piece_2 = 0; rotation_piece_2 < 4; rotation_piece_2++) {
                                        // Nur wenn zwei verschiedene Puzzleteile an benachbarten Feldern liegen
                                        if (piece_index_1 != piece_index_2) {

                                            if (pieces[piece_index_1].edges[rotation_piece_1][1] != pieces[piece_index_2].edges[rotation_piece_2][3] ||
                                                    pieces[piece_index_1].symbols[rotation_piece_1][1] == pieces[piece_index_2].symbols[rotation_piece_2][3]) {

                                                // Die Kantenfarben des rechten und linken Puzzleteils müssen übereinstimmen
                                                int leftVar = varMap.get("field_" + field_index + "_piece_" + pieces[piece_index_1].piece_id + "_rotation_" + rotation_piece_1);
                                                int rightVar = varMap.get("field_" + (field_index + 1) + "_piece_" + pieces[piece_index_2].piece_id + "_rotation_" + rotation_piece_2);

                                                GRBLinExpr expr = new GRBLinExpr();

                                                expr.addTerm(1.0, vars[leftVar]);
                                                expr.addTerm(1.0, vars[rightVar]);

                                                model.addConstr(expr, GRB.LESS_EQUAL, 1, "clause_" + clauseCounter++);

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                // Optimieren
                model.optimize();


                var placement = new String[fields.length];
                var placementIndex = 0;

                var index = 0;
                var subString1 = "";
                var subString2 = "";
                var subString3 = "";
                var subString4 = "";
                var subString5 = "";
                var subString6 = "";
                var subString7 = "";
                var outputstring = "";

                List<String> newForbidden = new ArrayList<>();

                List<PuzzlePiece> listPieces = new ArrayList<>();
                List<Integer> listRotations = new ArrayList<>();

                PuzzlePiece[] arrayPieces = new PuzzlePiece[dimension];
                int[] arrayRotations = new int[dimension];
                int arrayIndex = 0;

                //optimale Lösung ausgeben
                if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                    System.out.println("Optimale Lösung gefunden / Permutation für Reihe " + currentRow + ":");

                    double gesamtGewicht = model.get(GRB.DoubleAttr.ObjVal);
                    System.out.println("Gesamtgewicht verletzter Klauseln mit definierter Gewichtung: " + gesamtGewicht);

                    System.out.println("Zuweisung der Puzzleteile zu den Feldern:");
                    for (int field_index = 0; field_index < fields.length; field_index++) {
                        for (int piece_index = 0; piece_index < pieces.length; piece_index++) {
                            for (int rotation = 0; rotation < 4; rotation++) {
                                int belegungKey = varMap.get("field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation);
                                if (vars[belegungKey].get(GRB.DoubleAttr.X) == 1.0) { // Wenn die Variable positiv ist, ist das Puzzleteil zugewiesen

                                    newForbidden.add("field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation);
                                    //listPieces.add(pieces[varMap.get("field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation)]);
                                    //listRotations.add(rotation);
                                    arrayPieces[arrayIndex] = pieces[piece_index];
                                    arrayRotations[arrayIndex] = rotation;
                                    arrayIndex++;

                                    //neu
                                    if (index <= fields.length) {
                                        subString1 = subString1 + "------------------";
                                        subString2 = subString2 + "/       " + pieces[piece_index].edges[rotation][0] + "        /";
                                        subString3 = subString3 + "/       " + pieces[piece_index].symbols[rotation][0] + "        /";
                                        if (pieces[piece_index].piece_id < 10) {
                                            subString4 = subString4 + "/  " + pieces[piece_index].edges[rotation][3] + " " + pieces[piece_index].symbols[rotation][3] + " (" + pieces[piece_index].piece_id + ") " + pieces[piece_index].symbols[rotation][1] + " " + pieces[piece_index].edges[rotation][1] + "   /";
                                        } else {
                                            subString4 = subString4 + "/  " + pieces[piece_index].edges[rotation][3] + " " + pieces[piece_index].symbols[rotation][3] + " (" + pieces[piece_index].piece_id + ") " + pieces[piece_index].symbols[rotation][1] + " " + pieces[piece_index].edges[rotation][1] + "  /";
                                        }
                                        subString5 = subString5 + "/       " + pieces[piece_index].symbols[rotation][2] + "        /";
                                        subString6 = subString6 + "/       " + pieces[piece_index].edges[rotation][2] + "        /";
                                        subString7 = subString7 + "------------------";
                                        index++;

                                        if (index == fields.length) {
                                            outputstring = outputstring + "\n" + subString1 + "\n" + subString2 + "\n" + subString3 + "\n" + subString4 + "\n" + subString5 + "\n" + subString6 + "\n" + subString7;
                                            subString1 = "";
                                            subString2 = "";
                                            subString3 = "";
                                            subString4 = "";
                                            subString5 = "";
                                            subString6 = "";
                                            subString7 = "";
                                        }
                                    }
                                    //neu

                                    System.out.println("Feld (" + fields[field_index].x + ", " + fields[field_index].y + ") -> Puzzleteil " + pieces[piece_index].piece_id + " Rotation " + rotation);
                                    placement[placementIndex] = ("field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation);
                                    placementIndex++;
                                }
                            }
                        }
                    }

                    forbiddenPlacements.get(currentRow).add(newForbidden);

                    PuzzleManager manager = new PuzzleManager();
                    PuzzlePieceRow tmpRow = manager.createPuzzlePieceRow(puzzlePieceRowID, arrayPieces, arrayRotations);
                    puzzlePieceRows.get(currentRow).add(tmpRow);
                    puzzlePieceRowID++;
                    arrayIndex = 0;

                } else {
                    System.out.println("Keine optimale Lösung gefunden.");
                    System.out.println("Keine weitere Permutation gefunden für Reihe " + currentRow + " gefunden.");
                    forbiddenPlacements.get(currentRow).add(new ArrayList<>());
                    currentRow++;
                }

                //neu
                System.out.println(outputstring);
                //neu

                // Modell und Umgebung freigeben
                model.dispose();
                env.dispose();

            } catch (GRBException e) {
                System.out.println("Fehler: " + e.getMessage());
            }// Keine weiteren Permutationen können mehr gefunden werden


        }

/*
        System.out.println(puzzlePieceRows.size());
        System.out.println(puzzlePieceRows.get(0).get(0).piece_id);
        for (int i = 0; i < puzzlePieceRows.size(); i++){
            System.out.println(puzzlePieceRows.get(i).size());
        }

 */


        for (int i = 0; i < puzzlePieceRows.size(); i++) {
            System.out.println("Anzahl Permutationen in Reihe " + i + " = " + puzzlePieceRows.get(i).size());
            for (int j = 0; j < puzzlePieceRows.get(i).size(); j++) {
                System.out.println("ID der Reihe: " + puzzlePieceRows.get(i).get(j).piece_id);
                String ids = "";
                String rotas = "r:";
                for (int k = 0; k < puzzlePieceRows.get(i).get(j).pieces.length; k++) {
                    ids = ids + puzzlePieceRows.get(i).get(j).pieces[k].piece_id + " ";
                    rotas = rotas + puzzlePieceRows.get(i).get(j).rotations[k] + " ";
                    //System.out.print(puzzlePieceRows.get(i).get(j).pieces[k].piece_id + " r:" + puzzlePieceRows.get(i).get(j).rotations[k] + " ");
                }
                System.out.print(ids + " ");
                System.out.println(rotas);
            }
        }
        /*
        for ( int i = 0; i < puzzlePieceRows.get(0).get(0).edges.length; i++){
            System.out.println(puzzlePieceRows.get(1).get(0).edges[i]);
        }

         */

/*
        for ( int row = 0; row < forbiddenPlacements.size(); row++){
            for ( int forbiddenIndex = 0; forbiddenIndex < forbiddenPlacements.get(row).size(); forbiddenIndex++){
                System.out.println("Forbidden:" + row + " " + forbiddenIndex);
                for ( int string = 0; string < forbiddenPlacements.get(row).get(forbiddenIndex).size(); string++){
                    System.out.println(forbiddenPlacements.get(row).get(forbiddenIndex).get(string));
                }
                System.out.println();
            }
        }

 */


        combinePermutations(fields_old, pieces_old, dimension, timeLimit, puzzlePieceRows, fehlerToleranz, previousPuzzles);


    }


    public void combinePermutations(PuzzleField[] fields_old, PuzzlePiece[] pieces_old, int dimension, double timeLimit, List<List<PuzzlePieceRow>> puzzlePieceRows, int fehlerToleranz, List<List<String>> previousPuzzles) {

        // Erstelle ein Objekt der puzzleManager-Klasse
        PuzzleManager puzzlemanager = new PuzzleManager();
        PuzzleField[] fields = puzzlemanager.createnx1Field(dimension);

        double fehlerZahl = 0;

        /////////////////////////
        /////////////////////////
        //Hier startet Gurobi Modell
        /////////////////////////
        /////////////////////////

        try {
            // Erstelle ein neues Gurobi-Modell
            GRBEnv env = new GRBEnv(true);
            env.set("logFile", "OneSolverNxN.log");
            env.start();
            GRBModel model = new GRBModel(env);

            int howManyVars = 0;
            for (int i = 0; i < puzzlePieceRows.size(); i++) {
                for (int j = 0; j < puzzlePieceRows.get(i).size(); j++) {
                    howManyVars++;
                    //System.out.println(howManyVars);
                }
            }
            howManyVars = howManyVars*dimension; //jedes rowPiece wird auf jedem Feld gemapt

            //Array zum speichern aller Variablen
            GRBVar[] vars = new GRBVar[howManyVars];

            //globaler Counter für Anzahl der Klauseln
            int clauseCounter = 0;

            // Erstelle Hashmap / Dient zur Abfrage des key (int Wert) um abfrage in vars[key].get.... zu machen bzw. Belegungen zu mappen
            Map<String, Integer> varMap = new HashMap<>();
            int varCount = 0;

            // Variablen definieren: jedes Feld mit jedem Puzzleteil in jeder Rotation
            // füge sie anschließend der Hashmap hinzu
            for (int field_index = 0; field_index < fields.length; field_index++) {            // field_index iteriert durch jedes Feld
                for ( int row = 0; row < puzzlePieceRows.size(); row++){
                    for ( int piece = 0; piece < puzzlePieceRows.get(row).size(); piece++){
                        String belegungString = "field_" + field_index + "_piece_" + puzzlePieceRows.get(row).get(piece).piece_id;
                        // mappe nun auf jedes Feld jedes Puzzleteil in jeder Rotation
                        varMap.put(belegungString, varCount);
                        vars[varCount] = model.addVar(0, 1, 0, GRB.BINARY, belegungString); // Hier die Variablen erstellen
                        varCount++;
                    }
                }
            }
            System.out.println("Anzahl Variablen: " + varCount);



            // Jedes Feld bekommt genau 1 Puzzleteil zugewiesen
            for (int field_index = 0; field_index < fields.length; field_index++) {

                GRBLinExpr expr = new GRBLinExpr();
                for ( int row = 0; row < puzzlePieceRows.size(); row++){
                    for ( int piece = 0; piece < puzzlePieceRows.get(row).size(); piece++){
                        String belegungString = "field_" + field_index + "_piece_" + puzzlePieceRows.get(row).get(piece).piece_id;
                        int varIndex = varMap.get(belegungString);
                        expr.addTerm(1.0, vars[varIndex]);
                    }
                }
                model.addConstr(expr, GRB.EQUAL, 1.0, ("clause_" + clauseCounter++));
            }
            System.out.println("Anzahl Klauseln für genau 1 Teil pro Feld: " + clauseCounter);



            // Aus jeder Permutationsgruppe (eine row bildet eine gruppe) darf nur jeweils ein Teil verwendet werden
            for ( int row = 0; row < puzzlePieceRows.size(); row++){

                GRBLinExpr expr = new GRBLinExpr();
                for ( int piece = 0; piece < puzzlePieceRows.get(row).size(); piece++){
                    for ( int field_index = 0; field_index < fields.length; field_index++){
                        String belegungString = "field_" + field_index + "_piece_" + puzzlePieceRows.get(row).get(piece).piece_id;
                        int varIndex = varMap.get(belegungString);
                        expr.addTerm(1.0, vars[varIndex]);
                    }
                }
                model.addConstr(expr, GRB.EQUAL, 1.0, ("clause_" + clauseCounter++));
            }

            // Verbiete Randteile in der Mitte
            for ( int field_index = 0; field_index < fields.length; field_index++){
                for ( int row = 0; row < puzzlePieceRows.size(); row++){
                    for (int piece = 0; piece < puzzlePieceRows.get(row).size(); piece++){

                        if ( puzzlePieceRows.get(row).get(piece).edges[0] == 0 && field_index > 0){
                            String belegungString = "field_" + field_index + "_piece_" + puzzlePieceRows.get(0).get(piece).piece_id;
                            int varIndex = varMap.get(belegungString);
                            model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                        }else if( puzzlePieceRows.get(row).get(piece).edges[dimension+1] == 0 && field_index < dimension-1){
                            String belegungString = "field_" + field_index + "_piece_" + puzzlePieceRows.get(row).get(piece).piece_id;
                            int varIndex = varMap.get(belegungString);
                            model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                        }
                    }
                }
            }


            /*
            // Verbiete Teile der Oberkant überall sonst
            for ( int piece = 0; piece < puzzlePieceRows.get(0).size(); piece++){
                for ( int field = 1; field < fields.length; field++){
                    String belegungString = "field_" + field + "_piece_" + puzzlePieceRows.get(0).get(piece).piece_id;
                    int varIndex = varMap.get(belegungString);
                    model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                }
            }

            // Verbiete Teile der Unterkante überall sonst
            for ( int piece = 0; piece < puzzlePieceRows.get(puzzlePieceRows.size()-1).size(); piece++){
                for ( int field = 0; field < fields.length-1; field++){
                    String belegungString = "field_" + field + "_piece_" + puzzlePieceRows.get(puzzlePieceRows.size()-1).get(piece).piece_id;
                    int varIndex = varMap.get(belegungString);
                    model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                }
            }

             */


            int helperVariableCounter = 0;
            GRBLinExpr objective = new GRBLinExpr();

            // Benachbarte Felder müssen gleiche Kantenfarben haben
            for (int field_index = 0; field_index < fields.length-1; field_index++) {
                    for ( int row1 = 0; row1 < puzzlePieceRows.size(); row1++){
                        for ( int piece1 = 0; piece1 < puzzlePieceRows.get(row1).size(); piece1++){
                            for ( int row2 = 0; row2 < puzzlePieceRows.size(); row2++){
                                if ( row1 != row2){
                                    for ( int piece2 = 0; piece2 < puzzlePieceRows.get(row2).size(); piece2++){

                                        int countMismatches = 0;
                                        int topVar = varMap.get("field_" + field_index + "_piece_" + puzzlePieceRows.get(row1).get(piece1).piece_id);
                                        PuzzlePieceRow topRow = puzzlePieceRows.get(row1).get(piece1);

                                        int bottomVar = varMap.get("field_" + (field_index+1) + "_piece_" + puzzlePieceRows.get(row2).get(piece2).piece_id);
                                        PuzzlePieceRow bottomRow = puzzlePieceRows.get(row2).get(piece2);

                                        int howManyEdges = (dimension*2)+2;

                                        for ( int i = 0; i < dimension; i++){
                                            if ( topRow.edges[(howManyEdges-2)-i] != bottomRow.edges[i]){
                                                //System.out.println(topRow.edges[(howManyEdges-2)-i]  + " " + bottomRow.edges[i]);
                                                countMismatches++;
                                            }
                                            if (topRow.symbols[(howManyEdges-2)-i] == bottomRow.symbols[i]){
                                                //System.out.println(topRow.symbols[(howManyEdges-2)-i] + " " + bottomRow.symbols[i]);
                                                countMismatches++;
                                            }
                                        }

                                        if (countMismatches > 0){

                                            GRBLinExpr expr = new GRBLinExpr();

                                            // Hilfsvariable für die weiche Klausel
                                            GRBVar tmpHelperVar = model.addVar(0, 1, 0, GRB.BINARY, ("softclause_" + helperVariableCounter));

                                            expr.addTerm(1.0, vars[topVar]);
                                            expr.addTerm(1.0, vars[bottomVar]);
                                            expr.addTerm(-1.0, tmpHelperVar);
                                            model.addConstr(expr, GRB.LESS_EQUAL, 1, "softclause_" + helperVariableCounter++);

                                            objective.addTerm(countMismatches, tmpHelperVar);

                                        }




                                    }
                                }
                            }
                        }
                    }
            }


            model.setObjective(objective, GRB.MINIMIZE);

            // Optimieren
            model.optimize();

            if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                System.out.println("satisfiable");

                for (int field_index = 0; field_index < fields.length; field_index++) {
                    for ( int row = 0; row < puzzlePieceRows.size(); row++){
                        for ( int piece = 0; piece < puzzlePieceRows.get(row).size(); piece++){
                            String belegungString = "field_" + field_index + "_piece_" + puzzlePieceRows.get(row).get(piece).piece_id;
                            int belegungKey = varMap.get(belegungString);
                            if (vars[belegungKey].get(GRB.DoubleAttr.X) == 1.0) { // Wenn die Variable positiv ist, ist das Puzzleteil zugewiesen
                                System.out.println(belegungString);
                            }

                        }
                    }
                }

                double gesamtGewicht  = model.get(GRB.DoubleAttr.ObjVal);
                System.out.println("Gesamtgewicht verletzter Klauseln mit definierter Gewichtung: " + gesamtGewicht);
                fehlerZahl = fehlerZahl + (int) gesamtGewicht;

            }else{
                System.out.println("unsatisfiable");
            }



            // Modell und Umgebung freigeben
            model.dispose();
            env.dispose();



        } catch (GRBException e) {
            System.out.println("Fehler: " + e.getMessage());
        }

        /////////////////////////
        /////////////////////////
        //Hier endet Gurobi Modell
        /////////////////////////
        /////////////////////////

        if ( fehlerZahl > fehlerToleranz) {
            List<String> newForbiddenWholePuzzle = new ArrayList<>();
            int field_index = 0;
            for ( int row = 0; row < puzzlePieceRows.size(); row++){
                for ( int piece = 0; piece < dimension; piece++){
                    newForbiddenWholePuzzle.add("field_" + field_index + "_piece_" + puzzlePieceRows.get(row).get(0).pieces[piece].piece_id + "_rotation_" + puzzlePieceRows.get(row).get(0).rotations[piece]);
                }
            }
            previousPuzzles.add(newForbiddenWholePuzzle);
            rowSolver(fields_old, pieces_old, dimension, timeLimit, fehlerToleranz, previousPuzzles);
        }





    }
}

