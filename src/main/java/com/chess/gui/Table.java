package com.chess.gui;

import com.chess.engine.LoadGameUtil;
import com.chess.engine.board.Board;
import com.chess.engine.board.Coordinate;
import com.chess.engine.board.Move;
import com.chess.engine.board.Point;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.ai.MiniMax;
import com.chess.engine.player.ai.MoveBook;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import static com.chess.engine.pieces.Piece.*;
import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

public class Table {

    private static final Dimension BOARD_PANEL_DIMENSION = new Dimension(540, 600);
    private static final Dimension POINT_PANEL_DIMENSION = new Dimension(60, 60);
    private static final String GRAPHICS_MISC_PATH = "/graphics/misc/";
    private static final String GRAPHICS_PIECES_PATH = "/graphics/pieces/";
    private static final ImageIcon GAME_ICON = getGameIcon();
    private static final ImageIcon BOARD_ICON = getBoardIcon();
    private static final ImageIcon HIGHLIGHT_ICON = getHighlightIcon();
    private static final Color HIGHLIGHT_BORDER_COLOR = Color.WHITE;
    static final Map<String, ImageIcon> PIECE_ICON_MAP = getPieceIconMap();
    private static final Table TABLE_INSTANCE = new Table();

    private final JFrame gameFrame;
    private final BoardPanel boardPanel;
    private final MoveHistoryPanel moveHistoryPanel;
    private final InfoPanel infoPanel;
    private final MoveLog movelog;
    private final List<Board> boardHistory;
    private final GameSetup gameSetup;
    private final AIObserver aiObserver;
    private final PropertyChangeSupport support;
    private Board board;
    private BoardDirection boardDirection;
    private Point sourcePoint;
    private Point destPoint;
    private Piece humanMovedPiece;

    private Table() {
        gameFrame = new JFrame("CChess");
        gameFrame.setIconImage(GAME_ICON.getImage());

        board = Board.initialiseBoard();
        boardDirection = BoardDirection.NORMAL;
        boardPanel = new BoardPanel(BOARD_ICON.getImage());
        moveHistoryPanel = new MoveHistoryPanel();
        infoPanel = new InfoPanel();
        infoPanel.updateStatusPanel(board);
        movelog = new MoveLog();
        boardHistory = new ArrayList<>();
        boardHistory.add(board);
        gameSetup = new GameSetup(gameFrame, true);
        aiObserver = new AIObserver();
        support = new PropertyChangeSupport(this);
        support.addPropertyChangeListener(aiObserver);

        JMenuBar tableMenuBar = createMenuBar();
        gameFrame.setJMenuBar(tableMenuBar);
        gameFrame.setLayout(new BorderLayout());

        JPanel wrapperPanel = new JPanel();
        wrapperPanel.add(boardPanel);
        gameFrame.add(wrapperPanel, BorderLayout.CENTER);
        gameFrame.add(moveHistoryPanel, BorderLayout.EAST);
        gameFrame.add(infoPanel, BorderLayout.WEST);

        gameFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        gameFrame.setResizable(false);
        gameFrame.setVisible(true);
        gameFrame.pack();
    }

    public static Table getInstance() {
        return TABLE_INSTANCE;
    }

    private JMenuBar createMenuBar() {
        JMenuBar tableMenuBar = new JMenuBar();

        tableMenuBar.add(createGameMenu());
        tableMenuBar.add(createOptionsMenu());

        return tableMenuBar;
    }

