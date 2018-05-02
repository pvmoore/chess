package chess.engine.byteboard;

import chess.engine.Move;
import chess.engine.Piece;
import chess.engine.Side;

import java.util.*;

final public class Position {
    static final int EMPTY      = 0;
    static final int PIECE_MASK = 0b0111;
    static final int SIDE_MASK  = 0b1000;

    static final int FLAG_WHITE_OO 	 = 1; // Set if can castle
    static final int FLAG_WHITE_OOO	 = 2; // Set if can castle
    static final int FLAG_BLACK_OO	 = 4; // Set if can castle
    static final int FLAG_BLACK_OOO	 = 8; // Set if can castle
    //============================================================================
    // Could move most of this to flags to speed up hashing.
    // Could change board to longs and do some byte shifting - board would then be 8 longs.
    static final class State {
        boolean whiteToMove;
        int fullMoveNumber;
        int halfMoveClock;
        int availableEnpassant;  // points to possible enpassant attack square
        int flags;
        int[] board = new int[64];

        // Cached info not strictly part of the state
        int hash, whiteKingPos, blackKingPos;
        int whiteMaterial, blackMaterial;
        int whiteNumPieces, blackNumPieces;

        State copyTo(State to) {
            to.whiteToMove        = whiteToMove;
            to.fullMoveNumber     = fullMoveNumber;
            to.halfMoveClock      = halfMoveClock;
            to.availableEnpassant = availableEnpassant;
            to.flags              = flags;
            to.board              = Arrays.copyOf(board, 64);
            to.hash               = hash;
            to.whiteKingPos       = whiteKingPos;
            to.blackKingPos       = blackKingPos;
            to.whiteMaterial      = whiteMaterial;
            to.blackMaterial      = blackMaterial;
            to.whiteNumPieces     = whiteNumPieces;
            to.blackNumPieces     = blackNumPieces;
            return to;
        }
        @Override public int hashCode() {
            if(hash != 0) return hash;
            int h = 0;
            h = 31 * h + (whiteToMove ? 1 : 0);
            h = 31 * h + fullMoveNumber;
            h = 31 * h + halfMoveClock;
            h = 31 * h + flags;
            h = 31 * h + availableEnpassant;
            h = 31 * h + Arrays.hashCode(board);
            hash = h;
            return h;
        }
        @Override public boolean equals(Object obj) {
            State s = (State)obj;
            return whiteToMove == s.whiteToMove &&
                fullMoveNumber == s.fullMoveNumber &&
                halfMoveClock == s.halfMoveClock &&
                flags == s.flags &&
                availableEnpassant == s.availableEnpassant &&
                Arrays.equals(board, s.board);
        }

        @Override public String toString() {
            var buf = new StringBuilder();
            for(var rank = 7; rank >= 0; rank--) {
                for(var file = 0; file<8; file++) {
                    var b = board[file + (rank<<3)];
                    var p = Piece.get(b & PIECE_MASK);
                    var s = Side.get((b & SIDE_MASK) >>> 3);
                    if(b==EMPTY) {
                        buf.append("∙ ");
                    } else {
                        if(s==Side.WHITE) {
                            buf.append(p.fen());
                        } else {
                            buf.append((char)(p.fen().charAt(0)+32));
                        }
                        buf.append(" ");
                    }
                }
                if(rank==7) {
                    // Castling permissions
                    buf.append(" ");
                    String cp = "";
                    if((flags & FLAG_WHITE_OO)!=0) cp += "K";
                    if((flags & FLAG_WHITE_OOO)!=0) cp += "Q";
                    if((flags & FLAG_BLACK_OO)!=0) cp += "k";
                    if((flags & FLAG_BLACK_OOO)!=0) cp += "q";
                    if(cp.length()==0) cp = "-";
                    buf.append(cp);
                    buf.append(" ");

                    // En passant square
                    if(availableEnpassant!=0) {
                        buf.append(availableEnpassant);
                    } else {
                        buf.append("-");
                    }
                    buf.append(" ");

                    // Half move clock
                    buf.append(halfMoveClock).append(" ");

                    // Full move number
                    buf.append(fullMoveNumber+1).append(" ");

                    // Material
                    buf.append("Material: ").append(whiteMaterial-blackMaterial).append(" ");

                    // Num pieces
                    buf.append("Pieces: ").append(whiteNumPieces-blackNumPieces);
                }
                buf.append("\n");
            }
            return buf.toString();
        }
    }
    //============================================================================
    public State state = new State();
    public Deque<Integer> moveHistory = new ArrayDeque<>();
    private Deque<State> stateHistory  = new ArrayDeque<>();

