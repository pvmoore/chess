package chess.ui.window;

import chess.ui.ChessUI;
import juice.Frame;
import juice.Mouse;
import juice.components.DragComponent;
import juice.components.UIComponent;
import juice.graphics.*;
import juice.types.Int2;
import juice.types.RGBA;
import juice.types.Rect;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;

/**
 * Abstract Popup
 */
abstract public class AbsMovableWindow extends UIComponent implements DragComponent.Listener {
    private DragComponent dragComponent;
    private boolean firstTimeAdded = true;
    private TextRenderer couriernewText;
    private RoundRectangleRenderer roundRectangles;
    private ImageRenderer images;
    protected ChessUI chess;

    public AbsMovableWindow(ChessUI chess) {
        this.chess         = chess;
        this.dragComponent = new DragComponent(this);

        this.couriernewText = new TextRenderer(Font.get("couriernew"))
            .setVP(chess.getCamera().VP());
        this.roundRectangles = new RoundRectangleRenderer()
            .setVP(chess.getCamera().VP());
        this.images = new ImageRenderer(Texture.get("128/black_square2.png", new Texture.Attribs(GL_REPEAT, GL_LINEAR)))
            .setVP(chess.getCamera().VP());
    }
    @Override public void destroy() {
        if(roundRectangles!=null) roundRectangles.destroy();
        if(images!=null) images.destroy();
        if(couriernewText !=null) couriernewText.destroy();
        roundRectangles = null;
        images = null;
        couriernewText = null;
    }
    // Drag.Listener
    @Override public UIComponent getComponent() {
        return this;
    }
    @Override public void onAddedToStage() {
        if(firstTimeAdded) {
            // Set the position and size the first time we are added so that
            // any movement is remembered for next time

            // Get pos from options
            var key = getClass().getSimpleName() + "-";
            var p = chess.getOptions().getInt2(key+"pos");

            if(p==null) {
                // Centred on the screen
                var screen = chess.getWindow().getWindowSize();
                var mid    = screen.div(2).getX();

                p = new Int2(mid, 80).sub(getSize().getX() / 2, 0);
            }

            setRelPos(p);

            firstTimeAdded = false;
        }

        updateGraphics();
    }

    @Override public void update(Frame frame) {
        var p     = getAbsPos();
        var sz    = getSize();
        var hover = false;
        var box   = Rect.of(p.add(sz.getX()-30, 0), new Int2(30,27));

        // Change X colour on hover
        for(var e : frame.getLocalMouseEvents(this, Mouse.EventType.MOVE)) {
            if(box.contains(e.pos)) {
                hover = true;
                couriernewText.replaceColour(0, RGBA.RED);
            }
        }
        // Change X colour on not hovering
        if(!hover && !box.contains(frame.window.getMousePos())) {
            couriernewText.replaceColour(0, RGBA.WHITE);
        }

        // Check for close button click
        for(var e : frame.getLocalMouseEvents(this, Mouse.EventType.BUTTON_PRESS)) {
            if(box.contains(e.pos)) {
                getStage().addAfterUpdateHook(this::detach);
                frame.consume(e);
                closing();
            }
        }

        // Handle dragging
        dragComponent.update(frame);
    }
    @Override public void render(Frame frame) {
        roundRectangles.render();
        images.render();
        couriernewText.render();
    }

    @Override public void onDragMoved(Int2 delta) {
        updateGraphics();
        getStage().addAfterUpdateHook(()->getParent().moveToFront(this));
    }
    @Override public void onDragDropped(Int2 delta) {
        var key = getClass().getSimpleName() + "-";
        chess.getOptions().set(key+"pos", getRelPos());
    }
    //=========================================================================
    abstract protected void updateForeground();
    abstract protected void closing();

    protected void updateGraphics() {
        roundRectangles.clearRectangles();
        images.clearQuads();
        couriernewText.clearText();

        var p   = getAbsPos();
        var sz  = getSize();

        var c1 = RGBA.BLACK.alpha(0.1f);
        var c2 = RGBA.WHITE;
        var c3 = RGBA.RED;

        // Shadow and background
        roundRectangles.addRectangle(new RoundRectangleRenderer.Rectangle(
                            p.sub(5),
                            getSize().add(10),
                            c1.alpha(0.4f),c1,c1,c1,
                            5,5,5,5
                        ))
                       .addRectangle(new RoundRectangleRenderer.Rectangle(
                           p,
                           getSize(),
                           c2,c2.gamma(1.4f),c2.gamma(0.8f),c2.gamma(0.7f),
                           3,3,3,3
                       ));

        // Close window
        roundRectangles.addRectangle(new RoundRectangleRenderer.Rectangle(
            p.add(sz.getX()-30,0),
            new Int2(26,26),
            c3,c3,c3,c3,
            0,0,2,2
        ));

        // X
        couriernewText.setColour(RGBA.WHITE)
                      .setSize(24)
                      .appendText("x", p.add(sz.getX()-24,-3));

        // BG image
        images.addQuad(new Rect<>(p.getX(), p.getY(), sz.getX(), sz.getY()),
                       new Rect<>(0f,0f,3f,3f),
                       RGBA.WHITE.alpha(0.5f));

        updateForeground();
    }
}
