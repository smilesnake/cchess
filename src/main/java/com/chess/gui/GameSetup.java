package com.chess.gui;

import com.chess.engine.Alliance;
import com.chess.gui.Table.PlayerType;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;

import static com.chess.gui.Table.AIType;
import static javafx.scene.control.Alert.AlertType;
import static javafx.scene.control.ButtonBar.ButtonData;

/**
 * 游戏设置对话框
 */
class GameSetup extends Dialog {
    /**
     * 人类文本
     */
    private static final String HUMAN_TEXT = "人类";
    /**
     * AI文本
     */
    private static final String AI_TEXT = "AI";
    /**
     * 固定的深度文本
     */
    private static final String FIXED_DEPTH_TEXT = "深度(级别)";
    /**
     * 固定的时间文本
     */
    private static final String FIXED_TIME_TEXT = "时间(秒)";
    /**
     * 最小深度
     */
    private static final int MIN_DEPTH = 1;
    /**
     * 最大尝试
     */
    private static final int MAX_DEPTH = 12;
    /**
     * 最小时间
     */
    private static final int MIN_TIME = 1;
    /**
     * 最大时间
     */
    private static final int MAX_TIME = 180;
    /**
     * 游戏设置实例
     */
    private static final GameSetup SETUP = new GameSetup();

    /**
     * 红色玩家类型
     */
    private PlayerType redPlayerType;
    /**
     * 黑色玩家类型
     */
    private PlayerType blackPlayerType;
    /**
     * AI玩家类型
     */
    private AIType aiType;
    /**
     * 检索深度
     */
    private int searchDepth;
    /**
     * 检索时间
     */
    private int searchTime;
    /**
     * 当前AI的移动是否是随机的，true，随机，否则，false
     */
    private boolean isAIRandomised;

