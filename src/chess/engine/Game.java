package chess.engine;

import chess.engine.byteboard.MoveGenerator;
import chess.engine.byteboard.Position;
import chess.engine.byteboard.PositionWriter;
import chess.engine.computer.ComputerPlayer;

import java.util.ArrayList;
import java.util.List;

final public class Game {
    private List<Listener> listeners = new ArrayList<>();
    private Position position;
    private Side humanPlayersSide;
    private ComputerPlayer computerPlayer;
    private volatile int computersMove = 0;
    private boolean gameOver = false;
    //==============================================================================
    public interface Listener {
        default void onNewGame(Position pos) {}
        default void onGameMove(Position pos, int move) {}
        default void onGameMoveUndone(Position pos, int move) {}
        default void onGameOver(Position pos, boolean resignation) {}
    }

    public Position getPosition() { return position; }
    public Side humanPlayersSide() { return humanPlayersSide; }
    public boolean isHumansMove() { return humanPlayersSide==position.sideToMove(); }
    public boolean isComputersMove() { return !isHumansMove(); }
    public void addListener(Listener l) { listeners.add(l); }
    public int getComputersMove() { return computersMove; }
    public ComputerPlayer getComputerPlayer() { return computerPlayer; }

    /**
     * This is called by the ComputerPlayer on a separate thread.
     * When the updateForeground thread comes along next time it will enact the move.
     * This is so that the computerPlayer move thread does not get involved
     * in updates which would cause concurrency problems.
     */
    public void setComputersMove(int move) {
        computersMove = move;
    }
    //==============================================================================
    public void newGame(Position position, Side humanPlayerSide) {
        this.position         = position;
        this.humanPlayersSide = humanPlayerSide;
        this.computerPlayer   = new ComputerPlayer(this);
        this.computersMove    = 0;
        this.gameOver         = false;

        listeners.forEach(it->it.onNewGame(position));

        if(checkForMate()) {
            return;
        }

        if(isComputersMove() && !gameOver) {
            computerPlayer.yourMove();
        }

        System.out.println("Position:\n" + position.toString());
        System.out.println(PositionWriter.toFEN(position));
    }
    public void makeMove(int move) {
        //System.out.println("makeMove "+Move.toString(move));
        position.applyMove(move);
        listeners.forEach(it->it.onGameMove(position, move));

        //System.out.println("Enprise:\n"+ Enprise.toString(position, false));
        System.out.println(PositionWriter.toFEN(position));

        if(checkForMate()) {
            return;
        }

        // Computer's turn
        if(isComputersMove() && !gameOver) {
            computerPlayer.yourMove();
        }
    }
    public void undoMove() {
        var move = position.undoMove();
        listeners.forEach(it->it.onGameMoveUndone(position, move));
    }
    public void resign() {
        gameOver = true;
        listeners.forEach(it->it.onGameOver(position, true));
    }
    public void save() {
        // todo
    }
    //==============================================================================
    private boolean checkForMate() {
        var gen = new MoveGenerator();
        gen.generateForPosition(position, false);
        if(gen.numMoves==0) {
            gameOver = true;
            listeners.forEach(it->it.onGameOver(position, false));
            return true;
        }
        return false;
    }
}
