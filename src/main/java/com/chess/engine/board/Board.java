package com.chess.engine.board;

import com.chess.engine.Alliance;
import com.chess.engine.pieces.*;
import com.chess.engine.player.Player;

import java.util.*;

import static com.chess.engine.pieces.Piece.*;

/**
 * 中国象棋棋盘
 */
public class Board {

    /**
     * 行的数量
     */
    public static final int NUM_ROWS = 10;
    /**
     * 列的数量
     */
    public static final int NUM_COLS = 9;
    /**
     * 红方河所在行
     */
    public static final int RIVER_ROW_RED = 5;
    /**
     * 黑方河所在行
     */
    public static final int RIVER_ROW_BLACK = 4;
    private static final Zobrist ZOBRIST = new Zobrist();

    private final List<Point> points;
    private final List<PlayerInfo> playerInfoHistory;
    private PlayerInfo playerInfo;
    private Alliance currTurn;
    /**
     * Zobrist键值
     */
    private long zobristKey;

    private Board(Builder builder) {
        points = createBoard(builder);
        playerInfoHistory = new ArrayList<>();
        playerInfo = generatePlayerInfo();
        currTurn = builder.currTurn;
        zobristKey = ZOBRIST.getKey(points, currTurn);
    }

    /**
     * Returns a list of points representing a board based on the given builder.
     */
    private static List<Point> createBoard(Builder builder) {
        List<Point> points = new ArrayList<>();

        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                Coordinate position = new Coordinate(row, col);
                Point point = new Point(position);
                point.setPiece(builder.boardConfig.get(position));
                points.add(point);
            }
        }

        return Collections.unmodifiableList(points);
    }

    /**
     * Returns the original state of a board.
     */
    public static Board initialiseBoard() {
        Builder builder = new Builder();

        builder.putPiece(new Chariot(new Coordinate(0, 0), Alliance.BLACK)).putPiece(new Horse(new Coordinate(0, 1), Alliance.BLACK)).putPiece(new Elephant(new Coordinate(0, 2), Alliance.BLACK)).putPiece(new Advisor(new Coordinate(0, 3), Alliance.BLACK)).putPiece(new General(new Coordinate(0, 4), Alliance.BLACK)).putPiece(new Advisor(new Coordinate(0, 5), Alliance.BLACK)).putPiece(new Elephant(new Coordinate(0, 6), Alliance.BLACK)).putPiece(new Horse(new Coordinate(0, 7), Alliance.BLACK)).putPiece(new Chariot(new Coordinate(0, 8), Alliance.BLACK)).putPiece(new Cannon(new Coordinate(2, 1), Alliance.BLACK)).putPiece(new Cannon(new Coordinate(2, 7), Alliance.BLACK)).putPiece(new Soldier(new Coordinate(3, 0), Alliance.BLACK)).putPiece(new Soldier(new Coordinate(3, 2), Alliance.BLACK)).putPiece(new Soldier(new Coordinate(3, 4), Alliance.BLACK)).putPiece(new Soldier(new Coordinate(3, 6), Alliance.BLACK)).putPiece(new Soldier(new Coordinate(3, 8), Alliance.BLACK));

        builder.putPiece(new Chariot(new Coordinate(9, 0), Alliance.RED)).putPiece(new Horse(new Coordinate(9, 1), Alliance.RED)).putPiece(new Elephant(new Coordinate(9, 2), Alliance.RED)).putPiece(new Advisor(new Coordinate(9, 3), Alliance.RED)).putPiece(new General(new Coordinate(9, 4), Alliance.RED)).putPiece(new Advisor(new Coordinate(9, 5), Alliance.RED)).putPiece(new Elephant(new Coordinate(9, 6), Alliance.RED)).putPiece(new Horse(new Coordinate(9, 7), Alliance.RED)).putPiece(new Chariot(new Coordinate(9, 8), Alliance.RED)).putPiece(new Cannon(new Coordinate(7, 1), Alliance.RED)).putPiece(new Cannon(new Coordinate(7, 7), Alliance.RED)).putPiece(new Soldier(new Coordinate(6, 0), Alliance.RED)).putPiece(new Soldier(new Coordinate(6, 2), Alliance.RED)).putPiece(new Soldier(new Coordinate(6, 4), Alliance.RED)).putPiece(new Soldier(new Coordinate(6, 6), Alliance.RED)).putPiece(new Soldier(new Coordinate(6, 8), Alliance.RED));

        builder.setCurrTurn(Alliance.RED);

        return builder.build();
    }


    /**
     * 生成与此棋盘上两个玩家相关的信息
     *
     * @return 生成的玩家信息
     */
    private PlayerInfo generatePlayerInfo() {
        Collection<Piece> redPieces = new ArrayList<>();
        Collection<Move> redLegalMoves = new ArrayList<>();
        int redMobilityValue = 0;
        Collection<Attack> redAttacks = new ArrayList<>();
        Collection<Defense> redDefenses = new ArrayList<>();

        Collection<Piece> blackPieces = new ArrayList<>();
        Collection<Move> blackLegalMoves = new ArrayList<>();
        int blackMobilityValue = 0;
        Collection<Attack> blackAttacks = new ArrayList<>();
        Collection<Defense> blackDefenses = new ArrayList<>();

        for (Point point : points) {
            if (point.isEmpty()) continue;
            Piece piece = point.getPiece().get();

            if (piece.getAlliance().isRed()) {
                redPieces.add(piece);
                Collection<Move> moves = piece.getLegalMoves(this, redAttacks, redDefenses);
                redLegalMoves.addAll(moves);
                redMobilityValue += piece.getPieceType().getMobilityValue() * moves.size();
            } else {
                blackPieces.add(piece);
                Collection<Move> moves = piece.getLegalMoves(this, blackAttacks, blackDefenses);
                blackLegalMoves.addAll(moves);
                blackMobilityValue += piece.getPieceType().getMobilityValue() * moves.size();
            }
        }

        Player redPlayer = new Player(Alliance.RED, redPieces, redLegalMoves, blackLegalMoves, redMobilityValue, redAttacks, redDefenses);
        Player blackPlayer = new Player(Alliance.BLACK, blackPieces, blackLegalMoves, redLegalMoves, blackMobilityValue, blackAttacks, blackDefenses);
        return new PlayerInfo(redPlayer, blackPlayer);
    }

    /**
     * 从现有信息中更新与此棋盘上双方玩家相关的信息
     */
    private PlayerInfo updatePlayerInfo(Move move) {
        Piece movedPiece = move.getMovedPiece();
        Piece destPiece = movedPiece.movePiece(move);
        Piece capturedPiece = move.isCapture() ? move.getCapturedPiece().get() : null;

        Collection<Piece> redPieces = new ArrayList<>();
        Collection<Move> redLegalMoves = new ArrayList<>();
        int redMobilityValue = 0;
        Collection<Attack> redAttacks = new ArrayList<>();
        Collection<Defense> redDefenses = new ArrayList<>();

        Collection<Piece> blackPieces = new ArrayList<>();
        Collection<Move> blackLegalMoves = new ArrayList<>();
        int blackMobilityValue = 0;
        Collection<Attack> blackAttacks = new ArrayList<>();
        Collection<Defense> blackDefenses = new ArrayList<>();

        Player redPlayer = getPlayer(Alliance.RED);
        Player blackPlayer = getPlayer(Alliance.BLACK);

        for (Piece piece : redPlayer.getActivePieces()) {
            if (piece.equals(movedPiece) || piece.equals(capturedPiece)) continue;
            redPieces.add(piece);
            Collection<Move> moves = piece.getLegalMoves(this, redAttacks, redDefenses);
            redLegalMoves.addAll(moves);
            redMobilityValue += piece.getPieceType().getMobilityValue() * moves.size();
        }
        for (Piece piece : blackPlayer.getActivePieces()) {
            if (piece.equals(movedPiece) || piece.equals(capturedPiece)) continue;
            blackPieces.add(piece);
            Collection<Move> moves = piece.getLegalMoves(this, blackAttacks, blackDefenses);
            blackLegalMoves.addAll(moves);
            blackMobilityValue += piece.getPieceType().getMobilityValue() * moves.size();
        }
        if (destPiece.getAlliance().isRed()) {
            redPieces.add(destPiece);
            Collection<Move> moves = destPiece.getLegalMoves(this, redAttacks, redDefenses);
            redLegalMoves.addAll(moves);
            redMobilityValue += destPiece.getPieceType().getMobilityValue() * moves.size();
        } else {
            blackPieces.add(destPiece);
            Collection<Move> moves = destPiece.getLegalMoves(this, blackAttacks, blackDefenses);
            blackLegalMoves.addAll(moves);
            blackMobilityValue += destPiece.getPieceType().getMobilityValue() * moves.size();
        }

        redPlayer = new Player(Alliance.RED, redPieces, redLegalMoves, blackLegalMoves, redMobilityValue, redAttacks, redDefenses);
        blackPlayer = new Player(Alliance.BLACK, blackPieces, blackLegalMoves, redLegalMoves, blackMobilityValue, blackAttacks, blackDefenses);
        return new PlayerInfo(redPlayer, blackPlayer);
    }

    /**
     * 在这个棋盘上做出给定的移动，玩家信息和Zobrist键值更新
     *
     * @param move 要走的一步
     */
    public void makeMove(Move move) {
        // 移动的棋子
        Piece movedPiece = move.getMovedPiece();
        // 当前位置
        Coordinate srcPosition = movedPiece.getPosition();
        // 目录位置
        Coordinate destPosition = move.getDestPosition();

        Point srcPoint = points.get(BoardUtil.positionToIndex(srcPosition));
        // 源位置移除棋子
        srcPoint.removePiece();
        Point destPoint = points.get(BoardUtil.positionToIndex(destPosition));
        // 目标位置添加落子后的棋子对象
        destPoint.setPiece(movedPiece.movePiece(move));

        // 添加玩家信息
        playerInfoHistory.add(playerInfo);
        // 更新玩家信息
        playerInfo = updatePlayerInfo(move);
        // 变更当前落子对象
        changeTurn();
        // 更新Zobrist键值
        zobristKey = ZOBRIST.updateKey(zobristKey, move);
    }

    /**
     * 撤销棋盘上的给定走法。玩家信息和Zobrist键被更新。
     *
     * @param move 被撤销的动作.
     */
    public void unmakeMove(Move move) {
        // 移动的棋子
        Piece movedPiece = move.getMovedPiece();
        // 被吃的棋子
        Optional<Piece> capturedPiece = move.getCapturedPiece();
        // 当前位置
        Coordinate srcPosition = movedPiece.getPosition();
        // 目录位置
        Coordinate destPosition = move.getDestPosition();

        Point srcPoint = points.get(BoardUtil.positionToIndex(srcPosition));
        // 源位置还原移动的棋子
        srcPoint.setPiece(movedPiece);
        Point destPoint = points.get(BoardUtil.positionToIndex(destPosition));
        // 目标位置移除棋子
        destPoint.removePiece();
        // 被吃棋子存在则复原
        capturedPiece.ifPresent(destPoint::setPiece);

        // 更新玩家信息
        playerInfo = playerInfoHistory.isEmpty() ? generatePlayerInfo() : playerInfoHistory.remove(playerInfoHistory.size() - 1);
        // 变更当前落子对象
        changeTurn();
        // 更新Zobrist键值
        zobristKey = ZOBRIST.updateKey(zobristKey, move);
    }

    /**
     * 转变当前回合所属对象（变更当前落子对象）。Zobrist键已更新
     */
    public void changeTurn() {
        currTurn = currTurn.opposite();
        zobristKey ^= ZOBRIST.side;
    }

    /**
     * 获取此棋盘上给定的源位置和目标位置相对应的移动(如果有的话)
     *
     * @param srcPosition  给定的源位置
     * @param destPosition 目标位置
     * @return 此棋盘上给定的源位置和目标位置相对应的移动
     */
    public Optional<Move> getMove(Coordinate srcPosition, Coordinate destPosition) {
        // 遍历当前玩家在给定棋盘上可以采取的合法走法的集合
        for (Move move : getCurrPlayer().getLegalMoves()) {
            // 当前走法是合法的返回
            if (move.getMovedPiece().getPosition().equals(srcPosition) && move.getDestPosition().equals(destPosition)) {
                return Optional.of(move);
            }
        }
        // 不合法，直接返回空
        return Optional.empty();
    }

    /**
     * 检查当前玩家的对手是否处于受控状态。这种状态是不允许的
     *
     * @return 如果对手不处于受控状态，true，否则，false
     */
    public boolean isStateAllowed() {
        return !getOppPlayer().isInCheck();
    }

    /**
     * 检查当前玩家是否已被将死
     *
     * @return 如果当前玩家已被将，则为true，否则为false.
     */
    public boolean isCurrPlayerCheckmated() {
        // 默认为“将死”状态
        boolean isCheckmated = true;

        // 遍历当前玩家所有可以采取的合法走法的集合
        for (Move move : getCurrPlayer().getLegalMoves()) {
            // 棋盘上做出给定的移动
            makeMove(move);
            // 非受控状态,设置为非“将死”状态
            if (isStateAllowed()) {
                isCheckmated = false;
            }
            // 撤销棋盘上的给定走法
            unmakeMove(move);
            // 如果已经设置为非“将死”状态，跳出
            if (!isCheckmated) break;
        }

        return isCheckmated;
    }

    /**
     * 检查棋盘上的游戏是不是和棋.
     *
     * @return 如果是平局则为true，否则为false.
     */
    public boolean isGameDraw() {

        for (Piece piece : getPlayer(Alliance.RED).getActivePieces()) {
            // 如果有攻击性，说明不是和棋
            if (piece.getPieceType().isAttacking()) {
                return false;
            }
        }
        for (Piece piece : getPlayer(Alliance.BLACK).getActivePieces()) {
            // 如果有攻击性，说明不是和棋
            if (piece.getPieceType().isAttacking()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the current player has no capture moves.
     *
     * @return true if the current player has no capture moves, false otherwise.
     */
    public boolean isQuiet() {
        for (Move move : getCurrPlayer().getLegalMoves()) {
            if (move.isCapture()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a copy of this board.
     *
     * @return A copy of this board.
     */
    public Board getCopy() {
        Builder builder = new Builder();

        for (Point point : points) {
            Optional<Piece> piece = point.getPiece();
            piece.ifPresent(builder::putPiece);
        }
        builder.setCurrTurn(currTurn);

        return builder.build();
    }

    /**
     * Returns a mirrored copy (about the middle column) of this board.
     *
     * @return A mirrored copy of this board.
     */
    public Board getMirrorBoard() {
        Builder builder = new Builder();

        for (Point point : points) {
            Optional<Piece> piece = point.getPiece();
            piece.ifPresent(p -> builder.putPiece(p.getMirrorPiece()));
        }
        builder.setCurrTurn(currTurn);

        return builder.build();
    }

    /**
     * Checks if the current player has given a check for three consecutive times.
     *
     * @return true if the current player has given a check for three consecutive times, false otherwise.
     */
    public boolean lastThreeChecks() {
        if (playerInfoHistory.size() < 5) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            if (currTurn.isRed()) {
                if (!playerInfoHistory.get(playerInfoHistory.size() - 1 - i * 2).blackPlayer.isInCheck()) {
                    return false;
                }
            } else {
                if (!playerInfoHistory.get(playerInfoHistory.size() - 1 - i * 2).redPlayer.isInCheck()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the advisor structure of the player with the given alliance.
     *
     * @param alliance The alliance of the player to check.
     * @return The advisor structure of the player with the given alliance.
     */
    public AdvisorStructure getAdvisorStructure(Alliance alliance) {
        int lowRow = BoardUtil.rankToRow(1, alliance);
        int midRow = BoardUtil.rankToRow(2, alliance);
        int leftCol = BoardUtil.fileToCol(6, alliance);
        int rightCol = BoardUtil.fileToCol(4, alliance);

        Optional<Piece> left = getPoint(new Coordinate(lowRow, leftCol)).getPiece();
        Optional<Piece> right = getPoint(new Coordinate(lowRow, rightCol)).getPiece();
        Optional<Piece> mid = getPoint(new Coordinate(midRow, 4)).getPiece();
        boolean hasLeft = left.map(p -> p.getPieceType().equals(PieceType.ADVISOR)).orElse(false);
        boolean hasRight = right.map(p -> p.getPieceType().equals(PieceType.ADVISOR)).orElse(false);
        boolean hasMid = mid.map(p -> p.getPieceType().equals(PieceType.ADVISOR)).orElse(false);

        if (hasLeft && hasRight) {
            return AdvisorStructure.START;
        } else if (hasLeft && hasMid) {
            return AdvisorStructure.LEFT;
        } else if (hasRight && hasMid) {
            return AdvisorStructure.RIGHT;
        } else {
            return AdvisorStructure.OTHER;
        }
    }

    /**
     * 获取指定位置对应的点位
     *
     * @param position 指定位置
     * @return 指定位置对应的点位
     */
    public Point getPoint(Coordinate position) {
        return points.get(BoardUtil.positionToIndex(position));
    }

    public long getZobristKey() {
        return zobristKey;
    }

    public Player getPlayer(Alliance alliance) {
        return alliance.isRed() ? playerInfo.redPlayer : playerInfo.blackPlayer;
    }

    public Player getCurrPlayer() {
        return currTurn.isRed() ? getPlayer(Alliance.RED) : getPlayer(Alliance.BLACK);
    }

    public Player getOppPlayer() {
        return currTurn.isRed() ? getPlayer(Alliance.BLACK) : getPlayer(Alliance.RED);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int row = 0; row < NUM_ROWS; row++) {
            for (int col = 0; col < NUM_COLS; col++) {
                String pointText = points.get(BoardUtil.positionToIndex(row, col)).toString();
                sb.append(String.format("%3s", pointText));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Represents the structure of a player's advisors.
     */
    public enum AdvisorStructure {
        START, // both advisors on starting positions
        LEFT, // one on left file, one on middle
        RIGHT, // one on right file, one on middle
        OTHER // any other structure
    }

    /**
     * Represents both players on this board.
     */
    private static class PlayerInfo {

        private Player redPlayer;
        private Player blackPlayer;

        private PlayerInfo(Player redPlayer, Player blackPlayer) {
            this.redPlayer = redPlayer;
            this.blackPlayer = blackPlayer;
        }
    }

    /**
     * Helper class for calculating and updating Zobrist keys.
     */
    private static class Zobrist {

        private final long[][][] pieces;
        private final long side;

        private Zobrist() {
            Random rand = new Random();
            pieces = new long[7][2][90];
            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 2; j++) {
                    for (int k = 0; k < 90; k++) {
                        pieces[i][j][k] = rand.nextLong();
                    }
                }
            }
            side = rand.nextLong();
        }

        /**
         * Returns the Zobrist hash of the given piece.
         */
        private long getPieceHash(Piece piece) {
            int pieceIndex = piece.getPieceType().ordinal();
            int sideIndex = piece.getAlliance().isRed() ? 0 : 1;
            int posIndex = BoardUtil.positionToIndex(piece.getPosition());

            return pieces[pieceIndex][sideIndex][posIndex];
        }

        /**
         * Returns the Zobrist key given a list of points and current turn.
         */
        private long getKey(List<Point> points, Alliance currTurn) {
            long key = 0;

            for (Point point : points) {
                if (!point.isEmpty()) {
                    key ^= getPieceHash(point.getPiece().get());
                }
            }
            if (!currTurn.isRed()) {
                key ^= side;
            }

            return key;
        }

        /**
         * Returns the new Zobrist key given the old key and the move made.
         */
        private long updateKey(long key, Move move) {
            Piece movedPiece = move.getMovedPiece();
            Piece destPiece = movedPiece.movePiece(move);
            Optional<Piece> capturedPiece = move.getCapturedPiece();

            long movedPieceHash = getPieceHash(movedPiece);
            long destPieceHash = getPieceHash(destPiece);
            key ^= movedPieceHash ^ destPieceHash;
            if (capturedPiece.isPresent()) {
                key ^= getPieceHash(capturedPiece.get());
            }

            return key;
        }
    }

    /**
     * A helper class for building a board.
     */
    static class Builder {

        private Map<Coordinate, Piece> boardConfig;
        private Alliance currTurn;

        Builder() {
            boardConfig = new HashMap<>();
        }

        Builder putPiece(Piece piece) {
            boardConfig.put(piece.getPosition(), piece);
            return this;
        }

        Builder setCurrTurn(Alliance currTurn) {
            this.currTurn = currTurn;
            return this;
        }

        Board build() {
            return new Board(this);
        }
    }
}
