package chess.ui.window;

import chess.engine.Game;
import chess.engine.Move;
import chess.engine.byteboard.Position;
import chess.engine.byteboard.PositionBuilder;
import chess.ui.ChessUI;
import juice.Frame;
import juice.graphics.Font;
import juice.graphics.TextRenderer;
import juice.types.Int2;
import juice.types.RGBA;

import java.util.ArrayList;

final public class MovesWindow extends AbsMovableWindow implements Game.Listener {
    private static class MoveInfo {
        int move;
        boolean isCheck;
        MoveInfo(int m, boolean ic) { this.move = m; this.isCheck = ic; }
    }
    //===============================================================================
    private ArrayList<MoveInfo> moves = new ArrayList<>();
    private TextRenderer timesText;
    private Font font;
    //===============================================================================
    public MovesWindow(ChessUI chess) {
        super(chess);

        this.font = Font.get("couriernew");
        this.timesText = new TextRenderer(font)
            .setVP(chess.getCamera().VP())
            .setColour(RGBA.BLACK)
            .setSize(14);

        chess.getGame().addListener(this);
    }

    // UIComponent/PopupUI
    @Override public void destroy() {
        super.destroy();

        if(timesText !=null) timesText.destroy();
        timesText = null;
    }
    // Game.Listener
    @Override public void onNewGame(Position pos) {
        moves.clear();
        timesText.clearText();

        // Replay moves if this new game is a continuation
        if(pos.moveHistory.size() > 0) {

            var temp = PositionBuilder.fromFEN(chess.getOptions().getString("position-start"));
            for(var m : pos.moveHistory) {
                temp.applyMove(m);
                onGameMove(temp, m);
            }
        }
    }
    // Game.Listener
    @Override public void onGameMove(Position pos, int move) {
        moves.add(new MoveInfo(move, pos.isCheck()));

        updateForeground();
    }
    // Game.Listener
    @Override public void onGameMoveUndone(Position pos, int move) {
        moves.remove(moves.size()-1);
    }
    // UIComponent
    @Override public void render(Frame frame) {
        super.render(frame);

        timesText.render();
    }
    //===========================================================================
    @Override protected void closing() {
        chess.getWindowMenu().getItem("moves").setEnabled(true);
    }
    // PopupUI
    @Override protected void updateForeground() {
        var p               = getAbsPos();
        var height          = 16;
        var fullMovesToShow = 32;
        var index           = Math.max(0, moves.size() - fullMovesToShow*2) & ~1;

        timesText.clearText();

        var x = p.getX() + 33;
        int y = p.getY() + 30;

        for(int i=0; index<moves.size(); i++) {

            var m       = moves.get(index++);
            var moveStr = Move.toAlgebraicString(m.move, m.isCheck);


            if((i&1)==0) {
                // Display the move number
                timesText.appendText(""+(index/2+1)+":", new Int2(p.getX() + 8, y));

                // Display white move
                timesText.appendText(moveStr, new Int2(x, y));

                var width = (int)font.getDimension(moveStr, 14).getX();
                x += width + 10;

            } else {
                // Display black move

                timesText.appendText(moveStr, new Int2(x, y));

                x = p.getX() + 33;
                y += height;
            }
        }
    }
}
