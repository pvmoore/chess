package chess.engine.computer.eval;

import chess.engine.Side;
import chess.engine.byteboard.Enprise;
import chess.engine.byteboard.Position;

final public class Evaluator {
    private static final float[] SQUARE_CONTROL_SCORES = new float[64];
    private int[] enpriseCache = new int[64];

    private PawnEvaluator pawnEvaluator = new PawnEvaluator();
    private BishopEvaluator bishopEvaluator = new BishopEvaluator();
    private KnightEvaluator knightEvaluator = new KnightEvaluator();
    private RookEvaluator rookEvaluator = new RookEvaluator();
    private QueenEvaluator queenEvaluator = new QueenEvaluator();
    private KingEvaluator kingEvaluator = new KingEvaluator();

    static {
        var v = new double[]{
            /* 8 */	0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00,
            /* 7 */	0.00, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.00,
            /* 6 */	0.00, 0.02, 0.04, 0.04, 0.04, 0.04, 0.02, 0.00,
            /* 5 */	0.00, 0.02, 0.04, 0.04, 0.04, 0.04, 0.02, 0.00,
            /* 4 */	0.00, 0.02, 0.04, 0.04, 0.04, 0.04, 0.02, 0.00,
            /* 3 */	0.00, 0.02, 0.04, 0.04, 0.04, 0.04, 0.02, 0.00,
            /* 2 */	0.00, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.00,
            /* 1 */	0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00};
            /*       a     b     c     d     e     f     g     h  */
        var i = 0;
        for(var y = 7; y >= 0; y--) {
            for(var x = 0; x < 8; x++) {
                SQUARE_CONTROL_SCORES[i++] = (float)v[x + (y << 3)];
            }
        }
    }

    public String toString(Position pos) {
        var buf = new StringBuilder("General {");

        preCalculate(pos);

        buf.append(String.format("\n\tMaterial ........ % 6.4f", getMaterialScore(pos)));
        buf.append(String.format("\n\tControl ......... % 6.4f", getSquareControlScore(pos)));
        buf.append(String.format("\n\tEnprise ......... % 6.4f", getEnpriseScore(pos)));

        return buf.append("\n}").toString();
    }

    public float evaluate(Position pos) {
        float score = 0;

        preCalculate(pos);

        // Calculate scores assuming we are playing as white
        score += getMaterialScore(pos);
        score += getSquareControlScore(pos);
        score += getEnpriseScore(pos);

        score += pawnEvaluator.evaluate(pos);
        score += bishopEvaluator.evaluate(pos);
        score += knightEvaluator.evaluate(pos);
        score += rookEvaluator.evaluate(pos);
        score += queenEvaluator.evaluate(pos);
        score += kingEvaluator.evaluate(pos);

        // Negate the score if we are playing as black
        if(pos.sideToMove()==Side.BLACK) {
            score = -score;
        }

        return score;
    }
    //===========================================================================
    private void preCalculate(Position pos) {
        Enprise.getEnpriseBoard(pos, enpriseCache);
    }
    private float getMaterialScore(Position pos) {
        return pos.getMaterialValue(Side.WHITE) - pos.getMaterialValue(Side.BLACK);
    }
    private float getSquareControlScore(Position pos) {
        float score = 0;

        for(int i = 0; i < 64; i++) {
            var attackScore = (short)(enpriseCache[i] >> 16);

            // Add pressure on square
            score += SQUARE_CONTROL_SCORES[i] * attackScore;

            if(pos.isOccupied(i)) {
                // Control the square by being on it
                score += SQUARE_CONTROL_SCORES[i] * (pos.sideAt(i)==Side.WHITE ? 1 : -1);
            }
        }
        return score;
    }
    private float getEnpriseScore(Position pos) {
        float score = 0;
        //var enemy = pos.sideToMove().opposite();
        for(int i = 0; i < 64; i++) {
            if(pos.isOccupied(i) /*&& pos.sideAt(i)==enemy*/) {
                score += (short)(enpriseCache[i] & 0xffff);
            }
        }

        return score * 0.001f;
    }
}
