package com.chess.engine.board;

import com.chess.engine.Alliance;

/**
 * 棋盘工具类
 */
public class BoardUtil {

    /**
     * 检查给定位置是否在棋盘边界内
     *
     * @param position 给定位置
     * @return true，在边界内，否则，false
     */
    public static boolean isWithinBounds(Coordinate position) {
        int row = position.getRow();
        int col = position.getCol();

        return (row >= 0 && row < Board.NUM_ROWS) && (col >= 0 && col < Board.NUM_COLS);
    }

    /**
     * 给定位置的镜像版本
     *
     * @param position 给定位置
     * @return 给定位置的镜像版本
     */
    public static Coordinate getMirrorPosition(Coordinate position) {
        return new Coordinate(position.getRow(), Board.NUM_COLS - 1 - position.getCol());
    }

    /**
     * 根据位置的行和列返回位置的索引.
     *
     * @param row 行
     * @param col 列
     * @return 位置对应的索引
     */
    public static int positionToIndex(int row, int col) {
        return row * Board.NUM_COLS + col;
    }

    /**
     * 返回给定位置的索引
     *
     * @param position 给定的位置
     * @return 给定位置的索引
     */
    public static int positionToIndex(Coordinate position) {
        return positionToIndex(position.getRow(), position.getCol());
    }

    /**
     * 根据给定的所属阵营（红方或黑方）将列号转换为相应的档案编号
     *
     * @param col      列号
     * @param alliance 所属阵营（红方或黑方）
     * @return 转换后相应的档案编号
     */
    public static int colToFile(int col, Alliance alliance) {
        return alliance.isRed() ? Board.NUM_COLS - col : col + 1;
    }

    /**
     * 根据给定的所属阵营（红方或黑方）将档案编号转换为相应的列编号
     *
     * @param file     档案编号
     * @param alliance 给定的所属阵营（红方或黑方）
     * @return 转换后相应的列编号
     */
    public static int fileToCol(int file, Alliance alliance) {
        return alliance.isRed() ? Board.NUM_COLS - file : file - 1;
    }

    /**
     * 根据给定的所属阵营（红方或黑方）将行号转换为相应的棋盘横格即行
     *
     * @param row      行号
     * @param alliance 所属阵营（红方或黑方）
     * @return 转换后相应的棋盘横格即行
     */
    public static int rowToRank(int row, Alliance alliance) {
        return alliance.isRed() ? Board.NUM_ROWS - row : row + 1;
    }

    /**
     * 根据给定的所属阵营将行转换为相应的行号
     *
     * @param rank     行号
     * @param alliance 所属阵营（红方或黑方）
     * @return 转换后的相应行号
     */
    public static int rankToRow(int rank, Alliance alliance) {
        return alliance.isRed() ? Board.NUM_ROWS - rank : rank - 1;
    }

    /**
     * 检查给定的位置是否在同一行或列中
     *
     * @param first  给定第一个位置
     * @param others 给定的其它位置
     * @return true, 表示在同一行或列中，否则，false
     */
    public static boolean sameColOrRow(Coordinate first, Coordinate... others) {
        boolean sameRow = true;
        boolean sameCol = true;

        for (Coordinate position : others) {
            if (position.getRow() != first.getRow()) {
                sameRow = false;
            }
            if (position.getCol() != first.getCol()) {
                sameCol = false;
            }
            // 不在同一行或同一列，则返回false
            if (!(sameRow || sameCol)) {
                return false;
            }
        }

        // 默认在同一行或同一列
        return true;
    }
}
