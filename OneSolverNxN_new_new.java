import com.gurobi.gurobi.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class OneSolverNxN_new_new {

    public static void main(String[] args) {

        //Gib Dimensionierung und Anzahl der Farben ein
        Scanner scanner = new Scanner(System.in);

        System.out.print("Gib einen Wert für die Dimensionierung ein: ");
        int ersterWert = scanner.nextInt();

        System.out.print("Gib einen Wert für die Anzahl möglicher auftretender Farben ein: ");
        int zweiterWert = scanner.nextInt();

        scanner.close();

        System.out.println("Dimensionierung: " + ersterWert + "x" + ersterWert);
        System.out.println("Anzahl möglicher verschiedener Farben: " + zweiterWert);

        // Erstelle ein Objekt der puzzleManager-Klasse
        PuzzleManager puzzlemanager = new PuzzleManager();

        PuzzleAndPieces puzzleandpieces = puzzlemanager.createPuzzleAndPieces(ersterWert, zweiterWert);

        PuzzleField[] fields = puzzleandpieces.puzzle;
        PuzzlePiece[] pieces = puzzleandpieces.pieces;

        //erstelle unlösbares Puzzle indem eine ungerade Anzahl an Farbkanten erzeugt wird

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






        long start = System.nanoTime();

        /////////////////////////
        /////////////////////////
        //Hier startet Gurobi Modell
        /////////////////////////
        /////////////////////////

        try {
            // Erstelle ein neues Gurobi-Modell
            GRBEnv env = new GRBEnv(true);
            env.set("logFile", "OneSolverNxN_new_new.log");
            env.start();
            GRBModel model = new GRBModel(env);

            //Array zum speichern aller Variablen
            int variablenAnzahl = (ersterWert*ersterWert)*(ersterWert*ersterWert)*4;
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
                    for ( int rotation = 0; rotation < 4; rotation++ ) {                    // rotation iteriert durch jede Rotation eines Puzzleteils
                        String belegungString = "field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation;
                        // mappe nun auf jedes Feld jedes Puzzleteil in jeder Rotation
                        varMap.put(belegungString, varCount);
                        vars[varCount] = model.addVar(0, 1, 0, GRB.BINARY, belegungString); // Hier die Variablen erstellen
                        varCount++;
                    }
                }
            }


            // Jedes Feld bekommt mindestens ein Puzzleteil zugewiesen
            for (int field_index = 0; field_index < fields.length; field_index++) {

                GRBLinExpr expr = new GRBLinExpr();
                for (int piece_index = 0; piece_index < pieces.length; piece_index++) {
                    for (int rotation = 0; rotation < 4; rotation++) {

                        String belegungString = "field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation;
                        int varIndex = varMap.get(belegungString);
                        expr.addTerm(1.0, vars[varIndex]);
                    }
                }
                model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, ("clause_" + clauseCounter++));
            }

            // Jedes Feld bekommt höchstens ein Puzzleteil zugewiesen
            for (int field_index = 0; field_index < fields.length; field_index++) {
                // Ausschluss von Mehrfachzuweisungen pro Feld von verschiedenen Puzzleteilen
                for (int piece_index_1 = 0; piece_index_1 < pieces.length; piece_index_1++) {
                    for (int piece_index_2 = 0; piece_index_2 < pieces.length; piece_index_2++) {
                        for (int rotation_piece_1 = 0; rotation_piece_1 < 4; rotation_piece_1++) {
                            for (int rotation_piece_2 = 0; rotation_piece_2 < 4; rotation_piece_2++) {
                                if ( piece_index_1 != piece_index_2 ) {

                                    GRBLinExpr expr = new GRBLinExpr();
                                    String belegungString1 = "field_" + field_index + "_piece_" + pieces[piece_index_1].piece_id + "_rotation_" + rotation_piece_1;
                                    String belegungString2 = "field_" + field_index + "_piece_" + pieces[piece_index_2].piece_id + "_rotation_" + rotation_piece_2;
                                    int varIndex1 = varMap.get(belegungString1);
                                    int varIndex2 = varMap.get(belegungString2);
                                    expr.addTerm(1.0, vars[varIndex1]);
                                    expr.addTerm(1.0, vars[varIndex2]);
                                    model.addConstr(expr, GRB.LESS_EQUAL, 1.0, ("clause_" + clauseCounter++));
                                }
                            }
                        }
                    }
                }
            }


            // Jedes Puzzleteil wird mindestens 1 mal verwendet
            for ( int piece_index = 0; piece_index < pieces.length; piece_index++){
                GRBLinExpr expr = new GRBLinExpr();
                for ( int field_index = 0; field_index < fields.length; field_index++ ){
                    for ( int rotation = 0; rotation < 4; rotation ++){
                        String belegungString = "field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation;
                        int varIndex = varMap.get(belegungString);
                        expr.addTerm(1.0, vars[varIndex]);
                    }
                }
                model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, ("clause_" + clauseCounter++));
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
                        if (pieces[piece_index].edges[rotation][1] == 0 && y < ( (int) Math.sqrt(fields.length)) -1 ) { // rechte Kante
                            model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                        }
                        if (pieces[piece_index].edges[rotation][2] == 0 && x < ( (int) Math.sqrt(fields.length)) -1 ) { // Unterkante
                            model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                        }
                        if (pieces[piece_index].edges[rotation][3] == 0 && y > 0) { // linke Kante
                            model.addConstr(vars[varIndex], GRB.EQUAL, 0.0, ("clause_" + clauseCounter++));
                        }
                    }
                }
            }









            int helperVariableCounter = 0;
            GRBLinExpr objective = new GRBLinExpr();

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

                                        // Die Kantenfarben des rechten und linken Puzzleteils müssen übereinstimmen
                                        int countMismatches = 0;
                                        int leftVar = varMap.get("field_" + field_index + "_piece_" + pieces[piece_index_1].piece_id + "_rotation_" + rotation_piece_1);
                                        int rightVar = varMap.get("field_" + (field_index + 1) + "_piece_" + pieces[piece_index_2].piece_id + "_rotation_" + rotation_piece_2);

                                        if (pieces[piece_index_1].edges[rotation_piece_1][1] != pieces[piece_index_2].edges[rotation_piece_2][3]) {
                                            countMismatches++;
                                        } if (pieces[piece_index_1].symbols[rotation_piece_1][1] == pieces[piece_index_2].symbols[rotation_piece_2][3]) {
                                            countMismatches++;
                                        }

                                        GRBLinExpr expr = new GRBLinExpr();

                                        // Hilfsvariable für die weiche Klausel
                                        GRBVar tmpHelperVar = model.addVar(0, 1, 0, GRB.BINARY, ("softclause_" + helperVariableCounter));

                                        expr.addTerm(1.0, vars[leftVar]);
                                        expr.addTerm(1.0, vars[rightVar]);
                                        expr.addTerm(-1.0, tmpHelperVar);
                                        model.addConstr(expr, GRB.LESS_EQUAL, 1, "softclause_" + helperVariableCounter);
                                        if ( countMismatches == 1 ){
                                            objective.addTerm(1.0, tmpHelperVar);
                                        }else if (countMismatches == 2 ){
                                            objective.addTerm(2.0, tmpHelperVar);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Nachbar unten (x+1, y)
                if (x + 1 < ((int) Math.sqrt(fields.length))) { // Prüfe, ob das untere Nachbarfeld im Raster liegt
                    for (int piece_index_1 = 0; piece_index_1 < pieces.length; piece_index_1++) {
                        for (int rotation_piece_1 = 0; rotation_piece_1 < 4; rotation_piece_1++) {
                            for (int piexe_index_2 = 0; piexe_index_2 < pieces.length; piexe_index_2++) {
                                for (int rotation_piece_2 = 0; rotation_piece_2 < 4; rotation_piece_2++) {
                                    // Nur wenn zwei verschiedene Puzzleteile an benachbarten Feldern liegen
                                    if (piece_index_1 != piexe_index_2) {

                                        // Die Kantenfarben des rechten und linken Puzzleteils müssen übereinstimmen
                                        int countMismatches = 0;
                                        int topVar = varMap.get("field_" + field_index + "_piece_" + pieces[piece_index_1].piece_id + "_rotation_" + rotation_piece_1);
                                        int bottomVar = varMap.get("field_" + (field_index + ((int) Math.sqrt(fields.length))) + "_piece_" + pieces[piexe_index_2].piece_id + "_rotation_" + rotation_piece_2);

                                        if (pieces[piece_index_1].edges[rotation_piece_1][2] != pieces[piexe_index_2].edges[rotation_piece_2][0]) {
                                            countMismatches++;
                                        }if (pieces[piece_index_1].symbols[rotation_piece_1][2] == pieces[piexe_index_2].symbols[rotation_piece_2][0]){
                                            countMismatches++;
                                        }

                                        GRBLinExpr expr = new GRBLinExpr();

                                        // Hilfsvariable für die weiche Klausel
                                        GRBVar tmpHelperVar = model.addVar(0, 1, 0, GRB.BINARY, ("softclause_" + helperVariableCounter));

                                        expr.addTerm(1.0, vars[topVar]);
                                        expr.addTerm(1.0, vars[bottomVar]);
                                        expr.addTerm(-1.0, tmpHelperVar);
                                        model.addConstr(expr, GRB.LESS_EQUAL, 1, "softclause_" + helperVariableCounter);
                                        if ( countMismatches == 1 ){
                                            objective.addTerm(1.0, tmpHelperVar);
                                        }else if (countMismatches == 2 ){
                                            objective.addTerm(2.0, tmpHelperVar);
                                        }
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

            //optimale Lösung ausgeben
            if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                System.out.println("Optimale Lösung gefunden:");

                System.out.println("Zuweisung der Puzzleteile zu den Feldern:");
                for (int field_index = 0; field_index < fields.length; field_index++) {
                    for (int piece_index = 0; piece_index < pieces.length; piece_index++) {
                        for (int rotation = 0; rotation < 4; rotation++) {
                            int belegungKey = varMap.get("field_" + field_index + "_piece_" + pieces[piece_index].piece_id + "_rotation_" + rotation);
                            if (vars[belegungKey].get(GRB.DoubleAttr.X) == 1.0) { // Wenn die Variable positiv ist, ist das Puzzleteil zugewiesen

                                //neu
                                if (index <= fields.length) {
                                    subString1 = subString1 + "------------------";
                                    subString2 = subString2 + "/       " + pieces[piece_index].edges[rotation][0] + "        /";
                                    subString3 = subString3 + "/       " + pieces[piece_index].symbols[rotation][0] + "        /";
                                    if ( pieces[piece_index].piece_id < 10) {
                                        subString4 = subString4 + "/  " + pieces[piece_index].edges[rotation][3] + " " + pieces[piece_index].symbols[rotation][3] + " (" + pieces[piece_index].piece_id + ") " + pieces[piece_index].symbols[rotation][1] + " " + pieces[piece_index].edges[rotation][1] + "   /";
                                    }else{
                                        subString4 = subString4 + "/  " + pieces[piece_index].edges[rotation][3] + " " + pieces[piece_index].symbols[rotation][3] + " (" + pieces[piece_index].piece_id + ") " + pieces[piece_index].symbols[rotation][1] + " " + pieces[piece_index].edges[rotation][1] + "  /";
                                    }
                                    subString5 = subString5 + "/       " + pieces[piece_index].symbols[rotation][2] + "        /";
                                    subString6 = subString6 + "/       " + pieces[piece_index].edges[rotation][2] + "        /";
                                    subString7 = subString7 + "------------------";
                                    index++;
                                    if (index % (int) Math.sqrt(fields.length) == 0){
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
                int dimension = (int) Math.sqrt(fields.length);
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

        long end = System.nanoTime();
        long elapsedTime = end - start;
        double elapsedTimeInSecond = (double) elapsedTime / 1_000_000_000;
        System.out.println("Runtime:" + elapsedTimeInSecond + " seconds");

    }
}

