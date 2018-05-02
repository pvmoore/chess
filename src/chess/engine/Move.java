package chess.engine;

/**
 *  Bits   Value
 *  -----------------------
 *   0 -  5  from cell      (6 bits)
 *   6 - 11	to cell         (6 bits)
 *  12 - 14 piece           (3 bits)
 *  15 - 17 captured piece  (3 bits)
 *  18 - 20  flags          (3 bits)
 */
final public class Move {
    private static final int FROM_SHIFT    = 0;
    private static final int TO_SHIFT      = 6;
    private static final int PIECE_SHIFT   = 12;
    private static final int CAPTURE_SHIFT = 15;
    private static final int FLAGS_SHIFT   = 18;

    private static final int FROM_MASK     = 63 << FROM_SHIFT;
    private static final int TO_MASK       = 63 << TO_SHIFT;
    private static final int PIECE_MASK    = 7  << PIECE_SHIFT;
    private static final int CAPTURE_MASK  = 7  << CAPTURE_SHIFT;
    private static final int FLAGS_MASK    = 7  << FLAGS_SHIFT;
    //===============================================================================

    //===============================================================================
    public enum Flags {
        NONE(0),
        PROMOTE_BISHOP(1),
        PROMOTE_KNIGHT(2),
        PROMOTE_ROOK(3),
        PROMOTE_QUEEN(4),
        OOO(5),
        OO(6),
        ENPASSANT(7);

        public int value;
        public boolean isCastle() {
            return this==OOO || this==OO;
        }
        public boolean isPromotion() {
            return this==PROMOTE_BISHOP ||
                   this==PROMOTE_KNIGHT ||
                   this==PROMOTE_ROOK ||
                   this==PROMOTE_QUEEN;
        }
        public Piece getPromotionPiece() {
            return this==PROMOTE_QUEEN ? Piece.QUEEN :
                   this==PROMOTE_ROOK ? Piece.ROOK :
                   this==PROMOTE_KNIGHT ? Piece.KNIGHT :
                   this==PROMOTE_BISHOP ? Piece.BISHOP : Piece.NONE;
        }

        Flags(int value) { this.value = value; }
    }
    //===============================================================================

    public static int makeMove(Piece piece, int from, int to, Piece capture, Flags flags) {
        return
            (from              << FROM_SHIFT)    |
            (to                << TO_SHIFT)      |
            (piece.ordinal()   << PIECE_SHIFT)   |
            (capture.ordinal() << CAPTURE_SHIFT) |
            (flags.value       << FLAGS_SHIFT);
    }
    public static int from(int move) {
        return (move & FROM_MASK) >>> FROM_SHIFT;
    }
    public static int to(int move) {
        return (move & TO_MASK) >>> TO_SHIFT;
    }
    public static Piece piece(int move) {
        return Piece.get((move & PIECE_MASK) >>> PIECE_SHIFT);
    }
    public static Piece capture(int move) {
        return Piece.get((move & CAPTURE_MASK) >>> CAPTURE_SHIFT);
    }
    public static Flags flags(int move) {
        return Flags.values()[(move & FLAGS_MASK) >>> FLAGS_SHIFT];
    }
    public static String toString(int m) {
        if(m==-1) return "Move invalid";
        var s = String.format("Move %d to %d",from(m), to(m));
        if(flags(m)!=Flags.NONE) s += " ("+flags(m)+")";
        return s;
    }
    public static String toAlgebraicString(int m, boolean isCheck) {
        final String FILES = "abcdefgh";
        boolean isCapture = capture(m) != Piece.NONE;
        String pp = piece(m).algebraic();
        String c  = isCapture ? "x" : "";
        String f  = ""+FILES.charAt(to(m) & 7);
        String r  = "" + ((to(m) >>> 3) + 1);
        String ic = isCheck ? "+" : "";
        if(isCapture && pp.isEmpty()) pp = "" + FILES.charAt(from(m) & 7);
        return pp + c + f + r + ic;
    }
}
