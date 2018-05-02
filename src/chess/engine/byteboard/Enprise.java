package chess.engine.byteboard;

import chess.engine.Piece;
import chess.engine.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * Static exchange evaluator.
 */
final public class Enprise {
    private static final int EMPTY       = 0;
    private static final int NO_ATTACKER = -1;

    /**
     * Each value is (sum of attacks << 16) | (material sum of attacks)
     */
    public static void getEnpriseBoard(Position pos, int[] board) {
        for(int sq=0; sq<64; sq++) {
            board[sq] = enpriseForSquare(pos, sq);
        }
    }
    /**
     * Sums all square attackers for both sides on sq in piece order from pawn to king.
     * If a new attacker becomes eligible once a previous attacker has been moved then that too is added.
     *
     * The sum score of all attacks is returned (positive if white would be ahead).
     * Material score only really makes sense if sq holds an opponents piece.
     *
     * @return (sum of attacks << 16) | (material sum of attacks)
     */
    public static int enpriseForSquare(Position pos, final int sq) {

        Side originalSide = pos.sideToMove();
        Side side         = originalSide.opposite();

        var file            = sq & 7;
        var rank            = sq >>> 3;
        int attacksScore    = 0;
        int materialScore   = 0;
        boolean[] finished  = new boolean[2];
        Piece pieceAtSquare = pos.pieceAt(sq);

        List<Integer> savedSquares = new ArrayList<>(16);
        savedSquares.add((sq<<4) | pos.state.board[sq]);

        // Calculate enprise score
        while(!finished[0] || !finished[1]) {

            // Flip sides
            side = side.opposite();

            if(!finished[side.ordinal()]) {
                // Note: This ordering assumes bishops are worth more than knights
                int attackerSquare = getPawnAttacker(pos, sq, file, rank, side);
                if(attackerSquare == NO_ATTACKER) {
                    attackerSquare = getKnightAttacker(pos, sq, file, rank, side);
                    if(attackerSquare == NO_ATTACKER) {
                        attackerSquare = getBishopAttacker(pos, sq, file, rank, side);
                        if(attackerSquare == NO_ATTACKER) {
                            attackerSquare = getRookAttacker(pos, sq, file, rank, side);
                            if(attackerSquare == NO_ATTACKER) {
                                attackerSquare = getQueenAttacker(pos, sq, file, rank, side);
                                if(attackerSquare == NO_ATTACKER) {
                                    attackerSquare = getKingAttacker(pos, sq, file, rank, side);

                                    // This side has no more attacking options
                                    finished[side.ordinal()] = true;

                                    if(attackerSquare == NO_ATTACKER) {
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                }
                // Ok we have an attacker at attackerSquare

                if(savedSquares.size()>1 && pieceAtSquare==Piece.KING) {
                    // Previous side used their king which we now know would be captured
                    // so that was an illegal move. Remove their score for that move.
                    if(side.opposite()== Side.WHITE) {
                        attacksScore  -= 1;
                        materialScore -= pieceAtSquare.material;
                    } else {
                        attacksScore  += 1;
                        materialScore += pieceAtSquare.material;
                    }
                }

                // Add to the enprise score
                attacksScore  += side == Side.WHITE ? 1 : -1;
                materialScore += side == Side.WHITE ? pieceAtSquare.material : -pieceAtSquare.material;

                // Remove the attacking piece
                savedSquares.add((attackerSquare << 4) | pos.state.board[attackerSquare]);
                pos.state.board[sq] = pos.state.board[attackerSquare];
                pos.state.board[attackerSquare] = EMPTY;

                pieceAtSquare = pos.pieceAt(sq);
            }
        }

        // Put the pieces back
        savedSquares.forEach(it->pos.state.board[it>>>4] = it&0b1111);

        return (attacksScore << 16) | (materialScore & 0xffff);
    }
    public static boolean isSquareAttacked(Position pos, int sq, Side bySide) {
        var file = sq & 7;
        var rank = sq >>> 3;

        return getPawnAttacker(pos, sq, file, rank, bySide) != NO_ATTACKER ||
               getBishopAttacker(pos, sq, file, rank, bySide) != NO_ATTACKER ||
               getKnightAttacker(pos, sq, file, rank, bySide) != NO_ATTACKER ||
               getRookAttacker(pos, sq, file, rank, bySide) != NO_ATTACKER ||
               getQueenAttacker(pos, sq, file, rank, bySide) != NO_ATTACKER ||
               getKingAttacker(pos, sq, file, rank, bySide) != NO_ATTACKER;
    }
    public static String toString(Position pos, boolean material) {
        var buf = new StringBuilder();
        for(var rank = 7; rank >= 0; rank--) {
            for(var file = 0; file<8; file++) {
                int e = enpriseForSquare(pos, file + (rank<<3));

                if(material) e &= 0xffff; else e >>= 16;
                short score = (short)e;

                if(score>=0) buf.append(" "+score+" ");
                else buf.append(""+score+" ");
            }
            buf.append("\n");
        }
        return buf.toString();
    }
    //===============================================================================
    private static int getPawnAttacker(Position pos, int sq, int file, int rank, Side side) {
        var b     = pos.state.board;
        var pawn  = Piece.PAWN.ordinal() | (side.ordinal()<<3);

        // todo - handle enpassant

        if(side==Side.WHITE) {
            if(rank > 1) {
                // white
                if(file > 0 && b[sq - 9] == pawn) return checkKing(pos, side, sq-9);
                if(file < 7 && b[sq - 7] == pawn) return checkKing(pos, side, sq-7);
            }
        } else {
            if(rank < 6) {
                // black
                if(file > 0 && b[sq + 7] == pawn) return checkKing(pos, side, sq+7);
                if(file < 7 && b[sq + 9] == pawn) return checkKing(pos, side, sq+9);
            }
        }
        return NO_ATTACKER;
    }
    private static int getBishopAttacker(Position pos, final int sq, int file, int rank, Side side) {
        var bishop = Piece.BISHOP.ordinal() | (side.ordinal()<<3);
        return checkKing(pos, side, getDiagonalAttacker(pos, sq, file, rank, bishop));
    }
    private static int getKnightAttacker(Position pos, int sq, int file, int rank, Side side) {
        var b      = pos.state.board;
        var knight = Piece.KNIGHT.ordinal() | (side.ordinal()<<3);

        if(rank < 6) {
            if(file > 0 && b[sq + 15] == knight) return checkKing(pos, side, sq+15);
            if(file < 7 && b[sq + 17] == knight) return checkKing(pos, side, sq+17);
        }
        if(rank > 1) {
            if(file > 0 && b[sq - 17] == knight) return checkKing(pos, side, sq-17);
            if(file < 7 && b[sq - 15] == knight) return checkKing(pos, side, sq-15);
        }
        if(file < 6) {
            if(rank < 7 && b[sq + 10] == knight) return checkKing(pos, side, sq+10);
            if(rank > 0 && b[sq - 6]  == knight) return checkKing(pos, side, sq-6);
        }
        if(file > 1) {
            if(rank < 7 && b[sq + 6]  == knight) return checkKing(pos, side, sq+6);
            if(rank > 0 && b[sq - 10] == knight) return checkKing(pos, side, sq-10);
        }
        return NO_ATTACKER;
    }
    private static int getRookAttacker(Position pos, int sq, int file, int rank, Side side) {
        var rook = Piece.ROOK.ordinal() | (side.ordinal()<<3);
        return checkKing(pos, side, (getRankAndFileAttacker(pos, sq, file, rank, rook)));
    }
    private static int getQueenAttacker(Position pos, int sq, int file, int rank, Side side) {
        var queen = Piece.QUEEN.ordinal() | (side.ordinal()<<3);

        var r = checkKing(pos, side, (getRankAndFileAttacker(pos, sq, file, rank, queen)));
        if(r!=NO_ATTACKER) return r;

        return checkKing(pos, side, getDiagonalAttacker(pos, sq, file, rank, queen));
    }
    private static int getKingAttacker(Position pos, int sq, int file, int rank, Side side) {
        var b     = pos.state.board;
        var king  = Piece.KING.ordinal() | (side.ordinal()<<3);

        if(file > 0) {
            if(b[sq - 1] == king) return sq - 1;	// left

            if(rank > 0) {
                if(b[sq - 9] == king) return sq - 9;	// down left
            }
            if(rank < 7) {
                if(b[sq + 7] == king) return sq + 7;	// up left
            }
        }
        if(file < 7) {
            if(b[sq + 1] == king) return sq + 1;	// right
            if(rank > 0) {
                if(b[sq - 7] == king) return sq - 7; // down right
            }
            if(rank < 7) {
                if(b[sq + 9] == king) return sq + 9; // up right
            }
        }
        if(rank > 0) {
            if(b[sq - 8] == king) return sq - 8;	// down
        }
        if(rank < 7) {
            if(b[sq + 8] == king) return sq + 8;	// up
        }
        return NO_ATTACKER;
    }
    //=====================================================================================
    private static int getRankAndFileAttacker(Position pos, int sq, int file, int rank, int squareValue) {
        var b     = pos.state.board;
        int t;

        // left
        int p = sq;
        for(int xx = file - 1; xx >= 0; xx--) {
            t = b[--p];
            if(t != EMPTY) {
                if(t == squareValue) return p;
                break;
            }
        }
        // right
        p = sq;
        for(int xx = file + 1; xx <= 7; xx++ ) {
            t = b[++p];
            if(t != EMPTY) {
                if(t == squareValue) return p;
                break;
            }
        }
        // up
        p = sq;
        for(int yy = rank + 1; yy <= 7; yy++) {
            p += 8;
            t = b[p];
            if(t != EMPTY) {
                if(t == squareValue) return p;
                break;
            }
        }

        // down
        p = sq;
        for(int yy = rank - 1; yy >= 0; yy--) {
            p -= 8;
            t = b[p];
            if(t != EMPTY) {
                if(t == squareValue) return p;
                break;
            }
        }
        return NO_ATTACKER;
    }
    private static int getDiagonalAttacker(Position pos, int sq, int file, int rank, int squareValue) {
        var b = pos.state.board;
        int t;

        // up left
        int p = sq;
        for(int xx = file - 1, yy = rank + 1; xx >= 0 && yy <= 7; xx--, yy++) {
            p += 7;
            t = b[p];
            if(t != EMPTY) {
                if(t == squareValue) return p;
                break;
            }
        }
        // up right
        p = sq;
        for(int xx = file + 1, yy = rank + 1; xx <= 7 && yy <= 7; xx++, yy++) {
            p += 9;
            t = b[p];
            if(t != EMPTY) {
                if(t == squareValue) return p;
                break;
            }
        }
        // down left
        p = sq;
        for(int xx = file - 1, yy = rank - 1; xx >= 0 && yy >= 0; xx--, yy--) {
            p -= 9;
            t = b[p];
            if(t != EMPTY) {
                if(t == squareValue) return p;
                break;
            }
        }
        // down right
        p = sq;
        for(int xx = file + 1, yy = rank - 1; xx <= 7 && yy >= 0; xx++, yy--) {
            p -= 7;
            t = b[p];
            if(t != EMPTY) {
                if(t == squareValue) return p;
                break;
            }
        }
        return NO_ATTACKER;
    }

    /**
     * Do a quick check to see whether the king has become exposed to attack
     * after an enprise move of a piece away from sq.
     * We only need to check for sliding attacks.
     */
    private static int checkKing(Position pos, Side side, final int sq) {
        if(sq==-1) return sq;

        var ksq   = pos.getKingSquare(side);
        var kFile = ksq&7;
        var kRank = ksq>>>3;

        var sqFile = sq&7;
        var sqRank = sq>>>3;
        var diff   = sq-ksq;
        var enemy  = side.opposite();

        if(kRank == sqRank) { // If king is on the same rank
            if(ksq<sq) {
                // Check right of the king
                for(int i=ksq+1; (i&7)!=0; i++) {
                    if(i==sq) continue; // this is the square that will be empty
                    if(pos.isOccupied(i)) {
                        if(pos.pieceAt(i).isRookOrQueen() && pos.sideAt(i)==enemy) return NO_ATTACKER;
                        break;
                    }
                }
            } else {
                // Check left of the king
                for(int i=ksq-1; (i&7)!=7; i--) {
                    if(i==sq) continue; // this is the square that will be empty
                    if(pos.isOccupied(i)) {
                        if(pos.pieceAt(i).isRookOrQueen() && pos.sideAt(i)==enemy) return NO_ATTACKER;
                        break;
                    }
                }
            }
        } else if(kFile == sqFile) { // If king is on same file
            if(ksq<sq) {
                // Check down
                for(int i=ksq-8; i>=0; i-=8) {
                    if(i==sq) continue; // this is the square that will be empty
                    if(pos.isOccupied(i)) {
                        if(pos.pieceAt(i).isRookOrQueen() && pos.sideAt(i)==enemy) return NO_ATTACKER;
                        break;
                    }
                }
            } else {
                // Check up
                for(int i=ksq+8; i<64; i+=8) {
                    if(i==sq) continue; // this is the square that will be empty
                    if(pos.isOccupied(i)) {
                        if(pos.pieceAt(i).isRookOrQueen() && pos.sideAt(i)==enemy) return NO_ATTACKER;
                        break;
                    }
                }
            }
        } else if((diff%7)==0) { // If king is on \ diagonal
            if(ksq<sq) {
                // Check down-right
                for(int i=ksq-7; i>=0 && (i&7)!=0; i-=7) {
                    if(i==sq) continue; // this is the square that will be empty
                    if(pos.isOccupied(i)) {
                        if(pos.pieceAt(i).isBishopOrQueen() && pos.sideAt(i)==enemy) return NO_ATTACKER;
                        break;
                    }
                }
            } else {
                // Check up-left
                for(int i=ksq+7; i<64 && (i&7)!=7; i+=7) {
                    if(i==sq) continue; // this is the square that will be empty
                    if(pos.isOccupied(i)) {
                        if(pos.pieceAt(i).isBishopOrQueen() && pos.sideAt(i)==enemy) return NO_ATTACKER;
                        break;
                    }
                }
            }
        } else if((diff%9)==0) {  // If king is on / diagonal
            if(ksq<sq) {
                // Check up-right
                for(int i=ksq+9; i<64 && (i&7)!=0; i+=9) {
                    if(i==sq) continue; // this is the square that will be empty
                    if(pos.isOccupied(i)) {
                        if(pos.pieceAt(i).isBishopOrQueen() && pos.sideAt(i)==enemy) return NO_ATTACKER;
                        break;
                    }
                }
            } else {
                // Check down-left
                for(int i=ksq-9; i>=0 && (i&7)!=7; i-=9) {
                    if(i==sq) continue; // this is the square that will be empty
                    if(pos.isOccupied(i)) {
                        if(pos.pieceAt(i).isBishopOrQueen() && pos.sideAt(i)==enemy) return NO_ATTACKER;
                        break;
                    }
                }
            }
        }
        return sq;
    }
}
