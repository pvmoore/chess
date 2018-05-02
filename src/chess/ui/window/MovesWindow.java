package chess.ui.window;

import chess.engine.Game;
import chess.engine.Move;
import chess.engine.byteboard.Position;
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
    //===============================================================================
    public MovesWindow(ChessUI chess) {
        super(chess);

        this.timesText = new TextRenderer(Font.get("couriernew"))
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
        timesText.clearText();
    }
    // Game.Listener
    @Override public void onGameMove(Position pos, int move) {
        moves.add(new MoveInfo(move, chess.getGame().getPosition().isCheck()));

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
        var fullMovesToShow = 16;
        var index           = Math.max(0, moves.size() - fullMovesToShow*2) & ~1;

        timesText.clearText();

        int y = p.getY() + 30;

        for(int i=0; index<moves.size(); i++) {
            if((i&1)==0) {
                // Display the move number
                timesText.appendText(""+(index/2+1)+":", new Int2(p.getX() + 8, y));
            }
            // Display the move
            var m = moves.get(index++);
            timesText.appendText(Move.toAlgebraicString(m.move, m.isCheck),
                                 new Int2(p.getX()+33, y));

            y += height;
        }
    }
}
