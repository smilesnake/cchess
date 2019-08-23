package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.chess.gui.Table.*;

/**
 * Represents a fixed-time MiniMax algorithm.
 */
public class FixedTimeSearch extends MiniMax {

    private final PropertyChangeSupport support;
    private final long endTime;

    public FixedTimeSearch(Board board, Collection<Move> bannedMoves, FixedTimeAIPlayer fixedTimeAIPlayer, long endTime) {
        super(board, bannedMoves);
        this.endTime = endTime;
        support = new PropertyChangeSupport(this);
        support.addPropertyChangeListener(fixedTimeAIPlayer);
    }

    public Move search() {
        MoveEntry bestMoveEntry = null;

        int alpha = NEG_INF;
        int beta = POS_INF;
        int currDepth = 1;
        List<MoveEntry> oldMoveEntries = new ArrayList<>();
        for (Move move : MoveSorter.simpleSort(legalMoves)) {
            oldMoveEntries.add(new MoveEntry(move, 0));
        }

        while (System.currentTimeMillis() < endTime) {
            List<MoveEntry> newMoveEntries = alphaBetaRoot(oldMoveEntries, currDepth, alpha, beta);
            bestMoveEntry = newMoveEntries.get(0);

            int bestVal = bestMoveEntry.val;
            if (bestVal <= alpha || bestVal >= beta) {
                alpha = NEG_INF;
                beta = POS_INF;
                continue;
            }
            alpha = bestVal - ASP_WINDOW;
            beta = bestVal + ASP_WINDOW;

            support.firePropertyChange("currbestmove", currDepth, bestMoveEntry.move);
            oldMoveEntries = newMoveEntries;
            currDepth++;
        }

        assert bestMoveEntry != null;
        return bestMoveEntry.move;
    }
}
