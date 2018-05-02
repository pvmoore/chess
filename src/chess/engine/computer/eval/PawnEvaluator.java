package chess.engine.computer.eval;

import chess.engine.Piece;
import chess.engine.Side;
import chess.engine.byteboard.Position;

import java.util.Arrays;

final public class PawnEvaluator {
    private static final float[] ISOLATED_PAWN_PENALTY_WHITE = new float[64];
    private static final float[] ISOLATED_PAWN_PENALTY_BLACK = new float[64];

    private int numWhitePawns;
    private int numBlackPawns;
    private int[] whitePawnsOnFile   = new int[8];
    private int[] blackPawnsOnFile   = new int[8];
    private int[] whitePawnPositions = new int[8];
    private int[] blackPawnPositions = new int[8];

    private float doubledPawnsScore;
    private float isolatedPawnsScore;
    private float passedPawnsScore;
    private float centralPawnsScore;

    public String toString(Position pos) {
        var buf = new StringBuilder("Pawns {");

        var total = evaluate(pos);

        buf.append(String.format("\n\tDoubled pawns ... % 6.4f", doubledPawnsScore));
        buf.append(String.format("\n\tIsolated pawns .. % 6.4f", isolatedPawnsScore));
        buf.append(String.format("\n\tPassed pawns .... % 6.4f", passedPawnsScore));
        buf.append(String.format("\n\tCentral pawns ... % 6.4f", centralPawnsScore));
        buf.append(String.format("\n\tTotal ........... % 6.4f", total));

        return buf.append("\n}").toString();
    }

    static {
        var v = new double[]{
            /* 8 */ 0.00,  0.00,  0.00,  0.00,  0.00,  0.00,  0.00,  0.00,
            /* 7 */ 0.10,  0.12,  0.16,  0.20,  0.20,  0.16,  0.12,  0.10,
            /* 6 */ 0.10,  0.12,  0.16,  0.20,  0.20,  0.16,  0.12,  0.10,
            /* 5 */ 0.10,  0.12,  0.16,  0.20,  0.20,  0.16,  0.12,  0.10,
            /* 4 */ 0.06,  0.08,  0.10,  0.16,  0.16,  0.10,  0.08,  0.06,
            /* 3 */ 0.04,  0.06,  0.08,  0.10,  0.10,  0.08,  0.06,  0.04,
            /* 2 */ 0.02,  0.04,  0.04,  0.10,  0.10,  0.04,  0.04,  0.02,
            /* 1 */ 0.00,  0.00,  0.00,  0.00,  0.00,  0.00,  0.00,  0.00};
        /*            a      b      c      d      e      f      g      h */
        var i = 0;
        for(var y = 7; y >= 0; y--) {
            for(var x = 0; x < 8; x++) {
                ISOLATED_PAWN_PENALTY_WHITE[i]    = (float)v[x + (y << 3)];
                ISOLATED_PAWN_PENALTY_BLACK[63-i] = (float)v[x + (y << 3)];
                i++;
            }
        }
    }

