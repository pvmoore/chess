package chess.engine.bitboard;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static juice.Util.exceptionContext;

/**
 * A bit board is an unsigned long (64 bits) representing a flag for each square on the chess board.
 *
 *  Byte Bit
 *       80 40 20 10 08 04 02 01| row
 *  [7]   0  0  0  0  0  0  0  0|  8
 *  [6]   0  0  0  0  0  0  0  0|  7
 *  [5]   0  0  0  0  0  0  0  0|  6
 *  [4]   0  0  0  0  0  0  0  0|  5
 *  [3]   0  0  0  0  0  0  0  0|  4
 *  [2]   0  0  0  0  0  0  0  0|  3
 *  [1]   0  0  0  0  0  0  0  0|  2
 *  [0]   0  0  0  0  0  0  0  0|  1
 *        ----------------------|
 *  col   a  b  c  d  e  f  g  h
 *
 *  eg. a8 = byte[7] & 0x80
 *      h1 = byte[0] & 0x01
 */
final public class BitBoard {

    public static long[] whitePawnMoves;
    public static long[] blackPawnMoves;
    public static long[] whitePawnAttacks;
    public static long[] blackPawnAttacks;
    public static long[] knightMoves;
    public static int[] rankMoves;

    public static String toString(long bb) {
        long temp = bb;
        var buf = new StringBuilder();
        var rows = new ArrayList<String>();
        for(int r=0;r<8;r++) {
            long bit = 0x80;
            for(int c = 0; c < 8; c++) {
                if((temp&bit)!=0) {
                    buf.append("1");
                } else {
                    buf.append("0");
                }
                bit>>>=1;
            }
            if(r==7) buf.append(" ("+Long.toHexString(bb)+")");
            rows.add(0, buf.toString());
            buf.setLength(0);
            temp >>>= 8;
        }
        return rows.stream().collect(Collectors.joining("\n"));
    }
    /**
     *
     * @param file 0..7
     * @return 8-bit rank
     */
    static int rotateFileToRank(long bb, int file) {
        bb <<= file;
        bb &= 0x80808080_80808080L;
        bb |= (bb>>63) | (bb>>54) | (bb>>45) | (bb>>36) | (bb>>27) | (bb>>18) | (bb>>9);
        return (int)bb;
    }
    /**
     *
     * @param r
     * @param file
     * @return 64-bit bitboard
     */
    static long rotateRankToFile(int r, int file) {
        long t  = r;
        long bb = t | (t<<9) | (t<<18) | (t<<27) | (t<<36) | (t<<45) | (t<<54) | (t<<63);
        return bb >> file;
    }
    //=================================================================================
    static {
        whitePawnMoves   = loadLongs("data/pawn_up_moves.dat", 64);
        blackPawnMoves   = loadLongs("data/pawn_down_moves.dat", 64);
        whitePawnAttacks = loadLongs("data/pawn_up_attacks.dat", 64);
        blackPawnAttacks = loadLongs("data/pawn_down_attacks.dat", 64);
        knightMoves      = loadLongs("data/knight_moves.dat", 64);
        rankMoves        = loadInts("data/rank_moves.dat", 256*8);
    }
    private static long[] loadLongs(String filename, int count) {
        return exceptionContext(()->{
            try(var dis = new DataInputStream(new FileInputStream(filename))) {
                long[] array = new long[count];
                for(int i=0; i<64;i++) array[i] = (dis.readLong());
                return array;
            }
        });
    }
    private static int[] loadInts(String filename, int count) {
        return exceptionContext(()->{
            try(var dis = new DataInputStream(new FileInputStream(filename))) {
                int[] array = new int[count];
                for(int i=0; i<count;i++) array[i] = (dis.readInt());
                return array;
            }
        });
    }
}
