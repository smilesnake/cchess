package com.chess.gui;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.pieces.Piece;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.chess.gui.Table.*;

/**
 * 显示双方获子及当前游戏状态的面板，即游戏信息面板
 */
class InfoPane extends BorderPane {

    /**
     * 游戏信息面板宽度
     */
    private static final int INFO_PANE_WIDTH = 120;
    /**
     * 游戏信息面板高度
     */
    private static final int INFO_PANE_HEIGHT = 600;
    /**
     * 红方吃子面板
     */
    private final CapturedPane redCapturedPane;
    /**
     * 黑方吃子面板
     */
    private final CapturedPane blackCapturedPane;
    /**
     * 当前游戏状态面板
     */
    private final StatusPane statusPane;

    InfoPane() {
        redCapturedPane = new CapturedPane(Alliance.RED);
        blackCapturedPane = new CapturedPane(Alliance.BLACK);
        statusPane = new StatusPane();

        setTop(blackCapturedPane);
        setBottom(redCapturedPane);
        setCenter(statusPane);

        setPrefSize(INFO_PANE_WIDTH, INFO_PANE_HEIGHT);
        setMinSize(INFO_PANE_WIDTH, INFO_PANE_HEIGHT);
        setMaxSize(INFO_PANE_WIDTH, INFO_PANE_HEIGHT);
    }

    /**
     * 基于给定的棋盘和移动日志更新状态和显示吃子的面板。
     *
     * @param board   当前面板.
     * @param movelog 当前移动日志.
     */
    void update(Board board, MoveLog movelog) {
        redCapturedPane.update(movelog);
        blackCapturedPane.update(movelog);
        statusPane.update(board);
    }

    /**
     * 一个显示双方吃子的面板
     */
    private static class CapturedPane extends GridPane {

        /**
         * 面板高度
         */
        private static final int CAPTURED_PANE_HEIGHT = 250;
        /**
         * 吃子面板颜色
         */
        private static final Color CAPTURED_PANE_COLOR = Color.LIGHTGRAY;
        /**
         * 吃子面板背景颜色
         */
        private static final Background CAPTURED_PANE_BACKGROUND = new Background(new BackgroundFill(CAPTURED_PANE_COLOR, CornerRadii.EMPTY, Insets.EMPTY));

        /**
         * 所属阵营（红方或黑方）
         */
        private final Alliance alliance;

        /**
         * 构造
         *
         * @param alliance 所属阵营（红方或黑方）
         */
        private CapturedPane(Alliance alliance) {
            this.alliance = alliance;
            setBackground(CAPTURED_PANE_BACKGROUND);
            setPrefSize(INFO_PANE_WIDTH, CAPTURED_PANE_HEIGHT);
        }

        /**
         * 根据给定的移动日志更新吃子的面板
         */
        private void update(MoveLog movelog) {
            getChildren().clear();
            List<Piece> capturedPieces = new ArrayList<>();

            // 添加被吃的棋子
            movelog.getMoves().stream().map(Move::getCapturedPiece).forEach(capturedPiece -> capturedPiece.ifPresent(p -> {
                if (p.getAlliance().equals(alliance)) {
                    capturedPieces.add(p);
                }
            }));

            // 按棋子类型排序
            capturedPieces.sort(Comparator.comparing(Piece::getPieceType));

            for (int i = 0; i < capturedPieces.size(); i++) {
                Piece piece = capturedPieces.get(i);
                // 名称
                String name = (piece.getAlliance().toString().substring(0, 1) + piece.getPieceType().toString()).toLowerCase();
                // 所属图片
                Image image = PIECE_IMAGE_MAP.get(name);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(image.getWidth() / 2);
                imageView.setFitHeight(image.getHeight() / 2);

                // 棋子组件
                Label label = new Label();
                label.setGraphic(imageView);
                label.setPrefSize(INFO_PANE_WIDTH / 2, CAPTURED_PANE_HEIGHT / 8);
                label.setAlignment(Pos.CENTER);
                // 加入棋子
                add(label, i % 2, i / 2);
            }
        }
    }