    private JMenu createGameMenu() {
        JMenu gameMenu = new JMenu("Game");

        JMenuItem newGame = new JMenuItem("New game");
        newGame.addActionListener(e -> {
            if (movelog.isEmpty()) return;
            int option = JOptionPane.showConfirmDialog(gameFrame, "Start a new game?",
                    "", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                aiObserver.stopAI();
                undoAllMoves();
                support.firePropertyChange("newgame", null, null);
            }
        });
        gameMenu.add(newGame);
        gameMenu.addSeparator();

        JMenuItem saveGame = new JMenuItem("Save game");
        saveGame.addActionListener(e -> saveGame());
        gameMenu.add(saveGame);

        JMenuItem loadGame = new JMenuItem("Load game");
        loadGame.addActionListener(e -> loadGame());
        gameMenu.add(loadGame);
        gameMenu.addSeparator();

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));
        gameMenu.add(exit);

        return gameMenu;
    }

    private JMenu createOptionsMenu() {
        JMenu optionsMenu = new JMenu("Options");

        JMenuItem undoTurn = new JMenuItem("Undo last turn");
        undoTurn.addActionListener(e -> {
            aiObserver.stopAI();
            undoLastTurn();
            support.firePropertyChange("undoturn", null, null);
        });
        optionsMenu.add(undoTurn);

        JMenuItem undoMove = new JMenuItem("Undo last move");
        undoMove.addActionListener(e -> {
            aiObserver.stopAI();
            undoLastMove();
            support.firePropertyChange("undomove", null, null);
        });
        optionsMenu.add(undoMove);
        optionsMenu.addSeparator();

        JMenuItem setupMenuItem = new JMenuItem("Setup...");
        setupMenuItem.addActionListener(e -> {
            gameSetup.promptUser();
            setupUpdate();
        });
        optionsMenu.add(setupMenuItem);
        optionsMenu.addSeparator();

        JMenuItem flipMenuItem = new JMenuItem("Flip board");
        flipMenuItem.addActionListener(e -> flipBoard());
        optionsMenu.add(flipMenuItem);

        return optionsMenu;
    }

    private void saveGame() {
        if (movelog.isEmpty()) {
            JOptionPane.showMessageDialog(gameFrame, "No moves made");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save game");

        int val = fc.showSaveDialog(gameFrame);
        if (val == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                PrintWriter pw = new PrintWriter(file);
                for (Move move : movelog.getMoves()) {
                    pw.append(move.toString()).append("\n");
                }
                pw.flush();
                pw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            JOptionPane.showMessageDialog(gameFrame, "Save success");
        }
    }

    private void loadGame() {
        JFileChooser fc = new JFileChooser() {
            @Override
            public void approveSelection() {
                File file = getSelectedFile();
                LoadGameUtil lgu = new LoadGameUtil(file);
                if (!lgu.isValidFile()) {
                    JOptionPane.showMessageDialog(gameFrame, "Invalid file");
                } else {
                    aiObserver.stopAI();
                    List<Board> boards = lgu.getBoardHistory();
                    boardHistory.clear();
                    boardHistory.addAll(boards);
                    board = boardHistory.get(boardHistory.size() - 1);

                    movelog.clear();
                    for (Move move : lgu.getMoves()) {
                        movelog.addMove(move);
                    }
                    moveHistoryPanel.update(movelog);
                    infoPanel.updateCapturedPanel(movelog);
                    infoPanel.updateStatusPanel(board);
                    boardPanel.drawBoard(board);

                    JOptionPane.showMessageDialog(gameFrame, "Load success");
                }
            }
        };
        fc.setDialogTitle("Load game");

        fc.showOpenDialog(gameFrame);
    }

    private void undoLastMove() {
        if (!movelog.isEmpty()) {
            clearSelections();

            movelog.removeLastMove();
            boardHistory.remove(boardHistory.size() - 1);
            board = boardHistory.get(boardHistory.size() - 1);

            moveHistoryPanel.update(movelog);
            infoPanel.updateCapturedPanel(movelog);
            infoPanel.updateStatusPanel(board);
            boardPanel.drawBoard(board);
        }
    }

    private void undoLastTurn() {
        if (movelog.getSize() > 1) {
            undoLastMove();
            undoLastMove();
        }
    }

    private void undoAllMoves() {
        while (!movelog.isEmpty()) {
            undoLastMove();
        }
    }

    private void flipBoard() {
        boardDirection = boardDirection.opposite();
        boardPanel.drawBoard(board);
        infoPanel.setDirection(boardDirection);
    }

    private void moveMadeUpdate() {
        support.firePropertyChange("movemade", null, null);
    }

    private void setupUpdate() {
        aiObserver.stopAI();
        support.firePropertyChange("setupupdate", null, null);
    }

    private void clearSelections() {
        sourcePoint = null;
        destPoint = null;
        humanMovedPiece = null;
    }

    private static ImageIcon getGameIcon() {
        try {
            BufferedImage image = ImageIO.read(Table.class.getResource("/graphics/icon.png"));
            return new ImageIcon(image);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static ImageIcon getBoardIcon() {
        try {
            BufferedImage image = ImageIO.read(Table.class.getResource("/graphics/board.png"));
            return new ImageIcon(image);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static ImageIcon getHighlightIcon() {
        try {
            BufferedImage image = ImageIO.read(Table.class.getResource(GRAPHICS_MISC_PATH + "green_dot.png"));
            return new ImageIcon(image);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Map<String, ImageIcon> getPieceIconMap() {
        Map<String, ImageIcon> stringToIconMap = new HashMap<>();

        for (PieceType type : PieceType.values()) {
            try {
                String name = ("R" + type.toString()).toLowerCase();
                BufferedImage image
                        = ImageIO.read(Table.class.getResource(GRAPHICS_PIECES_PATH + name + ".png"));
                stringToIconMap.put(name, new ImageIcon(image));
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                String name = ("B" + type.toString()).toLowerCase();
                BufferedImage image
                        = ImageIO.read(Table.class.getResource(GRAPHICS_PIECES_PATH + name + ".png"));
                stringToIconMap.put(name, new ImageIcon(image));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return stringToIconMap;
    }

    private static Color getHighlightBorderColor(Piece piece) {
        return piece.getAlliance().isRed() ? Color.RED : Color.BLACK;
    }

    public static class MoveLog {

        private final List<Move> moves;

        MoveLog() {
            this.moves = new ArrayList<>();
        }

        public List<Move> getMoves() {
            return moves;
        }

        public int getSize() {
            return moves.size();
        }

        public boolean isEmpty() {
            return moves.isEmpty();
        }

        public void addMove(Move move) {
            moves.add(move);
        }

        public void removeLastMove() {
            if (!moves.isEmpty()) {
                moves.remove(moves.size() - 1);
            }
        }

        public Move getLastMove() {
            if (!moves.isEmpty()) {
                return moves.get(moves.size() - 1);
            }

            return null;
        }

        public void clear() {
            moves.clear();
        }
    }

    private class BoardPanel extends JPanel {

        final List<PointPanel> pointPanels;
        final Image background;

        BoardPanel(Image background) {
            super(new GridLayout(Board.NUM_ROWS, Board.NUM_COLS));
            pointPanels = new ArrayList<>();
            this.background = background;

            for (int row = 0; row < Board.NUM_ROWS; row++) {
                for (int col = 0; col < Board.NUM_COLS; col++) {
                    PointPanel pointPanel
                            = new PointPanel(this, new Coordinate(row, col));
                    pointPanels.add(pointPanel);
                    add(pointPanel);
                }
            }

            setPreferredSize(BOARD_PANEL_DIMENSION);
            setMaximumSize(BOARD_PANEL_DIMENSION);
            setMinimumSize(BOARD_PANEL_DIMENSION);

            validate();
        }

        @Override
        public void paintComponent(Graphics g) {
            g.drawImage(background, 0, 0, null);
        }

        private void drawBoard(Board board) {
            removeAll();

            for (PointPanel pointPanel : boardDirection.traverse(pointPanels)) {
                pointPanel.drawPoint(board);
                add(pointPanel);
            }

            validate();
            repaint();
        }
    }

    public enum BoardDirection {
        NORMAL {
            @Override
            boolean isNormal() {
                return true;
            }

            @Override
            List<PointPanel> traverse(List<PointPanel> pointPanels) {
                return pointPanels;
            }

            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }
        },
        FLIPPED {
            @Override
            boolean isNormal() {
                return false;
            }

            @Override
            List<PointPanel> traverse(List<PointPanel> pointPanels) {
                List<PointPanel> copy = new ArrayList<>(pointPanels);
                Collections.reverse(copy);
                return copy;
            }

            @Override
            BoardDirection opposite() {
                return NORMAL;
            }
        };

        abstract boolean isNormal();

        abstract List<PointPanel> traverse(List<PointPanel> pointPanels);

        abstract BoardDirection opposite();
    }

    private class PointPanel extends JLayeredPane {

        private final Coordinate position;

        PointPanel(BoardPanel boardPanel, Coordinate position) {
            super();
            this.position = position;

            setSize(POINT_PANEL_DIMENSION);
            assignPointPieceIcon(board);

            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isRightMouseButton(e)) {
                        clearSelections();
                    } else if (isLeftMouseButton(e)) {
                        if (sourcePoint == null) {
                            sourcePoint = board.getPoint(position);
                            Optional<Piece> selectedPiece = sourcePoint.getPiece();
                            if (selectedPiece.isPresent()
                                    && selectedPiece.get().getAlliance() == board.getCurrPlayer().getAlliance()
                                    && !gameSetup.isAIPlayer(board.getCurrPlayer())) {
                                humanMovedPiece = selectedPiece.get();
                            } else {
                                sourcePoint = null;
                            }
                        } else {
                            destPoint = board.getPoint(position);
                            Optional<Move> move = Move.getMove(board, sourcePoint.getPosition(),
                                    destPoint.getPosition());
                            if (!move.isPresent()) return;

                            MoveTransition transition = board.getCurrPlayer().makeMove(move.get());
                            if (transition.getMoveStatus().isDone()) {
                                board = transition.getNextBoard();
                                boardHistory.add(board);
                                movelog.addMove(transition.getMove());

                                clearSelections();
                                SwingUtilities.invokeLater(() -> {
                                    moveHistoryPanel.update(movelog);
                                    infoPanel.updateCapturedPanel(movelog);
                                    infoPanel.updateStatusPanel(board);
                                    moveMadeUpdate();
                                });
                            }
                        }
                    }

                    SwingUtilities.invokeLater(() -> boardPanel.drawBoard(board));
                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {

                }

                @Override
                public void mouseExited(MouseEvent e) {

                }
            });

            validate();
        }

        private void drawPoint(Board board) {
            assignPointPieceIcon(board);
            highlightLastMoveAndSelectedPiece();
            highlightPossibleMoves(board);

            validate();
            repaint();
        }

        private void assignPointPieceIcon(Board board) {
            removeAll();

            Point point = board.getPoint(position);
            Optional<Piece> destPiece = point.getPiece();
            destPiece.ifPresent(p -> {
                String name = (p.getAlliance().toString().substring(0, 1) + p.getPieceType().toString()).toLowerCase();
                ImageIcon icon = PIECE_ICON_MAP.get(name);
                JLabel label = new JLabel(icon);
                add(label, PALETTE_LAYER);

                int offset = (int) (POINT_PANEL_DIMENSION.getWidth() - icon.getIconWidth()) / 2;
                label.setBounds(offset, offset, icon.getIconWidth(), icon.getIconHeight());
            });
        }

        private void highlightLastMoveAndSelectedPiece() {
            Move lastMove = movelog.getLastMove();

            if (lastMove != null) {
                Piece lastMovedPiece = lastMove.getMovedPiece();
                if (lastMovedPiece.getPosition().equals(position)) {
                    setBorder(BorderFactory.createDashedBorder(HIGHLIGHT_BORDER_COLOR, 2, 2, 2, true));
                    return;
                }
                if (lastMove.getDestPosition().equals(position)) {
                    setBorder(BorderFactory.createDashedBorder(HIGHLIGHT_BORDER_COLOR, 2, 2, 2, true));
                    return;
                }
            }

            if (humanMovedPiece != null && humanMovedPiece.getPosition().equals(position)) {
                setBorder(BorderFactory.createLineBorder(HIGHLIGHT_BORDER_COLOR, 2, true));
                return;
            }

            setBorder(null);
        }

        private void highlightPossibleMoves(Board board) {
            for (Move move : pieceLegalMoves(board)) {
                // check for suicidal move
                MoveTransition transition = board.getCurrPlayer().makeMove(move);
                if (!transition.getMoveStatus().isDone()) {
                    continue;
                }

                // legal AND non-suicidal move
                if (move.getDestPosition().equals(position)) {
                    JLabel label = new JLabel(HIGHLIGHT_ICON);
                    add(label, MODAL_LAYER);

                    int offset = (int) (POINT_PANEL_DIMENSION.getWidth() - HIGHLIGHT_ICON.getIconWidth()) / 2;
                    label.setBounds(offset, offset, HIGHLIGHT_ICON.getIconWidth(), HIGHLIGHT_ICON.getIconHeight());
                }
            }
        }

        private Collection<Move> pieceLegalMoves(Board board) {
            if (humanMovedPiece != null) {
                return humanMovedPiece.getLegalMoves(board);
            }
            return Collections.emptyList();
        }
    }

    private static class AIObserver implements PropertyChangeListener {

        private static final MoveBook movebook = MoveBook.getInstance();

        private FixedDepthAIPlayer fixedDepthAIPlayer;
        private FixedTimeAIPlayer fixedTimeAIPlayer;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (getInstance().gameSetup.isAIPlayer(getInstance().board.getCurrPlayer())
                    && !getInstance().board.getCurrPlayer().isInCheckmate()) {
                Board board = getInstance().board;
                Optional<Move> move = movebook.getRandomMove(board);
                if (move.isPresent()) {
                    makeMove(move.get());
                    System.out.println(move.get() + " [movebook]");
                    return;
                }
                if (getInstance().gameSetup.isAITimeLimited()) {
                    fixedTimeAIPlayer = new FixedTimeAIPlayer();
                    fixedTimeAIPlayer.execute();
                } else {
                    fixedDepthAIPlayer = new FixedDepthAIPlayer();
                    fixedDepthAIPlayer.execute();
                }
            }
        }

        private static void makeMove(Move move) {
            getInstance().board = getInstance().board.getCurrPlayer().makeMove(move).getNextBoard();
            getInstance().boardHistory.add(getInstance().board);
            getInstance().movelog.addMove(move);
            getInstance().boardPanel.drawBoard(getInstance().board);
            getInstance().moveHistoryPanel.update(getInstance().movelog);
            getInstance().infoPanel.updateCapturedPanel(getInstance().movelog);
            getInstance().infoPanel.updateStatusPanel(getInstance().board);
            getInstance().moveMadeUpdate();
        }

        private void stopAI() {
            if (fixedDepthAIPlayer != null) {
                fixedDepthAIPlayer.cancel(true);
            }
            if (fixedTimeAIPlayer != null) {
                fixedTimeAIPlayer.cancelTimer();
                fixedTimeAIPlayer.cancel(true);
            }
        }
    }

    private abstract static class AIPlayer extends SwingWorker<Move, String> {

        private static final int MAX_CONSEC_CHECKS = 3;

        Piece bannedPiece;

        private AIPlayer() {
            bannedPiece = getBannedPiece();
        }

        private Piece getBannedPiece() {
            List<Board> boardHistory = getInstance().boardHistory;
            List<Move> moveHistory = getInstance().movelog.getMoves();
            if (moveHistory.size() < MAX_CONSEC_CHECKS * 2) return null;

            for (int i = 0; i < MAX_CONSEC_CHECKS; i++) {
                Board board = boardHistory.get(boardHistory.size() - 2 - i*2);
                if (!board.getCurrPlayer().isInCheck()) return null;
            }

            Board board = boardHistory.get(boardHistory.size() - MAX_CONSEC_CHECKS*2);
            Move move = moveHistory.get(moveHistory.size() - MAX_CONSEC_CHECKS*2);
            if (move.getCapturedPiece().isPresent()) return null;
            Piece movedPiece = board.getPoint(move.getDestPosition()).getPiece().get();
            for (int i = 1; i < MAX_CONSEC_CHECKS; i++) {
                move = moveHistory.get(moveHistory.size() - MAX_CONSEC_CHECKS*2 + i*2);
                if (!move.getMovedPiece().equals(movedPiece) || move.getCapturedPiece().isPresent()) return null;
                board = boardHistory.get(boardHistory.size() - MAX_CONSEC_CHECKS*2 + i*2);
                movedPiece = board.getPoint(move.getDestPosition()).getPiece().get();
            }

            // check limit reached
            return movedPiece;
        }
    }

    private static class FixedDepthAIPlayer extends AIPlayer {

        private int searchDepth;
        private long startTime;
        private long endTime;

        @Override
        public void done() {
            if (isCancelled()) return;
            try {
                Move bestMove = get();
                AIObserver.makeMove(bestMove);
                endTime = System.currentTimeMillis();
                System.out.println(bestMove.toString() + " | " + (endTime - startTime)/1000 + "s | " + "depth " + searchDepth);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Move doInBackground() {
            startTime = System.currentTimeMillis();
            searchDepth = getInstance().gameSetup.getSearchDepth();
            return MiniMax.getInstance().fixedDepth(getInstance().board, searchDepth, bannedPiece);
        }
    }

    public static class FixedTimeAIPlayer extends AIPlayer implements PropertyChangeListener {

        private final Timer timer;
        private Move currBestMove;
        private int currDepth;
        private TimerTask currTask;
        private int searchTime;

        private FixedTimeAIPlayer() {
            timer = new Timer("AI Timer");
        }

        @Override
        protected Move doInBackground() throws Exception {
            currTask = getTimerTask();
            searchTime = getInstance().gameSetup.getSearchTime() * 1000;
            timer.schedule(currTask, searchTime);
            return MiniMax.getInstance().iterativeDeepening(getInstance().board, bannedPiece, this,
                    System.currentTimeMillis() + searchTime);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            currBestMove = (Move) evt.getNewValue();
            currDepth = (int) evt.getOldValue();
        }

        private TimerTask getTimerTask() {
            return new TimerTask() {
                @Override
                public void run() {
                    AIObserver.makeMove(currBestMove);
                    System.out.println(currBestMove.toString() + " | "
                            + searchTime + "s | " + "depth " + currDepth);
                    FixedTimeAIPlayer.this.cancel(true);
                }
            };
        }

        void cancelTimer() {
            if (currTask != null) {
                currTask.cancel();
            }
        }
    }

    public enum PlayerType {
        HUMAN {
            @Override
            boolean isAI() {
                return false;
            }
        },
        AI {
            @Override
            boolean isAI() {
                return true;
            }
        };

        abstract boolean isAI();
    }

    public enum AIType {
        TIME {
            @Override
            boolean isTimeLimited() {
                return true;
            }
        },
        DEPTH {
            @Override
            boolean isTimeLimited() {
                return false;
            }
        };

        abstract boolean isTimeLimited();
    }
}
