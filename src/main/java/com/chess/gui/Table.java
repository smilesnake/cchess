package com.chess.gui;

import com.chess.CChess;
import com.chess.engine.LoadGameUtil;
import com.chess.engine.board.Board;
import com.chess.engine.board.Coordinate;
import com.chess.engine.board.Move;
import com.chess.engine.board.Point;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.ai.FixedDepthSearch;
import com.chess.engine.player.ai.FixedTimeSearch;
import com.chess.engine.player.ai.MoveBook;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.chess.engine.pieces.Piece.PieceType;
import static javafx.scene.control.Alert.AlertType;

/**
 * 棋盘表格
 */
public class Table extends BorderPane {

    /**
     * 面板宽度
     */
    private static final int BOARD_WIDTH = 540;
    /**
     * 面板高度
     */
    private static final int BOARD_HEIGHT = 600;
    /**
     * 点的宽度
     */
    private static final int POINT_WIDTH = 60;
    /**
     * 棋盘图片
     */
    private static final Image BOARD_IMAGE = new Image(Table.class.getResourceAsStream("/graphics/board.png"));
    /**
     * 规则图片高亮图片，即当前棋子所有可落子点位的单个点位图片
     */
    private static final Image HIGHLIGHT_LEGALS_IMAGE = new Image(Table.class.getResourceAsStream(GuiUtil.GRAPHICS_MISC_PATH + "dot.png"));
    /**
     * 高亮最后移动的虚线边框
     */
    private static final Border HIGHLIGHT_LAST_MOVE_BORDER = new Border(new BorderStroke(Color.WHITE, BorderStrokeStyle.DASHED, new CornerRadii(POINT_WIDTH / 2), new BorderWidths(2)));
    /**
     * 高亮选中移动的虚线边框
     */
    private static final Border HIGHLIGHT_SELECTED_PIECE_BORDER = new Border(new BorderStroke(Color.WHITE, BorderStrokeStyle.SOLID, new CornerRadii(POINT_WIDTH / 2), new BorderWidths(2)));
    /**
     * 象棋WIKI
     */
    private static final String WIKI_XIANGQI = "https://en.wikipedia.org/wiki/Xiangqi";
    /**
     * 棋子图片Map
     */
    static final Map<String, Image> PIECE_IMAGE_MAP = getPieceImageMap();
    /**
     * 当前实例
     */
    private static final Table TABLE_INSTANCE = new Table();
    /**
     * 游戏设置对话框
     */
    private final GameSetup gameSetup;
    /**
     * 游戏棋盘的面板
     */
    private final BoardPane boardPane;
    /**
     * 移动历史面板
     */
    private final MoveHistoryPane moveHistoryPane;
    /**
     * 信息面板，显示双方获子及当前游戏状态的面板
     */
    private final InfoPane infoPane;
    /**
     * 帮助窗口，显示游戏控制的对话框
     */
    private final HelpWindow helpWindow;
    /**
     * AI玩家观察者
     */
    private final AIObserver aiObserver;
    /**
     * 属性变更支持
     */
    private final PropertyChangeSupport propertyChangeSupport;
    /**
     * 中国象棋棋盘
     */
    private Board board;
    /**
     * 所有移动日志
     */
    private MoveLog fullMovelog;
    /**
     * 部分移动日志
     */
    private MoveLog partialMovelog;
    /**
     * 源点
     */
    private Point sourcePoint;
    /**
     * 目录点
     */
    private Point destPoint;
    /**
     * 选中的棋子
     */
    private Piece selectedPiece;
    /**
     * 禁止移动列表
     */
    private Collection<Move> bannedMoves;
    /**
     * 是否高亮显示移动路径
     */
    private boolean highlightLegalMoves;

    /**
     * 私有化当前实例
     */
    private Table() {
        board = Board.initialiseBoard();
        gameSetup = GameSetup.getInstance();
        boardPane = new BoardPane();
        moveHistoryPane = new MoveHistoryPane();
        infoPane = new InfoPane();
        fullMovelog = new MoveLog();
        infoPane.update(board, fullMovelog);
        helpWindow = new HelpWindow();
        aiObserver = new AIObserver();
        propertyChangeSupport = new PropertyChangeSupport(this);
        propertyChangeSupport.addPropertyChangeListener(aiObserver);
        bannedMoves = new ArrayList<>();
        highlightLegalMoves = true;

        // 顶部菜单条
        setTop(createMenuBar());
        // 棋盘面板
        setCenter(boardPane);
        // 右边移动历史面板
        setRight(moveHistoryPane);
        // 左边信息面板
        setLeft(infoPane);
    }

