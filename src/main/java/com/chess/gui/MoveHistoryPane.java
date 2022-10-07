package com.chess.gui;

import com.chess.engine.board.Move;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;

import static com.chess.gui.Table.MoveLog;
import static javafx.scene.control.Alert.AlertType;

/**
 * 显示游戏的移动历史的面板
 */
class MoveHistoryPane extends BorderPane {

    /**
     * 历史面板宽度
     */
    private static final int HISTORY_PANE_WIDTH = 120;
    /**
     * 历史面板高度
     */
    private static final int HISTORY_PANE_HEIGHT = 600;
    /**
     * 空表格消息
     */
    private static final Label EMPTY_TABLE_MESSAGE = new Label("没有移动记录");
    /**
     * 上一步
     */
    private static final Image PREV_MOVE = new Image(MoveHistoryPane.class.getResourceAsStream(GuiUtil.GRAPHICS_MISC_PATH + "prev.png"));
    /**
     * 下一步
     */
    private static final Image NEXT_MOVE = new Image(MoveHistoryPane.class.getResourceAsStream(GuiUtil.GRAPHICS_MISC_PATH + "next.png"));
    /**
     * 第一步
     */
    private static final Image START_MOVE = new Image(MoveHistoryPane.class.getResourceAsStream(GuiUtil.GRAPHICS_MISC_PATH + "start.png"));
    /**
     * 最后一步
     */
    private static final Image END_MOVE = new Image(MoveHistoryPane.class.getResourceAsStream(GuiUtil.GRAPHICS_MISC_PATH + "end.png"));

    private final ReplayPane replayPane;
    private final TableView<Turn> turnTableView;
    /**
     * 回合列表
     */
    private final ObservableList<Turn> turnList;

    MoveHistoryPane() {
        turnList = FXCollections.observableList(new ArrayList<>());
        turnTableView = new TableView<>(turnList);

        TableColumn<Turn, String> redMoveCol = new TableColumn<>("RED");
        redMoveCol.setCellValueFactory(new PropertyValueFactory<>("redMove"));
        redMoveCol.setSortable(false);
//        redMoveCol.setReorderable(false);
        TableColumn<Turn, String> blackMoveCol = new TableColumn<>("BLACK");
        blackMoveCol.setCellValueFactory(new PropertyValueFactory<>("blackMove"));
        blackMoveCol.setSortable(false);
//        blackMoveCol.setReorderable(false);

        turnTableView.getColumns().setAll(redMoveCol, blackMoveCol);
        turnTableView.getSelectionModel().setCellSelectionEnabled(true);
        turnTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        turnTableView.setPrefWidth(HISTORY_PANE_WIDTH);
        turnTableView.setPrefHeight(HISTORY_PANE_HEIGHT);
        turnTableView.setPlaceholder(EMPTY_TABLE_MESSAGE);

        replayPane = new ReplayPane();
        ObservableList<TablePosition> selectedCells = turnTableView.getSelectionModel().getSelectedCells();
        selectedCells.addListener((ListChangeListener<TablePosition>) c -> {
            if (selectedCells.isEmpty()) {
                if (!turnList.isEmpty()) {
                    Table.getInstance().jumpToMove(-1);
                }
                return;
            }
            if (!replayPane.toggleReplay.isSelected()) {
                replayPane.toggleReplay.fire();
            }
            TablePosition tablePosition = selectedCells.get(0);
            int moveIndex = tablePosition.getRow() * 2 + tablePosition.getColumn();
            Table.getInstance().jumpToMove(moveIndex);
        });

        setTop(replayPane);
        setCenter(turnTableView);
        setVisible(true);
    }

    /**
     * 根据给定的移动日志更新此移动历史面板
     *
     * @param movelog 给定的移动日志.
     */
    void update(MoveLog movelog) {
        // 清空回合列表
        turnList.clear();
        // 没有落子记录设置占位符消息
        if (movelog.isEmpty()) {
            turnTableView.setPlaceholder(EMPTY_TABLE_MESSAGE);
            return;
        }

        Turn currTurn = new Turn();
        for (Move move : movelog.getMoves()) {
            // 红方，说明又是新的回合
            if (move.getMovedPiece().getAlliance().isRed()) {
                currTurn = new Turn();
                // 设置红方移动
                currTurn.setRedMove(move.toString());
                turnList.add(currTurn);
            } else {
                // 设置黑方移动
                currTurn.setBlackMove(move.toString());
            }
        }
        // 滚动至指定位置
        turnTableView.scrollTo(turnList.size() - 1);
    }

    /**
     * 用于回放/重播的面板。
     */
    private class ReplayPane extends GridPane {
        /**
         * 回放/重播按钮
         */
        private final ToggleButton toggleReplay;
        /**
         * 上一步按钮
         */
        private final Button prevMove;
        /**
         * 下一步按钮
         */
        private final Button nextMove;
        /**
         * 第一步按钮
         */
        private final Button startMove;
        /**
         * 最后一步按钮
         */
        private final Button endMove;

