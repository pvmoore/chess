package chess.ui;

import chess.engine.Game;
import chess.engine.Move;
import chess.engine.Piece;
import chess.engine.Side;
import chess.engine.byteboard.MoveGenerator;
import chess.engine.byteboard.Position;
import juice.Frame;
import juice.animation.Animation;
import juice.animation.Key;
import juice.animation.easing.EasingType;
import juice.components.DragComponent;
import juice.components.Sprite;
import juice.components.UIComponent;
import juice.graphics.Texture;
import juice.types.Int2;

final public class PieceUI extends Sprite implements DragComponent.Listener, Game.Listener {
    private ChessUI chess;
    private Piece piece;
    private Side side;
    private DragComponent dragComponent;
    private Int2 originalPos;
    private int square = -1; // the square we are on, or -1 if not on the board

    public Piece getPiece() { return piece; }
    public Side getSide() { return side; }

    public PieceUI(ChessUI chess, Piece p, Side side) {
        this.chess = chess;
        this.piece = p;
        this.side  = side;
        this.dragComponent = new DragComponent(this);
        this.dragComponent.disable();

        var pieceStr = p.toString().toLowerCase();
        var sideStr  = side.toString().toLowerCase();

        String filename = "128/"+sideStr+"_"+pieceStr+".png";

        setTexture(Texture.get(filename, Texture.standardAttribs));
        setVP(chess.getCamera().VP());

        chess.getGame().addListener(this);
    }
    // Drag.Listener
    @Override public UIComponent getComponent() {
        return this;
    }
    public void setSquare(int sq) {
        this.square = sq;
        enableDragIfItsHumansTurn();
    }

