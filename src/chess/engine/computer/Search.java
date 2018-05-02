package chess.engine.computer;

import chess.engine.Side;
import chess.engine.byteboard.MoveGenerator;
import chess.engine.byteboard.Position;
import chess.engine.computer.eval.Evaluator;

/**
 * Negamax with alpha-beta pruning.
 */
final public class Search {
    private static final int SEARCH_DEPTH            = 3;
    private static final int QUIESCENCE_SEARCH_DEPTH = -1; // -4;

    private Side computerSide;
    private int positionsEvaluated;
    private int mateIn;
    private Evaluator evaluator = new Evaluator();
    private MoveGenerator[] moveGenerators = new MoveGenerator[11];

    public int getPositionsEvaluated() { return positionsEvaluated; }

    public Search(Side computerSide) {
        this.computerSide = computerSide;

        for(int i=0; i<moveGenerators.length; i++) {
            moveGenerators[i] = new MoveGenerator();
        }
    }
    /**
     * @param pos
     * @param alpha
     * @param depth
     * @return
     */
    public float getScore(Position pos, int depth, float alpha) {
        positionsEvaluated = 0;
        mateIn             = Integer.MAX_VALUE;
        return search(pos, depth, Float.NEGATIVE_INFINITY, -alpha);
    }

    /**
     * @param alpha represents current player best score
     * @param beta  represents previous player best score
     */
    private float search(Position pos, int depth, float alpha, float beta) {
        if(depth<=0) {
            return evaluate(pos);
        }

        var gen = moveGenerators[depth+5]; // allow room for quiescence search
        gen.generateForPosition(pos, false);
        //System.out.println("\t["+depth+"] numMoves="+gen.numMoves);

        if(gen.numMoves==0) {
            if(pos.isCheck()) {
                // Checkmate
                return -(9999 + depth);
            } else {
                // Stalemate

                // Crude estimate of who is in front
                boolean computerIsWinning = pos.getMaterialValue(computerSide) >
                                            pos.getMaterialValue(computerSide.opposite());
                if(computerIsWinning) {
                    // computer is winning - avoid stalemate
                    return 50;
                }
                // player is winning - encourage stalemate
                return -50;
            }
        }

        for(int i = 0; i < gen.numMoves; i++) {
            var move = gen.moves[i];
            pos.applyMove(move);
            var score = -search(pos, depth-1, -beta, -alpha);
            pos.undoMove();
            //System.out.println("\tMove["+i+"]: "+Move.toString(move)+" score:"+score+" alpha:"+alpha+" beta:"+beta);

            if(score >= beta) {
                // beta cutoff
                //System.out.println("\tBeta butoff");
//                if(USE_KILLERMOVE) {
//                    killerMoves.addMove(m, depth);
//                }
                return score;
            }
            if(score > alpha) {
                alpha = score;
                //if(p.whiteToMove && p.moveHistory[0] == 49972 /*&& p.moveHistory[1]==287499*/) {
                //	trace("[alpha "+alpha+" - "+(p.whiteToMove?"black":"white")+" maximising] best move = " + p);
                //}
            }
        }

        return alpha;
    }
    private float evaluate(Position pos) {

        positionsEvaluated++;
        float eval =  evaluator.evaluate(pos);

        return eval;
    }
}