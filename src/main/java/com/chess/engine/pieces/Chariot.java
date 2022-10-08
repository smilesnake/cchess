package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtil;
import com.chess.engine.board.Coordinate;
import com.chess.engine.board.Move;

import java.util.*;

/**
 * 车
 */
public class Chariot extends Piece {
    /**
     * 移动的向量
     */
    private static final List<Coordinate> MOVE_VECTORS = List.of(new Coordinate(-1, 0), new Coordinate(0, -1), new Coordinate(1, 0), new Coordinate(0, 1));

    /**
     * 构造
     *
     * @param position 位置
     * @param alliance 所属阵营（红方或黑方）
     */
    public Chariot(Coordinate position, Alliance alliance) {
        super(PieceType.CHARIOT, position, alliance);
    }

    @Override
    public Collection<Move> getLegalMoves(Board board) {
        // 当前棋子在给定棋盘上可以采取的合法走法的集合
        List<Move> legalMoves = new ArrayList<>();

        for (Coordinate vector : MOVE_VECTORS) {
            // 目标位置
            Coordinate destPosition = position.add(vector);

            // 当目标位置在棋盘范围内
            while (BoardUtil.isWithinBounds(destPosition)) {
                // 目标位置的棋子
                Optional<Piece> destPiece = board.getPoint(destPosition).getPiece();
                // 将移动加入至当前棋子在给定棋盘上可以采取的合法走法的集合
                if (destPiece.isPresent()) {
                    if (!destPiece.get().alliance.equals(this.alliance)) {
                        // 如果不属于同一阵营，吃子
                        legalMoves.add(new Move(board.getZobristKey(), this, destPosition, destPiece.get()));
                    }
                    break;
                } else {
                    legalMoves.add(new Move(board.getZobristKey(), this, destPosition));
                }

                // 继续追加
                destPosition = destPosition.add(vector);
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

            // 当目标位置在棋盘范围内
            while (BoardUtil.isWithinBounds(destPosition)) {
                // 目标位置的棋子
                Optional<Piece> destPiece = board.getPoint(destPosition).getPiece();
                // 将移动加入至当前棋子在给定棋盘上可以采取的合法走法的集合
                if (destPiece.isPresent()) {
                    if (!destPiece.get().alliance.equals(this.alliance)) {
                        legalMoves.add(new Move(board.getZobristKey(), this, destPosition, destPiece.get()));
                        // 不是同一阵营，加入攻击棋子列表
                        attackedPieces.add(destPiece.get());
                    } else {
                        // 是同一阵营，加入防御棋子列表
                        defendedPieces.add(destPiece.get());
                    }
                    break;
                } else {
                    legalMoves.add(new Move(board.getZobristKey(), this, destPosition));
                }
                // 继续追加
                destPosition = destPosition.add(vector);
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
    public Chariot movePiece(Move move) {
        // 移动棋子并生成一个新坐标的棋子对象
        return new Chariot(move.getDestPosition(), move.getMovedPiece().getAlliance());
    }

    @Override
    public Chariot getMirrorPiece() {
        // 镜像位置
        Coordinate mirrorPosition = new Coordinate(position.getRow(), Board.NUM_COLS - 1 - position.getCol());
        // 生成一个新坐标的棋子对象
        return new Chariot(mirrorPosition, alliance);
    }
}
