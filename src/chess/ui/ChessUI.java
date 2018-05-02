package chess.ui;

import chess.Options;
import chess.engine.Game;
import chess.engine.Side;
import chess.engine.byteboard.Position;
import chess.engine.byteboard.PositionBuilder;
import chess.engine.byteboard.PositionWriter;
import chess.ui.popup.GameOverPopup;
import chess.ui.popup.PromotionPopup;
import chess.ui.window.EvaluationWindow;
import chess.ui.window.MovesWindow;
import chess.ui.window.ThinkingWindow;
import juice.Camera2D;
import juice.Frame;
import juice.Window;
import juice.components.Menu;
import juice.components.MenuBar;
import juice.components.MenuItem;
import juice.components.UIComponent;
import juice.types.Int2;
import juice.types.RGBA;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

final public class ChessUI extends UIComponent implements Game.Listener {
    // Utilities
    private Window window;
    private Camera2D camera;

    // UI components
    private BoardUI boardUI;
    private CapturesUI capturesUI;
    private ChessSet pieces;
    private GameOverPopup winPopupUI;
    private PromotionPopup promotionPopupUI;
    private MenuBar menuBar;

    private EvaluationWindow evaluationPopupUI;
    private MovesWindow movesWindow;
    private ThinkingWindow thinkingWindow;

    // Domain components
    private Game game = new Game();
    private Options options = new Options();

    //============================================================================
    public Window getWindow() { return window; }
    public Camera2D getCamera() { return camera; }
    public Game getGame() { return game; }
    public ChessSet getPieces() { return pieces; }
    public Options getOptions() { return options; }

