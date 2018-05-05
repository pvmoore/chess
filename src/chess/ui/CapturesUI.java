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

import java.util.ArrayList;

final public class CapturesUI extends UIComponent implements Game.Listener {
    private static final int PIECE_SIZE = 64;
    private ChessUI chess;
    private boolean settingUpNewGame = false;

    public CapturesUI(ChessUI chess) {
        this.chess = chess;

        chess.getGame().addListener(this);
    }
    // Implement Game.Listener
    @Override public void onNewGame(Position pos) {

        // Remove all captures from previous game
        getChildren().forEach(UIComponent::detach);

        // Check pos for captured pieces and add them
        if(pos.moveHistory.size()>0) {
            // Flag this so that we don't attempt to animate the pieces in the onChildAdded method
            settingUpNewGame = true;

            var whitePieces = new ArrayList<Piece>();
            var blackPieces = new ArrayList<Piece>();

            var side = pos.sideToMove();

            for(int i=pos.moveHistory.size()-1; i>=0; i--) {
                var m = pos.moveHistory.get(i);

                var capture = Move.capture(m);
                if(capture != Piece.NONE) {
                    if(side==Side.WHITE) {
                        whitePieces.add(capture);
                    } else {
                        blackPieces.add(capture);
                    }
                }
                side = side.opposite();
            }

            // Order pieces by material value
            whitePieces.sort((a, b) -> -Integer.compare(a.material, b.material));
            blackPieces.sort((a, b) -> -Integer.compare(a.material, b.material));

            int x      = 10;
            var y      = 10;
            var sz     = new Int2(PIECE_SIZE, PIECE_SIZE);
            var pieces = chess.getPieces();

            for(var p : whitePieces) {
                var ui = pieces.takeFromBox(p, Side.WHITE);
                ui.setSize(sz);
                ui.setRelPos(new Int2(x,y));
                ui.setSquare(-1);
                add(ui);
                x += PIECE_SIZE;
            }

            x = 10;
            y = PIECE_SIZE + 15;

            for(var p : blackPieces) {
                var ui = pieces.takeFromBox(p, Side.BLACK);
                ui.setSize(sz);
                ui.setRelPos(new Int2(x,y));
                ui.setSquare(-1);
                add(ui);
                x += PIECE_SIZE;
            }
            settingUpNewGame = false;
        }
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
        if(settingUpNewGame) return;

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

        var x     = targetPosition * PIECE_SIZE;
        var y     = side==Side.WHITE ? 0 : PIECE_SIZE + 5;

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
                            .values(new double[]{toPos.getX(), toPos.getY(), PIECE_SIZE})
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
                         var p2   = p.add(new Int2(PIECE_SIZE, 0));
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
