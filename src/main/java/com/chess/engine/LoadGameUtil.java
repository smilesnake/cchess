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

        board = Board.initialiseBoard();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str;

            while (isValid && (str = br.readLine()) != null) {
                Optional<Move> move = Move.stringToMove(board, str);
                if (move.isPresent()) {
                    board.makeMove(move.get());
                    moves.add(move.get());
                } else {
                    isValid = false;
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Board getBoard() {
        return board;
    }

    public List<Move> getMoves() {
        return moves;
    }

    public boolean isValidFile() {
        return isValid;
    }
}
