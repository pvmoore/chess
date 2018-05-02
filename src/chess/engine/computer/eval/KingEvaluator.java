package chess.engine.computer.eval;

import chess.engine.Piece;
import chess.engine.Side;
import chess.engine.byteboard.Position;

// Single threaded
final public class KingEvaluator {
    private static final float[] TROPISM = {0, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0, 0};

    private int[] positions = new int[20];
    private int whiteKingSq;
    private int blackKingSq;
    private int whiteKingFile;
    private int blackKingFile;
    private int whiteKingRank;
    private int blackKingRank;

    public String toString(Position pos) {
        var s = pos.isEndGame() ? "(End game)" : "(Start/middle game)";

        var buf = new StringBuilder("Kings ").append(s).append(" {");

        var total = evaluate(pos);

        if(pos.isEndGame()) {
            buf.append(String.format("\n\tKing position .... % 6.4f", evaluateKingPositionEndGame(pos)));
        } else {
            buf.append(String.format("\n\tKing position ... % 6.4f", evaluateKingPosition(pos)));
            buf.append(String.format("\n\tCastling ........ % 6.4f", evaluateCastling(pos)));
            buf.append(String.format("\n\tTropism ......... % 6.4f", getTropismScore(pos)));
            buf.append(String.format("\n\tPawn shield ..... % 6.4f", getPawnShieldScore(pos)));
        }
        buf.append(String.format("\n\tTotal ........... % 6.4f", total));

        return buf.append("\n}").toString();
    }

    /** From white's perspective */
    public float evaluate(Position pos) {
        float score = 0;

        preCalculate(pos);

        if(pos.isEndGame()) {

            score += evaluateKingPositionEndGame(pos);

        } else {

            score += evaluateKingPosition(pos);

            score += evaluateCastling(pos);

            score += getTropismScore(pos);

            score += getPawnShieldScore(pos);

            // todo - Pawn storm
        }
        return score;
    }
    private void preCalculate(Position pos) {
        whiteKingSq   = pos.getKingSquare(Side.WHITE);
        blackKingSq   = pos.getKingSquare(Side.BLACK);
        whiteKingFile = whiteKingSq & 7;
        blackKingFile = blackKingSq & 7;
        whiteKingRank = whiteKingSq >>> 3;
        blackKingRank = blackKingSq >>> 3;
    }
    private float evaluateKingPositionEndGame(Position pos) {
        float score = 0;

        // Penalise being on back row to encourage the king into play
        if(whiteKingRank==0) score -= 0.1;
        if(blackKingRank==7) score += 0.1;

        // Penalise being on sides to encourage the king into play
        if(whiteKingFile == 0 || whiteKingFile == 7) score -= 0.1;
        if(blackKingFile == 0 || blackKingFile == 7) score += 0.1;

        return score;
    }
    private float evaluateKingPosition(Position pos) {
        float score = 0;

        // Encourage white king to be either on his own spot or castled
        if(whiteKingSq != 4 &&
           whiteKingSq != 6 &&
           whiteKingSq != 2) score -= 0.3;

        // Penalty for king not being on bottom rank
        if(whiteKingSq >= 8) score -= 0.3;


        // Encourage black king to be either on his own spot or castled
        if(blackKingSq != 60 &&
           blackKingSq != 62 &&
           blackKingSq != 58) score += 0.3;

        // Penalty for king not being on bottom rank
        if(blackKingSq < 56) score += 0.3;

        return score;
    }

    /**
     * Encourage castling.
     */
    private float evaluateCastling(Position pos) {
        float score = 0;

        if(pos.canCastle(Side.WHITE)) score -= 0.1;
        if(pos.canCastle(Side.BLACK)) score += 0.1;

        return score;
    }
    /**
     * Pawn shield. This is the pawn shield we want:
     *
     *  PPP
     *   K
     */
    private float getPawnShieldScore(Position pos) {
        float score = 0;

        // White
        if(whiteKingRank == 0) {
            if(!pos.squareContains(whiteKingSq + 7, Piece.PAWN, Side.WHITE)) score -= 0.1;
            if(!pos.squareContains(whiteKingSq + 8, Piece.PAWN, Side.WHITE)) score -= 0.1;
            if(!pos.squareContains(whiteKingSq + 9, Piece.PAWN, Side.WHITE)) score -= 0.1;
        }
        // Black
        if(blackKingRank == 7) {
            if(!pos.squareContains(blackKingSq - 7, Piece.PAWN, Side.BLACK)) score += 0.1;
            if(!pos.squareContains(blackKingSq - 8, Piece.PAWN, Side.BLACK)) score += 0.1;
            if(!pos.squareContains(blackKingSq - 9, Piece.PAWN, Side.BLACK)) score += 0.1;
        }
        return score;
    }
    private float getTropismScore(Position pos) {
        float score = 0;

        score += getTropismScore(pos, Side.WHITE);
        score -= getTropismScore(pos, Side.BLACK);

        return score;
    }
    /**
     * Sums distances between the king and opposing pieces.
     * The nearer the enemy pieces are the higher the score
     */
    private float getTropismScore(Position pos, Side side) {
        float score = 0;

        int kingFile = pos.getKingSquare(side) & 7;
        int kingRank = pos.getKingSquare(side) >>> 3;
        int count    = pos.getPiecePositions(side.opposite(), positions);

        for(var i = 0; i < count; i++) {
            var sq   = positions[i];
            var file = sq & 7;
            var rank = sq >>> 3;
            var distance = Math.max( Math.abs(kingFile - file), Math.abs(kingRank - rank) );
            if(distance < 1 || distance>7) throw new RuntimeException("unexpected distance score");

            score += TROPISM[distance];
            if(pos.pieceAt(sq) == Piece.QUEEN) {
                // extra danger - double the score
                score += TROPISM[distance];
            }
        }
        return score;
    }
}
