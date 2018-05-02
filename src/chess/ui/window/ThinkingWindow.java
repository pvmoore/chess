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

final public class ThinkingWindow extends AbsMovableWindow implements Game.Listener {
    private TextRenderer text;

    public ThinkingWindow(ChessUI chess) {
        super(chess);

        this.text = new TextRenderer(Font.get("couriernew"))
            .setVP(chess.getCamera().VP())
            .setColour(RGBA.BLACK)
            .setSize(14);

        chess.getGame().addListener(this);
    }
    @Override public void destroy() {
        super.destroy();

        if(text!=null) text.destroy();
        text = null;
    }

    @Override public void onNewGame(Position pos) {
        text.clearText();
    }

    @Override public void onGameMove(Position pos, int move) {
        if(chess.getGame().isComputersMove()) {
            updateForeground();
        }
    }

    @Override public void update(Frame frame) {
        super.update(frame);

        // todo - only update this every half a second or so rather than every frame

        if(chess.getGame().isComputersMove()) {
            // we are interested

            updateForeground();
        }
    }

    // UIComponent
    @Override public void render(Frame frame) {
        super.render(frame);

        text.render();
    }
    //================================================================================
    @Override protected void closing() {
        chess.getWindowMenu().getItem("think").setEnabled(true);
    }
    // PopupUI
    @Override protected void updateForeground() {

        text.clearText();

        var p = getAbsPos();
        var y = 30;
        var moves = chess.getGame().getComputerPlayer().getTopMoves();

        for(int i=0; i<moves.length; i++) {
            var str = String.format("%7.4f  %s", moves[i].score, Move.toAlgebraicString(moves[i].move, false));
            text.appendText(str, new Int2(p.getX()+8, p.getY() + y));
            y += 20;
        }

        var gamePosEval = chess.getGame().getComputerPlayer().getGamePositionsEvaluated();
        var movePosEval = chess.getGame().getComputerPlayer().getMovePositionsEvaluated();

        var pep = new Int2(p.getX()+8, p.getY() + y + 40);

        text.appendText("Positions", pep)
            .appendText("evaluated:", pep.add(0,20))
            .appendText("" + movePosEval + " (" + gamePosEval+")", pep.add(0,40));
    }
}
