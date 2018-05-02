package chess.engine.computer;

import chess.engine.Game;
import chess.engine.Side;
import chess.engine.byteboard.MoveGenerator;
import chess.engine.byteboard.Position;
import juice.Util;

import java.util.Arrays;
import java.util.stream.IntStream;

final public class ComputerPlayer {
    private Game game;
    private Side side;
    private Search search;
    private Position position = new Position();
    private long moveStart;
    private Thread thread;
    public static final class MoveInfo {
        public int move;
        public float score;
    }
    private int movePositionsEvaluated, gamePositionsEvaluated;
    private final Object topMovesLock = new Object();
    private MoveInfo[] topMoves;
    private int topMovesIndex;

    public MoveInfo[] getTopMoves() { synchronized(topMovesLock) { return Arrays.copyOf(topMoves, topMovesIndex); } }
    public int getMovePositionsEvaluated() { return movePositionsEvaluated; }
    public int getGamePositionsEvaluated() { return gamePositionsEvaluated; }

    public ComputerPlayer(Game game) {
        this.game       = game;
        this.side       = game.humanPlayersSide().opposite();
        this.search     = new Search(side);
        this.topMoves   = IntStream.range(0, 10).mapToObj(it->new MoveInfo()).toArray(MoveInfo[]::new);
    }
    public void yourMove() {
        //System.out.println("Computer player's move");

        Arrays.stream(topMoves).forEach(it->{it.move = 0; it.score = 0;});
        topMovesIndex = 0;
        movePositionsEvaluated = 0;

        // Spawn off a new thread to calculate move
        thread = new Thread(() -> {
            moveStart = System.nanoTime();

            // todo - check the opening book

            search();
        });

        thread.start();
    }
    //==========================================================================
    private void makeMove(int move) {
        var moveEnd = System.nanoTime();

        var ms = (int)((moveEnd-moveStart) * 1e-6);

        if(ms<300) {
            Util.exceptionContext(()->Thread.sleep(300-ms));
        }

        // Inform the game object that we have made a move.
        // The updateForeground thread will pick this up next time it comes round.
        game.setComputersMove(move);
    }
    private void search() {

        game.getPosition().copyTo(position);

        // Get all available moves
        var moveGen = new MoveGenerator();
        moveGen.generateForPosition(position, false);

        if(moveGen.numMoves==0) {
            throw new RuntimeException("No moves - we shouldn't get here");
        }
        System.out.println("We have "+moveGen.numMoves+" initial moves");

        // Only 1 possible move available
        if(moveGen.numMoves==1) {
            makeMove(moveGen.moves[0]);
            return;
        }

        // More than 1 possible move. Evaluate them all
        float bestScore         = Float.NEGATIVE_INFINITY;
        float alpha             = Float.NEGATIVE_INFINITY;
        int bestMoveIndex       = 0;
        int depth               = 2; // 3

        // todo - Parallelise this
        // todo - Each thread will need own Search instance and position
        for(int i=0; i<moveGen.numMoves; i++) {

            position.applyMove(moveGen.moves[i]);
            float score = -search.getScore(position, depth, alpha);
            position.undoMove();

            movePositionsEvaluated += search.getPositionsEvaluated();

            if(score > alpha) {
                alpha = score;

                if(score > bestScore) {
                    bestScore = score;
                    bestMoveIndex = i;
                }
            }

            updateTopMoves(moveGen.moves[i], score);

            //System.out.println("Move["+i+"]: "+Move.toString(moveGen.moves[i])+" score="+score+" alpha="+alpha);
        }

        gamePositionsEvaluated += movePositionsEvaluated;

        makeMove(moveGen.moves[bestMoveIndex]);

        //System.out.println("Highest score = "+bestScore);
        //System.out.println("Positions evaluated = "+movePositionsEvaluated);
    }
    private void updateTopMoves(int move, float score) {
        synchronized(topMovesLock) {
            if(topMovesIndex < topMoves.length) {
                topMoves[topMovesIndex].move = move;
                topMoves[topMovesIndex].score = score;
                topMovesIndex++;
            } else {
                if(score > topMoves[topMovesIndex - 1].score) {
                    topMoves[topMovesIndex - 1].move = move;
                    topMoves[topMovesIndex - 1].score = score;
                }
            }
            Arrays.sort(topMoves, 0, topMovesIndex, (a, b) -> -Float.compare(a.score, b.score));
        }
    }
}
