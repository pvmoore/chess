package chess.engine.computer.eval;

import chess.engine.Piece;
import chess.engine.Side;
import chess.engine.byteboard.Position;

final public class BishopEvaluator {

    public String toString(Position pos) {
        var buf = new StringBuilder("Bishops {");

        var total = evaluate(pos);

        buf.append(String.format("\n\tBack row ........ % 6.4f", evaluateBackRow(pos)));
        buf.append(String.format("\n\tTotal ........... % 6.4f", total));

        return buf.append("\n}").toString();
    }
    /** From white's perspective */
    public float evaluate(Position pos) {
        return evaluateBackRow(pos);
    }
    private float evaluateBackRow(Position pos) {
        float score = 0;

        // Penalise bishops still on back row
        if(pos.pieceAt(2) == Piece.BISHOP && pos.sideAt(2)==Side.WHITE) score -= 0.1;
        if(pos.pieceAt(5) == Piece.BISHOP && pos.sideAt(5)==Side.WHITE) score -= 0.1;

        var r = 7<<3;
        if(pos.pieceAt(r+2) == Piece.BISHOP && pos.sideAt(r+2)==Side.BLACK) score += 0.1;
        if(pos.pieceAt(r+5) == Piece.BISHOP && pos.sideAt(r+5)==Side.BLACK) score += 0.1;

        return score;
    }
}