        private ReplayPane() {
            toggleReplay = new ToggleButton("回放/重播");
            toggleReplay.setOnAction(e -> {
                if (toggleReplay.isSelected()) {
                    if (!turnList.isEmpty()) {
                        disableReplayButtons(false);
                        if (turnTableView.getSelectionModel().getSelectedCells().isEmpty()) {
                            turnTableView.getSelectionModel().select(0, turnTableView.getColumns().get(0));
                            turnTableView.scrollTo(0);
                        }
                    } else {
                        Alert alert = new Alert(AlertType.INFORMATION, "No moves made");
                        alert.showAndWait();
                        toggleReplay.setSelected(false);
                    }
                } else {
                    disableReplayButtons(true);
                    turnTableView.getSelectionModel().clearSelection();
                }
            });
            toggleReplay.setSelected(false);
            toggleReplay.setPrefWidth(HISTORY_PANE_WIDTH);

            GridPane navigationPane = new GridPane();
            prevMove = new Button("", new ImageView(PREV_MOVE));
            nextMove = new Button("", new ImageView(NEXT_MOVE));
            startMove = new Button("", new ImageView(START_MOVE));
            endMove = new Button("", new ImageView(END_MOVE));

            TableColumn<Turn, ?> redMoveCol = turnTableView.getColumns().get(0);
            TableColumn<Turn, ?> blackMoveCol = turnTableView.getColumns().get(1);
            ObservableList<TablePosition> selectedCells = turnTableView.getSelectionModel().getSelectedCells();
            prevMove.setOnAction(e -> {
                TablePosition tablePosition = selectedCells.get(0);
                if (tablePosition.getTableColumn().equals(blackMoveCol)) {
                    turnTableView.getSelectionModel().clearAndSelect(tablePosition.getRow(), redMoveCol);
                } else if (tablePosition.getTableColumn().equals(redMoveCol) && tablePosition.getRow() > 0) {
                    turnTableView.getSelectionModel().clearAndSelect(tablePosition.getRow() - 1, blackMoveCol);
                }
                turnTableView.scrollTo(turnTableView.getSelectionModel().getSelectedIndex());
            });
            nextMove.setOnAction(e -> {
                TablePosition tablePosition = selectedCells.get(0);
                Turn currTurn = turnList.get(tablePosition.getRow());
                if (tablePosition.getTableColumn().equals(redMoveCol) && currTurn.getBlackMove() != null) {
                    turnTableView.getSelectionModel().clearAndSelect(tablePosition.getRow(), blackMoveCol);
                } else if (tablePosition.getTableColumn().equals(blackMoveCol) && tablePosition.getRow() < turnList.size() - 1) {
                    turnTableView.getSelectionModel().clearAndSelect(tablePosition.getRow() + 1, redMoveCol);
                }
                turnTableView.scrollTo(turnTableView.getSelectionModel().getSelectedIndex());
            });
            startMove.setOnAction(e -> {
                turnTableView.getSelectionModel().clearAndSelect(0, redMoveCol);
                turnTableView.scrollTo(turnTableView.getSelectionModel().getSelectedIndex());
            });
            endMove.setOnAction(e -> {
                Turn lastTurn = turnList.get(turnList.size() - 1);
                if (lastTurn.getBlackMove() != null) {
                    turnTableView.getSelectionModel().clearAndSelect(turnList.size() - 1, blackMoveCol);
                } else {
                    turnTableView.getSelectionModel().clearAndSelect(turnList.size() - 1, redMoveCol);
                }
                turnTableView.scrollTo(turnTableView.getSelectionModel().getSelectedIndex());
            });
            disableReplayButtons(true);
            prevMove.setPrefWidth(HISTORY_PANE_WIDTH / 2);
            nextMove.setPrefWidth(HISTORY_PANE_WIDTH / 2);
            startMove.setPrefWidth(HISTORY_PANE_WIDTH / 2);
            endMove.setPrefWidth(HISTORY_PANE_WIDTH / 2);
            navigationPane.add(prevMove, 0, 0);
            navigationPane.add(nextMove, 1, 0);
            navigationPane.add(startMove, 0, 1);
            navigationPane.add(endMove, 1, 1);

            add(toggleReplay, 0, 0);
            add(navigationPane, 0, 1);
        }

        /**
         * Disables/enables all replay buttons.
         */
        private void disableReplayButtons(boolean disabled) {
            nextMove.setDisable(disabled);
            prevMove.setDisable(disabled);
            startMove.setDisable(disabled);
            endMove.setDisable(disabled);
        }
    }

    /**
     * Disables replay mode.
     */
    void disableReplay() {
        if (replayPane.toggleReplay.isSelected()) {
            replayPane.toggleReplay.fire();
        }
    }

    /**
     * 检查游戏是否处于回放/重播模式
     *
     * @return true, 当前处于回放/重播模式，否则，false
     */
    boolean isInReplayMode() {
        return replayPane.toggleReplay.isSelected();
    }

    /**
     * 回合类，表示一对连续的红黑棋手移动
     */
    public static class Turn {

        /**
         * 红方移动
         */
        private StringProperty redMove;
        /**
         * 黑方移动
         */
        private StringProperty blackMove;

        /**
         * 获取红方移动字符
         *
         * @return 红方移动字符
         */
        public String getRedMove() {
            return redMoveProperty().get();
        }

        /**
         * 获取黑方移动字符
         *
         * @return 黑方移动字符
         */
        public String getBlackMove() {
            return blackMoveProperty().get();
        }

        /**
         * 设置红方移动字符
         *
         * @param move 红方移动字符
         */
        private void setRedMove(String move) {
            redMoveProperty().set(move);
        }

        /**
         * 设置黑方移动字符
         *
         * @param move 黑方移动字符
         */
        private void setBlackMove(String move) {
            blackMoveProperty().set(move);
        }

        /**
         * 红方移动属性
         *
         * @return 字符串属性
         */
        private StringProperty redMoveProperty() {
            if (redMove == null) {
                redMove = new SimpleStringProperty(this, "redMove");
            }
            return redMove;
        }

        /**
         * 黑方移动属性
         *
         * @return 字符串属性
         */
        private StringProperty blackMoveProperty() {
            if (blackMove == null) {
                blackMove = new SimpleStringProperty(this, "blackMove");
            }
            return blackMove;
        }
    }
}
