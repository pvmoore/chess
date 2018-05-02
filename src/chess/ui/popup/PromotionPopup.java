package chess.ui.popup;

import chess.engine.Move;
import chess.engine.Piece;
import chess.ui.ChessUI;
import chess.ui.PieceUI;
import juice.Frame;
import juice.Mouse;
import juice.components.DragComponent;
import juice.components.UIComponent;
import juice.graphics.Font;
import juice.graphics.RoundRectangleRenderer;
import juice.graphics.TextRenderer;
import juice.types.Int2;
import juice.types.RGBA;

import java.util.Arrays;

final public class PromotionPopup extends UIComponent implements DragComponent.Listener {
    private RoundRectangleRenderer roundRectangles;
    private TextRenderer text;
    private Font font;
    private DragComponent dragComponent;
    private ChessUI chess;

    private PieceUI[] pieces = new PieceUI[4];

    // Currently promoting this piece/move
    private PieceUI piece;
    private int from, to;

    public PromotionPopup(ChessUI chess) {
        this.chess = chess;
        this.font  = Font.get("segoe-ui-black");
        this.dragComponent = new DragComponent(this);
        this.roundRectangles = new RoundRectangleRenderer()
            .setVP(chess.getCamera().VP());
        this.text = new TextRenderer(font)
            .setVP(chess.getCamera().VP())
            .setColour(RGBA.WHITE)
            .setSize(24);
    }
    @Override public void destroy() {
        if(text!=null) text.destroy();
        if(roundRectangles!=null) roundRectangles.destroy();
        text = null;
        roundRectangles = null;
    }
    // Drag.Listener
    @Override public UIComponent getComponent() {
        return this;
    }
    @Override public void onAddedToStage() {
        var screen = chess.getWindow().getWindowSize();
        var mid    = screen.div(2).getX();

        setSize(new Int2(350, 160));
        setRelPos(new Int2(mid, 150).sub(getSize().getX()/2, 0));

        updateGraphics();
    }
    public void activate(PieceUI pieceUI, int from, int to) {
        this.piece = pieceUI;
        this.from  = from;
        this.to    = to;

        chess.add(this);
    }
    @Override public void onDragMoved(Int2 delta) {
        updateGraphics();
    }
    @Override public void update(Frame frame) {

        dragComponent.update(frame);

        for(var e : frame.getLocalMouseEvents(this, Mouse.EventType.BUTTON_PRESS)) {

            if(e.button == 0) {
                frame.consume(e);

                var opt = Arrays.stream(pieces).filter(it -> it.enclosesPoint(e.pos)).findFirst();

                if(opt.isPresent()) {
                    Piece selected     = opt.get().getPiece();
                    PieceUI newPieceUI = chess.getPieces()
                                              .takeFromBox(selected, chess.getGame().getPosition().sideToMove());
                    newPieceUI.setRelPos(piece.getRelPos());
                    newPieceUI.setSize(piece.getSize());
                    newPieceUI.setSquare(from);

                    Move.Flags flags;
                    var capture = chess.getGame().getPosition().pieceAt(to);

                    switch(selected) {
                        case ROOK:
                            flags = Move.Flags.PROMOTE_ROOK;
                            break;
                        case KNIGHT:
                            flags = Move.Flags.PROMOTE_KNIGHT;
                            break;
                        case BISHOP:
                            flags = Move.Flags.PROMOTE_BISHOP;
                            break;
                        default:
                            flags = Move.Flags.PROMOTE_QUEEN;
                            break;
                    }
                    int move = Move.makeMove(Piece.PAWN, from, to, capture, flags);

                    getStage().addAfterUpdateHook(() -> {
                        detach();
                        piece.detach();
                        chess.getBoardUI().add(newPieceUI);
                        chess.getGame().makeMove(move);
                    });
                }
            }
        }
    }

    @Override public void render(Frame frame) {
        roundRectangles.render();
        text.render();
    }
    //=============================================================================
    private void updateGraphics() {
        var p = getAbsPos();
        var s = getSize();
        var centre = p.add(getSize().div(2).getX(), 0);

        var bg = RGBA.RED.blend(RGBA.BLUE).gamma(0.5f);

        roundRectangles.clearRectangles()
                       .addRectangle(new RoundRectangleRenderer.Rectangle(
                           p, s,
                           RGBA.BLACK.alpha(0.0f), RGBA.BLACK.alpha(0.0f),RGBA.BLACK.alpha(0.4f),RGBA.BLACK.alpha(0.4f),
                           12, 12, 12, 12
                       ))
                       .addRectangle(new RoundRectangleRenderer.Rectangle(
                           p.add(5,0), s.sub(5),
                           RGBA.WHITE, RGBA.WHITE, RGBA.WHITE.gamma(0.9f), RGBA.WHITE.gamma(0.9f),
                           10, 10, 10, 10
                       ))
                       .addRectangle(new RoundRectangleRenderer.Rectangle(
                           p.add(10,5), s.sub(15),
                           bg, bg, bg.alpha(0.5f), bg.alpha(0.5f),
                           8, 8, 8, 8
                       ));

        String t1 = "Select a promotion";
        var tp = font.centreText(t1, 24, centre.add(0,20));

        text.clearText();
        text.appendText(t1, tp);

        // Display queen, rook, knight and bishop to be selected
        if(getChildren().size()>0) {
            Arrays.stream(pieces).forEach(UIComponent::detach);
        }

        pieces[0] = chess.getPieces().takeFromBox(Piece.QUEEN, chess.getGame().humanPlayersSide());
        pieces[0].setRelPos(new Int2(30, 65));
        pieces[0].setSize(new Int2(64,64));
        add(pieces[0]);

        pieces[1] = chess.getPieces().takeFromBox(Piece.ROOK, chess.getGame().humanPlayersSide());
        pieces[1].setRelPos(new Int2(110, 65));
        pieces[1].setSize(new Int2(64,64));
        add(pieces[1]);

        pieces[2] = chess.getPieces().takeFromBox(Piece.KNIGHT, chess.getGame().humanPlayersSide());
        pieces[2].setRelPos(new Int2(190, 65));
        pieces[2].setSize(new Int2(64,64));
        add(pieces[2]);

        pieces[3] = chess.getPieces().takeFromBox(Piece.BISHOP, chess.getGame().humanPlayersSide());
        pieces[3].setRelPos(new Int2(270, 65));
        pieces[3].setSize(new Int2(64,64));
        add(pieces[3]);
    }
}
