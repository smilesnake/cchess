package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtil;
import com.chess.engine.board.Coordinate;
import com.chess.engine.board.Move;

import java.util.*;

/**
 * 将/帅
 */
public class General extends Piece {
    /**
     * 移动的向量
     */
    private static final List<Coordinate> MOVE_VECTORS = List.of(new Coordinate(-1, 0), new Coordinate(0, -1), new Coordinate(1, 0), new Coordinate(0, 1));
    /**
     * 向前的向量
     */
    private static final Coordinate FORWARD_VECTOR = MOVE_VECTORS.get(2);
    /**
     * 红方开始位置
     */
    private static final Coordinate STARTING_POSITION_RED = new Coordinate(9, 4);
    /**
     * 黑方开始位置
     */
    private static final Coordinate STARTING_POSITION_BLACK = new Coordinate(0, 4);

    /**
     * 构造
     *
     * @param position 位置
     * @param alliance 所属阵营（红方或黑方）
     */
    public General(Coordinate position, Alliance alliance) {
        super(PieceType.GENERAL, position, alliance);
    }

    @Override
    public Collection<Move> getLegalMoves(Board board) {
        // 当前棋子在给定棋盘上可以采取的合法走法的集合
        List<Move> legalMoves = new ArrayList<>();

        // 遍历移动的向量
        for (Coordinate vector : MOVE_VECTORS) {
            // 目标位置
            Coordinate destPosition = position.add(vector);
            // 验证位置是否有效
            if (isValidPosition(destPosition)) {
                // 目标位置的棋子
                Optional<Piece> destPiece = board.getPoint(destPosition).getPiece();
                // 将移动加入至当前棋子在给定棋盘上可以采取的合法走法的集合
                destPiece.ifPresentOrElse(p -> {
                    if (!p.alliance.equals(this.alliance)) {
                        // 如果不属于同一阵营，吃子
                        legalMoves.add(new Move(board.getZobristKey(), this, destPosition, p));
                    }
                }, () -> legalMoves.add(new Move(board.getZobristKey(), this, destPosition)));
            }
        }

        // 飞将即将、帅碰面（仅用于强制检查）
        Coordinate vector = FORWARD_VECTOR.scale(alliance.getDirection());
        // 当前位置
        Coordinate currPosition = position.add(vector);
        // 当前位置在棋盘范围内
        while (BoardUtil.isWithinBounds(currPosition)) {
            // 当前位置上的棋子
            Optional<Piece> piece = board.getPoint(currPosition).getPiece();
            if (piece.isPresent()) {
                if (piece.get().getPieceType().equals(PieceType.GENERAL)) {
                    // 吃将/帅
                    legalMoves.add(new Move(board.getZobristKey(), this, currPosition, piece.get()));
                }
                break;
            }
            // 继续追加
            currPosition = currPosition.add(vector);
        }

        // 返回符合所选棋子在给定棋盘上可以采取的合法走法的集合
        return Collections.unmodifiableList(legalMoves);
    }

    @Override
    public Collection<Move> getLegalMoves(Board board, Collection<Attack> attacks, Collection<Defense> defenses) {
        // 当前棋子在给定棋盘上可以采取的合法走法的集合
        List<Move> legalMoves = new ArrayList<>();
        // 攻击棋子列表
        List<Piece> attackedPieces = new ArrayList<>();
        // 防御棋子列表.
        List<Piece> defendedPieces = new ArrayList<>();
        // 遍历移动的向量
        for (Coordinate vector : MOVE_VECTORS) {
            // 目标位置
            Coordinate destPosition = position.add(vector);
            // 验证位置是否有效
            if (isValidPosition(destPosition)) {
                // 目标位置的棋子
                Optional<Piece> destPiece = board.getPoint(destPosition).getPiece();
                // 将移动加入至当前棋子在给定棋盘上可以采取的合法走法的集合
                destPiece.ifPresentOrElse(p -> {
                    if (!p.alliance.equals(this.alliance)) {
                        legalMoves.add(new Move(board.getZobristKey(), this, destPosition, p));
                        // 不是同一阵营，加入攻击棋子列表
                        attackedPieces.add(p);
                    } else {
                        // 是同一阵营，加入防御棋子列表
                        defendedPieces.add(p);
                    }
                }, () -> legalMoves.add(new Move(board.getZobristKey(), this, destPosition)));
            }
        }

        // 飞将即将、帅碰面（仅用于强制检查）
        Coordinate vector = FORWARD_VECTOR.scale(alliance.getDirection());
        // 当前位置
        Coordinate currPosition = position.add(vector);
        // 当前位置在棋盘范围内
        while (BoardUtil.isWithinBounds(currPosition)) {
            // 当前位置上的棋子
            Optional<Piece> piece = board.getPoint(currPosition).getPiece();
            if (piece.isPresent()) {
                if (piece.get().getPieceType().equals(PieceType.GENERAL)) {
                    // 吃将/帅
                    legalMoves.add(new Move(board.getZobristKey(), this, currPosition, piece.get()));
                    // 加入攻击棋子列表
                    attackedPieces.add(piece.get());
                }
                break;
            }

            // 继续追加
            currPosition = currPosition.add(vector);
        }

        // 构建攻击对象
        Attack attack = new Attack(this, attackedPieces);
        // 构建防卫对象
        Defense defense = new Defense(this, defendedPieces);

        // 加入列表
        attacks.add(attack);
        defenses.add(defense);

        // 返回符合所选棋子在给定棋盘上可以采取的合法走法的集合
        return Collections.unmodifiableList(legalMoves);
    }

    @Override
    public General movePiece(Move move) {
        // 移动棋子并生成一个新坐标的棋子对象
        return new General(move.getDestPosition(), move.getMovedPiece().getAlliance());
    }

    @Override
    public General getMirrorPiece() {
        // 镜像位置
        Coordinate mirrorPosition = new Coordinate(position.getRow(), Board.NUM_COLS - 1 - position.getCol());
        // 生成一个新坐标的棋子对象
        return new General(mirrorPosition, alliance);
    }

    /**
     * 检查给定的位置对当前棋子(将/帅)是否有效
     *
     * @return true，有效，否则，false
     */
    private boolean isValidPosition(Coordinate positionToTest) {
        // 行
        int row = positionToTest.getRow();
        // 列
        int col = positionToTest.getCol();

        // 检查是否在棋子行棋范围
        if (alliance.isRed()) {
            return (row >= 7 && row <= 9) && (col >= 3 && col <= 5);
        } else {
            return (row >= 0 && row <= 2) && (col >= 3 && col <= 5);
        }
    }

    /**
     * 获取开始位置
     *
     * @param alliance 所属阵营（红方或黑方）
     * @return 所属阵营（红方或黑方）的将/帅开始位置
     */
    public static Coordinate getStartingPosition(Alliance alliance) {
        return alliance.isRed() ? STARTING_POSITION_RED : STARTING_POSITION_BLACK;
    }
}