    public void copyTo(Position p) {
        state.copyTo(p.state);
        // Ensure p has no history
        p.moveHistory.clear();
        p.stateHistory.clear();
    }
    public void applyMove(int move) {
        stateHistory.addLast(state.copyTo(new State()));
        moveHistory.addLast(move);

        var from    = Move.from(move);
        var to      = Move.to(move);
        var piece   = Piece.get(state.board[from] & PIECE_MASK);
        var side    = Side.get(state.board[from] >>> 3);
        var flags   = Move.flags(move);

        var capture = Piece.get(state.board[to] & PIECE_MASK);
        if(flags==Move.Flags.ENPASSANT) {
            capture = Piece.PAWN;
        }

        // Pawn move or any capture resets half move clock
        var resetHalfMove = piece==Piece.PAWN || capture!=Piece.NONE;

        // Move the piece
        state.board[from] = EMPTY;
        state.board[to]   = piece.ordinal() | (side.ordinal()<<3);

        // Remove any old en passant target
        state.availableEnpassant = 0;

        // Adjust num pieces
        if(capture!=Piece.NONE) {
            if(side == Side.WHITE) {
                state.blackNumPieces--;
            } else {
                state.whiteNumPieces--;
            }
        }

        if(piece==Piece.PAWN) {
            // handle en passant capture
            if(flags == Move.Flags.ENPASSANT) {
                // remove en passant captured pawn
                if(state.whiteToMove) {
                    state.board[to - 8] = EMPTY;
                } else {
                    state.board[to + 8] = EMPTY;
                }
            }
            // add a possible en passant target
            if(to - from == 16) {
                state.availableEnpassant = from + 8;
            } else if(from - to == 16) {
                state.availableEnpassant = from - 8;
            }

            // promotion
            if(flags.isPromotion()) {
                state.board[to] &= ~PIECE_MASK;
                int materialChange = 0;

                if(flags == Move.Flags.PROMOTE_QUEEN) {
                    state.board[to] |= Piece.QUEEN.ordinal();
                    materialChange = Piece.QUEEN.material - Piece.PAWN.material;
                } else if(flags == Move.Flags.PROMOTE_ROOK) {
                    state.board[to] |= Piece.ROOK.ordinal();
                    materialChange = Piece.ROOK.material - Piece.PAWN.material;
                } else if(flags == Move.Flags.PROMOTE_BISHOP) {
                    state.board[to] |= Piece.BISHOP.ordinal();
                    materialChange = Piece.BISHOP.material - Piece.PAWN.material;
                } else if(flags == Move.Flags.PROMOTE_KNIGHT) {
                    state.board[to] |= Piece.KNIGHT.ordinal();
                    materialChange = Piece.KNIGHT.material - Piece.PAWN.material;
                }
                if(state.whiteToMove) {
                    state.whiteMaterial += materialChange;
                } else {
                    state.blackMaterial += materialChange;
                }
            }
        } else if(piece==Piece.KING) {
            // remove castling permissions and updateForeground king pos
            if(state.whiteToMove) {
                state.whiteKingPos = to;
                state.flags &= ~(FLAG_WHITE_OO | FLAG_WHITE_OOO);
            } else {
                state.blackKingPos = to;
                state.flags &= ~(FLAG_BLACK_OO | FLAG_BLACK_OOO);
            }

            // move the rook if castling
            if(flags == Move.Flags.OO) {
                if(state.whiteToMove) {
                    state.board[5] = state.board[7];
                    state.board[7] = EMPTY;
                } else {
                    state.board[61] = state.board[63];
                    state.board[63] = EMPTY;
                }
            } else if(flags == Move.Flags.OOO) {
                if(state.whiteToMove) {
                    state.board[3] = state.board[0];
                    state.board[0] = EMPTY;
                } else {
                    state.board[59] = state.board[56];
                    state.board[56] = EMPTY;
                }
            }
        } else if(piece==Piece.ROOK) {
            // remove castling permissions
            if(state.whiteToMove) {
                if(from == 0) {
                    state.flags &= ~FLAG_WHITE_OOO;
                } else {
                    state.flags &= ~FLAG_WHITE_OO;
                }
            } else {
                if(from == 56) {
                    state.flags &= ~FLAG_BLACK_OOO;
                } else {
                    state.flags &= ~FLAG_BLACK_OO;
                }
            }
        }

        if(capture!=Piece.NONE) {
            // Remove castling permissions if a rook was captured
            if(capture == Piece.ROOK) {
                if(to == 0) state.flags &= ~FLAG_WHITE_OOO;
                if(to == 7) state.flags &= ~FLAG_WHITE_OO;
                if(to == 56) state.flags &= ~FLAG_BLACK_OOO;
                if(to == 63) state.flags &= ~FLAG_BLACK_OO;
            }
            // Update opponent's material
            if(state.whiteToMove) {
                state.blackMaterial -= capture.material;
            } else {
                state.whiteMaterial -= capture.material;
            }
        }

        if(resetHalfMove) {
            state.halfMoveClock = 0;
        } else {
            state.halfMoveClock++;
            // todo - if this gets to 100 then a draw can be claimed
            // todo - if this gets to 150 then a draw is forced
        }
        // todo - Threefold repetition rule. Player can claim a draw if same position occurs 3 times
        // todo - Fivefold repetition rule. Draw is forced if the same position occurs 5 times
        state.whiteToMove     = !state.whiteToMove;
        state.fullMoveNumber += (state.whiteToMove ? 1 : 0);
    }
    public int undoMove() {
        state = stateHistory.removeLast();
        int move = moveHistory.removeLast();

        // It's probably quicker to not copy the state

        return move;
    }