    private GameSetup() {
        // 默认设置
        redPlayerType = PlayerType.HUMAN;
        blackPlayerType = PlayerType.HUMAN;
        aiType = AIType.DEPTH;
        searchDepth = 5;
        searchTime = 10;
        isAIRandomised = false;

        // 对话框面板
        DialogPane dialogPane = new DialogPane();
        // 网格面板
        GridPane gridPane = new GridPane();
        // 节点列表
        List<Node> nodes = new ArrayList<>();

        Label redHeader = GuiUtil.getHeader("RED player");
        Label blackHeader = GuiUtil.getHeader("BLACK player");
        Label aiHeader = GuiUtil.getHeader("AI settings");

        RadioButton redHumanButton = new RadioButton(HUMAN_TEXT);
        RadioButton redAIButton = new RadioButton(AI_TEXT);
        ToggleGroup redGroup = new ToggleGroup();
        redHumanButton.setToggleGroup(redGroup);
        redAIButton.setToggleGroup(redGroup);
        redHumanButton.setSelected(true);

        RadioButton blackHumanButton = new RadioButton(HUMAN_TEXT);
        RadioButton blackAIButton = new RadioButton(AI_TEXT);
        ToggleGroup blackGroup = new ToggleGroup();
        blackHumanButton.setToggleGroup(blackGroup);
        blackAIButton.setToggleGroup(blackGroup);
        blackHumanButton.setSelected(true);

        RadioButton fixedDepthAIButton = new RadioButton(FIXED_DEPTH_TEXT);
        RadioButton fixedTimeAIButton = new RadioButton(FIXED_TIME_TEXT);
        ToggleGroup aiGroup = new ToggleGroup();
        fixedDepthAIButton.setToggleGroup(aiGroup);
        fixedTimeAIButton.setToggleGroup(aiGroup);
        fixedDepthAIButton.setSelected(true);

        Spinner searchDepthSpinner = new Spinner(MIN_DEPTH, MAX_DEPTH, searchDepth, 1);
        searchDepthSpinner.setEditable(true);
        Spinner searchTimeSpinner = new Spinner(MIN_TIME, MAX_TIME, searchTime, 10);
        searchTimeSpinner.setEditable(true);

        CheckBox randomiseAICheckbox = new CheckBox("AI的移动是否随机");
        randomiseAICheckbox.setAllowIndeterminate(false);
        randomiseAICheckbox.setSelected(isAIRandomised);

        ButtonType cancel = new ButtonType("取消", ButtonData.CANCEL_CLOSE);
        ButtonType ok = new ButtonType("确定", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(ok, cancel);
        Button cancelButton = (Button) dialogPane.lookupButton(cancel);
        cancelButton.setOnAction(e -> {
            if (redPlayerType.isAI()) {
                redAIButton.setSelected(true);
            } else {
                redHumanButton.setSelected(true);
            }
            if (blackPlayerType.isAI()) {
                blackAIButton.setSelected(true);
            } else {
                blackHumanButton.setSelected(true);
            }
            if (isAITimeLimited()) {
                fixedTimeAIButton.setSelected(true);
            } else {
                fixedDepthAIButton.setSelected(true);
            }
            searchDepthSpinner.getEditor().textProperty().set(Integer.toString(searchDepth));
            searchTimeSpinner.getEditor().textProperty().set(Integer.toString(searchTime));
            randomiseAICheckbox.setSelected(isAIRandomised);
        });
        Button okButton = (Button) dialogPane.lookupButton(ok);
        okButton.setOnAction(e -> {
            redPlayerType = redAIButton.isSelected() ? PlayerType.AI : PlayerType.HUMAN;
            blackPlayerType = blackAIButton.isSelected() ? PlayerType.AI : PlayerType.HUMAN;
            aiType = fixedTimeAIButton.isSelected() ? AIType.TIME : AIType.DEPTH;
            try {
                Integer.parseInt(searchDepthSpinner.getEditor().textProperty().get());
                searchDepth = (int) searchDepthSpinner.getValue();
            } catch (NumberFormatException nfe) {
                Alert alert = new Alert(AlertType.ERROR, "Depth must be an integer from 2 to 8");
                alert.setTitle("设置");
                alert.showAndWait();
                searchDepthSpinner.getEditor().textProperty().set(Integer.toString(searchDepth));
            }
            try {
                Integer.parseInt(searchTimeSpinner.getEditor().textProperty().get());
                searchTime = (int) searchTimeSpinner.getValue();
            } catch (NumberFormatException nfe) {
                Alert alert = new Alert(AlertType.ERROR, "Time must be an integer from 1 to 180");
                alert.setTitle("设置");
                alert.showAndWait();
                searchTimeSpinner.getEditor().textProperty().set(Integer.toString(searchTime));
            }
            isAIRandomised = randomiseAICheckbox.isSelected();

            hide();
        });

        nodes.add(redHeader);
        nodes.add(redHumanButton);
        nodes.add(redAIButton);
        nodes.add(blackHeader);
        nodes.add(blackHumanButton);
        nodes.add(blackAIButton);
        nodes.add(GuiUtil.getSeparator());
        nodes.add(aiHeader);
        nodes.add(fixedDepthAIButton);
        nodes.add(searchDepthSpinner);
        nodes.add(fixedTimeAIButton);
        nodes.add(searchTimeSpinner);
        nodes.add(randomiseAICheckbox);

        for (int i = 0; i < nodes.size(); i++) {
            gridPane.add(nodes.get(i), 0, i);
        }
        dialogPane.setContent(gridPane);

        setTitle("设置");
        setDialogPane(dialogPane);
        setResizable(false);
        hide();
    }

    static GameSetup getInstance() {
        return SETUP;
    }

    boolean isAIPlayer(Alliance alliance) {
        return alliance.isRed() ? redPlayerType.isAI() : blackPlayerType.isAI();
    }

    boolean isAITimeLimited() {
        return aiType.isTimeLimited();
    }

    boolean isAIRandomised() {
        return isAIRandomised;
    }

    int getSearchDepth() {
        return searchDepth;
    }

    int getSearchTime() {
        return searchTime;
    }
}
