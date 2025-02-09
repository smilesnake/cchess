package com.chess.engine.pieces;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.Coordinate;
import com.chess.engine.board.Move;

import java.util.Collection;
import java.util.Objects;

/**
 * 中国象棋棋子
 */
public abstract class Piece {

    /**
     * 所属阵营（红方或黑方）
     */
    protected final Alliance alliance;
    /**
     * 所属位置
     */
    protected final Coordinate position;
    /**
     * 棋子类型，如車、炮等
     */
    private final PieceType pieceType;
    /**
     * 哈希码
     */
    private final int hashCode;

    /**
     * 构造棋子
     *
     * @param pieceType 棋子类型
     * @param position  位置
     * @param alliance  所属阵营（红方或黑方）
     */
    Piece(PieceType pieceType, Coordinate position, Alliance alliance) {
        this.pieceType = pieceType;
        this.position = position;
        this.alliance = alliance;
        // 设置hash码
        hashCode = getHashCode();
    }

    /**
     * 返回当前棋子在给定棋盘上可以采取的合法走法的集合
     *
     * @param board 当前棋盘
     * @return 当前棋子在给定棋盘上可以采取的合法走法的集合.
     */
    public abstract Collection<Move> getLegalMoves(Board board);

    /**
     * 返回此棋子在给定棋盘上可以采取的合法走法的集合。将此棋子的攻击和防御添加到给定的各自集合中
     *
     * @param board    当前棋盘.
     * @param attacks  要添加的攻击集合.
     * @param defenses 要添加的防御集合.
     * @return 在给定的棋盘上，这一棋子可以下的合法走法的集合.
     */
    public abstract Collection<Move> getLegalMoves(Board board, Collection<Attack> attacks, Collection<Defense> defenses);

    /**
     * 根据给定的棋子落子对象落子并返回落子后的棋子对象
     *
     * @param move 棋子落子对象
     * @return 落子后的棋子对象
     */
    public abstract Piece movePiece(Move move);

    /**
     * 返回此棋子的镜像副本(大约中间一列)
     *
     * @return 此棋子的镜像副本.
     */
    public abstract Piece getMirrorPiece();

    /**
     * 获取结合材料和位置该棋子在中局的价值
     *
     * @return 结合材料和位置该棋子在中局的价值
     */
    public int getMidgameValue() {
        return alliance.isRed() ? pieceType.midGameValues[position.getRow()][position.getCol()] : pieceType.midGameValues[Board.NUM_ROWS - position.getRow() - 1][Board.NUM_COLS - position.getCol() - 1];
    }

    /**
     * 获取结合材料和位置该棋子在终局的价值
     *
     * @return 结合材料和位置该棋子在终局的价值.
     */
    public int getEndgameValue() {
        return alliance.isRed() ? pieceType.endGameValues[position.getRow()][position.getCol()] : pieceType.endGameValues[Board.NUM_ROWS - position.getRow() - 1][Board.NUM_COLS - position.getCol() - 1];
    }

    /**
     * 检查该棋子是否过了河
     *
     * @return 如果该棋子越过了河对岸，则为true，否则为false.
     */
    public boolean crossedRiver() {
        return alliance.isRed() ? position.getRow() < Board.RIVER_ROW_RED : position.getRow() > Board.RIVER_ROW_BLACK;
    }

    /**
     * 该棋子棋子类型
     *
     * @return 该棋子棋子类型
     */
    public PieceType getPieceType() {
        return pieceType;
    }

    /**
     * 该棋子所属位置
     *
     * @return 该棋子所属位置
     */
    public Coordinate getPosition() {
        return position;
    }

    /**
     * 获取该棋子所属阵营（红方或黑方）
     *
     * @return 该棋子所属阵营（红方或黑方）
     */
    public Alliance getAlliance() {
        return alliance;
    }

