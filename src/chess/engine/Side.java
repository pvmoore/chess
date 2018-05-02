package chess.engine;

public enum Side {
    WHITE(),    // 0
    BLACK();    // 1

    public static Side get(int value) {
        return value==0 ? WHITE : BLACK;
    }

    public Side opposite() {
        return this==WHITE ? BLACK : WHITE;
    }
}
