package chess.engine.byteboard;

import chess.engine.Piece;
import chess.engine.Side;

import java.util.Arrays;

final public class PositionBuilder {

    public static Position standard() {
        var pos = new Position();
        var s   = pos.state;
        s.whiteToMove  = true;
        s.whiteKingPos = 4;
        s.blackKingPos = 60;
        s.flags        = 0b1111;

        for(int i=0; i<8;i++) {
            s.board[i+8]   = Piece.PAWN.ordinal() | (Side.WHITE.ordinal()<<3);
            s.board[i+6*8] = Piece.PAWN.ordinal() | (Side.BLACK.ordinal()<<3);
        }
        s.board[0] = Piece.ROOK.ordinal()   | (Side.WHITE.ordinal()<<3);
        s.board[1] = Piece.KNIGHT.ordinal() | (Side.WHITE.ordinal()<<3);
        s.board[2] = Piece.BISHOP.ordinal() | (Side.WHITE.ordinal()<<3);
        s.board[3] = Piece.QUEEN.ordinal()  | (Side.WHITE.ordinal()<<3);
        s.board[4] = Piece.KING.ordinal()   | (Side.WHITE.ordinal()<<3);
        s.board[5] = Piece.BISHOP.ordinal() | (Side.WHITE.ordinal()<<3);
        s.board[6] = Piece.KNIGHT.ordinal() | (Side.WHITE.ordinal()<<3);
        s.board[7] = Piece.ROOK.ordinal()   | (Side.WHITE.ordinal()<<3);

        s.board[56] = Piece.ROOK.ordinal()   | (Side.BLACK.ordinal()<<3);
        s.board[57] = Piece.KNIGHT.ordinal() | (Side.BLACK.ordinal()<<3);
        s.board[58] = Piece.BISHOP.ordinal() | (Side.BLACK.ordinal()<<3);
        s.board[59] = Piece.QUEEN.ordinal()  | (Side.BLACK.ordinal()<<3);
        s.board[60] = Piece.KING.ordinal()   | (Side.BLACK.ordinal()<<3);
        s.board[61] = Piece.BISHOP.ordinal() | (Side.BLACK.ordinal()<<3);
        s.board[62] = Piece.KNIGHT.ordinal() | (Side.BLACK.ordinal()<<3);
        s.board[63] = Piece.ROOK.ordinal()   | (Side.BLACK.ordinal()<<3);

        s.whiteMaterial = sumMaterial(s, Side.WHITE);
        s.blackMaterial = sumMaterial(s, Side.BLACK);

        s.whiteNumPieces = sumPieces(s, Side.WHITE);
        s.blackNumPieces = sumPieces(s, Side.BLACK);
        return pos;
    }
    /******************************************************************************************************
     *
     * Forsyth-Edwards Notation.
     * http://en.wikipedia.org/wiki/Forsyth-Edwards_Notation
     *
     * example: "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
     * 		or:	"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"  (start position)
     *          "Q7/8/4p1p1/3kP2r/1R3PKP/8/8/8 b - - 0 54" (no castling)
     *
     **/
    public static Position fromFEN(String fen) {
        Position pos = new Position();

        // Parse the piece positions
        int ch;
        int i    = 0;
        int file = 0;
        int rank = 7;
        while(rank>0 || file<8) {
            var sq   = file + (rank<<3);
            var side = Side.WHITE;
            ch = fen.charAt(i++);
            switch(ch) {
                case 'p':
                    side = side.opposite();
                case 'P':
                    pos.state.board[sq] = (Piece.PAWN.ordinal() | (side.ordinal() << 3));
                    break;
                case 'b':
                    side = side.opposite();
                case 'B':
                    pos.state.board[sq] = (Piece.BISHOP.ordinal() | (side.ordinal() << 3));
                    break;
                case 'n':
                    side = side.opposite();
                case 'N':
                    pos.state.board[sq] = (Piece.KNIGHT.ordinal() | (side.ordinal() << 3));
                    break;
                case 'r':
                    side = side.opposite();
                case 'R':
                    pos.state.board[sq] = (Piece.ROOK.ordinal() | (side.ordinal() << 3));
                    break;
                case 'q':
                    side = side.opposite();
                case 'Q':
                    pos.state.board[sq] = (Piece.QUEEN.ordinal() | (side.ordinal() << 3));
                    break;
                case 'k':
                    side = side.opposite();
                case 'K':
                    pos.state.board[sq] = (Piece.KING.ordinal() | (side.ordinal() << 3));
                    break;
                case '/':
                    rank--;
                    file = -1;
                    break;
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    var val = (ch-'0');
                    file += (val-1);
                    break;
                default:
                    throw new RuntimeException("Badly formatted FEN");
            }
            file++;
        }

        // Now expect a space followed by side to move ('b' or 'w')
        if(fen.charAt(i++)!=' ') throw new RuntimeException("Badly formatted FEN");
        pos.state.whiteToMove = fen.charAt(i++)=='w';

        // Castling permissions ('KQkq' or '-')
        if(fen.charAt(i++)!=' ') throw new RuntimeException("Badly formatted FEN");

        int flags = 0;
        if(fen.charAt(i)=='-') {
            i+=2;
        } else {
            while((ch = fen.charAt(i++))!=' ') {
                switch(ch) {
                    case 'k':
                        flags |= Position.FLAG_BLACK_OO;
                        break;
                    case 'K':
                        flags |= Position.FLAG_WHITE_OO;
                        break;
                    case 'q':
                        flags |= Position.FLAG_BLACK_OOO;
                        break;
                    case 'Q':
                        flags |= Position.FLAG_WHITE_OOO;
                        break;
                    default: throw new RuntimeException("Badly formatted FEN");
                }
            }
        }
        pos.state.flags = flags;

        // En passant target square ('e3' for example)
        if(fen.charAt(i)=='-') {
            i++;
        } else {
            var f = fen.charAt(i++) - 97;
            var r = fen.charAt(i++) - 48 - 1;
            pos.state.availableEnpassant = f + (r<<3);
        }
        i++;

        // Half move clock
        String hmc = "";
        while((ch=fen.charAt(i++))!=' ') {
            hmc += (char)ch;
        }
        pos.state.halfMoveClock = Integer.valueOf(hmc);

        // Full move number (starting from 1)
        String fm = "";
        while(i<fen.length()) {
            ch=fen.charAt(i++);
            fm += (char)ch;
        }
        pos.state.fullMoveNumber = Integer.valueOf(fm) - 1;

        pos.state.whiteMaterial = sumMaterial(pos.state, Side.WHITE);
        pos.state.blackMaterial = sumMaterial(pos.state, Side.BLACK);

        pos.state.whiteNumPieces = sumPieces(pos.state, Side.WHITE);
        pos.state.blackNumPieces = sumPieces(pos.state, Side.BLACK);

        var array = new int[1];
        pos.getPiecePositions(Piece.KING, Side.WHITE, array);
        pos.state.whiteKingPos = array[0];
        pos.getPiecePositions(Piece.KING, Side.BLACK, array);
        pos.state.blackKingPos = array[0];

//        System.out.println("White King pos = "+pos.getKingSquare(Side.WHITE));
//        System.out.println("Black King pos = "+pos.getKingSquare(Side.BLACK));
//
//        System.out.println("White material = "+pos.getMaterialValue(Side.WHITE));
//        System.out.println("Black material = "+pos.getMaterialValue(Side.BLACK));

        return pos;
    }
    /**
     * https://en.wikipedia.org/wiki/Portable_Game_Notation
     */
    public static Position fromPGN(String pgn) {
        return null;
    }
    /**
     * https://en.wikipedia.org/wiki/Chess_notation#Notation_systems_for_computers
     */
    public static Position fromEPD(String epd) {
        return null;
    }
    //===============================================================================
    private static int sumMaterial(Position.State state, Side side) {
        return Arrays.stream(state.board)
                     .filter(it->it != Position.EMPTY)
                     .filter(it->Side.get(it&Position.SIDE_MASK)==side)
                     .map(it->Piece.get(it & Position.PIECE_MASK).material)
                     .sum();
    }
    private static int sumPieces(Position.State state, Side side) {
        return (int)Arrays.stream(state.board)
                          .filter(it->(it & Position.PIECE_MASK) != 0)
                          .filter(it->Side.get(it&Position.SIDE_MASK)==side)
                          .count();
    }
}
