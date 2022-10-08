package com.chess.engine.board;

import com.chess.engine.pieces.Piece;

import java.util.Optional;

/**
 * 棋盘上的每一个点
 */
public class Point {
    /**
     * 位置
     */
    private final Coordinate position;
    /**
     * 棋子
     */
    private Piece piece;

    /**
     * 构造
     *
     * @param position 位置
     */
    Point(Coordinate position) {
        this.position = position;
    }

    /**
     * 设置棋子
     *
     * @param piece 棋子
     */
    void setPiece(Piece piece) {
        this.piece = piece;
    }

    /**
     * 移除棋子
     */
    void removePiece() {
        piece = null;
    }

    /**
     * 当前点位是否为空
     *
     * @return true, 当前点位为空，否则，false
     */
    public boolean isEmpty() {
        return piece == null;
    }

    /**
     * 获取当前点位坐标
     *
     * @return 当前点位坐标
     */
    public Coordinate getPosition() {
        return position;
    }

    /**
     * 获取当前位置的棋子
     *
     * @return 当前位置的棋子（Optional对象）
     */
    public Optional<Piece> getPiece() {
        return Optional.ofNullable(piece);
    }

    @Override
    public String toString() {
        if (piece == null) {
            return "-";
        }
        if (piece.getAlliance().isRed()) {
            return piece.toString();
        } else {
            return piece.toString().toLowerCase();
        }
    }
}
