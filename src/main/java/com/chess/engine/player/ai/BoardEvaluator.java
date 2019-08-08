package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.Player;
import com.chess.gui.Table;

import java.util.Collection;
import java.util.Random;

import static com.chess.engine.board.Board.*;
import static com.chess.engine.pieces.Piece.*;

/**
 * A helper class for evaluating a board.
 */
final class BoardEvaluator {

    private static final BoardEvaluator INSTANCE = new BoardEvaluator();

    private final Random rand;

    private BoardEvaluator() {
        rand = new Random();
    }

    /**
     * Returns an instance of this board evaluator.
     * @return An instance of this board evaluator.
     */
    static BoardEvaluator getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the heuristic value of the given board at the current search depth.
     * The higher the value, the better for the red player.
     * @param board The current board.
     * @param depth The current search depth.
     * @return The heuristic value of the given board at the current search depth.
     */
    int evaluate(Board board, int depth) {
        return getPlayerScore(board, board.getRedPlayer(), depth)
                - getPlayerScore(board, board.getBlackPlayer(), depth)
                + getRelationScore(board)
                + (Table.getInstance().isAIRandomised() ? rand.nextInt(5) : 0);
    }

    /**
     * Returns a heuristic value of a player based on the given board and search depth.
     */
    private static int getPlayerScore(Board board, Player player, int depth) {
        int standardValue = getTotalPieceValue(board, player) + getTotalMobilityValue(player);

        // only need to get checkmate value for opp
        int checkmateValue = 0;
        if (board.getCurrPlayer().getOpponent().getAlliance().equals(player.getAlliance())) {
            checkmateValue = getCheckmateValue(player) * (depth + 1);
        }

        return standardValue + checkmateValue;
    }

    /**
     * Returns the total value of all active pieces of the given player on the given board.
     */
    private static int getTotalPieceValue(Board board, Player player) {
        int totalMaterialValue = 0;
        int totalPositionValue = 0;
        int totalMiscValue = 0;
        BoardStatus boardStatus = board.getStatus();

        int chariotCount = 0, cannonCount = 0, horseCount = 0;
        for (Piece piece : player.getActivePieces()) {
            totalMaterialValue += piece.getMaterialValue(boardStatus);
            totalPositionValue += piece.getPositionValue();
            switch (piece.getPieceType()) {
                case CHARIOT:
                    chariotCount++;
                    break;
                case CANNON:
                    cannonCount++;
                    break;
                case HORSE:
                    horseCount++;
                    break;
                default:
                    break;
            }
        }
        int oppChariotCount = 0,  oppElephantCount = 0, oppAdvisorCount = 0;
        for (Piece piece : player.getOpponent().getActivePieces()) {
            switch (piece.getPieceType()) {
                case CHARIOT:
                    oppChariotCount++;
                    break;
                case ELEPHANT:
                    oppElephantCount++;
                    break;
                case ADVISOR:
                    oppAdvisorCount++;
                    break;
                default:
                    break;
            }
        }

        // compare number of chariots
        totalMiscValue += 100 * (chariotCount - oppChariotCount);
        if (boardStatus.equals(BoardStatus.END)) {
            // cannon+horse might be better than cannon+cannon or horse+horse
            if (cannonCount > 0 && horseCount > 0) {
                totalMiscValue += 50;
            }
            // cannon might be strong against lack of elephants
            if (oppElephantCount == 0) {
                totalMiscValue += 75 * cannonCount;
            }
        }
        if (!boardStatus.equals(BoardStatus.OPENING)) {
            // double chariots might be strong against lack of advisors
            if (chariotCount == 2) {
                if (oppAdvisorCount == 1) {
                    totalMiscValue += (350 - 100 * oppChariotCount);
                } else if (oppAdvisorCount == 0) {
                    totalMiscValue += (500 - 100 * oppChariotCount);
                }
            }
        }

        return totalMaterialValue + totalPositionValue + totalMiscValue;
    }

    /**
     * Returns the total mobility value of the given player.
     */
    private static int getTotalMobilityValue(Player player) {
        int totalMobilityValue = 0;

        for (Move move : player.getLegalMoves()) {
            totalMobilityValue += move.getMovedPiece().getPieceType().getMobilityValue();
        }

        return totalMobilityValue;
    }