    public BoardUI getBoardUI() { return boardUI; }
    public CapturesUI getCapturesUI() { return capturesUI; }
    public PromotionPopup getPromotionPopupUI() { return promotionPopupUI; }
    public Menu getFileMenu() { return menuBar.getMenu(0); }
    public Menu getGameMenu() { return menuBar.getMenu(1); }
    public Menu getWindowMenu() { return menuBar.getMenu(2); }
    //============================================================================
    public ChessUI(Window window) {
        this.window = window;
        this.camera = new Camera2D(window.getWindowSize());

        window.setClearColour(RGBA.WHITE.gamma(0.2f));
        window.setWindowCloseCallback(this::exit);

        pieces = new ChessSet(this);

        game.addListener(this);

        // Add the UI components
        var size         = window.getWindowSize();
        var mid          = size.div(2);
        var squareSize   = 70;
        var margin       = 34;

        var boardPos     = new Int2(0, 32);
        var boardSize    = new Int2(8*squareSize+2*margin, 8*squareSize+2*margin);

        var movesUISize  = new Int2(85, boardSize.getY() - margin*2);

        var thinkingSize = new Int2(120, boardSize.getY() - margin*2);

        var capturesPos  = new Int2(0, boardSize.getY() + 26);
        var capturesSize = new Int2(boardSize.getX(), squareSize*2 + 20);

        // The board
        boardUI = new BoardUI(this, margin);
        boardUI.setRelPos(boardPos);
        boardUI.setSize(boardSize);
        add(boardUI);

        // Captures
        capturesUI = new CapturesUI(this);
        capturesUI.setRelPos(capturesPos);
        capturesUI.setSize(capturesSize);
        add(capturesUI);

        // Popups
        winPopupUI        = new GameOverPopup(this);
        promotionPopupUI  = new PromotionPopup(this);

        // Movable windows
        evaluationPopupUI = new EvaluationWindow(this);
        evaluationPopupUI.setSize(new Int2(300, 500));

        movesWindow = new MovesWindow(this);
        movesWindow.setSize(movesUISize);

        thinkingWindow = new ThinkingWindow(this);
        thinkingWindow.setSize(thinkingSize);

        // Menu
        menuBar = new MenuBar();
        menuBar.setSize(new Int2(30,30));
        menuBar.setHighlightColour(new RGBA(0x8f4707));

        var fileMenu   = new Menu("File", 100);
        var gameMenu   = new Menu("Game", 100);
        var windowMenu = new Menu("Window", 100);
        var helpMenu   = new Menu("Help", 100);

        menuBar.add(fileMenu);
        menuBar.add(gameMenu);
        menuBar.add(windowMenu);
        menuBar.add(helpMenu);

        fileMenu.add(new MenuItem("Exit", this::exit));
        fileMenu.addSeparator();
        fileMenu.add(new MenuItem("Open", this::open).setEnabled(false));
        fileMenu.add(new MenuItem("Save", this::save).setEnabled(false));

        gameMenu.add(new MenuItem("New Game", ()->newGame(null)).setEnabled(false));
        gameMenu.add(new MenuItem("Resign", this::resign).setEnabled(false));
        gameMenu.addSeparator();
        gameMenu.add(new MenuItem("Undo Move", this::undo).setEnabled(false));
        gameMenu.addSeparator();
        gameMenu.add(new MenuItem("Options", this::options).setEnabled(false));

        windowMenu.add(new MenuItem("Moves", this::displayMovesWindow));
        windowMenu.add(new MenuItem("Thinking", this::displayThinkingWindow));
        windowMenu.add(new MenuItem("Evaluation", this::displayEvaluation));
        windowMenu.add(new MenuItem("Stats", ()->{}));

        helpMenu.add(new MenuItem("Hint", ()->{}).setEnabled(false));
        helpMenu.addSeparator();
        helpMenu.add(new MenuItem("About", ()->{}).setEnabled(false));

        add(menuBar);
    }
    @Override public void destroy() {
        // Some pieces might not be attached to the stage
        pieces.destroy();
        // Popups which might not be attached to the stage
        winPopupUI.destroy();
        promotionPopupUI.destroy();
        // Windows which might not be attached to the stage
        evaluationPopupUI.destroy();
        movesWindow.destroy();
    }
    @Override public void onAddedToStage() {
        if(options.getBool("MovesWindow-visible", false)) {
            displayMovesWindow();
        }
        if(options.getBool("ThinkingWindow-visible", false)) {
            displayThinkingWindow();
        }
        if(options.getBool("EvaluationWindow-visible", false)) {
            displayEvaluation();
        }

        continueGame();
    }
    // Game.Listener
    @Override public void onGameOver(Position pos, boolean resignation) {
        getGameMenu().getItem(0).setEnabled(true);  // new game
        getGameMenu().getItem(1).setEnabled(false); // resign
    }
    // UIComponent
    @Override public void update(Frame frame) {
        var keys = window.getKeysPressed();

        // Escape key quits
        if(keys.contains(GLFW_KEY_ESCAPE)) {
            exit();
        }

        // Check to see whether the computer has made its move
        if(game.isComputersMove()) {
            var m = game.getComputersMove();
            if(m!=0) {
                game.setComputersMove(0);
                game.makeMove(m);
            }
        }
    }
    //================================================================================
    private void exit() {
        options.set("position", PositionWriter.toFEN(game.getPosition()));

        options.set("EvaluationWindow-visible", !getWindowMenu().getItem("eval").isEnabled());
        options.set("MovesWindow-visible", !getWindowMenu().getItem("move").isEnabled());
        options.set("ThinkingWindow-visible", !getWindowMenu().getItem("think").isEnabled());

        options.save();
        window.close();
    }
    private void open() {
        // todo
    }
    private void save() {
        // todo
    }
    private void continueGame() {
        String fen = options.getString("position");
        if(fen!=null) {
            newGame(PositionBuilder.fromFEN(fen));

            // add highlight squares
        } else {
            newGame(null);
        }
    }
    private void newGame(Position pos) {
        getGameMenu().getItem(0).setEnabled(false); // new game
        getGameMenu().getItem(1).setEnabled(true);  // resign

        var p  = PositionBuilder.standard();
        var p1 = PositionBuilder.fromFEN("rnbqkbnr/8/8/8/8/4r3/4q3/RN2K2R w KQkq - 0 1");
        var p2 = PositionBuilder.fromFEN("8/k3PP2/8/3P4/PPP5/8/6PP/7K w KQ - 1 10"); // white promo
        var p3 = PositionBuilder.fromFEN("7k/8/8/8/8/8/pp6/7K w KQ - 1 10"); // black promo
        var p4 = PositionBuilder
            .fromFEN("r1bqkb1r/ppp1pppp/35/3pP3/3P4/8/PPP1P2P/RNBQKBNR w KQkq d6 0 5"); // white en passant

        if(pos!=null) {
            p = pos;
        }

        game.newGame(p, Side.WHITE);

        // Add all pieces to the board
        pieces.detachAllFromUI();
        boardUI.setupPosition(game.getPosition(), pieces);
    }
    private void resign() {
        getStage().addAfterUpdateHook(()->game.resign());
    }
    private void undo() {
        // todo
    }
    private void options() {
        // todo
    }
    private void displayEvaluation() {
        getStage().addAfterUpdateHook(()->add(evaluationPopupUI));
        getWindowMenu().getItem("eval").setEnabled(false);
    }
    private void displayMovesWindow() {
        getStage().addAfterUpdateHook(()->add(movesWindow));
        getWindowMenu().getItem("moves").setEnabled(false);
    }
    private void displayThinkingWindow() {
        getStage().addAfterUpdateHook(()->add(thinkingWindow));
        getWindowMenu().getItem("think").setEnabled(false);
    }
}
