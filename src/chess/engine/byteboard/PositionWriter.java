package chess.engine.byteboard;

import chess.engine.Piece;
import chess.engine.Side;

final public class PositionWriter {

    /** Write the position as FEN notation. */
    public static String toFEN(Position pos) {
        var buf = new StringBuilder();

        // Piece positions
        int count = 0;
        for(int rank=7; rank>=0; rank--) {
            for(int file = 0; file<8; file++) {
                var piece = pos.pieceAt(file, rank);
                if(piece== Piece.NONE) {
                    count++;
                } else {
                    if(count>0) buf.append(count); count = 0;

                    var side = pos.sideAt(file, rank);
                    var ch   = piece.fen();
                    if(side==Side.BLACK) ch = ch.toLowerCase();

                    buf.append(ch);
                }
            }
            if(count>0) buf.append(count); count = 0;
            if(rank>0) {
                buf.append("/");
            }
        }
        buf.append(" ");

        // Side to move
        if(pos.sideToMove()==Side.WHITE) {
            buf.append("w ");
        } else {
            buf.append("b ");
        }

        // Castling permissions
        String perms = "";
        if(pos.canCastleKingSide(Side.WHITE)) {
            perms += "K";
        }
        if(pos.canCastleQueenSide(Side.WHITE)) {
            perms += "Q";
        }
        if(pos.canCastleKingSide(Side.BLACK)) {
            perms += "k";
        }
        if(pos.canCastleQueenSide(Side.BLACK)) {
            perms += "q";
        }
        if(perms.length()==0) {
            buf.append("- ");
        } else {
            buf.append(perms).append(" ");
        }

        // En passant square
        if(pos.availableEnPassantSquare()==0) {
            buf.append("- ");
        } else {
            var f = "abcdefgh".charAt(pos.availableEnPassantSquare() & 7);
            var r = (pos.availableEnPassantSquare() >>> 3) + 1;
            buf.append(f).append(r).append(" ");
        }

        // Half move clock
        buf.append(pos.halfMoveClock()).append(" ");

        // Full move number
        buf.append(pos.fullMoveNumber()+1);

        return buf.toString();
    }
}
