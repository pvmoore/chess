package chess.ui;

import chess.engine.Game;
import chess.engine.Move;
import chess.engine.Piece;
import chess.engine.byteboard.Position;
import juice.Frame;
import juice.components.Sprite;
import juice.components.UIComponent;
import juice.graphics.Font;
import juice.graphics.ImageRenderer;
import juice.graphics.TextRenderer;
import juice.graphics.Texture;
import juice.types.Int2;
import juice.types.RGBA;
import juice.types.Rect;

import java.util.ArrayList;
import java.util.List;

final public class BoardUI extends UIComponent implements Game.Listener {
    private ChessUI chess;
    private ImageRenderer whiteSquares, blackSquares;
    private List<Sprite> highlights = new ArrayList<>();
    private int squareSize;
    private TextRenderer text;
    private int margin;

    BoardUI(ChessUI chess, int margin) {
        this.chess  = chess;
        this.margin = margin;

        whiteSquares = new ImageRenderer(Texture.get("128/white_square2.png", Texture.standardAttribs))
            .setVP(chess.getCamera().VP());
        blackSquares = new ImageRenderer(Texture.get("128/black_square2.png", Texture.standardAttribs))
            .setVP(chess.getCamera().VP());
        text = new TextRenderer(Font.get("segoe-ui-black"))
            .setVP(chess.getCamera().VP())
            .setColour(RGBA.WHITE)
            .setSize(24);

        for(int i=0; i<3; i++) {
            highlights.add(
                new Sprite()
                    .setVP(chess.getCamera().VP())
                    .setTexture(Texture.get("128/highlight.png", Texture.standardAttribs))
            );
        }
        chess.getGame().addListener(this);

        // In check highlight
        highlights.get(2).setColour(new RGBA(0.9f,0.1f,0.1f,1));

        // Add highlights to the board with size 0
        highlights.forEach(this::add);
    }

    @Override public void onAdded() {
        var p = getAbsPos();
        whiteSquares.clearQuads();
        blackSquares.clearQuads();
        text.clearText();

        squareSize = getSize().sub(margin*2).getX() / 8;

        var y = p.getY() + margin;

        var files = "abcdefgh";

        for(var rank = 0; rank<8; rank++) {
            var x = p.getX() + margin;
            for(var file = 0; file<8; file++) {
                if(((file^rank)&1)==0) {
                    whiteSquares.addQuad(new Rect<>(x, y, squareSize, squareSize), new Rect<>(0f, 0f, 1f, 1f), RGBA.WHITE);
                } else {
                    blackSquares.addQuad(new Rect<>(x, y, squareSize, squareSize), new Rect<>(0f, 0f, 1f, 1f), RGBA.WHITE);
                }

                if(rank==0) {
                    text.appendText(""+files.charAt(file), new Int2(x + margin-6, p.getY()-4));
                } else if(rank==7) {
                    text.appendText(""+files.charAt(file), new Int2(x + margin-6, y + squareSize));
                }
                x += squareSize;
            }
            text.appendText(""+(8-rank), new Int2(p.getX()+8,     y + margin/2));
            text.appendText(""+(8-rank), new Int2(x + margin/2-6, y + margin/2));
            y += squareSize;
        }
    }
    public void setupPosition(Position p, ChessSet pieces) {

        // Detach any pieces already on the board
        getChildren().stream()
                     .filter(it->it instanceof PieceUI)
                     .forEach(UIComponent::detach);

        var size = new Int2(squareSize-6, squareSize-6);

        for(int rank=7; rank>=0; rank--) {
            for(int file=0; file<8; file++) {
                var piece = p.pieceAt(file, rank);
                if(piece==Piece.NONE) continue;
                var side  = p.sideAt(file, rank);

                var k = pieces.takeFromBox(piece, side);
                k.setSquare(file + (rank<<3));
                k.setSize(size);
                k.setRelPos(new Int2(margin + file*squareSize + 3, margin + (7-rank)*squareSize + 3));
                add(k);
            }
        }
    }
    public Int2 getPosForSquare(int sq) {
        var file = sq&7;
        var rank = 7-(sq>>>3);
        return new Int2(margin+file*squareSize+3, margin+rank*squareSize+3);
    }
    public int getBoardSquare(Int2 pos, Int2 size) {
        var midX = pos.getX() + size.getX()/2;
        var midY = pos.getY() + size.getY()/2;

        var file = (midX - margin) / squareSize;
        var rank = 7 - ((midY - margin) / squareSize);

        if(file<0 || file>7) return -1;
        if(rank<0 || rank>7) return -1;
        return file + (rank<<3);
    }
    @Override public void destroy() {
        if(whiteSquares!=null) whiteSquares.destroy();
        if(blackSquares!=null) blackSquares.destroy();
        if(text!=null) text.destroy();
        highlights.forEach(Sprite::destroy);
        whiteSquares = null;
        blackSquares = null;
        text = null;
        highlights = null;
    }
    @Override public void render(Frame frame) {
        whiteSquares.render();
        blackSquares.render();
        text.render();
    }
    // Implement Game.Listener
    @Override public void onNewGame(Position pos) {
        // Remove highlights
        highlights.forEach(it->it.setSize(Int2.ZERO));

        // Add highlight if position has a previous move
        if(pos.moveHistory.size()>0) {
            addHighlight(pos, pos.getLastMove());
        }
    }

    @Override public void onGameMove(Position pos, int move) {

        if(chess.getGame().isHumansMove()) {

            addHighlight(pos, move);

        } else {
            highlights.get(0).setSize(Int2.ZERO);
            highlights.get(1).setSize(Int2.ZERO);
            highlights.get(2).setSize(Int2.ZERO);
        }
    }

    @Override public void onGameMoveUndone(Position pos, int move) {

    }
    //========================================================================
    private void addHighlight(Position pos, int move) {
        var size = new Int2(squareSize,squareSize);

        if(pos.isCheck()) {
            var sq = pos.getKingSquare(pos.sideToMove());
            highlights.get(2).setSize(size);
            highlights.get(2).setRelPos(getPosForSquare(sq).sub(3));
        }

        highlights.get(0).setSize(size);
        highlights.get(1).setSize(size);

        highlights.get(0).setRelPos(getPosForSquare(Move.from(move)).sub(3));
        highlights.get(1).setRelPos(getPosForSquare(Move.to(move)).sub(3));
    }
}