    /** From white's perspective */
    public float evaluate(Position pos) {

        preCalculate(pos);

        doubledPawnsScore =
            evaluateDoubledPawns(whitePawnsOnFile) -
            evaluateDoubledPawns(blackPawnsOnFile);

        isolatedPawnsScore =
            evaluateIsolatedPawns(whitePawnPositions, numWhitePawns, whitePawnsOnFile, ISOLATED_PAWN_PENALTY_WHITE) -
            evaluateIsolatedPawns(blackPawnPositions, numBlackPawns, blackPawnsOnFile, ISOLATED_PAWN_PENALTY_BLACK);

        passedPawnsScore =
            evaluatePassedPawns(whitePawnsOnFile, blackPawnsOnFile) -
            evaluatePassedPawns(blackPawnsOnFile, whitePawnsOnFile);

        centralPawnsScore = evaluateCentralPawns(pos);

        return doubledPawnsScore +
               isolatedPawnsScore +
               passedPawnsScore +
               centralPawnsScore;
    }
    private void preCalculate(Position pos) {
        // Recalculate structure
        Arrays.fill(whitePawnsOnFile, 0);
        Arrays.fill(blackPawnsOnFile, 0);

        numWhitePawns = pos.getPiecePositions(Piece.PAWN, Side.WHITE, whitePawnPositions);
        numBlackPawns = pos.getPiecePositions(Piece.PAWN, Side.BLACK, blackPawnPositions);

        for(int i = 0; i < numWhitePawns; i++) {
            whitePawnsOnFile[whitePawnPositions[i] & 7]++;
        }
        for(int i = 0; i < numBlackPawns; i++) {
            blackPawnsOnFile[blackPawnPositions[i] & 7]++;
        }
    }
    private float evaluateCentralPawns(Position pos) {
        float score = 0;
        // Lose points if central pawns have not moved
        if(pos.squareContains(11, Piece.PAWN, Side.WHITE)) score -= 0.01;
        if(pos.squareContains(12, Piece.PAWN, Side.WHITE)) score -= 0.01;

        if(pos.squareContains(51, Piece.PAWN, Side.BLACK)) score += 0.01;
        if(pos.squareContains(52, Piece.PAWN, Side.BLACK)) score += 0.01;
        return score;
    }
    private float evaluateDoubledPawns(int[] pawnsOnFile) {
        float score = 0;

        // lose points for each doubled pawn
        for(int i = 0; i < 8; i++) {
            if(pawnsOnFile[i] > 1) {
                score -= (0.08 * (pawnsOnFile[i] - 1) );
            }
        }
        return score;
    }
    private float evaluateIsolatedPawns(int[] pawnPositions,
                                        int numPawns,
                                        int[] pawnsOnFile,
                                        float[] penalties)
    {
        float score = 0;

        for(var i = 0; i < numPawns; i++) {
            int file = pawnPositions[i] & 7;

            if(file > 0) {
                if(pawnsOnFile[file-1] > 0) continue;
            }
            if(file < 7) {
                if(pawnsOnFile[file + 1] > 0) continue;
            }
            // if we get here then the pawn has no protection on either side
            score -= penalties[pawnPositions[i]];
        }

        return score;
    }
    private float evaluatePassedPawns(int[] ownPawnsOnFile, int[] enemyPawnsOnFile) {
        float score = 0;

        // Obvious cases
        if(ownPawnsOnFile[0] > 0) {
            if(enemyPawnsOnFile[0] == 0 && enemyPawnsOnFile[1] == 0) score += 0.1;
        }
        if(ownPawnsOnFile[1] > 0 && enemyPawnsOnFile[1] == 0 &&
            enemyPawnsOnFile[0] == 0 && enemyPawnsOnFile[2] == 0) score += 0.1;

        if(ownPawnsOnFile[2] > 0 && enemyPawnsOnFile[2] == 0 &&
            enemyPawnsOnFile[1] == 0 &&
            enemyPawnsOnFile[3] == 0) score += 0.1;

        if(ownPawnsOnFile[3] > 0 && enemyPawnsOnFile[3] == 0 &&
            enemyPawnsOnFile[2] == 0 &&
            enemyPawnsOnFile[4] == 0) score += 0.1;

        if(ownPawnsOnFile[4] > 0 && enemyPawnsOnFile[4] == 0 &&
            enemyPawnsOnFile[3] == 0 &&
            enemyPawnsOnFile[5] == 0) score += 0.1;

        if(ownPawnsOnFile[5] > 0 && enemyPawnsOnFile[5] == 0 &&
            enemyPawnsOnFile[4] == 0 &&
            enemyPawnsOnFile[6] == 0) score += 0.1;

        if(ownPawnsOnFile[6] > 0 && enemyPawnsOnFile[6] == 0 &&
            enemyPawnsOnFile[5] == 0 &&
            enemyPawnsOnFile[7] == 0) score += 0.1;

        if(ownPawnsOnFile[7] > 0) {
            if(enemyPawnsOnFile[7] == 0 && enemyPawnsOnFile[6] == 0) score += 0.1;
        }

        // todo - for non-obvious cases, need to check whether the enemy pawns have moved beyond our pawn

        return score;
    }
}
