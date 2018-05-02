package chess.engine.computer.eval;

import chess.engine.Piece;
import chess.engine.Side;
import chess.engine.byteboard.Position;

final public class KnightEvaluator {

    public String toString(Position pos) {
        var buf = new StringBuilder("Knights {");

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

        // penalise knights still on back row
        if(pos.pieceAt(1) == Piece.KNIGHT && pos.sideAt(1)== Side.WHITE) score -= 0.1;
        if(pos.pieceAt(6) == Piece.KNIGHT && pos.sideAt(6)== Side.WHITE) score -= 0.1;

        var r = 7<<3;
        if(pos.pieceAt(r+1) == Piece.KNIGHT && pos.sideAt(r+1)== Side.BLACK) score += 0.1;
        if(pos.pieceAt(r+6) == Piece.KNIGHT && pos.sideAt(r+6)== Side.BLACK) score += 0.1;

        return score;
    }
}
