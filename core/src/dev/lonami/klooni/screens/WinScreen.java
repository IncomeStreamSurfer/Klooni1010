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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.actors.SoftButton;

/**
 * Win/Results screen shown after casino mode game ends.
 * Displays score, multiplier, winnings with flashy animations.
 */
public class WinScreen implements Screen {

    //region Members

    private final Klooni game;
    private final Stage stage;
    private final SpriteBatch batch;

    private final int finalScore;
    private final int betAmount;
    private final float multiplier;
    private final int winnings;
    private final int netProfit;
    private final boolean isBigWin;
    private final boolean isJackpot;

    // Animation
    private float animTimer;
    private final Array<CoinParticle> particles;

    // Labels for animation
    private final Label titleLabel;
    private final Label scoreLabel;
    private final Label multiplierLabel;
    private final Label winningsLabel;
    private final Label profitLabel;

    //endregion

    //region Inner class for coin particles

    private static class CoinParticle {
        float x, y;
        float velocityX, velocityY;
        float rotation;
        float rotationSpeed;
        float size;
        float lifetime;
        float elapsed;
        Color color;

        CoinParticle(float startX, float startY) {
            this.x = startX;
            this.y = startY;
            this.velocityX = MathUtils.random(-200f, 200f);
            this.velocityY = MathUtils.random(150f, 350f);
            this.rotation = MathUtils.random(360f);
            this.rotationSpeed = MathUtils.random(-360f, 360f);
            this.size = MathUtils.random(15f, 30f);
            this.lifetime = MathUtils.random(2f, 3.5f);
            this.elapsed = 0f;
            this.color = new Color(1f, 0.84f, 0f, 1f); // Gold
        }

        void update(float delta) {
            elapsed += delta;
            velocityY -= 400f * delta; // Gravity
            x += velocityX * delta;
            y += velocityY * delta;
            rotation += rotationSpeed * delta;
        }

        boolean isDone() {
            return elapsed >= lifetime;
        }

        float getAlpha() {
            return 1f - (elapsed / lifetime);
        }
    }

    //endregion

    //region Constructor

    public WinScreen(final Klooni game, int finalScore, int betAmount, float multiplier) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.stage = new Stage();
        this.particles = new Array<CoinParticle>();

        this.finalScore = finalScore;
        this.betAmount = betAmount;
        this.multiplier = multiplier;
        this.winnings = (int)(betAmount * multiplier);
        this.netProfit = winnings - betAmount;
        this.isBigWin = multiplier >= 3.0f;
        this.isJackpot = multiplier >= 10.0f;

        // Add winnings to player's money
        Klooni.addWinnings(winnings);

        // Label styles
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = game.skin.getFont("font");

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = game.skin.getFont("font_large");

        // Main table
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // Title based on result
        String titleText;
        Color titleColor;
        if (isJackpot) {
            titleText = "JACKPOT!!!";
            titleColor = new Color(1f, 0.84f, 0f, 1f); // Gold
        } else if (isBigWin) {
            titleText = "BIG WIN!";
            titleColor = new Color(0f, 1f, 0f, 1f); // Neon green
        } else if (multiplier > 1.0f) {
            titleText = "YOU WIN!";
            titleColor = new Color(0f, 1f, 0f, 1f);
        } else {
            titleText = "GAME OVER";
            titleColor = new Color(1f, 0.08f, 0.58f, 1f); // Hot pink
        }

        titleLabel = new Label(titleText, titleStyle);
        titleLabel.setColor(titleColor);
        mainTable.add(titleLabel).padBottom(30);
        mainTable.row();

        // Final score
        scoreLabel = new Label("Final Score: " + finalScore, labelStyle);
        scoreLabel.setColor(Color.WHITE);
        mainTable.add(scoreLabel).padBottom(15);
        mainTable.row();

        // Multiplier achieved
        // GWT doesn't support String.format, so we format manually
        int wholePart = (int) multiplier;
        int decimalPart = (int) ((multiplier - wholePart) * 10);
        String multiplierText = "Multiplier: " + wholePart + "." + decimalPart + "x";
        multiplierLabel = new Label(multiplierText, labelStyle);
        if (multiplier > 1.0f) {
            multiplierLabel.setColor(new Color(0f, 1f, 1f, 1f)); // Cyan
        } else {
            multiplierLabel.setColor(Color.GRAY);
        }
        mainTable.add(multiplierLabel).padBottom(30);
        mainTable.row();

        // Divider line (using dashes)
        Label divider = new Label("------------------------", labelStyle);
        divider.setColor(new Color(0.5f, 0.5f, 0.5f, 1f));
        mainTable.add(divider).padBottom(15);
        mainTable.row();

        // Bet amount
        Label betLabel = new Label("Bet: $" + betAmount, labelStyle);
        betLabel.setColor(Color.WHITE);
        mainTable.add(betLabel).padBottom(10);
        mainTable.row();

