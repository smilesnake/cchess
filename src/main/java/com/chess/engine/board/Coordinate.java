package com.chess.engine.board;

import java.util.Objects;

/**
 * 表示棋盘上的位置或移动向量
 */
public class Coordinate {
    /**
     * 行
     */
    private final int row;
    /**
     * 列
     */
    private final int col;

    /**
     * 构造
     *
     * @param row 行
     * @param col 列
     */
    public Coordinate(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * 将给定的移动向量添加到此位置并返回新位置
     *
     * @param vector 要相加的向量.
     * @return 新位置(不保证在棋盘范围内).
     */
    public Coordinate add(Coordinate vector) {
        return new Coordinate(this.row + vector.row, this.col + vector.col);
    }

    /**
     * 用给定的缩放倍数缩放这个移动向量，并返回缩放后的向量
     *
     * @param factor 缩放倍数.
     * @return 按比例缩小的向量.
     */
    public Coordinate scale(int factor) {
        return new Coordinate(this.row * factor, this.col * factor);
    }

    /**
     * 获取所在行
     * @return 所在行
     */
    public int getRow() {
        return row;
    }
    /**
     * 获取所在列
     * @return 所在列
     */
    public int getCol() {
        return col;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Coordinate)) {
            return false;
        }
        Coordinate other = (Coordinate) obj;
        return (this.row == other.row) && (this.col == other.col);
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}