    /**
     * Returns a complex relationship analysis score on the given board.
     * The higher the score, the better for the red player.
     */
    private static int getRelationScore(Board board) {
        int[] scores = new int[2];
        BoardStatus boardStatus = board.getStatus();

        for (Piece piece : board.getAllPieces()) {
            Player player = piece.getAlliance().isRed() ? board.getRedPlayer() : board.getBlackPlayer();
            int index = player.getAlliance().isRed() ? 0 : 1;
            int pieceValue = piece.getMaterialValue(boardStatus) + piece.getPositionValue();

            int oppTotalAttack = 0;
            int oppMinAttack = Integer.MAX_VALUE;
            int oppMaxAttack = 0;
            int playerTotalDefense = 0;
            int playerMinDefense = Integer.MAX_VALUE;
            int playerMaxDefense = 0;
            int flagValue = Integer.MAX_VALUE;
            int unitValue = pieceValue >> 3;

            // tabulate incoming attacks
            Collection<Move> attackMoves =
                    Player.getIncomingAttacks(piece.getPosition(), player.getOpponent().getLegalMoves());
            for (Move move : attackMoves) {
                Piece oppPiece = move.getMovedPiece();
                int attackValue = oppPiece.getMaterialValue(boardStatus) + oppPiece.getPositionValue();
                if (attackValue < pieceValue && attackValue < flagValue) {
                    flagValue = attackValue;
                }
                oppMinAttack = Math.min(oppMinAttack, attackValue);
                oppMaxAttack = Math.max(oppMaxAttack, attackValue);
                oppTotalAttack += attackValue;
            }

            // tabulate defenses
            Collection<Piece> defendingPieces = player.getDefenses(piece.getPosition());
            for (Piece defendingPiece : defendingPieces) {
                int defendingValue = defendingPiece.getMaterialValue(boardStatus);
                playerMinDefense = Math.min(playerMinDefense, defendingValue);
                playerMaxDefense = Math.max(playerMaxDefense, defendingValue);
                playerTotalDefense += defendingValue;
            }

            // calculate scores
            boolean isCurrTurn = board.getCurrPlayer().getAlliance().equals(player.getAlliance());
            if (oppTotalAttack == 0) {
                scores[index] += 10 * defendingPieces.size();
            } else {
                if (defendingPieces.isEmpty()) {
                    scores[index] -= isCurrTurn ? unitValue : 5 * unitValue;
                } else {
                    int playerScore = 0;
                    int oppScore = 0;

                    if (flagValue != Integer.MAX_VALUE) {
                        playerScore = unitValue;
                        oppScore = flagValue >> 3;
                    } else if (defendingPieces.size() == 1 && attackMoves.size() > 1
                            && oppMinAttack < (pieceValue + playerTotalDefense)) {
                        playerScore = unitValue + (playerTotalDefense >> 3);
                        oppScore = oppMinAttack >> 3;
                    } else if (defendingPieces.size() == 2 && attackMoves.size() == 3
                            && (oppTotalAttack - oppMaxAttack) < (pieceValue + playerTotalDefense)) {
                        playerScore = unitValue + (playerTotalDefense >> 3);
                        oppScore = (oppTotalAttack - oppMaxAttack) >> 3;
                    } else if (defendingPieces.size() == attackMoves.size()
                            && oppTotalAttack < (pieceValue + playerTotalDefense - playerMaxDefense)) {
                        playerScore = unitValue + ((playerTotalDefense - playerMaxDefense) >> 3);
                        oppScore = oppTotalAttack >> 3;
                    }

                    scores[index] -= isCurrTurn ? playerScore : 5 * playerScore;
                    scores[1 - index] -= isCurrTurn ? oppScore  : 5 * oppScore;
                }
            }
        }

        return scores[0] - scores[1];
    }

    /**
     * Returns the checkmate value for the given player.
     */
    private static int getCheckmateValue(Player player) {
        return player.getOpponent().isInCheckmate() ? PieceType.GENERAL.getDefaultValue() : 0;
    }
}