    /**
     * 显示游戏当前状态的面板
     */
    private static class StatusPane extends GridPane {

        private static final int STATUS_PANE_HEIGHT = 100;
        private static final Font TOP_FONT = Font.font("System", FontWeight.MEDIUM, Font.getDefault().getSize() + 2);
        private static final Font BOTTOM_FONT = Font.font("System", FontWeight.BOLD, Font.getDefault().getSize() + 4);
        private static final Label CHECK_LABEL = getCheckLabel();
        private static final Label CHECKMATE_LABEL = getCheckmateLabel();
        private static final Label DRAW_LABEL = getDrawLabel();

        private StatusPane() {
            setPrefSize(INFO_PANE_WIDTH, STATUS_PANE_HEIGHT);
        }

        /**
         * Returns a label for CHECK status.
         */
        private static Label getCheckLabel() {
            Label label = new Label("Check");
            label.setFont(BOTTOM_FONT);
            label.setAlignment(Pos.CENTER);
            label.setPrefSize(INFO_PANE_WIDTH, STATUS_PANE_HEIGHT / 2);

            return label;
        }

        /**
         * Returns a label for CHECKMATE status.
         */
        private static Label getCheckmateLabel() {
            Label label = new Label("Checkmate");
            label.setFont(BOTTOM_FONT);
            label.setAlignment(Pos.CENTER);
            label.setPrefSize(INFO_PANE_WIDTH, STATUS_PANE_HEIGHT / 2);

            return label;
        }

        /**
         * Returns a label for DRAW status.
         */
        private static Label getDrawLabel() {
            Label label = new Label("Draw");
            label.setFont(BOTTOM_FONT);
            label.setAlignment(Pos.CENTER);
            label.setPrefSize(INFO_PANE_WIDTH, STATUS_PANE_HEIGHT / 2);

            return label;
        }

        /**
         * Updates this status pane based on the given board.
         */
        private void update(Board board) {
            getChildren().clear();

            if (board.isCurrPlayerCheckmated()) {
                Label gameOverLabel = new Label(board.getOppPlayer().getAlliance().toString() + " wins");
                gameOverLabel.setFont(TOP_FONT);
                gameOverLabel.setAlignment(Pos.CENTER);
                gameOverLabel.setPrefSize(INFO_PANE_WIDTH, STATUS_PANE_HEIGHT / 2);
                add(gameOverLabel, 0, 0);
                add(GuiUtil.getSeparator(), 0, 1);
                add(CHECKMATE_LABEL, 0, 2);
                return;
            }

            String moveString = board.getCurrPlayer().getAlliance().toString() + "'s move";
            if (GameSetup.getInstance().isAIPlayer(board.getCurrPlayer().getAlliance())) {
                moveString += " (AI)";
            }
            Label moveLabel = new Label(moveString);
            moveLabel.setFont(TOP_FONT);
            moveLabel.setAlignment(Pos.CENTER);
            moveLabel.setPrefSize(INFO_PANE_WIDTH, STATUS_PANE_HEIGHT / 2);
            add(moveLabel, 0, 0);
            add(GuiUtil.getSeparator(), 0, 1);
            if (board.isGameDraw()) {
                add(DRAW_LABEL, 0, 2);
            } else if (board.getCurrPlayer().isInCheck()) {
                add(CHECK_LABEL, 0, 2);
            }
        }
    }

    /**
     * Sets the direction of the captured panes based on the given board direction.
     *
     * @param direction The current board direction.
     */
    void setDirection(BoardDirection direction) {
        getChildren().remove(blackCapturedPane);
        getChildren().remove(redCapturedPane);

        if (direction.isNormal()) {
            setTop(blackCapturedPane);
            setBottom(redCapturedPane);
        } else {
            setTop(redCapturedPane);
            setBottom(blackCapturedPane);
        }
    }
}