    public boolean isOccupied(int sq) {
        return state.board[sq] != EMPTY;
    }
    public boolean squareContains(int sq, Piece p, Side side) {
        return pieceAt(sq)==p && sideAt(sq)==side;
    }
    /**
     * @param file 0..7
     * @param rank 0..7
     */
    public Piece pieceAt(int file, int rank) {
        return pieceAt(file + (rank<<3));
    }
    public Piece pieceAt(int sq) {
        if(sq<0 || sq>63) {
            System.out.println("!Pos=\n"+toString());
            System.out.println(PositionWriter.toFEN(this));
            throw new RuntimeException("Bad square: "+sq);
        }
        return Piece.get(state.board[sq] & PIECE_MASK);
    }
    public Side sideAt(int file, int rank) {
        return sideAt(file + (rank<<3));
    }
    public Side sideAt(int sq) {
        return Side.get(state.board[sq] & SIDE_MASK);
    }
    public Side sideToMove() {
        return state.whiteToMove ? Side.WHITE : Side.BLACK;
    }
    public int availableEnPassantSquare() {
        return state.availableEnpassant;
    }
    public int fullMoveNumber() {
        return state.fullMoveNumber;
    }
    public int halfMoveClock() {
        return state.halfMoveClock;
    }
    public int getKingSquare(Side side) {
        return side==Side.WHITE ? state.whiteKingPos : state.blackKingPos;
    }
    public int getMaterialValue(Side side) {
        return side==Side.WHITE ? state.whiteMaterial : state.blackMaterial;
    }
    public boolean canCastle(Side side) {
        return canCastleKingSide(side) || canCastleQueenSide(side);
    }
    public boolean canCastleKingSide(Side side) {
        return side == Side.WHITE ? (state.flags&FLAG_WHITE_OO)!=0
                                  : (state.flags&FLAG_BLACK_OO)!=0;
    }
    public boolean canCastleQueenSide(Side side) {
        return side == Side.WHITE ? (state.flags&FLAG_WHITE_OOO)!=0
                                  : (state.flags&FLAG_BLACK_OOO)!=0;
    }
    public boolean isSquareAttacked(int sq, Side by) {
        return Enprise.isSquareAttacked(this, sq, by);
    }
    public boolean isCheck() {
        return isSquareAttacked(getKingSquare(sideToMove()), sideToMove().opposite());
    }
    public int getPiecePositions(Piece piece, Side side, int[] array) {
        int count = 0;
        for(int i=0; i<64; i++) {
            if(pieceAt(i)==piece && sideAt(i)==side) {
                array[count++] = i;
            }
        }
        return count;
    }
    public int getPiecePositions(Side side, int[] array) {
        int count = 0;
        for(int i=0; i<64; i++) {
            if(isOccupied(i) && sideAt(i)==side) {
                array[count++] = i;
            }
        }
        return count;
    }
    public boolean isEndGame() {
        return state.whiteMaterial <= 14 ||
               state.blackMaterial <= 14 ||
               state.whiteNumPieces < 5 ||
               state.blackNumPieces < 5;
    }
    @Override public int hashCode() {
        return state.hashCode();
    }
    @Override public boolean equals(Object obj) {
        Position p = (Position)obj;
        return state.equals(p.state);
    }
    @Override public String toString() {
        return state.toString();
    }
}
