package chess.ui;

import chess.engine.Game;
import chess.engine.Move;
import chess.engine.Piece;
import chess.engine.Side;
import chess.engine.byteboard.Position;
import juice.animation.Animation;
import juice.animation.easing.EasingType;
import juice.components.UIComponent;
import juice.types.Int2;

final public class CapturesUI extends UIComponent implements Game.Listener {
    private ChessUI chess;

    public CapturesUI(ChessUI chess) {
        this.chess = chess;
    }
    // Implement Game.Listener
    @Override public void onNewGame(Position pos) {
        // Remove all captures
        getChildren().forEach(UIComponent::detach);

        // todo - check pos for captured pieces and add them

    }
    // Implement Game.Listener
    @Override public void onGameMoveUndone(Position pos, int move) {
        Piece captured = Move.capture(move);
        if(captured!=Piece.NONE) {
            // todo
        }
    }
    // Implement UIComponent
    @Override public void onChildAdded(UIComponent child) {
        if(!(child instanceof PieceUI)) return;

        // Find the correct position for this piece in the captures list
        // depending on material value and side
        PieceUI pieceUI = (PieceUI)child;
        Piece piece     = pieceUI.getPiece();
        var side        = pieceUI.getSide();

        var targetPosition = (int)
            getChildren().stream()
                         .map(it->(PieceUI)it)
                         .filter(it->it.getSide()==side)
                         .filter(it->it!=pieceUI)
                         .filter(it->it.getPiece().ordinal() >= piece.ordinal())
                         .count();

        var fromSize = pieceUI.getSize().getX();
        var toSize   = 64;

        var x     = targetPosition * toSize;
        var y     = side==Side.WHITE ? 0 : toSize + 5;

        var fromPos  = child.getRelPos();
        var toPos    = new Int2(10,10).add(new Int2(x,y));

        Animation a = new Animation(
            60,
            new double[]{fromPos.getX(), fromPos.getY(), fromSize},
            Animation.EndPolicy.DISCARD);

        if(chess.getGame().isHumansMove()) {
            a.addKey(key -> key.frame(20)
                               .values(new double[]{fromPos.getX(), fromPos.getY(), fromSize}));
        }

        a.addKey(key->key.frame(60)
                            .values(new double[]{toPos.getX(), toPos.getY(), toSize})
                            .easing(EasingType.EASE_OUT)
                            .eachFrame((frame, values) -> {
                                int xx = (int)Math.round(values[0]);
                                int yy = (int)Math.round(values[1]);
                                int ss = (int)Math.round(values[2]);
                                child.setRelPos(new Int2(xx, yy));
                                child.setSize(new Int2(ss,ss));
                            }));

        getStage().getAnimations().add(a, true);

        // All pieces to the right of this one need to be shuffled 1 place to the right
        getChildren().stream()
                     .map(it->(PieceUI)it)
                     .filter(it->it.getSide()==side)
                     .filter(it->it!=pieceUI)
                     .filter(it->it.getPiece().ordinal() < piece.ordinal())
                     .forEach(it->{
                         var p    = it.getRelPos();
                         var p2   = p.add(new Int2(toSize, 0));
                         var anim = new Animation(
                                 60,
                                 new double[]{p.getX(), p.getY()},
                                 Animation.EndPolicy.DISCARD)
                            .addKey(k->k.frame(60)
                                        .easing(EasingType.EASE_IN_OUT)
                                        .values(new double[]{p2.getX(), p2.getY()})
                                        .eachFrame((frame, values)->
                                            it.setRelPos(new Int2(
                                                (int)Math.round(values[0]),
                                                (int)Math.round(values[1])
                                            ))
                                        ));
                         getStage().getAnimations().add(anim, true);
                     });
    }
}
