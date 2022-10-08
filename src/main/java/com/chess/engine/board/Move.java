package com.chess.engine.board;

import com.chess.engine.Alliance;
import com.chess.engine.pieces.Piece;

import java.util.Objects;
import java.util.Optional;

import static com.chess.engine.pieces.Piece.PieceType;

/**
 * 棋手在棋盘上的移动
 */
public class Move {
    /**
     * Zobrist哈希的键值
     */
    private final long zobristKey;
    /**
     * 移动的棋子
     */
    private final Piece movedPiece;
    /**
     * 目标位置
     */
    private final Coordinate destPosition;
    /**
     * 被吃的棋子
     */
    private final Piece capturedPiece;

    /**
     * 构造
     *
     * @param zobristKey    Zobrist哈希的键值
     * @param movedPiece    移动的棋子
     * @param destPosition  目标位置
     * @param capturedPiece 被吃的棋子
     */
    public Move(long zobristKey, Piece movedPiece, Coordinate destPosition, Piece capturedPiece) {
        this.zobristKey = zobristKey;
        this.movedPiece = movedPiece;
        this.destPosition = destPosition;
        this.capturedPiece = capturedPiece;
    }

    /**
     * 构造
     *
     * @param zobristKey   Zobrist哈希的键值
     * @param movedPiece   移动的棋子
     * @param destPosition 目标位置
     */
    public Move(long zobristKey, Piece movedPiece, Coordinate destPosition) {
        this(zobristKey, movedPiece, destPosition, null);
    }

    /**
     * 与给定字符串表示的相对应的移动(如果有的话)
     *
     * @param board 棋盘
     * @param str   给定的字符串
     * @return 对应于给定字符串表示的移动(如果有的话)
     */
    public static Optional<Move> stringToMove(Board board, String str) {
        // 不合法
        if (str.length() != 6) {
            return Optional.empty();
        }

        // 当前玩家所属阵营（红方或黑方）
        Alliance alliance = board.getCurrPlayer().getAlliance();
        // 旧行
        int formerRow = BoardUtil.rankToRow(charToRank(str.charAt(1)), alliance);
        // 旧列
        int formerCol = BoardUtil.fileToCol(Character.getNumericValue(str.charAt(2)), alliance);
        // 新行
        int newRow = BoardUtil.rankToRow(charToRank(str.charAt(4)), alliance);
        // 新列
        int newCol = BoardUtil.fileToCol(Character.getNumericValue(str.charAt(5)), alliance);

        // 源位置
        Coordinate srcPosition = new Coordinate(formerRow, formerCol);
        // 目标位置
        Coordinate destPosition = new Coordinate(newRow, newCol);

        // 生成对应于给定字符串表示的移动
        return board.getMove(srcPosition, destPosition);
    }

    /**
     * 返回具有给定棋子类型和所属阵营（红方或黑方）的棋子的字符串表示形式，即棋子简写
     *
     * @param pieceType 给定棋子类型
     * @param alliance  所属阵营（红方或黑方）
     * @return 棋子的简写名称
     */
    private static String getPieceAbbrev(PieceType pieceType, Alliance alliance) {
        return alliance.isRed() ? pieceType.toString() : pieceType.toString().toLowerCase();
    }

    /**
     * 返回给定棋盘横格的字符串表示形式
     *
     * @param rank 棋盘横格
     * @return 棋盘横格的字符串表示形式
     */
    private static String rankToString(int rank) {
        return rank < 10 ? Integer.toString(rank) : "X";
    }

    /**
     * 给定字符对应的棋盘横格即行
     *
     * @param rank 棋盘横格的字符串表示形式
     * @return 给定字符对应的棋盘横格即行
     */
    private static int charToRank(char rank) {
        return rank == 'X' ? 10 : Character.getNumericValue(rank);
    }

    /**
     * 获取移动的棋子
     *
     * @return 移动的棋子
     */
    public Piece getMovedPiece() {
        return movedPiece;
    }

    /**
     * 获取目标位置
     *
     * @return 目标位置
     */
    public Coordinate getDestPosition() {
        return destPosition;
    }

    /**
     * 获取被吃的棋子
     *
     * @return 被吃的棋子
     */
    public Optional<Piece> getCapturedPiece() {
        return Optional.ofNullable(capturedPiece);
    }

    /**
     * 有没有吃子
     *
     * @return true, 有吃子，否则，false
     */
    public boolean isCapture() {
        return capturedPiece != null;
    }

    @Override
    public String toString() {
        // 返回表示此移动的字符串符号，表达式遵循以下格式:[棋子缩写][旧的行][旧的列]-[新的行][新的列];第10位用“X”表示
        Coordinate srcPosition = movedPiece.getPosition();
        Alliance alliance = movedPiece.getAlliance();
        PieceType pieceType = movedPiece.getPieceType();

        String formerRank = rankToString(BoardUtil.rowToRank(srcPosition.getRow(), alliance));
        String formerFile = Integer.toString(BoardUtil.colToFile(srcPosition.getCol(), alliance));
        String newRank = rankToString(BoardUtil.rowToRank(destPosition.getRow(), alliance));
        String newFile = Integer.toString(BoardUtil.colToFile(destPosition.getCol(), alliance));

        return getPieceAbbrev(pieceType, alliance) + formerRank + formerFile + "-" + newRank + newFile;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Move)) {
            return false;
        }
        Move other = (Move) obj;
        return this.zobristKey == other.zobristKey && this.movedPiece.equals(other.movedPiece) && this.destPosition.equals(other.destPosition) && this.getCapturedPiece().equals(other.getCapturedPiece());
    }

    @Override
    public int hashCode() {
        return Objects.hash(zobristKey, movedPiece, destPosition, getCapturedPiece());
    }
}