        // Winnings
        winningsLabel = new Label("Winnings: $" + winnings, labelStyle);
        winningsLabel.setColor(new Color(1f, 0.84f, 0f, 1f)); // Gold
        mainTable.add(winningsLabel).padBottom(10);
        mainTable.row();

        // Net profit/loss
        String profitText;
        Color profitColor;
        if (netProfit > 0) {
            profitText = "Profit: +$" + netProfit;
            profitColor = new Color(0f, 1f, 0f, 1f); // Green
        } else if (netProfit < 0) {
            profitText = "Loss: -$" + Math.abs(netProfit);
            profitColor = Color.RED;
        } else {
            profitText = "Break Even: $0";
            profitColor = Color.WHITE;
        }
        profitLabel = new Label(profitText, labelStyle);
        profitLabel.setColor(profitColor);
        mainTable.add(profitLabel).padBottom(15);
        mainTable.row();

        // Another divider
        Label divider2 = new Label("------------------------", labelStyle);
        divider2.setColor(new Color(0.5f, 0.5f, 0.5f, 1f));
        mainTable.add(divider2).padBottom(15);
        mainTable.row();

        // New balance
        Label newBalanceLabel = new Label("New Balance: $" + Klooni.getMoney(), labelStyle);
        newBalanceLabel.setColor(new Color(1f, 0.84f, 0f, 1f));
        mainTable.add(newBalanceLabel).padBottom(40);
        mainTable.row();

        // Buttons
        Table buttonTable = new Table();

        // Play again button
        final SoftButton playAgainButton = new SoftButton(0, "replay_texture");
        playAgainButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                WinScreen.this.game.transitionTo(new BetScreen(WinScreen.this.game));
            }
        });
        buttonTable.add(playAgainButton).pad(16);

        // Cash out (main menu) button
        final SoftButton cashOutButton = new SoftButton(1, "home_texture");
        cashOutButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                WinScreen.this.game.transitionTo(new MainMenuScreen(WinScreen.this.game));
            }
        });
        buttonTable.add(cashOutButton).pad(16);

        mainTable.add(buttonTable);

        // Spawn initial coin particles for big wins
        if (isBigWin) {
            spawnCoinBurst();
        }
    }

    //endregion

    //region Private methods

    private void spawnCoinBurst() {
        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;

        int particleCount = isJackpot ? 50 : 25;
        for (int i = 0; i < particleCount; i++) {
            particles.add(new CoinParticle(
                centerX + MathUtils.random(-100f, 100f),
                centerY + MathUtils.random(-50f, 50f)
            ));
        }
    }

    private void updateParticles(float delta) {
        // Update existing particles
        for (int i = particles.size - 1; i >= 0; i--) {
            CoinParticle p = particles.get(i);
            p.update(delta);
            if (p.isDone()) {
                particles.removeIndex(i);
            }
        }

        // Continuously spawn more particles for big wins
        if (isBigWin && particles.size < 30) {
            float chance = isJackpot ? 0.3f : 0.1f;
            if (MathUtils.random() < chance) {
                particles.add(new CoinParticle(
                    MathUtils.random(0f, Gdx.graphics.getWidth()),
                    Gdx.graphics.getHeight() + 20f
                ));
            }
        }
    }

    private void drawParticles() {
        // Draw coin particles as colored rectangles (simple representation)
        batch.begin();
        for (CoinParticle p : particles) {
            float alpha = p.getAlpha();
            batch.setColor(p.color.r, p.color.g, p.color.b, alpha);
            // Draw a simple gold "coin" using the batch's draw capabilities
            // Since we don't have a specific coin texture, we'll skip detailed drawing
            // The effect is handled by the color flash below
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    //endregion

    //region Screen

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        animTimer += delta;

        // Background color with occasional flash for big wins
        float bgR = 0.1f;
        float bgG = 0.04f;
        float bgB = 0.18f;

        if (isBigWin) {
            float flash = (float)(Math.sin(animTimer * 8f) + 1f) / 2f * 0.1f;
            bgR += flash;
            bgB += flash * 0.5f;
        }

        Gdx.gl.glClearColor(bgR, bgG, bgB, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update particles
        updateParticles(delta);
        drawParticles();

        // Animate labels
        if (isBigWin) {
            float pulse = (float)(Math.sin(animTimer * 4f) + 1f) / 2f;
            titleLabel.setColor(
                titleLabel.getColor().r,
                titleLabel.getColor().g,
                titleLabel.getColor().b + pulse * 0.3f,
                1f
            );

            float scale = 1f + pulse * 0.1f;
            titleLabel.setFontScale(scale);
        }

        // Pulsing gold effect on winnings
        if (multiplier > 1.0f) {
            float glow = (float)(Math.sin(animTimer * 3f) + 1f) / 2f;
            winningsLabel.setColor(1f, 0.84f + glow * 0.16f, glow * 0.3f, 1f);
        }

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
