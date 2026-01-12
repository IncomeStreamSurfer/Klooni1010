/*
    1010! Klooni, a free customizable puzzle game for Android and Desktop
    Copyright (C) 2017-2019  Lonami Exo @ lonami.dev

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package dev.lonami.klooni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.actors.SoftButton;

/**
 * Betting screen where players select their bet amount before playing casino mode.
 */
public class BetScreen implements Screen {

    //region Members

    private final Klooni game;
    private final Stage stage;
    private final SpriteBatch batch;

    private int selectedBet;
    private final Label balanceLabel;
    private final Label selectedBetLabel;
    private final Label multiplierInfoLabel;
    private final TextButton[] betButtons;

    // Neon glow animation
    private float glowTimer;

    //endregion

    //region Constructor

    public BetScreen(final Klooni game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.stage = new Stage();
        this.selectedBet = Klooni.BET_AMOUNTS[0]; // Default to lowest bet
        this.betButtons = new TextButton[Klooni.BET_AMOUNTS.length];

        // Get label style from skin
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = game.skin.getFont("font");

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = game.skin.getFont("font_large");

        // Create main table
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // Title
        Label titleLabel = new Label("CASINO MODE", titleStyle);
        titleLabel.setColor(new Color(1f, 0.08f, 0.58f, 1f)); // Hot pink
        mainTable.add(titleLabel).padBottom(20).colspan(3);
        mainTable.row();

        // Balance display
        balanceLabel = new Label("Balance: $" + Klooni.getMoney(), labelStyle);
        balanceLabel.setColor(new Color(1f, 0.84f, 0f, 1f)); // Gold
        mainTable.add(balanceLabel).padBottom(30).colspan(3);
        mainTable.row();

        // Bet selection label
        Label selectLabel = new Label("SELECT YOUR BET", labelStyle);
        selectLabel.setColor(Color.WHITE);
        mainTable.add(selectLabel).padBottom(15).colspan(3);
        mainTable.row();

        // Bet amount buttons - 2 rows of 3
        Table betTable = new Table();
        for (int i = 0; i < Klooni.BET_AMOUNTS.length; i++) {
            final int betAmount = Klooni.BET_AMOUNTS[i];
            final int buttonIndex = i;

            TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
            btnStyle.font = game.skin.getFont("font");
            btnStyle.fontColor = Color.WHITE;

            TextButton betBtn = new TextButton("$" + betAmount, btnStyle);
            betBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    selectBet(betAmount, buttonIndex);
                }
            });
            betButtons[i] = betBtn;
            betTable.add(betBtn).pad(8).width(100).height(60);

            if ((i + 1) % 3 == 0) {
                betTable.row();
            }
        }
        mainTable.add(betTable).colspan(3).padBottom(20);
        mainTable.row();

        // Selected bet display
        selectedBetLabel = new Label("Selected: $" + selectedBet, labelStyle);
        selectedBetLabel.setColor(new Color(0f, 1f, 0f, 1f)); // Neon green
        mainTable.add(selectedBetLabel).padBottom(30).colspan(3);
        mainTable.row();

        // Multiplier info
        Label multiplierTitle = new Label("-- MULTIPLIER TIERS --", labelStyle);
        multiplierTitle.setColor(new Color(0f, 0.75f, 1f, 1f)); // Electric blue
        mainTable.add(multiplierTitle).padBottom(10).colspan(3);
        mainTable.row();

        // Build multiplier info text
        StringBuilder multiplierInfo = new StringBuilder();
        for (int i = 0; i < Klooni.MULTIPLIER_THRESHOLDS.length; i++) {
            String tierText = Klooni.MULTIPLIER_THRESHOLDS[i] + " pts = " + Klooni.MULTIPLIERS[i] + "x";
            if (i == Klooni.MULTIPLIER_THRESHOLDS.length - 1) {
                tierText += " JACKPOT!";
            }
            multiplierInfo.append(tierText);
            if (i < Klooni.MULTIPLIER_THRESHOLDS.length - 1) {
                multiplierInfo.append("\n");
            }
        }
        multiplierInfoLabel = new Label(multiplierInfo.toString(), labelStyle);
        multiplierInfoLabel.setColor(Color.WHITE);
        multiplierInfoLabel.setAlignment(Align.center);
        mainTable.add(multiplierInfoLabel).padBottom(30).colspan(3);
        mainTable.row();

        // Bottom buttons
        Table buttonTable = new Table();

        // Back button
        final SoftButton backButton = new SoftButton(3, "back_texture");
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                BetScreen.this.game.transitionTo(new MainMenuScreen(BetScreen.this.game));
            }
        });
        buttonTable.add(backButton).pad(16);

        // Place bet button
        final SoftButton placeBetButton = new SoftButton(0, "play_texture");
        placeBetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                placeBet();
            }
        });
        buttonTable.add(placeBetButton).pad(16);

        mainTable.add(buttonTable).colspan(3);

        // Highlight first bet button as selected
        updateBetButtonStyles();
    }

    //endregion

    //region Private methods

    private void selectBet(int betAmount, int buttonIndex) {
        if (Klooni.canAffordBet(betAmount)) {
            selectedBet = betAmount;
            selectedBetLabel.setText("Selected: $" + selectedBet);
            updateBetButtonStyles();
        } else {
            selectedBetLabel.setText("Not enough money!");
            selectedBetLabel.setColor(Color.RED);
        }
    }

    private void updateBetButtonStyles() {
        for (int i = 0; i < betButtons.length; i++) {
            int betAmount = Klooni.BET_AMOUNTS[i];
            TextButton btn = betButtons[i];

            if (betAmount == selectedBet) {
                // Selected - bright neon green
                btn.getLabel().setColor(new Color(0f, 1f, 0f, 1f));
            } else if (Klooni.canAffordBet(betAmount)) {
                // Affordable - white
                btn.getLabel().setColor(Color.WHITE);
            } else {
                // Can't afford - gray
                btn.getLabel().setColor(Color.GRAY);
            }
        }
        selectedBetLabel.setColor(new Color(0f, 1f, 0f, 1f));
    }

    private void placeBet() {
        if (Klooni.placeBet(selectedBet)) {
            // Transition to game screen in casino mode
            game.transitionTo(new GameScreen(game, Klooni.GAME_MODE_CASINO, false, selectedBet));
        } else {
            selectedBetLabel.setText("Not enough money!");
            selectedBetLabel.setColor(Color.RED);
        }
    }

    //endregion

    //region Screen

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        // Update balance in case it changed
        balanceLabel.setText("Balance: $" + Klooni.getMoney());
        updateBetButtonStyles();
    }

    @Override
    public void render(float delta) {
        // Vegas-style dark purple background
        Gdx.gl.glClearColor(0.1f, 0.04f, 0.18f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Animate glow effect on title and multiplier labels
        glowTimer += delta;
        float glow = (float)(Math.sin(glowTimer * 2f) + 1f) / 2f;

        // Update balance color with glow
        balanceLabel.setColor(1f, 0.84f + glow * 0.16f, glow * 0.3f, 1f);

        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.transitionTo(new MainMenuScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    //endregion
}
