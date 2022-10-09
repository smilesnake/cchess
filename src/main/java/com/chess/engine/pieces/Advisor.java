package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Coordinate;
import com.chess.engine.board.Move;

import java.util.*;

/**
 * 士/仕
 */
public class Advisor extends Piece {
    /**
     * 移动的向量
     */
    private static final List<Coordinate> MOVE_VECTORS = List.of(new Coordinate(-1, -1), new Coordinate(1, -1), new Coordinate(1, 1), new Coordinate(-1, 1));

    /**
     * 红方时棋子有效的位置
     */
    private static final Set<Coordinate> VALID_POSITIONS_RED = Set.of(new Coordinate(7, 3), new Coordinate(7, 5), new Coordinate(8, 4), new Coordinate(9, 3), new Coordinate(9, 5));
    /**
     * 黑方时棋子有效的位置
     */
    private static final Set<Coordinate> VALID_POSITIONS_BLACK = Set.of(new Coordinate(0, 3), new Coordinate(0, 5), new Coordinate(1, 4), new Coordinate(2, 3), new Coordinate(2, 5));

    /**
     * 构造
     *
     * @param position 位置
     * @param alliance 所属阵营（红方或黑方）
     */
    public Advisor(Coordinate position, Alliance alliance) {
        super(PieceType.ADVISOR, position, alliance);
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
    public Advisor movePiece(Move move) {
        // 移动棋子并生成一个新坐标的棋子对象
        return new Advisor(move.getDestPosition(), move.getMovedPiece().getAlliance());
    }

    @Override
    public Advisor getMirrorPiece() {
        // 镜像位置
        Coordinate mirrorPosition = new Coordinate(position.getRow(), Board.NUM_COLS - 1 - position.getCol());
        // 生成一个新坐标的棋子对象
        return new Advisor(mirrorPosition, alliance);
    }

    /**
     * 检查给定的位置对当前棋子(士/仕)是否有效
     *
     * @return true，有效，否则，false
     */
    private boolean isValidPosition(Coordinate positionToTest) {
        if (alliance.isRed()) {
            return VALID_POSITIONS_RED.contains(positionToTest);
        } else {
            return VALID_POSITIONS_BLACK.contains(positionToTest);
        }
    }
}
