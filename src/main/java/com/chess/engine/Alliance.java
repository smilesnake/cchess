package com.chess.engine;

/**
 * 所属阵营（红方或黑方），代表游戏中玩家一方，
 */
public enum Alliance {
    /**
     * 红方
     */
    RED {
        @Override
        public boolean isRed() {
            return true;
        }

        @Override
        public int getDirection() {
            return -1;
        }

        @Override
        public Alliance opposite() {
            return BLACK;
        }
    },
    /**
     * 黑方
     */
    BLACK {
        @Override
        public boolean isRed() {
            return false;
        }

        @Override
        public int getDirection() {
            return 1;
        }

        @Override
        public Alliance opposite() {
            return RED;
        }
    };

    /**
     * 是否为红方
     *
     * @return true, 红方, 否则，false
     */
    public abstract boolean isRed();

    /**
     * 方向
     *
     * @return 红方，-1，黑方，1
     */
    public abstract int getDirection();

    /**
     * 对手
     *
     * @return 对手所属阵营（红方或黑方）
     */
    public abstract Alliance opposite();
}
