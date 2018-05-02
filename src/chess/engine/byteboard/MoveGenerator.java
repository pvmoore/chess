package chess.engine.byteboard;

import chess.engine.Move;
import chess.engine.Piece;
import chess.engine.Side;

/**
 *  Squares:
 *
 *   7| 56 57 58 59 60 61 62 63
 *   6| 48 49 50 51 52 53 54 55
 * r 5| 40 41 42 43 44 45 46 47
 * a 4| 32 33 34 35 36 37 38 39
 * n 3| 24 25 26 27 28 29 30 31
 * k 2| 16 17 18 19 20 21 22 23
 *   1| 08 09 10 11 12 13 14 15
 *   0| 00 01 02 03 04 05 06 07
 *    ------------------------
 *       0  1  2  3  4  5  6  7
 *               file
 */
final public class MoveGenerator {
    private static final int EMPTY = 0;

    public int[] moves = new int[128];  // adjust this if necessary
    public int numMoves;

    /** Return the move if valid otherwise return -1. */
    public int getMove(Position pos, int from, int to) {
        numMoves = 0;
        generateForSquare(pos, from);

        for(int i=0; i<numMoves; i++) {
            var move = moves[i];
            if(Move.from(move)==from && Move.to(move)==to) return move;
        }
        return -1;
    }
    public void generateForPosition(Position pos, boolean quiescence) {
        numMoves = 0;

        Side side = pos.sideToMove();

        for(int i=0; i<64; i++) {
            if(pos.isOccupied(i) && pos.sideAt(i)==side) {
                generateForSquare(pos, i);
            }
        }

        // todo - reorder captures/checks to the front and use quiescence flag

        // better idea - keep 2 arrays of moves: moves and captures

    }
    @Override public String toString() {
        var buf = new StringBuilder();
        for(int i=0; i<numMoves; i++) {
            buf.append(String.format("[%d] %s", i, Move.toString(moves[i])));
            buf.append("\n");
        }
        return buf.toString();
    }
    //==================================================================================
    private void generateForSquare(Position pos, int sq) {
        Piece piece = pos.pieceAt(sq);

        if(piece==Piece.NONE) return;

        int file      = sq & 7;
        int rank      = sq>>>3;
        Side side     = pos.sideAt(sq);
        int moveIndex = numMoves;

        switch(piece) {
            case PAWN:  generatePawnMoves(pos, sq, file, rank, side); break;
            case BISHOP: generateBishopMoves(pos, sq, file, rank, side); break;
            case KNIGHT: generateKnightMoves(pos, sq, file, rank, side); break;
            case ROOK: generateRookMoves(pos, sq, file, rank, side); break;
            case QUEEN:
                generateRookMoves(pos, sq, file, rank, side);
                generateBishopMoves(pos, sq, file, rank, side);
                break;
        }

        // Remove any moves where the king is attacked after the move is made
        int dest    = moveIndex;
        Side enemy  = side.opposite();
        int kingPos = pos.getKingSquare(side);

        for(int i = moveIndex; i< numMoves; i++) {
            int move = moves[i];
            pos.applyMove(move);
            if(!pos.isSquareAttacked(kingPos, enemy)) {
                moves[dest++] = move;
            }
            pos.undoMove();
        }
        numMoves = dest;

        // Do king moves. These are guaranteed to be safe
        if(piece==Piece.KING) {
            generateKingMoves(pos, sq, file, rank, side);
        }
    }
    private void generatePawnMoves(Position pos, int sq, int file, int rank, Side side) {
        var enemy      = side.opposite();
        var b          = pos.state.board;

        if(side==Side.WHITE) {
            // moves
            if(b[sq+8] == EMPTY) {
                if(rank == 1) {
                    addMove(pos, sq, sq+8);
                    if(b[sq+16] == EMPTY) {
                        addMove(pos, sq, sq+16);
                    }
                } else if(rank==6) {
                    addMove(pos, sq, sq+8, Move.Flags.PROMOTE_QUEEN);
                    addMove(pos, sq, sq+8, Move.Flags.PROMOTE_ROOK);
                    addMove(pos, sq, sq+8, Move.Flags.PROMOTE_BISHOP);
                    addMove(pos, sq, sq+8, Move.Flags.PROMOTE_KNIGHT);
                } else {
                    addMove(pos, sq, sq+8);
                }
            }
            // attacks
            if(file > 0) {
                if(b[sq+7] != EMPTY && pos.sideAt(sq + 7) == enemy) {
                    if(rank == 6) {
                        addMove(pos, sq, sq + 7, Move.Flags.PROMOTE_QUEEN);
                        addMove(pos, sq, sq + 7, Move.Flags.PROMOTE_ROOK);
                        addMove(pos, sq, sq + 7, Move.Flags.PROMOTE_BISHOP);
                        addMove(pos, sq, sq + 7, Move.Flags.PROMOTE_KNIGHT);
                    } else {
                        addMove(pos, sq, sq + 7);
                    }
                }
            }
            if(file < 7) {
                if(b[sq+9] != EMPTY && pos.sideAt(sq + 9) == enemy) {
                    if(rank==6) {
                        addMove(pos, sq, sq + 9, Move.Flags.PROMOTE_QUEEN);
                        addMove(pos, sq, sq + 9, Move.Flags.PROMOTE_ROOK);
                        addMove(pos, sq, sq + 9, Move.Flags.PROMOTE_BISHOP);
                        addMove(pos, sq, sq + 9, Move.Flags.PROMOTE_KNIGHT);
                    } else {
                        addMove(pos, sq, sq + 9);
                    }
                }
            }
            // en passant
            if(pos.availableEnPassantSquare() != 0 && rank == 4) {
                if(file>0 && sq+7 == pos.availableEnPassantSquare()) {
                    addMove(pos, sq, sq+7, Move.Flags.ENPASSANT);
                } else if(file<7 && sq+9 == pos.availableEnPassantSquare()) {
                    addMove(pos, sq, sq+9, Move.Flags.ENPASSANT);
                }
            }
        } else { // down the board (BLACK)
            // moves
            if(b[sq - 8] == EMPTY) {
                if(rank == 6) {
                    addMove(pos, sq, sq-8);
                    if(b[sq - 16] == EMPTY) {
                        addMove(pos, sq, sq-16);
                    }
                } else if(rank==1) {
                    addMove(pos, sq, sq - 8, Move.Flags.PROMOTE_QUEEN);
                    addMove(pos, sq, sq - 8, Move.Flags.PROMOTE_ROOK);
                    addMove(pos, sq, sq - 8, Move.Flags.PROMOTE_BISHOP);
                    addMove(pos, sq, sq - 8, Move.Flags.PROMOTE_KNIGHT);
                } else {
                    addMove(pos, sq, sq - 8);
                }
            }
            // attacks
            if(file > 0) {
                if(b[sq-9] != EMPTY && pos.sideAt(sq-9)==enemy) {
                    if(rank==1) {
                        addMove(pos, sq, sq - 9, Move.Flags.PROMOTE_QUEEN);
                        addMove(pos, sq, sq - 9, Move.Flags.PROMOTE_ROOK);
                        addMove(pos, sq, sq - 9, Move.Flags.PROMOTE_BISHOP);
                        addMove(pos, sq, sq - 9, Move.Flags.PROMOTE_KNIGHT);
                    } else {
                        addMove(pos, sq, sq - 9);
                    }
                }
            }
            if(file < 7) {
                if(b[sq-7] != EMPTY && pos.sideAt(sq-7)==enemy) {
                    if(rank==1) {
                        addMove(pos, sq, sq - 7, Move.Flags.PROMOTE_QUEEN);
                        addMove(pos, sq, sq - 7, Move.Flags.PROMOTE_ROOK);
                        addMove(pos, sq, sq - 7, Move.Flags.PROMOTE_BISHOP);
                        addMove(pos, sq, sq - 7, Move.Flags.PROMOTE_KNIGHT);
                    } else {
                        addMove(pos, sq, sq - 7);
                    }
                }
            }
            // en passant
            if(pos.availableEnPassantSquare() != 0 && rank == 3) {
                if(file>0 && sq - 7 == pos.availableEnPassantSquare()) {
                    addMove(pos, sq, sq - 7, Move.Flags.ENPASSANT);
                } else if(file<7 && sq - 9 == pos.availableEnPassantSquare()) {
                    addMove(pos, sq, sq - 9, Move.Flags.ENPASSANT);
                }
            }
        }
    }
    private void generateBishopMoves(Position pos, int sq, int file, int rank, Side side) {
        var from = sq;
        var tx = file - 1;
        var ty = rank + 1;
        var enemy = side.opposite();
        var b     = pos.state.board;

        // up-left
        while(tx >= 0 && ty <= 7) {
            sq += 7;
            if(b[sq] == EMPTY) {
                addMove(pos, from, sq);
                tx--;
                ty++;
                continue;
            } else if(pos.sideAt(sq) == enemy) {
                addMove(pos, from, sq);
            }
            break;
        }
        // up-right
        sq = from;
        tx = file + 1;
        ty = rank + 1;
        while(tx <= 7 && ty <= 7) {
            sq += 9;
            if(b[sq] == EMPTY) {
                addMove(pos, from, sq);
                tx++;
                ty++;
                continue;
            } else if(pos.sideAt(sq) == enemy) {
                addMove(pos, from, sq);
            }
            break;
        }
        // down-right
        sq = from;
        tx = file + 1;
        ty = rank - 1;
        while(tx <= 7 && ty >= 0) {
            sq -= 7;
            if(b[sq] == EMPTY) {
                addMove(pos, from, sq);
                tx++;
                ty--;
                continue;
            } else if(pos.sideAt(sq) == enemy) {
                addMove(pos, from, sq);
            }
            break;
        }
        // down-left
        sq = from;
        tx = file - 1;
        ty = rank - 1;
        while(tx >= 0 && ty >= 0) {
            sq -= 9;
            if(b[sq] == EMPTY) {
                addMove(pos, from, sq);
                tx--;
                ty--;
                continue;
            } else if(pos.sideAt(sq) == enemy) {
                addMove(pos, from, sq);
            }
            break;
        }
    }
    private void generateKnightMoves(Position pos, int sq, int file, int rank, Side side) {
        var enemy = side.opposite();
        var b     = pos.state.board;

        if(rank < 6) {
            // up up left
            if(file > 0) {
                if(b[sq + 15] == EMPTY) {
                   addMove(pos, sq, sq + 15);
                } else if(pos.sideAt(sq+15) == enemy) {
                    addMove(pos, sq, sq + 15);
                }
            }
            // up up right
            if(file < 7) {
                if(b[sq + 17] == EMPTY) {
                    addMove(pos, sq, sq + 17);
                } else if(pos.sideAt(sq+17) == enemy) {
                    addMove(pos, sq, sq + 17);
                }
            }
        }
        if(file < 6) {
            // right right up
            if(rank < 7) {
                if(b[sq + 10] == EMPTY) {
                    addMove(pos, sq, sq + 10);
                } else if(pos.sideAt(sq+10) == enemy) {
                    addMove(pos, sq, sq + 10);
                }
            }
            // right right down
            if(rank > 0) {
                if(b[sq - 6] == EMPTY) {
                    addMove(pos, sq, sq - 6);
                } else if(pos.sideAt(sq-6) == enemy) {
                    addMove(pos, sq, sq - 6);
                }
            }
        }
        if(rank > 1) {
            // down down right
            if(file < 7) {
                if(b[sq - 15] == EMPTY) {
                    addMove(pos, sq, sq - 15);
                } else if(pos.sideAt(sq-15) == enemy) {
                    addMove(pos, sq, sq - 15);
                }
            }
            // down down left
            if(file > 0) {
                if(b[sq - 17] == EMPTY) {
                    addMove(pos, sq, sq - 17);
                } else if(pos.sideAt(sq-17) == enemy) {
                    addMove(pos, sq, sq - 17);
                }
            }
        }
        if(file > 1) {
            // left left down
            if(rank > 0) {
                if(b[sq - 10] == EMPTY) {
                    addMove(pos, sq, sq - 10);
                } else if(pos.sideAt(sq-10) == enemy) {
                    addMove(pos, sq, sq - 10);
                }
            }
            // left left up
            if(rank < 7) {
                if(b[sq + 6] == EMPTY) {
                    addMove(pos, sq, sq + 6);
                } else if(pos.sideAt(sq+6) == enemy) {
                    addMove(pos, sq, sq + 6);
                }
            }
        }
    }
    private void generateRookMoves(Position pos, int sq, int file, int rank, Side side) {
        var enemy = side.opposite();
        var b     = pos.state.board;
        var from  = sq;

        // left
        for(int i = file - 1; i >= 0; i--) {
            sq--;
            if(b[sq] == EMPTY) {
                addMove(pos, from, sq);
                continue;
            } else if(pos.sideAt(sq) == enemy) {
                addMove(pos, from, sq);
            }
            break;
        }
        // right
        sq = from;
        for(int i = file + 1; i <= 7; i++) {
            sq++;
            if(b[sq] == EMPTY) {
                addMove(pos, from, sq);
                continue;
            } else if(pos.sideAt(sq) == enemy) {
                addMove(pos, from, sq);
            }
            break;
        }
        // up
        sq = from;
        for(int i = rank + 1; i <= 7; i++) {
            sq += 8;
            if(b[sq] == EMPTY) {
                addMove(pos, from, sq);
                continue;
            } else if(pos.sideAt(sq) == enemy) {
                addMove(pos, from, sq);
            }
            break;
        }
        // down
        sq = from;
        for(int i = rank - 1; i >= 0; i--) {
            sq -= 8;
            if(b[sq] == EMPTY) {
                addMove(pos, from, sq);
                continue;
            } else if(pos.sideAt(sq) == enemy) {
                addMove(pos, from, sq);
            }
            break;
        }

    }
    /** Generate all king moves. Only non-attacked moves are generated. */
    private void generateKingMoves(Position pos, int sq, int file, int rank, Side side) {
        var enemy = side.opposite();
        var b     = pos.state.board;

        if(file > 0) {
            // left
            if(b[sq - 1] == EMPTY || pos.sideAt(sq-1) == enemy) {
                if(!pos.isSquareAttacked(sq - 1, enemy)) {
                    addMove(pos, sq, sq - 1);
                }
            }
            // up left
            if(rank < 7) {
                if(b[sq + 7] == EMPTY || pos.sideAt(sq+7) == enemy) {
                    if(!pos.isSquareAttacked(sq + 7, enemy)) {
                        addMove(pos, sq, sq + 7);
                    }
                }
            }
            // down left
            if(rank > 0) {
                if(b[sq - 9] == EMPTY || pos.sideAt(sq-9) == enemy) {
                    if(!pos.isSquareAttacked(sq - 9, enemy)) {
                        addMove(pos, sq, sq - 9);
                    }
                }
            }
        }
        if(file < 7) {
            // right
            if(b[sq + 1] == EMPTY || pos.sideAt(sq+1) == enemy) {
                if(!pos.isSquareAttacked(sq + 1, enemy)) {
                    addMove(pos, sq, sq + 1);
                }
            }
            // up right
            if(rank < 7) {
                if(b[sq + 9] == EMPTY || pos.sideAt(sq+9) == enemy) {
                    if(!pos.isSquareAttacked(sq + 9, enemy)) {
                        addMove(pos, sq, sq + 9);
                    }
                }
            }
            // down right
            if(rank > 0) {
                if(b[sq - 7] == EMPTY || pos.sideAt(sq-7) == enemy) {
                    if(!pos.isSquareAttacked(sq - 7, enemy)) {
                        addMove(pos, sq, sq - 7);
                    }
                }
            }
        }
        if(rank < 7) {
            // up
            if(b[sq + 8] == EMPTY || pos.sideAt(sq+8) == enemy) {
                if(!pos.isSquareAttacked(sq + 8, enemy)) {
                    addMove(pos, sq, sq + 8);
                }
            }
        }
        if(rank > 0) {
            // down
            if(b[sq - 8] == EMPTY || pos.sideAt(sq-8) == enemy) {
                if(!pos.isSquareAttacked(sq - 8, enemy)) {
                    addMove(pos, sq, sq - 8);
                }
            }
        }

        // castling
        if(pos.canCastleKingSide(side)) {
            if(b[sq + 1] == EMPTY && b[sq + 2] == EMPTY) {
                if(!pos.isSquareAttacked(sq, enemy) &&
                   !pos.isSquareAttacked(sq + 1, enemy) &&
                   !pos.isSquareAttacked(sq + 2, enemy))
                {
                    addMove(pos, sq, sq + 2, Move.Flags.OO);
                }
            }
        }
        if(pos.canCastleQueenSide(side)) {
            if(b[sq - 1] == EMPTY && b[sq - 2] == EMPTY && b[sq - 3] == EMPTY) {
                if(!pos.isSquareAttacked(sq, enemy) &&
                   !pos.isSquareAttacked(sq - 1, enemy) &&
                   !pos.isSquareAttacked(sq - 2, enemy))
                {
                    addMove(pos, sq, sq - 2, Move.Flags.OOO);
                }
            }
        }
    }
    private void addMove(Position pos, int from, int to) {
        addMove(pos, from, to, Move.Flags.NONE);
    }
    private void addMove(Position pos, int from, int to, Move.Flags flags) {
        Piece piece   = pos.pieceAt(from);
        Piece capture = pos.pieceAt(to);
        if(flags==Move.Flags.ENPASSANT) capture = Piece.PAWN;
        moves[numMoves++] = Move.makeMove(piece, from, to, capture, flags);
    }
}
