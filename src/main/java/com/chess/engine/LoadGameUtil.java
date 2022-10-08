package com.chess.engine;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用于加载已保存游戏的帮助类
 */
public class LoadGameUtil {

    /**
     * 中国象棋棋盘
     */
    private Board board;
    /**
     * 移动列表
     */
    private List<Move> moves;
    /**
     * 是否有效的，true,有效的，false，无效的
     */
    private boolean isValid;

    public LoadGameUtil(File file) {
        moves = new ArrayList<>();
        isValid = true;

        // 初始化棋盘
        board = Board.initialiseBoard();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str;

            while (isValid && (str = br.readLine()) != null) {
                // 给定字符串转换为落子对象，即棋手在棋盘上的移动
                Optional<Move> move = Move.stringToMove(board, str);
                if (move.isPresent()) {
                    // 落子
                    board.makeMove(move.get());
                    // 添加落子记录
                    moves.add(move.get());
                } else {
                    // 无效字符串
                    isValid = false;
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取当前棋盘
     *
     * @return 当前棋盘对象
     */
    public Board getBoard() {
        return board;
    }

    /**
     * 获取落子记录
     *
     * @return 落子记录
     */
    public List<Move> getMoves() {
        return moves;
    }

    /**
     * 文件是否有效
     *
     * @return true, 有效，否则，无效
     */
    public boolean isValidFile() {
        return isValid;
    }
}
