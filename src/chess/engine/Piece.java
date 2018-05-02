package chess.engine;

public enum Piece {
    NONE(0),
    PAWN(1),
    BISHOP(3),
    KNIGHT(3),
    ROOK(5),
    QUEEN(9),
    KING(999);

    private static final String[] ALGEBRAIC = {"?", "",  "B", "N", "R", "Q", "K"};
    private static final String[] FEN       = {"?", "P", "B", "N", "R", "Q", "K"};

    public int material;

    Piece(int material) {
        this.material = material;
    }

    public boolean isRookOrQueen() { return this==ROOK || this==QUEEN; }
    public boolean isBishopOrQueen() { return this==BISHOP || this==QUEEN; }
    public String algebraic() { return ALGEBRAIC[ordinal()]; }
    public String fen() { return FEN[ordinal()]; }

    public static Piece get(int type) {
        return Piece.values()[type];
    }
}
