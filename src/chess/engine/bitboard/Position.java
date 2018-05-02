package chess.engine.bitboard;

import chess.engine.Piece;
import chess.engine.Side;
import juice.types.Pair;

final public class Position {
    private static final int FLAG_WHITE_OO 	     = 1; // has castled
    private static final int FLAG_WHITE_OOO	     = 2; // has castled
    private static final int FLAG_BLACK_OO	     = 4; // has castled
    private static final int FLAG_BLACK_OOO	     = 8; // has castled

    // Start of state
    public long whitePositions;
    public long blackPositions;

    public long pawns;
    public long bishops;
    public long knights;
    public long rooks;
    public long queens;
    public long kings;

    public boolean whiteToMove;
    public int fullMoveNumber;
    public int halfMoveClock;
    public int availableEnpassant;  // points to possible enpassant attack square

    public int flags;
    // end of state

    private int hash;

    public Position() {
        whitePositions = 0x00000000_0000ffffL;
        blackPositions = 0xffff0000_00000000L;

        pawns   = 0x00ff0000_0000ff00L;
        bishops = 0x24000000_00000024L;
        knights = 0x42000000_00000042L;
        rooks   = 0x81000000_00000081L;
        queens  = 0x10000000_00000010L;
        kings   = 0x08000000_00000008L;

        whiteToMove    = true;
        fullMoveNumber = 0;
        halfMoveClock  = 0;
        availableEnpassant = 0;

        flags = 0;
    }

    public void applyMove(int move) {

    }
    public void undoMove() {

    }
    public boolean isOccupied(int sq) {
        // todo
        return false;
    }
    /**
     * @param file 0..7
     * @param rank 0..7
     */
    public Pair<Piece,Side> getPieceAt(int file, int rank) {
        long m = (0x80L>>>file) << (rank*8);

        Piece piece = Piece.NONE;
        if((pawns & m)!=0) piece = Piece.PAWN;
        if((bishops & m)!=0) piece = Piece.BISHOP;
        if((knights & m)!=0) piece = Piece.KNIGHT;
        if((rooks & m)!=0) piece = Piece.ROOK;
        if((queens & m)!=0) piece = Piece.QUEEN;
        if((kings & m)!=0) piece = Piece.KING;

        Side side = (whitePositions & m) != 0 ? Side.WHITE : Side.BLACK;

        return new Pair<>(piece, side);
    }
    public Pair<Piece,Side> getPieceAt(int sq) {
        return getPieceAt(sq&7, sq>>3);
    }
    public int getKingSquare(Side side) {
        // todo
        return 0;
    }
    public boolean canCastleKingSide(Side side) {
        return side == Side.WHITE ? (flags&FLAG_WHITE_OO)!=0
                                  : (flags&FLAG_BLACK_OO)!=0;
    }
    public boolean canCastleQueenSide(Side side) {
        return side == Side.WHITE ? (flags&FLAG_WHITE_OOO)!=0
                                  : (flags&FLAG_BLACK_OOO)!=0;
    }
    public boolean isSquareAttacked(int sq, Side by) {
        // todo
        return false;
    }
    @Override public int hashCode() {
        if(hash!=0) return hash;
        long h = 0;
        h = 31 * h + whitePositions;
        h = 31 * h + blackPositions;
        h = 31 * h + pawns;
        h = 31 * h + bishops;
        h = 31 * h + knights;
        h = 31 * h + rooks;
        h = 31 * h + queens;
        h = 31 * h + kings;
        h = 31 * h + (whiteToMove ? 1 : 0);
        h = 31 * h + fullMoveNumber;
        h = 31 * h + halfMoveClock;
        h = 31 * h + flags;
        h = 31 * h + availableEnpassant;
        hash = (int)(h&0xffffff) ^ (int)(h>>>32);
        return hash;
    }
    @Override public boolean equals(Object obj) {
        Position p = (Position)obj;
        if(p==this) return true;

        return fullMoveNumber==p.fullMoveNumber &&
            whitePositions==p.whitePositions &&
            blackPositions==p.blackPositions &&
            bishops==p.bishops &&
            knights==p.knights &&
            rooks==p.rooks &&
            queens==p.queens &&
            kings==p.kings &&
            whiteToMove==p.whiteToMove &&
            halfMoveClock==p.halfMoveClock &&
            availableEnpassant==p.availableEnpassant &&
            flags==p.flags;
    }
    @Override public String toString() {
        return super.toString();
    }
}
