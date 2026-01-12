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
package dev.lonami.klooni.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.serializer.BinSerializable;

/**
 * Casino mode scorer that tracks bets and calculates winnings based on multipliers.
 */
public class BetScorer extends BaseScorer implements BinSerializable {

    //region Members

    private int betAmount;
    private float currentMultiplier;
    private int lastMultiplierTier;

    // Listener for multiplier changes
    private MultiplierListener multiplierListener;

    // Neon glow effect for score display
    private float glowTimer;
    private final Color glowColor;

    //endregion

    //region Interfaces

    public interface MultiplierListener {
        void onMultiplierChanged(float newMultiplier, int tier);
    }

    //endregion

    //region Constructor

    public BetScorer(final Klooni game, GameLayout layout, int betAmount) {
        super(game, layout, 0); // No high score tracking for casino mode
        this.betAmount = betAmount;
        this.currentMultiplier = 1.0f;
        this.lastMultiplierTier = -1;
        this.glowColor = new Color(1f, 0.84f, 0f, 1f); // Gold color

        // Update high score label to show bet amount
        highScoreLabel.setText("BET: " + betAmount);
    }

    //endregion

    //region Public methods

    public void setMultiplierListener(MultiplierListener listener) {
        this.multiplierListener = listener;
    }

    @Override
    public void addPieceScore(final int areaPut) {
        super.addPieceScore(areaPut);
        updateMultiplier();
    }

    @Override
    public int addBoardScore(int stripsCleared, int boardSize) {
        int score = super.addBoardScore(stripsCleared, boardSize);
        updateMultiplier();
        return score;
    }

    private void updateMultiplier() {
        int newTier = Klooni.getMultiplierTier(currentScore);
        if (newTier > lastMultiplierTier) {
            lastMultiplierTier = newTier;
            currentMultiplier = Klooni.getMultiplierForTier(newTier);

            if (multiplierListener != null) {
                multiplierListener.onMultiplierChanged(currentMultiplier, newTier);
            }
        }
    }

    public int getBetAmount() {
        return betAmount;
    }

    public float getCurrentMultiplier() {
        return currentMultiplier;
    }

    public int getMultiplierTier() {
        return lastMultiplierTier;
    }

    public int calculateWinnings() {
        return (int)(betAmount * currentMultiplier);
    }

    public boolean isWinning() {
        return currentMultiplier > 1.0f;
    }

    public int getNetProfit() {
        return calculateWinnings() - betAmount;
    }

    @Override
    public boolean isGameOver() {
        return false; // Casino mode doesn't have time-based game over
    }

    @Override
    protected boolean isNewRecord() {
        return false; // No records in casino mode
    }

    @Override
    public void saveScore() {
        // Winnings are handled separately in WinScreen
    }

    @Override
    public void draw(SpriteBatch batch) {
        // Add pulsing glow effect to score when winning
        glowTimer += 0.05f;

        if (isWinning()) {
            float glow = (float)(Math.sin(glowTimer * 3f) + 1f) / 2f;
            glowColor.r = 1f;
            glowColor.g = 0.84f + glow * 0.16f;
            glowColor.b = glow * 0.3f;
            currentScoreLabel.setColor(glowColor);
        }

        super.draw(batch);

        // Draw multiplier indicator
        if (currentMultiplier > 1.0f) {
            // GWT doesn't support String.format, so we format manually
            int wholePart = (int) currentMultiplier;
            int decimalPart = (int) ((currentMultiplier - wholePart) * 10);
            highScoreLabel.setText(wholePart + "." + decimalPart + "x");
            highScoreLabel.setColor(glowColor);
        }
    }

    //endregion

    //region Serialization

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(currentScore);
        out.writeInt(betAmount);
        out.writeFloat(currentMultiplier);
        out.writeInt(lastMultiplierTier);
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        currentScore = in.readInt();
        betAmount = in.readInt();
        currentMultiplier = in.readFloat();
        lastMultiplierTier = in.readInt();
    }

    //endregion
}
