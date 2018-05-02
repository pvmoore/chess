package chess.ui.window;

import chess.engine.Game;
import chess.engine.byteboard.Position;
import chess.engine.computer.eval.*;
import chess.ui.ChessUI;
import juice.Frame;
import juice.graphics.Font;
import juice.graphics.ParagraphTextRenderer;
import juice.graphics.TextRenderer;
import juice.types.Int2;
import juice.types.RGBA;

final public class EvaluationWindow extends AbsMovableWindow implements Game.Listener {
    private TextRenderer segoeText;
    private ParagraphTextRenderer paragraph;

    public EvaluationWindow(ChessUI chess) {
        super(chess);

        this.segoeText = new TextRenderer(Font.get("segoe-ui"))
            .setVP(chess.getCamera().VP());
        this.paragraph = new ParagraphTextRenderer(Font.get("couriernew"), Int2.ZERO, Int2.ZERO)
            .setVP(chess.getCamera().VP());

        chess.getGame().addListener(this);
    }
    @Override public void destroy() {
        super.destroy();

        if(segoeText !=null) segoeText.destroy();
        if(paragraph!=null) paragraph.destroy();
        segoeText = null;
        paragraph = null;
    }

    @Override public void onNewGame(Position pos) {
        updateForeground();
    }
    @Override public void onGameMove(Position pos, int move) {
        updateForeground();
    }
    @Override public void render(Frame frame) {
        super.render(frame);
        segoeText.render();
        paragraph.render();
    }
    //============================================================================

    @Override protected void closing() {
        chess.getWindowMenu().getItem("eval").setEnabled(true);
    }

    @Override protected void updateForeground() {

        paragraph.clear();
        segoeText.clearText();

        var p   = getAbsPos();
        var sz  = getSize();
        var mid = p.getX() + sz.getX()/2;

        var title = "Position Evaluation";
        var ttp   = Font.get("segoe-ui").centreText(title, 26, new Int2(mid, p.getY()+3));

        segoeText.setSize(26)
                 .setColour(new RGBA(0xae6829).gamma(0.5f))
                 .appendText(title, ttp);

        segoeText.setSize(18)
                 .setColour(RGBA.BLACK);

        var game = chess.getGame();

        var standard = new Evaluator().toString(game.getPosition());
        var bishops  = new BishopEvaluator().toString(game.getPosition());
        var knights  = new KnightEvaluator().toString(game.getPosition());
        var kings    = new KingEvaluator().toString(game.getPosition());
        var pawns    = new PawnEvaluator().toString(game.getPosition());

        paragraph.setColour(RGBA.BLACK)
                 .setSize(14)
                 .setRect(p.add(30, 45), new Int2(240, 400));

        paragraph.centred("(From White's perspective)")
                 .newLine()
                 .left(standard)
                 .left(bishops)
                 .left(knights)
                 .left(kings)
                 .left(pawns);
    }
}
