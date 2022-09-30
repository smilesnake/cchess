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
     * 表格实例
     */
    private static final Table TABLE_INSTANCE = new Table();
    /**
     * 游戏设置对话框
     */
    private final GameSetup gameSetup;
    private final BoardPane boardPane;
    private final MoveHistoryPane moveHistoryPane;
    private final InfoPane infoPane;
    private final HelpWindow helpWindow;
    private final AIObserver aiObserver;
    private final PropertyChangeSupport propertyChangeSupport;
    private Board board;
    private MoveLog fullMovelog;
    private MoveLog partialMovelog;
    private Point sourcePoint;
    private Point destPoint;
    private Piece selectedPiece;
    private Collection<Move> bannedMoves;
    private boolean highlightLegalMoves;

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

        setTop(createMenuBar());
        setCenter(boardPane);
        setRight(moveHistoryPane);
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
     * Creates and returns a menu bar for this table.
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.getMenus().addAll(createGameMenu(), createOptionsMenu(), createPreferencesMenu(), createHelpMenu());
        return menuBar;
    }

    /**
     * Creates and returns a game menu for the menu bar.
     */
    private Menu createGameMenu() {
        Menu gameMenu = new Menu("游戏");

        MenuItem newGame = new MenuItem("新局");
        newGame.setOnAction(e -> {
            if (fullMovelog.isEmpty()) {
                showAlert(AlertType.INFORMATION, "New", "No moves made");
                return;
            }
            showAlert(AlertType.CONFIRMATION, "New", "Start a new game?").ifPresent(response -> {
                if (response.equals(ButtonType.OK)) {
                    aiObserver.stopAI();
                    exitReplayMode();
                    restart();
                    notifyAIObserver("新游戏");
                }
            });
        });

        MenuItem saveGame = new MenuItem("保存...");
        saveGame.setOnAction(e -> {
            if (fullMovelog.isEmpty()) {
                showAlert(AlertType.INFORMATION, "Save", "No moves made");
                return;
            }
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
                showAlert(AlertType.INFORMATION, "从选定的移动开始游戏（即从指定步数开始玩）", "没有选择移动");
                return;
            }
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
                showAlert(AlertType.INFORMATION, "禁止选择移动", "没有移动选择");
                return;
            }
            Move bannedMove = partialMovelog.getLastMove();
            bannedMoves.add(bannedMove);
            showAlert(AlertType.INFORMATION, "禁止选择移动", bannedMove.toString() + " 已经被禁止");
        });

        MenuItem unbanAll = new MenuItem("解除所有禁止移动（取消所有禁止）");
        unbanAll.setOnAction(e -> {
            if (bannedMoves.isEmpty()) {
                showAlert(AlertType.INFORMATION, "解除所有禁止移动（取消所有禁止）", "没有禁止移动");
                return;
            }
            aiObserver.stopAI();
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
     * Saves the current game in-progress into a loadable text file.
     */
    private void saveGame() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save");
        File file = fc.showSaveDialog(CChess.stage);

        if (file != null) {
            try {
                PrintWriter pw = new PrintWriter(file);
                for (Move move : fullMovelog.getMoves()) {
                    pw.append(move.toString()).append("\n");
                }
                pw.flush();
                pw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            showAlert(AlertType.INFORMATION, "Save", "Save success");
        }
    }

    /**
     * Loads a text file to restore a previously saved game.
     */
    private void loadGame() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load");
        File file = fc.showOpenDialog(CChess.stage);

        if (file != null) {
            LoadGameUtil lgu = new LoadGameUtil(file);
            if (!lgu.isValidFile()) {
                showAlert(AlertType.ERROR, "Load", "Invalid file");
            } else {
                clearSelections();
                aiObserver.stopAI();
                exitReplayMode();

                board = lgu.getBoard();
                fullMovelog.clear();
                for (Move move : lgu.getMoves()) {
                    fullMovelog.addMove(move);
                }
                moveHistoryPane.update(fullMovelog);
                infoPane.update(board, fullMovelog);
                boardPane.drawBoard(board);

                notifyAIObserver("load");

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
     * Exits replay mode if currently in it.
     */
    private void exitReplayMode() {
        if (moveHistoryPane.isInReplayMode()) {
            moveHistoryPane.disableReplay();
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
     * Clears all mouse selections made by the human player.
     */
    private void clearSelections() {
        sourcePoint = null;
        destPoint = null;
        selectedPiece = null;
    }

    /**
     * Creates an alert given the alert type, title and content strings.
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
     * Helper class for storing all moves made.
     */
    static class MoveLog {

        private final List<Move> moves;

        private MoveLog() {
            this.moves = new ArrayList<>();
        }

        List<Move> getMoves() {
            return moves;
        }

        int getSize() {
            return moves.size();
        }

        boolean isEmpty() {
            return moves.isEmpty();
        }

        void addMove(Move move) {
            moves.add(move);
        }

        void removeLastMove() {
            if (!moves.isEmpty()) {
                moves.remove(moves.size() - 1);
            }
        }

        Move getLastMove() {
            if (!moves.isEmpty()) {
                return moves.get(moves.size() - 1);
            }

            return null;
        }

        MoveLog getPartialLog(int moveIndex) {
            MoveLog partialLog = new MoveLog();
            for (int i = 0; i < moveIndex + 1; i++) {
                partialLog.addMove(moves.get(i));
            }
            return partialLog;
        }

        void clear() {
            moves.clear();
        }
    }

    /**
     * A pane for displaying the game board.
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
         * Redraws this board pane given the board.
         *
         * @param board The current board.
         */
        private void drawBoard(Board board) {
            getChildren().clear();

            for (PointPane pointPane : pointPanes) {
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
         * Redraws this point pane given the board.
         *
         * @param board The current board.
         */
        private void drawPoint(Board board) {
            assignPointPieceIcon(board);
            highlightLastMoveAndSelectedPiece();
            highlightPossibleMoves(board);
        }

        /**
         * Assigns an image to this point pane given the current piece (if any) on it.
         *
         * @param board The current board.
         */
        private void assignPointPieceIcon(Board board) {
            getChildren().clear();

            Point point = board.getPoint(position);
            Optional<Piece> destPiece = point.getPiece();
            destPiece.ifPresent(p -> {
                String name = (p.getAlliance().toString().substring(0, 1) + p.getPieceType().toString()).toLowerCase();
                Label label = new Label();
                label.setGraphic(new ImageView(PIECE_IMAGE_MAP.get(name)));
                getChildren().add(label);
            });
        }

        /**
         * Highlights (using a border) this point pane if it contains the selected piece OR it was part of the previous move.
         */
        private void highlightLastMoveAndSelectedPiece() {
            Move lastMove;
            if (moveHistoryPane.isInReplayMode()) {
                lastMove = partialMovelog.getLastMove();
            } else {
                lastMove = fullMovelog.getLastMove();
            }

            if (lastMove != null) {
                Piece lastMovedPiece = lastMove.getMovedPiece();
                if (lastMovedPiece.getPosition().equals(position)) {
                    setBorder(HIGHLIGHT_LAST_MOVE_BORDER);
                    return;
                }
                if (lastMove.getDestPosition().equals(position)) {
                    setBorder(HIGHLIGHT_LAST_MOVE_BORDER);
                    return;
                }
            }

            if (selectedPiece != null && selectedPiece.getPosition().equals(position)) {
                setBorder(HIGHLIGHT_SELECTED_PIECE_BORDER);
                return;
            }

            setBorder(null);
        }

        /**
         * Highlights (using a dot) this point pane if it is one of the legal destinations of the selected piece.
         *
         * @param board The current board.
         */
        private void highlightPossibleMoves(Board board) {
            if (!highlightLegalMoves) {
                return;
            }
            for (Move move : pieceLegalMoves(board)) {
                board.makeMove(move);
                // check for suicidal move
                if (!board.isStateAllowed()) {
                    board.unmakeMove(move);
                    continue;
                }
                board.unmakeMove(move);
                // legal AND non-suicidal move
                if (move.getDestPosition().equals(position)) {
                    Label label = new Label();
                    label.setGraphic(new ImageView(HIGHLIGHT_LEGALS_IMAGE));
                    getChildren().add(label);
                }
            }
        }

        /**
         * Returns a collection of legal moves of the selected piece.
         */
        private Collection<Move> pieceLegalMoves(Board board) {
            if (selectedPiece != null) {
                return selectedPiece.getLegalMoves(board);
            }
            return Collections.emptyList();
        }
    }

    /**
     * Helper class for player communication involving AI.
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
         * Terminates all running AI (if any).
         */
        private void stopAI() {
            if (task != null) {
                task.cancel();
            }
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
         * Stops this AI player and its timer task.
         */
        private void stop() {
            if (task != null) {
                task.cancel();
            }
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