    /**
     * Returns an instance of this table.
     *
     * @return An instance of this table.
     */
    public static Table getInstance() {
        return TABLE_INSTANCE;
    }

    /**
     * 创建并返回一个菜单栏
     *
     * @return 创建好的菜单栏
     */
    private MenuBar createMenuBar() {
        // 菜单条
        MenuBar menuBar = new MenuBar();
        // 添加菜单
        menuBar.getMenus().addAll(createGameMenu(), createOptionsMenu(), createPreferencesMenu(), createHelpMenu());
        return menuBar;
    }

    /**
     * 为菜单栏创建并返回一个游戏菜单
     *
     * @return 创建好的游戏菜单
     */
    private Menu createGameMenu() {
        Menu gameMenu = new Menu("游戏");

        MenuItem newGame = new MenuItem("开始新游戏");
        newGame.setOnAction(e -> {
            // 没有移动，弹窗提示
            if (fullMovelog.isEmpty()) {
                showAlert(AlertType.INFORMATION, "开始新游戏", "未移动");
                return;
            }
            // 弹窗提示
            showAlert(AlertType.CONFIRMATION, "开始新游戏", "开始新游戏?").ifPresent(response -> {
                // 同意开始新游戏
                if (response.equals(ButtonType.OK)) {
                    // 终止所有正在运行的AI
                    aiObserver.stopAI();
                    // 退出回放/重播模式
                    exitReplayMode();
                    // 重新开始
                    restart();
                    notifyAIObserver("新游戏");
                }
            });
        });

        MenuItem saveGame = new MenuItem("保存...");
        saveGame.setOnAction(e -> {
            // 没有移动，弹窗提示
            if (fullMovelog.isEmpty()) {
                showAlert(AlertType.INFORMATION, "保存", "未移动");
                return;
            }
            // 保存游戏
            saveGame();
        });

        MenuItem loadGame = new MenuItem("加载...");
        loadGame.setOnAction(e -> loadGame());

        MenuItem exit = new MenuItem("退出");
        exit.setOnAction(e -> System.exit(0));

        gameMenu.getItems().addAll(newGame, new SeparatorMenuItem(), saveGame, loadGame, new SeparatorMenuItem(), exit);

        return gameMenu;
    }

    /**
     * Creates and returns an options menu for the menu bar.
     */
    private Menu createOptionsMenu() {
        Menu optionsMenu = new Menu("选项");

        MenuItem undoTurn = new MenuItem("悔棋(对手且当前玩家皆退一步)");
        undoTurn.setOnAction(e -> {
            if (fullMovelog.getSize() < 2) {
                // 弹窗提示
                showAlert(AlertType.INFORMATION, "悔棋(对手且当前玩家皆退一步)", "无法悔棋");
                return;
            }
            aiObserver.stopAI();
            exitReplayMode();
            undoLastTurn();
            notifyAIObserver("undoturn");
        });

        MenuItem undoMove = new MenuItem("悔棋(当前玩家悔一步)");
        undoMove.setOnAction(e -> {
            if (fullMovelog.isEmpty()) {
                // 弹窗提示
                showAlert(AlertType.INFORMATION, "悔棋(当前玩家悔一步)", "无法悔棋");
                return;
            }
            aiObserver.stopAI();
            exitReplayMode();
            undoLastMove();
            notifyAIObserver("undomove");
        });

        MenuItem playFromMove = new MenuItem("从选定的移动开始游戏（即从指定步数开始玩）");
        playFromMove.setOnAction(e -> {
            if (!moveHistoryPane.isInReplayMode()) {
                // 弹窗提示
                showAlert(AlertType.INFORMATION, "从选定的移动开始游戏（即从指定步数开始玩）", "没有选择移动");
                return;
            }
            // 弹窗提示
            showAlert(AlertType.CONFIRMATION, "从选定的移动开始游戏（即从指定步数开始玩）", "S后续的移动将被删除.继续?").ifPresent(response -> {
                if (response.equals(ButtonType.OK)) {
                    playFromSelectedMove();
                    notifyAIObserver("playfrommove");
                }
            });
        });

        MenuItem banMove = new MenuItem("禁止选择移动");
        banMove.setOnAction(e -> {
            if (!moveHistoryPane.isInReplayMode()) {
                // 弹窗提示
                showAlert(AlertType.INFORMATION, "禁止选择移动", "没有移动选择");
                return;
            }
            Move bannedMove = partialMovelog.getLastMove();
            bannedMoves.add(bannedMove);
            // 弹窗提示
            showAlert(AlertType.INFORMATION, "禁止选择移动", bannedMove.toString() + " 已经被禁止");
        });

        MenuItem unbanAll = new MenuItem("解除所有禁止移动（取消所有禁止）");
        unbanAll.setOnAction(e -> {
            if (bannedMoves.isEmpty()) {
                // 弹窗提示
                showAlert(AlertType.INFORMATION, "解除所有禁止移动（取消所有禁止）", "没有禁止移动");
                return;
            }
            aiObserver.stopAI();
            // 弹窗提示
            showAlert(AlertType.INFORMATION, "解除所有禁止移动（取消所有禁止）", "所有禁止移动被解除");
            bannedMoves.clear();
            notifyAIObserver("unban");
        });

        MenuItem setup = new MenuItem("设置...");
        setup.setOnAction(e -> {
            clearSelections();
            aiObserver.stopAI();
            gameSetup.showAndWait();
            if (partialMovelog != null) {
                infoPane.update(board, partialMovelog);
            } else {
                infoPane.update(board, fullMovelog);
            }
            notifyAIObserver("setup");
        });

        optionsMenu.getItems().addAll(undoTurn, undoMove, playFromMove, new SeparatorMenuItem(), banMove, unbanAll, new SeparatorMenuItem(), setup);

        return optionsMenu;
    }

