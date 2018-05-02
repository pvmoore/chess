package chess;

import chess.ui.ChessUI;
import juice.Window;

public class Main {
    private static final String VERSION = "2.0.1";
    private Window window;
    private ChessUI chess;

    public static void main(String[] args) {
        Main main = null;
        try{
            main = new Main();
            main.run();
        }catch(Throwable t) {
            t.printStackTrace();
        }finally{
            if(main!=null) main.destroy();
        }
    }
    private Main() {
        this.window = new Window((p) -> {
            p.windowed   = true;
            p.vsync      = true;
            p.width      = 1200;
            p.height     = 800;
            p.title      = "Chess "+VERSION;
            p.textureDir = "./images/";
            p.fontDir    = "./fonts/";
        });
        this.chess = new ChessUI(window);

        // Add chess as our main UI component
        window.getStage().add(chess);

        window.show(true);
    }
    private void destroy() {
        if(window !=null) window.destroy();
    }
    private void run() {
        window.loop();
    }
}
