package chess.ui;

import chess.engine.Piece;
import chess.engine.Side;
import juice.components.UIComponent;

import java.util.ArrayList;
import java.util.List;

final public class ChessSet {
    private ChessUI chess;
    private List<PieceUI> pieces = new ArrayList<>();

    public ChessSet(ChessUI chess) {
        this.chess = chess;
    }
    public void destroy() {
        // Ensure these are destroyed as they might not be attached to the UI
        pieces.forEach(UIComponent::destroy);
    }
    public void detachAllFromUI() {
        pieces.forEach(it-> {
            it.detach();
            it.setSquare(-1);
        });
    }
    /**
     * Assumes that if a piece is attached to the display then a new one should be generated
     * otherwise it will be re-used.
     */
    public PieceUI takeFromBox(Piece piece, Side side) {
        var opt = pieces.stream()
                        .filter(it->it.getPiece()==piece && it.getSide()==side && !it.isAttached())
                        .findFirst();

        if(opt.isPresent()) return opt.get();

        // Create a new one
        var p = new PieceUI(chess, piece, side);
        pieces.add(p);
        return p;
    }
}