    // UIComponent
    @Override public void onRemoved() {
        square = -1;
    }
    // Implement UIComponent / extend Sprite
    @Override public void update(Frame frame) {
        super.update(frame);

        dragComponent.update(frame);
    }
    // Implement DragComponent.Listener
    @Override public void onDragMoved(Int2 delta) {
        if(originalPos==null) {
            originalPos = getRelPos();
            // Move this component to the end of the draw queue so that
            // it is drawn over all other pieces
            getStage().addAfterUpdateHook(()-> getParent().moveToFront(this));
        }
    }
    // Implement DragComponent.Listener
    @Override public void onDragDropped(Int2 delta) {
        var board = (BoardUI)getParent();
        var from  = board.getBoardSquare(originalPos, getSize());
        var to    = board.getBoardSquare(getRelPos(), getSize());
        var gen   = new MoveGenerator();

        var move = gen.getMove(chess.getGame().getPosition(), from, to);
        //System.out.println("move "+Move.toString(move));

        if(move==-1) {
            // Move the piece back to its original position
            animateTo(originalPos, EasingType.EASE_OUT);
        } else {
            if(piece==Piece.PAWN && (to<8 || to>55)) {
                // If move is a promotion then ask the player to select a piece

                var popup = chess.getPromotionPopupUI();

                getStage().addAfterUpdateHook(() -> {
                    setSquare(-1);
                    popup.activate(this, from, to);
                });

            } else {
                chess.getGame().makeMove(move);
            }
        }
        originalPos = null;
    }
    // Implement Game.Listener
    @Override public void onNewGame(Position pos) {
        enableDragIfItsHumansTurn();
    }
    // Implement Game.Listener
    @Override public void onGameMove(Position pos, int move) {
        if(!isOnBoard()) {
            return;
        }

        var game         = chess.getGame();
        var to           = Move.to(move);
        boolean moved    = Move.from(move)==square;
        boolean captured = Move.to(move)==square;
        var flags        = Move.flags(move);
        var moveSide     = pos.sideToMove().opposite();

        if(flags==Move.Flags.ENPASSANT) {

            var enPassantPieceSquare = moveSide==Side.WHITE ? to-8 : to+8;

            if(square==enPassantPieceSquare) {
                captured = true;
            }
        }

        if(moved) {
            // This piece was moved
            square = Move.to(move);
            var board  = (BoardUI)getParent();
            var sqPos  = board.getPosForSquare(square);
            var easing = game.isHumansMove() ? EasingType.EASE_IN_OUT : EasingType.EASE_OUT;
            Key.EndCallback atEnd = null;

            // Handle promotion by computer
            if(game.isHumansMove() && flags.isPromotion()) {
                // After the move animation, detach this piece and add the promoted piece
                atEnd = ()-> {
                    var ui = chess.getPieces().takeFromBox(flags.getPromotionPiece(), pos.sideToMove().opposite());
                    ui.setRelPos(getRelPos());
                    ui.setSize(getSize());
                    ui.setSquare(square);
                    chess.getBoardUI().add(ui);

                    setSquare(-1);
                    detach();
                };
            }

            animateTo(sqPos, easing, atEnd);
        } else if(captured) {
            // This piece was captured
            getStage().addAfterUpdateHook(()->{
                // Move this piece to the captures UI component

                var oldPos = getAbsPos();
                var newPos = chess.getCapturesUI().getAbsPos();
                var diff   = oldPos.sub(newPos);

                setRelPos(diff);

                chess.getCapturesUI().add(this);
                setSquare(-1);
            });
        } else if(piece==Piece.ROOK && Move.flags(move).isCastle()) {
            // We are a rook and the move was a castle.
            // Check to see if we are the affected rook
            var rookFrom = to==6 ? 7 :
                           to==2 ? 0 :
                           to==62 ? 63 : 56;
            if(rookFrom==square) {
                // Yes. It's us
                var board  = (BoardUI)getParent();
                var easing = chess.getGame().isHumansMove() ? EasingType.EASE_IN_OUT : EasingType.EASE_OUT;
                var rookTo = Move.flags(move)==Move.Flags.OO ? square-2 : square+3;
                var sqPos  = board.getPosForSquare(rookTo);
                square = rookTo;
                animateTo(sqPos, easing);
            }
        }
        enableDragIfItsHumansTurn();
    }
    // Implement Game.Listener
    @Override public void onGameMoveUndone(Position pos, int move) {
        if(Move.to(move)==square) {
            // We got moved back
            square    = Move.from(move);
            var board = (BoardUI)getParent();
            var sqPos = board.getPosForSquare(square);
            animateTo(sqPos, EasingType.EASE_IN_OUT);
        }
        enableDragIfItsHumansTurn();
    }
    // Implement Game.Listener
    @Override public void onGameOver(Position pos, boolean resignation) {
        dragComponent.disable();
    }
    //===========================================================================
    private boolean isOnBoard() {
        return square != -1;
    }
    private boolean itsHumansTurn() {
        var game              = chess.getGame();
        var thisIsHumansPiece = (side==game.humanPlayersSide());
        return thisIsHumansPiece && game.isHumansMove();
    }
    private void enableDragIfItsHumansTurn() {
        if(isOnBoard() && itsHumansTurn()) {
            dragComponent.enable();
        } else {
            dragComponent.disable();

        }
    }
    private void animateTo(Int2 to, EasingType easingType) {
        animateTo(to, easingType, null);
    }
    private void animateTo(Int2 to, EasingType easingType, Key.EndCallback atEnd) {
        var pos = getRelPos();
        Animation a = new Animation(60, new double[]{pos.getX(),pos.getY()}, Animation.EndPolicy.DISCARD)
            .addKey(key->key.frame(30)
                            .values(new double[]{to.getX(), to.getY()})
                            .easing(easingType)
                            .atEnd(atEnd)
                            .eachFrame((frame, values) -> {
                                if(!isOnBoard()) {
                                    return;
                                }
                                int x = (int)Math.round(values[0]);
                                int y = (int)Math.round(values[1]);
                                setRelPos(new Int2(x, y));
                            })
            );
        getStage().getAnimations().add(a, true);
    }
}
