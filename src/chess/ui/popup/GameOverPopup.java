package chess.ui.popup;

import chess.engine.Game;
import chess.engine.byteboard.Position;
import chess.ui.ChessUI;
import juice.Frame;
import juice.components.DragComponent;
import juice.components.UIComponent;
import juice.graphics.Font;
import juice.graphics.RoundRectangleRenderer;
import juice.graphics.TextRenderer;
import juice.types.Int2;
import juice.types.RGBA;

final public class GameOverPopup extends UIComponent implements Game.Listener, DragComponent.Listener {
    private RoundRectangleRenderer roundRectangles;
    private TextRenderer text;
    private Font font;
    private ChessUI chess;
    private DragComponent dragComponent;

    private String title;
    private String info;

    public GameOverPopup(ChessUI chess) {
        this.chess = chess;
        this.font  = Font.get("segoe-ui-black");
        this.dragComponent = new DragComponent(this);
        this.roundRectangles = new RoundRectangleRenderer()
            .setVP(chess.getCamera().VP());
        this.text = new TextRenderer(font)
            .setVP(chess.getCamera().VP())
            .setColour(RGBA.WHITE)
            .setSize(24);

        chess.getGame().addListener(this);

    }
    @Override public void destroy() {
        if(roundRectangles!=null) roundRectangles.destroy();
        if(text!=null) text.destroy();
        roundRectangles = null;
        text = null;
    }

    // Drag.Listener
    @Override public UIComponent getComponent() {
        return this;
    }

    @Override public void onAddedToStage() {
        var screen = chess.getWindow().getWindowSize();
        var mid    = screen.div(2).getX();

        setSize(new Int2(400, 150));
        setRelPos(new Int2(mid, 150).sub(getSize().getX()/2,0));

        updateGraphics();
    }
    @Override public void onNewGame(Position pos) {
        if(isAttached()) {
            getStage().addAfterUpdateHook(this::detach);
        }
    }
    @Override public void onGameOver(Position pos, boolean resignation) {

        info = "";

        if(resignation) {
            title = "Resignation";
            info = "The computer wins";
        } else if(pos.isCheck()) {
            title = "Checkmate";

            if(chess.getGame().isComputersMove()) {
                info = "Congratulations! You won";
            } else {
                info = "The computer wins";
            }
        } else {
            title = "Stalemate";
        }

        chess.add(this);
    }

    @Override public void onDragMoved(Int2 delta) {
        updateGraphics();
    }

    @Override public void update(Frame frame) {
        dragComponent.update(frame);
    }
    @Override public void render(Frame frame) {
        roundRectangles.render();
        text.render();
    }
    //=====================================================================
    private void updateGraphics() {
        var p = getAbsPos();
        var s = getSize();

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

        var centre    = p.add(getSize().div(2).getX(), 0);
        var titlePos  = font.centreText(title, 40, centre.add(0, 20));
        var infoPos   = font.centreText(info, 24, centre.add(0, 80));

        text.clearText()
            .setSize(40).appendText(title, titlePos)
            .setSize(24).appendText(info, infoPos);
    }

}