    /**
     * Creates and returns a preferences menu for the menu bar.
     */
    private Menu createPreferencesMenu() {
        Menu prefMenu = new Menu("首选项");

        CheckMenuItem highlight = new CheckMenuItem("突出棋子可移动点位");
        highlight.setSelected(highlightLegalMoves);
        highlight.setOnAction(e -> highlightLegalMoves = highlight.isSelected());

        MenuItem flipBoard = new MenuItem("翻转棋盘");
        flipBoard.setOnAction(e -> boardPane.flipBoard());

        prefMenu.getItems().addAll(highlight, new SeparatorMenuItem(), flipBoard);

        return prefMenu;
    }

    /**
     * Creates and returns a help menu for the menu bar.
     */
    private Menu createHelpMenu() {
        Menu helpMenu = new Menu("帮助");

        MenuItem rules = new MenuItem("规则...");
        rules.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URL(WIKI_XIANGQI).toURI());
            } catch (IOException | URISyntaxException ex) {
                ex.printStackTrace();
            }
        });

        MenuItem controls = new MenuItem("控制...");
        controls.setOnAction(e -> helpWindow.showAndWait());

        helpMenu.getItems().addAll(rules, controls);

        return helpMenu;
    }

    /**
     * Restarts the game.
     */
    private void restart() {
        clearSelections();
        board = Board.initialiseBoard();
        fullMovelog.clear();
        bannedMoves.clear();

        boardPane.drawBoard(board);
        moveHistoryPane.update(fullMovelog);
        infoPane.update(board, fullMovelog);
    }

    /**
     * 保存游戏,将当前正在进行的游戏保存到可加载的文本文件中
     */
    private void saveGame() {
        FileChooser fc = new FileChooser();
        fc.setTitle("保存");
        File file = fc.showSaveDialog(CChess.stage);

        if (file != null) {
            try {
                PrintWriter pw = new PrintWriter(file);
                // 移动日志追加换行
                fullMovelog.getMoves().forEach(move -> pw.append(move.toString()).append("\n"));
                pw.flush();
                pw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            // 弹窗提示
            showAlert(AlertType.INFORMATION, "保存", "保存成功");
        }
    }

    /**
     * 加载一个文本文件来恢复之前保存的游戏
     */
    private void loadGame() {
        FileChooser fc = new FileChooser();
        fc.setTitle("加载");
        File file = fc.showOpenDialog(CChess.stage);

        if (file != null) {
            LoadGameUtil lgu = new LoadGameUtil(file);
            // 非有效的文件，弹窗提示
            if (!lgu.isValidFile()) {
                showAlert(AlertType.ERROR, "加载", "无效的文件");
            } else {
                // 清除所有鼠标选择
                clearSelections();
                // 终止所有正在运行的AI
                aiObserver.stopAI();
                // 退出回放/重播模式
                exitReplayMode();

                // 初始化棋盘
                board = lgu.getBoard();

                // 清空所有历史移动日志，添加新的移动日志
                fullMovelog.clear();
                for (Move move : lgu.getMoves()) {
                    fullMovelog.addMove(move);
                }

                // 更新移动历史面板
                moveHistoryPane.update(fullMovelog);
                // 更新信息面板
                infoPane.update(board, fullMovelog);
                boardPane.drawBoard(board);

                notifyAIObserver("load");
                // 弹窗提示
                showAlert(AlertType.INFORMATION, "Load", "Load success");
            }
        }
    }

    /**
     * Undoes the last move of either player.
     */
    private void undoLastMove() {
        if (!fullMovelog.isEmpty()) {
            clearSelections();

            board.unmakeMove(fullMovelog.getLastMove());
            fullMovelog.removeLastMove();

            moveHistoryPane.update(fullMovelog);
            infoPane.update(board, fullMovelog);
            boardPane.drawBoard(board);
        }
    }

    /**
     * Undoes two consecutive moves.
     */
    private void undoLastTurn() {
        if (fullMovelog.getSize() > 1) {
            undoLastMove();
            undoLastMove();
        }
    }

    /**
     * Enters play mode from the selected move in replay mode.
     */
    private void playFromSelectedMove() {
        if (!moveHistoryPane.isInReplayMode()) {
            return;
        }

        fullMovelog = partialMovelog;
        exitReplayMode();
        moveHistoryPane.update(fullMovelog);
    }

    /**
     * 如果当前处于回放/重播模式,则退出回放/重播模式
     */
    private void exitReplayMode() {
        // 当前为回放/重播模式时
        if (moveHistoryPane.isInReplayMode()) {
            // 禁止回放/重播
            moveHistoryPane.disableReplay();
            // 终止所有正在运行的AI
            aiObserver.stopAI();
        }
    }

    /**
     * Exits replay mode if moveIndex = -1; else enters replay mode at the given moveIndex.
     *
     * @param moveIndex The index of the move in the full movelog.
     */
    void jumpToMove(int moveIndex) {
        if (moveIndex < -1 || moveIndex >= fullMovelog.getSize()) {
            return;
        }
        if (moveIndex == -1) {
            int currIndex = partialMovelog.getSize() - 1;
            partialMovelog = null;
            for (int i = currIndex + 1; i < fullMovelog.getSize(); i++) {
                board.makeMove(fullMovelog.getMoves().get(i));
            }
            boardPane.drawBoard(board);
            infoPane.update(board, fullMovelog);
            Table.getInstance().notifyAIObserver("exitreplay");
        } else {
            aiObserver.stopAI();
            int currIndex = partialMovelog == null ? fullMovelog.getSize() - 1 : partialMovelog.getSize() - 1;
            partialMovelog = fullMovelog.getPartialLog(moveIndex);
            clearSelections();
            for (int i = currIndex + 1; i <= moveIndex; i++) {
                board.makeMove(fullMovelog.getMoves().get(i));
            }
            for (int i = currIndex; i > moveIndex; i--) {
                board.unmakeMove(fullMovelog.getMoves().get(i));
            }
            boardPane.drawBoard(board);
            infoPane.update(board, partialMovelog);
        }
    }

    /**
     * Notifies the AI observer with the given property name.
     */
    private void notifyAIObserver(String propertyName) {
        propertyChangeSupport.firePropertyChange(propertyName, null, null);
    }

    /**
     * Checks if the current AI's moves are randomised.
     *
     * @return true if the current AI's moves are randomised, false otherwise.
     */
    public boolean isAIRandomised() {
        return gameSetup.isAIRandomised();
    }

    /**
     * 清除所有鼠标选择
     */
    private void clearSelections() {
        sourcePoint = null;
        destPoint = null;
        selectedPiece = null;
    }

    /**
     * 显示弹窗
     *
     * @param alertType 警告类型
     * @param title     标题
     * @param content   内容
     * @return 按钮类型的Optional对象
     */
    private static Optional<ButtonType> showAlert(AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType, content);
        alert.setTitle(title);
        return alert.showAndWait();
    }

    /**
     * Returns a mapping from a string representing a piece to its corresponding image.
     */
    private static Map<String, Image> getPieceImageMap() {
        Map<String, Image> pieceImageMap = new HashMap<>();

        for (PieceType pieceType : PieceType.values()) {
            String name = ("R" + pieceType.toString()).toLowerCase();
            Image image = new Image(Table.class.getResourceAsStream(GuiUtil.GRAPHICS_PIECES_PATH + name + ".png"));
            pieceImageMap.put(name, image);

            name = ("B" + pieceType.toString()).toLowerCase();
            image = new Image(Table.class.getResourceAsStream(GuiUtil.GRAPHICS_PIECES_PATH + name + ".png"));
            pieceImageMap.put(name, image);
        }

        return pieceImageMap;
    }

    /**
     * 用于存储所有移动的帮助类，即移动日志
     */
    static class MoveLog {

        /**
         * 移动记录列表
         */
        private final List<Move> moves;

        /**
         * 构造
         */
        private MoveLog() {
            this.moves = new ArrayList<>();
        }

        /**
         * 获取移动记录列表
         *
         * @return 移动记录列表
         */
        List<Move> getMoves() {
            return moves;
        }

        /**
         * 移动步数，即移动记录列表大小
         *
         * @return 移动步数，即移动记录列表大小
         */
        int getSize() {
            return moves.size();
        }

        /**
         * 有无移动记录
         *
         * @return true, 有移动记录，否则，false
         */
        boolean isEmpty() {
            return moves.isEmpty();
        }

        /**
         * 添加移动记录
         *
         * @param move 移动记录
         */
        void addMove(Move move) {
            moves.add(move);
        }

        /**
         * 移除最后的移动
         */
        void removeLastMove() {
            if (moves.isEmpty()) {
                return;
            }
            moves.remove(moves.size() - 1);
        }

        /**
         * 获取最后的移动记录，即最后一步
         *
         * @return 最后的移动记录，即最后一步
         */
        Move getLastMove() {
            return moves.isEmpty() ? null : moves.get(moves.size() - 1);
        }

        /**
         * 获取部分的移动记录
         *
         * @param moveIndex 指定的移动下标
         * @return 获取指定移动下标（包含）前的移动记录
         */
        MoveLog getPartialLog(int moveIndex) {
            MoveLog partialLog = new MoveLog();
            for (int i = 0; i < moveIndex + 1; i++) {
                partialLog.addMove(moves.get(i));
            }
            return partialLog;
        }

        /**
         * 清空移动历史
         */
        void clear() {
            moves.clear();
        }
    }

    /**
     * 显示游戏棋盘的面板
     */
    private class BoardPane extends GridPane {

        private final List<PointPane> pointPanes;
        private BoardDirection boardDirection;

        private BoardPane() {
            pointPanes = new ArrayList<>();
            boardDirection = BoardDirection.NORMAL;

            for (int row = 0; row < Board.NUM_ROWS; row++) {
                for (int col = 0; col < Board.NUM_COLS; col++) {
                    PointPane pointPane = new PointPane(new Coordinate(row, col));
                    pointPanes.add(pointPane);
                    add(pointPane, col, row);
                }
            }

            BackgroundImage boardImage = new BackgroundImage(BOARD_IMAGE, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, null, null);
            setBackground(new Background(boardImage));
            setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
            setMinSize(BOARD_WIDTH, BOARD_HEIGHT);
            setMaxSize(BOARD_WIDTH, BOARD_HEIGHT);
            setGridLinesVisible(false);
        }

        /**
         * 根据给定的棋盘重绘此棋盘面板
         *
         * @param board 当前棋盘.
         */
        private void drawBoard(Board board) {
            getChildren().clear();
            // 遍历点位
            for (PointPane pointPane : pointPanes) {
                // 绘制点位
                pointPane.drawPoint(board);
                if (boardDirection.isNormal()) {
                    add(pointPane, pointPane.position.getCol(), pointPane.position.getRow());
                } else {
                    add(pointPane, Board.NUM_COLS - pointPane.position.getCol(), Board.NUM_ROWS - pointPane.position.getRow());
                }
            }
        }

        /**
         * Flips the current board.
         */
        private void flipBoard() {
            boardDirection = boardDirection.opposite();
            drawBoard(board);
            infoPane.setDirection(boardDirection);
        }
    }

    /**
     * Represents the direction of the board.
     */
    public enum BoardDirection {
        NORMAL {
            @Override
            boolean isNormal() {
                return true;
            }

            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }
        }, FLIPPED {
            @Override
            boolean isNormal() {
                return false;
            }

            @Override
            BoardDirection opposite() {
                return NORMAL;
            }
        };

        abstract boolean isNormal();

        abstract BoardDirection opposite();
    }

    /**
     * A pane for displaying a point on the board.
     */
    private class PointPane extends StackPane {

        private final Coordinate position;

        PointPane(Coordinate position) {
            this.position = position;

            setPrefSize(POINT_WIDTH, POINT_WIDTH);
            setMinSize(POINT_WIDTH, POINT_WIDTH);
            setMaxSize(POINT_WIDTH, POINT_WIDTH);
            setOnMouseClicked(getMouseEventHandler());

            assignPointPieceIcon(board);
        }

        /**
         * Returns the mouse event handler for this point pane.
         */
        private EventHandler<MouseEvent> getMouseEventHandler() {
            return event -> {
                if (moveHistoryPane.isInReplayMode()) {
                    return;
                }
                if (event.getButton().equals(MouseButton.SECONDARY)) {
                    clearSelections();
                } else if (event.getButton().equals(MouseButton.PRIMARY)) {
                    if (sourcePoint == null) {
                        sourcePoint = board.getPoint(position);
                        Optional<Piece> selectedPiece = sourcePoint.getPiece();
                        if (selectedPiece.isPresent() && selectedPiece.get().getAlliance() == board.getCurrPlayer().getAlliance() && !gameSetup.isAIPlayer(board.getCurrPlayer().getAlliance())) {
                            Table.this.selectedPiece = selectedPiece.get();
                        } else {
                            sourcePoint = null;
                        }
                    } else {
                        destPoint = board.getPoint(position);
                        Optional<Move> move = board.getMove(sourcePoint.getPosition(), destPoint.getPosition());
                        if (!move.isPresent()) {
                            return;
                        }

                        board.makeMove(move.get());
                        if (board.isStateAllowed()) {
                            fullMovelog.addMove(move.get());

                            clearSelections();
                            Platform.runLater(() -> {
                                moveHistoryPane.update(fullMovelog);
                                infoPane.update(board, fullMovelog);
                                notifyAIObserver("movemade");
                            });
                        } else {
                            board.unmakeMove(move.get());
                        }
                    }
                }
                Platform.runLater(() -> boardPane.drawBoard(board));
            };
        }

        /**
         * 根据棋盘重绘此点窗格
         *
         * @param board 当前棋盘.
         */
        private void drawPoint(Board board) {
            // 给给定点上面的棋子分配图片
            assignPointPieceIcon(board);
            // 高亮最后移动和选中的棋子
            highlightLastMoveAndSelectedPiece();
            // 高亮可能移动
            highlightPossibleMoves(board);
        }

        /**
         * 给定点上的当前棋子(如果有)，将图片分配给该点
         *
         * @param board 当前棋盘
         */
        private void assignPointPieceIcon(Board board) {
            getChildren().clear();

            // 指定位置在棋盘上的点位
            Point point = board.getPoint(position);
            // 点位上的棋子
            Optional<Piece> destPiece = point.getPiece();
            // 存在柜子，分配图片并添加进子组件
            destPiece.ifPresent(p -> {
                String name = (p.getAlliance().toString().substring(0, 1) + p.getPieceType().toString()).toLowerCase();
                Label label = new Label();
                label.setGraphic(new ImageView(PIECE_IMAGE_MAP.get(name)));
                getChildren().add(label);
            });
        }

        /**
         * 如果包含选中的棋子或它是上一次移动的部分，高亮显示(使用边框)这个点
         */
        private void highlightLastMoveAndSelectedPiece() {
            // 最后的移动
            Move lastMove;
            // 当前处于回放/重播模式
            if (moveHistoryPane.isInReplayMode()) {
                lastMove = partialMovelog.getLastMove();
            } else { // 非回放/重播模式
                lastMove = fullMovelog.getLastMove();
            }

            // 存在最后的行棋
            if (lastMove != null) {
                Piece lastMovedPiece = lastMove.getMovedPiece();
                // 棋子当前点位设置虚线边框
                if (lastMovedPiece.getPosition().equals(position)) {
                    setBorder(HIGHLIGHT_LAST_MOVE_BORDER);
                    return;
                }
                // 棋子目标点位设置虚线边框
                if (lastMove.getDestPosition().equals(position)) {
                    setBorder(HIGHLIGHT_LAST_MOVE_BORDER);
                    return;
                }
            }

            // 选中的棋子不为空且选中棋子的位置与当前位置是同一位置，设置虚线边框
            if (selectedPiece != null && selectedPiece.getPosition().equals(position)) {
                setBorder(HIGHLIGHT_SELECTED_PIECE_BORDER);
                return;
            }

            // 默认不设置边框
            setBorder(null);
        }

        /**
         * 如果此点符合所选棋子规则行棋位置之一，则高亮显示(使用圆点)
         *
         * @param board 当前棋盘.
         */
        private void highlightPossibleMoves(Board board) {
            // 不高亮显示移动路径，跳过
            if (!highlightLegalMoves) {
                return;
            }
            // 遍历所选棋子所有可以采取的合法走法
            for (Move move : pieceLegalMoves(board)) {
                // 检查自杀行为
                board.makeMove(move);
                if (!board.isStateAllowed()) {
                    // 撤销移动
                    board.unmakeMove(move);
                    continue;
                }
                // 撤销移动
                board.unmakeMove(move);

                // 合法且非自杀性的落点,使用圆点高亮显示
                if (move.getDestPosition().equals(position)) {
                    Label label = new Label();
                    label.setGraphic(new ImageView(HIGHLIGHT_LEGALS_IMAGE));
                    getChildren().add(label);
                }
            }
        }

        /**
         * 返回符合所选棋子在给定棋盘上可以采取的合法走法的集合
         *
         * @param board 当前棋盘
         * @return 符合所选棋子在给定棋盘上可以采取的合法走法的集合
         */
        private Collection<Move> pieceLegalMoves(Board board) {
            return selectedPiece != null ? selectedPiece.getLegalMoves(board) : Collections.emptyList();
        }
    }

    /**
     * AI玩家帮助类
     */
    private static class AIObserver implements PropertyChangeListener {

        private static final int MIN_TIME = 1000;

        private final Timer timer;
        private final Stack<AIPlayer> aiPlayers;
        private TimerTask task;

        private AIObserver() {
            timer = new Timer("Movebook Timer");
            aiPlayers = new Stack<>();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (!Table.getInstance().moveHistoryPane.isInReplayMode() && getInstance().gameSetup.isAIPlayer(getInstance().board.getCurrPlayer().getAlliance()) && !getInstance().board.isCurrPlayerCheckmated()) {
                Optional<Move> move = MoveBook.getRandomMove(getInstance().board.getZobristKey());
                if (move.isPresent()) {
                    task = getTimerTask(move.get());
                    timer.schedule(task, MIN_TIME);
                    return;
                }

                AIPlayer aiPlayer = getInstance().gameSetup.isAITimeLimited() ? new FixedTimeAIPlayer() : new FixedDepthAIPlayer();
                aiPlayers.push(aiPlayer);
                Thread th = new Thread(aiPlayer);
                th.setDaemon(true);
                th.start();
            }
        }

        /**
         * Executes the given move on the board.
         */
        private static void makeMove(Move move) {
            getInstance().board.makeMove(move);
            getInstance().fullMovelog.addMove(move);
            getInstance().boardPane.drawBoard(getInstance().board);
            getInstance().moveHistoryPane.update(getInstance().fullMovelog);
            getInstance().infoPane.update(getInstance().board, getInstance().fullMovelog);
            getInstance().notifyAIObserver("movemade");
        }

        /**
         * Returns a timer task for a move in the movebook.
         */
        private TimerTask getTimerTask(Move move) {
            return new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> makeMove(move));
                    System.out.println(move + " [movebook]");
                }
            };
        }

        /**
         * 终止所有正在运行的AI(如果有)
         */
        private void stopAI() {
            // 取消任务
            if (task != null) {
                task.cancel();
            }
            // 暂停且取消全部AI玩家及其计时器任务
            while (!aiPlayers.isEmpty()) {
                AIPlayer aiPlayer = aiPlayers.pop();
                aiPlayer.stop();
            }
        }
    }

    /**
     * Represents an AI player.
     */
    private static abstract class AIPlayer extends Task<Move> {

        private static final int MAX_CONSEC_CHECKS = 3;

        final Collection<Move> legalMoves;
        final Timer timer;
        TimerTask task;

        private AIPlayer() {
            timer = new Timer("AI Timer");
            legalMoves = new ArrayList<>(getInstance().board.getCurrPlayer().getLegalMoves());

            Piece bannedPiece = getBannedCheckingPiece();
            Collection<Move> bannedMoves = new ArrayList<>();
            if (bannedPiece != null) {
                Board board = getInstance().board.getCopy();
                for (Move move : legalMoves) {
                    board.makeMove(move);
                    if (move.getMovedPiece().equals(bannedPiece) && !move.isCapture() && board.getCurrPlayer().isInCheck()) {
                        bannedMoves.add(move);
                    }
                    board.unmakeMove(move);
                }
            }

            legalMoves.removeAll(bannedMoves);
            legalMoves.removeAll(getInstance().bannedMoves);
        }

        /**
         * Returns the piece not to check the opponent with, if any.
         */
        private Piece getBannedCheckingPiece() {
            if (!getInstance().board.lastThreeChecks()) {
                return null;
            }

            List<Move> moveHistory = getInstance().fullMovelog.getMoves();
            Move move = moveHistory.get(moveHistory.size() - MAX_CONSEC_CHECKS * 2);
            if (move.isCapture()) {
                return null;
            }
            Piece movedPiece = move.getMovedPiece().movePiece(move);
            for (int i = 1; i < MAX_CONSEC_CHECKS; i++) {
                move = moveHistory.get(moveHistory.size() - MAX_CONSEC_CHECKS * 2 + i * 2);
                if (!move.getMovedPiece().equals(movedPiece) || move.isCapture()) {
                    return null;
                }
                movedPiece = move.getMovedPiece().movePiece(move);
            }

            return movedPiece;
        }

        /**
         * 暂停且取消全部AI玩家及其计时器任务
         */
        private void stop() {
            // 取消任务
            if (task != null) {
                task.cancel();
            }
            // 标记为可以停止运行
            cancel(true);
        }
    }

    /**
     * Represents a fixed-depth AI player.
     */
    private static class FixedDepthAIPlayer extends AIPlayer {

        private Move bestMove;
        private int searchDepth;
        private long startTime;

        @Override
        public void done() {
            if (isCancelled()) {
                return;
            }
            try {
                bestMove = get();
                if (System.currentTimeMillis() > startTime + AIObserver.MIN_TIME) {
                    move();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Move call() {
            task = getTimerTask();
            timer.schedule(task, AIObserver.MIN_TIME);
            startTime = System.currentTimeMillis();
            searchDepth = getInstance().gameSetup.getSearchDepth();
            return new FixedDepthSearch(getInstance().board.getCopy(), legalMoves, searchDepth).search();
        }

        /**
         * Executes the move.
         */
        private void move() {
            Platform.runLater(() -> AIObserver.makeMove(bestMove));
            System.out.println(bestMove.toString() + " | " + (System.currentTimeMillis() - startTime) / 1000 + "s | " + "depth " + searchDepth);
        }

        /**
         * Returns a timer task for keeping a minimum time before AI moves.
         */
        private TimerTask getTimerTask() {
            return new TimerTask() {
                @Override
                public void run() {
                    if (bestMove != null) {
                        move();
                    }
                }
            };
        }
    }

    /**
     * Represents a fixed-time AI player.
     */
    public static class FixedTimeAIPlayer extends AIPlayer implements PropertyChangeListener {

        private Move currBestMove;
        private int currDepth;
        private int searchTime;

        @Override
        protected Move call() {
            task = getTimerTask();
            searchTime = getInstance().gameSetup.getSearchTime();
            timer.schedule(task, searchTime * 1000);
            return new FixedTimeSearch(getInstance().board.getCopy(), legalMoves, this, System.currentTimeMillis() + searchTime * 1000).search();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            currBestMove = (Move) evt.getNewValue();
            currDepth = (int) evt.getOldValue();
        }

        /**
         * Returns a timer task for forcing a move when time is up.
         */
        private TimerTask getTimerTask() {
            return new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> AIObserver.makeMove(currBestMove));
                    System.out.println(currBestMove.toString() + " | " + searchTime + "s | " + "depth " + currDepth);
                    FixedTimeAIPlayer.this.cancel(true);
                }
            };
        }
    }

    /**
     * 玩家类型.
     */
    public enum PlayerType {
        /**
         * 人类
         */
        HUMAN {
            @Override
            boolean isAI() {
                return false;
            }
        },
        /**
         * 电脑
         */
        AI {
            @Override
            boolean isAI() {
                return true;
            }
        };

        /**
         * 是否AI
         *
         * @return true，ai,否则，false
         */
        abstract boolean isAI();
    }

    /**
     * AI玩家类型
     */
    public enum AIType {
        /**
         * 时间
         */
        TIME {
            @Override
            boolean isTimeLimited() {
                return true;
            }
        },
        /**
         * 深度
         */
        DEPTH {
            @Override
            boolean isTimeLimited() {
                return false;
            }
        };

        /**
         * 是否限制时间
         *
         * @return true, 有时间限制，否则，false
         */
        abstract boolean isTimeLimited();
    }
}