    @Override
    public String toString() {
        return pieceType.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Piece)) {
            return false;
        }
        Piece other = (Piece) obj;
        return this.position.equals(other.position) && this.alliance.equals(other.alliance) && this.pieceType.equals(other.pieceType);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * 获取哈希码
     *
     * @return 哈希码
     */
    private int getHashCode() {
        return Objects.hash(pieceType, alliance, position);
    }

    /**
     * 棋子的类型
     */
    public enum PieceType {
        /**
         * 卒/兵
         */
        SOLDIER("S", MIDGAME_VALUES_SOLDIER, ENDGAME_VALUES_SOLDIER, 1, 3, 0, 1, 1),
        /**
         * 士/仕
         */
        ADVISOR("A", VALUES_ADVISOR, VALUES_ADVISOR, 0, 3, 0, 0, 1),
        /**
         * 象/相
         */
        ELEPHANT("E", VALUES_ELEPHANT, VALUES_ELEPHANT, 0, 3, 0, 0, 1),
        /**
         * 马
         */
        HORSE("H", MIDGAME_VALUES_HORSE, ENDGAME_VALUES_HORSE, 5, 2, 1, 2, 3),
        /**
         * 炮
         */
        CANNON("C", MIDGAME_VALUES_CANNON, ENDGAME_VALUES_CANNON, 1, 2, 1, 1, 3),
        /**
         * 车
         */
        CHARIOT("R", MIDGAME_VALUES_CHARIOT, ENDGAME_VALUES_CHARIOT, 3, 1, 2, 2, 6),
        /**
         * 将/帅
         */
        GENERAL("G", MIDGAME_VALUES_GENERAL, ENDGAME_VALUES_GENERAL, 0, 4, 0, 0, 0);

        /**
         * 简写
         */
        private final String abbrev;
        /**
         * 中局值
         */
        private final int[][] midGameValues;
        /**
         * 终局值
         */
        private final int[][] endGameValues;
        /**
         * 机动性值
         */
        private final int mobilityValue;
        /**
         * 移动的优先级
         */
        private final int movePriority;
        /**
         * 值单位
         */
        private final int valueUnits;
        /**
         * 攻击性
         */
        private final int attackUnits;
        /**
         * 简单的单位
         */
        private final int simpleUnits;

        /**
         * 构造
         *
         * @param abbrev        简写
         * @param midGameValues 中局值
         * @param endGameValues 终局值
         * @param mobilityValue 灵活性
         * @param movePriority  移动的优先级
         * @param valueUnits    值单位
         * @param attackUnits   攻击性
         * @param simpleUnits   简单的单位
         */
        PieceType(String abbrev, int[][] midGameValues, int[][] endGameValues, int mobilityValue, int movePriority, int valueUnits, int attackUnits, int simpleUnits) {
            this.abbrev = abbrev;
            this.midGameValues = midGameValues;
            this.endGameValues = endGameValues;
            this.mobilityValue = mobilityValue;
            this.movePriority = movePriority;
            this.valueUnits = valueUnits;
            this.attackUnits = attackUnits;
            this.simpleUnits = simpleUnits;
        }

        /**
         * 是否正在攻击
         *
         * @return true, 正在攻击，否则，false
         */
        public boolean isAttacking() {
            return attackUnits > 0;
        }

        /**
         * 获取机动性值
         *
         * @return 机动性值
         */
        public int getMobilityValue() {
            return mobilityValue;
        }

        /**
         * 获取移动的优先级
         *
         * @return 移动的优先级
         */
        public int getMovePriority() {
            return movePriority;
        }

        /**
         * 获取值单位
         *
         * @return 值单位
         */
        public int getValueUnits() {
            return valueUnits;
        }

        /**
         * 获取攻击性
         *
         * @return 攻击性
         */
        public int getAttackUnits() {
            return attackUnits;
        }

        /**
         * 获取简单的单位
         *
         * @return 简单的单位
         */
        public int getSimpleUnits() {
            return simpleUnits;
        }

        @Override
        public String toString() {
            return abbrev;
        }
    }

    // ---------------- 下面的2D数组表示在棋盘的所有位置上，在游戏中期和结束阶段，每个棋子的组合材料和位置值 ----------------
    /**
     * 卒/兵中局值
     */
    private static int[][] MIDGAME_VALUES_SOLDIER = {{45, 45, 45, 55, 65, 55, 45, 45, 45}, {145, 180, 255, 315, 330, 315, 255, 180, 145}, {145, 180, 240, 275, 275, 275, 240, 180, 145}, {145, 170, 200, 220, 225, 220, 200, 170, 145}, {105, 135, 150, 200, 220, 200, 150, 135, 105}, {35, 0, 65, 0, 80, 0, 65, 0, 35}, {35, 0, 35, 0, 75, 0, 35, 0, 35}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}};
    /**
     * 卒/兵终局值
     */
    private static int[][] ENDGAME_VALUES_SOLDIER = {{50, 50, 50, 75, 75, 75, 50, 50, 50}, {150, 175, 200, 325, 400, 325, 200, 175, 150}, {225, 250, 250, 275, 275, 275, 250, 250, 225}, {275, 300, 300, 310, 310, 310, 300, 300, 275}, {235, 250, 235, 260, 260, 260, 235, 250, 235}, {175, 0, 160, 0, 175, 0, 160, 0, 175}, {150, 0, 135, 0, 150, 0, 135, 0, 150}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}};
    /**
     * 士/仕值
     */
    private static int[][] VALUES_ADVISOR = {{0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 150, 0, 150, 0, 0, 0}, {0, 0, 0, 0, 165, 0, 0, 0, 0}, {0, 0, 0, 150, 0, 150, 0, 0, 0}};
    /**
     * 象/相值
     */
    private static int[][] VALUES_ELEPHANT = {{0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 150, 0, 0, 0, 150, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {140, 0, 0, 0, 165, 0, 0, 0, 140}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 150, 0, 0, 0, 150, 0, 0}};
    /**
     * 马中局值
     */
    private static int[][] MIDGAME_VALUES_HORSE = {{450, 450, 450, 480, 450, 480, 450, 450, 450}, {450, 480, 515, 485, 470, 485, 515, 480, 450}, {460, 490, 495, 515, 495, 515, 495, 490, 460}, {465, 540, 500, 535, 500, 535, 500, 540, 465}, {450, 500, 495, 515, 520, 515, 495, 500, 450}, {450, 490, 505, 510, 515, 510, 505, 490, 450}, {460, 470, 490, 475, 490, 475, 490, 470, 460}, {465, 460, 470, 475, 460, 475, 470, 460, 465}, {425, 450, 460, 465, 390, 465, 460, 450, 425}, {440, 425, 450, 440, 450, 440, 450, 425, 440}};
    /**
     * 马终局值
     */
    private static int[][] ENDGAME_VALUES_HORSE = {{460, 470, 480, 480, 480, 480, 480, 470, 460}, {470, 480, 490, 490, 490, 490, 490, 480, 470}, {480, 490, 500, 500, 500, 500, 500, 490, 480}, {480, 490, 500, 500, 500, 500, 500, 490, 480}, {480, 490, 500, 500, 500, 500, 500, 490, 480}, {470, 480, 490, 490, 490, 490, 490, 480, 470}, {470, 480, 490, 490, 490, 490, 490, 480, 470}, {460, 470, 480, 480, 480, 480, 480, 470, 460}, {450, 460, 470, 460, 460, 460, 470, 460, 450}, {440, 450, 460, 450, 450, 450, 460, 450, 440}};
    /**
     * 炮中局值
     */
    private static int[][] MIDGAME_VALUES_CANNON = {{500, 500, 480, 455, 450, 455, 480, 500, 500}, {490, 490, 480, 460, 445, 460, 480, 490, 490}, {485, 485, 480, 455, 460, 455, 480, 485, 485}, {480, 495, 495, 490, 500, 490, 495, 495, 480}, {480, 480, 480, 480, 500, 480, 480, 480, 480}, {475, 480, 495, 480, 500, 480, 495, 480, 475}, {480, 480, 480, 480, 480, 480, 480, 480, 480}, {485, 480, 500, 495, 505, 495, 500, 480, 485}, {480, 485, 490, 490, 490, 490, 490, 485, 480}, {480, 480, 485, 495, 495, 495, 485, 480, 480}};
    /**
     * 炮终局值
     */
    private static int[][] ENDGAME_VALUES_CANNON = {{500, 500, 500, 500, 500, 500, 500, 500, 500}, {500, 500, 500, 500, 500, 500, 500, 500, 500}, {500, 500, 500, 500, 500, 500, 500, 500, 500}, {500, 500, 500, 510, 520, 510, 500, 500, 500}, {500, 500, 500, 510, 520, 510, 500, 500, 500}, {500, 500, 500, 510, 520, 510, 500, 500, 500}, {500, 500, 500, 510, 520, 510, 500, 500, 500}, {500, 500, 500, 510, 520, 510, 500, 500, 500}, {500, 500, 500, 520, 530, 520, 500, 500, 500}, {500, 500, 500, 520, 530, 520, 500, 500, 500},};
    /**
     * 车中局值
     */
    private static int[][] MIDGAME_VALUES_CHARIOT = {{1030, 1040, 1035, 1065, 1070, 1065, 1035, 1040, 1030}, {1030, 1060, 1045, 1080, 1165, 1080, 1045, 1060, 1030}, {1030, 1040, 1035, 1070, 1080, 1070, 1035, 1040, 1030}, {1030, 1065, 1065, 1080, 1080, 1080, 1065, 1065, 1030}, {1040, 1055, 1055, 1070, 1075, 1070, 1055, 1055, 1040}, {1040, 1060, 1060, 1070, 1075, 1070, 1060, 1060, 1040}, {1020, 1045, 1020, 1060, 1070, 1060, 1020, 1045, 1020}, {990, 1040, 1020, 1060, 1060, 1060, 1020, 1040, 990}, {1000, 1040, 1030, 1060, 1000, 1060, 1030, 1040, 1000}, {970, 1030, 1020, 1060, 1000, 1060, 1020, 1030, 970}};
    /**
     * 车终局值
     */
    private static int[][] ENDGAME_VALUES_CHARIOT = {{910, 910, 910, 920, 930, 920, 910, 910, 910}, {920, 920, 920, 930, 950, 930, 920, 920, 920}, {910, 910, 910, 920, 930, 920, 910, 910, 910}, {900, 900, 900, 910, 920, 910, 900, 900, 900}, {900, 900, 900, 910, 920, 910, 900, 900, 900}, {900, 900, 900, 910, 920, 910, 900, 900, 900}, {900, 900, 900, 910, 920, 910, 900, 900, 900}, {900, 900, 900, 910, 920, 910, 900, 900, 900}, {900, 900, 900, 910, 920, 910, 900, 900, 900}, {900, 900, 900, 910, 920, 910, 900, 900, 900}};
    /**
     * 将/帅中局值
     */
    private static int[][] MIDGAME_VALUES_GENERAL = {{0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 5, 5, 5, 0, 0, 0}, {0, 0, 0, 10, 10, 10, 0, 0, 0}, {0, 0, 0, 55, 75, 55, 0, 0, 0}};
    /**
     * 将/帅终局值
     */
    private static int[][] ENDGAME_VALUES_GENERAL = {{0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 25, 70, 25, 0, 0, 0}, {0, 0, 0, 15, 60, 15, 0, 0, 0}, {0, 0, 0, 5, 55, 5, 0, 0, 0}};

    /**
     * 关系
     */
    public static abstract class Relation {

        /**
         * 棋子
         */
        final Piece piece;
        /**
         * 相关棋子
         */
        final Collection<Piece> relatedPieces;

        /**
         * 构造
         *
         * @param piece         棋子
         * @param relatedPieces 相关棋子
         */
        private Relation(Piece piece, Collection<Piece> relatedPieces) {
            this.piece = piece;
            this.relatedPieces = relatedPieces;
        }

        /**
         * 获取棋子
         *
         * @return 棋子
         */
        public Piece getPiece() {
            return piece;
        }

        /**
         * 获取相关棋子
         *
         * @return 相关棋子
         */
        public Collection<Piece> getRelatedPieces() {
            return relatedPieces;
        }
    }

    /**
     * 棋子的攻击
     */
    public static class Attack extends Relation {
        /**
         * 构造
         *
         * @param attackingPiece 进攻性棋子
         * @param attackedPieces 被攻击的棋子
         */
        public Attack(Piece attackingPiece, Collection<Piece> attackedPieces) {
            super(attackingPiece, attackedPieces);
        }
    }

    /**
     * 棋子的防御
     */
    public static class Defense extends Relation {
        /**
         * 构造
         *
         * @param defendingPiece 防护性棋子
         * @param defendedPieces 被防护的棋子
         */
        public Defense(Piece defendingPiece, Collection<Piece> defendedPieces) {
            super(defendingPiece, defendedPieces);
        }
    }
}
