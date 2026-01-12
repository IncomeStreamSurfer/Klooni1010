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
package dev.lonami.klooni;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.util.HashMap;
import java.util.Map;

import dev.lonami.klooni.effects.EvaporateEffectFactory;
import dev.lonami.klooni.effects.ExplodeEffectFactory;
import dev.lonami.klooni.effects.SpinEffectFactory;
import dev.lonami.klooni.effects.VanishEffectFactory;
import dev.lonami.klooni.effects.WaterdropEffectFactory;
import dev.lonami.klooni.interfaces.IEffectFactory;
import dev.lonami.klooni.screens.MainMenuScreen;
import dev.lonami.klooni.screens.TransitionScreen;

public class Klooni extends Game {

    //region Members

    // FIXME theme should NOT be static as it might load textures which will expose it to the race condition iff GDX got initialized before or not
    public static Theme theme;
    public IEffectFactory effect;

    // ordered list of effects. index 0 will get default if VanishEffectFactory is removed from list
    public final static IEffectFactory[] EFFECTS = {
            new VanishEffectFactory(),
            new WaterdropEffectFactory(),
            new EvaporateEffectFactory(),
            new SpinEffectFactory(),
            new ExplodeEffectFactory(),
    };

    private Map<String, Sound> effectSounds;
    public Skin skin;

    public final ShareChallenge shareChallenge;

    public static boolean onDesktop;

    private final static float SCORE_TO_MONEY = 1f / 100f;

    public static final int GAME_HEIGHT = 680;
    public static final int GAME_WIDTH = 408;

    // Casino mode constants
    public static final int GAME_MODE_SCORE = 0;
    public static final int GAME_MODE_TIME = 1;
    public static final int GAME_MODE_CASINO = 2;

    // Bet amounts available
    public static final int[] BET_AMOUNTS = {10, 25, 50, 100, 250, 500};

    // Multiplier tiers (score thresholds and their multipliers)
    public static final int[] MULTIPLIER_THRESHOLDS = {100, 200, 300, 500, 1000};
    public static final float[] MULTIPLIERS = {1.5f, 2.0f, 3.0f, 5.0f, 10.0f};

    //endregion

    //region Creation

    // TODO Possibly implement a 'ShareChallenge'
    //      for other platforms instead passing null
    public Klooni(final ShareChallenge shareChallenge) {
        this.shareChallenge = shareChallenge;
    }

    @Override
    public void create() {
        onDesktop = Gdx.app.getType().equals(Application.ApplicationType.Desktop);
        prefs = Gdx.app.getPreferences("dev.lonami.klooni.game");

        // Load the best match for the skin (depending on the device screen dimensions)
        skin = SkinLoader.loadSkin();

        // Use only one instance for the theme, so anyone using it uses the most up-to-date
        Theme.skin = skin; // Not the best idea
        final String themeName = prefs.getString("themeName", "default");
        if (Theme.exists(themeName))
            theme = Theme.getTheme(themeName);
        else
            theme = Theme.getTheme("default");

        Gdx.input.setCatchBackKey(true); // To show the pause menu
        setScreen(new MainMenuScreen(this));
        String effectName = prefs.getString("effectName", "vanish");
        effectSounds = new HashMap<String, Sound>(EFFECTS.length);
        effect = EFFECTS[0];
        for (IEffectFactory e : EFFECTS) {
            loadEffectSound(e.getName());
            if (e.getName().equals(effectName)) {
                effect = e;
            }
        }
    }

    //endregion

    //region Screen

    // TransitionScreen will also dispose by default the previous screen
    public void transitionTo(Screen screen) {
        transitionTo(screen, true);
    }

    public void transitionTo(Screen screen, boolean disposeAfter) {
        setScreen(new TransitionScreen(this, getScreen(), screen, disposeAfter));
    }

    //endregion

    //region Disposing

    @Override
    public void dispose() {
        super.dispose();
        skin.dispose();
        theme.dispose();
        if (effectSounds != null) {
            for (Sound s : effectSounds.values()) {
                s.dispose();
            }
            effectSounds = null;
        }
    }

    //endregion

    // region Effects

    private void loadEffectSound(final String effectName) {
        FileHandle soundFile = Gdx.files.internal("sound/effect_" + effectName + ".mp3");
        if (!soundFile.exists())
            soundFile = Gdx.files.internal("sound/effect_vanish.mp3");

        effectSounds.put(effectName, Gdx.audio.newSound(soundFile));
    }

    public void playEffectSound() {
        effectSounds.get(effect.getName())
                .play(MathUtils.random(0.7f, 1f), MathUtils.random(0.8f, 1.2f), 0);
    }

    // endregion

    //region Settings

    private static Preferences prefs;

    // Score related
    public static int getMaxScore() {
        return prefs.getInteger("maxScore", 0);
    }

    public static int getMaxTimeScore() {
        return prefs.getInteger("maxTimeScore", 0);
    }

    public static void setMaxScore(int score) {
        prefs.putInteger("maxScore", score).flush();
    }

    public static void setMaxTimeScore(int maxTimeScore) {
        prefs.putInteger("maxTimeScore", maxTimeScore).flush();
    }

    // Settings related
    public static boolean soundsEnabled() {
        return !prefs.getBoolean("muteSound", false);
    }

    public static boolean toggleSound() {
        final boolean result = soundsEnabled();
        prefs.putBoolean("muteSound", result).flush();
        return !result;
    }

    public static boolean shouldSnapToGrid() {
        return prefs.getBoolean("snapToGrid", false);
    }

    public static boolean toggleSnapToGrid() {
        final boolean result = !shouldSnapToGrid();
        prefs.putBoolean("snapToGrid", result).flush();
        return result;
    }

    // Themes related
    public static boolean isThemeBought(Theme theme) {
        if (theme.getPrice() == 0)
            return true;

        String[] themes = prefs.getString("boughtThemes", "").split(":");
        for (String t : themes)
            if (t.equals(theme.getName()))
                return true;

        return false;
    }

    public static boolean buyTheme(Theme theme) {
        final float money = getRealMoney();
        if (theme.getPrice() > money)
            return false;

        setMoney(money - theme.getPrice());

        String bought = prefs.getString("boughtThemes", "");
        if (bought.equals(""))
            bought = theme.getName();
        else
            bought += ":" + theme.getName();

        prefs.putString("boughtThemes", bought);

        return true;
    }

    public static void updateTheme(Theme newTheme) {
        prefs.putString("themeName", newTheme.getName()).flush();
        theme.update(newTheme.getName());
    }

    // Effects related
    public static boolean isEffectBought(IEffectFactory effect) {
        if (effect.getPrice() == 0)
            return true;

        String[] effects = prefs.getString("boughtEffects", "").split(":");
        for (String e : effects)
            if (e.equals(effect.getName()))
                return true;

        return false;
    }

    public static boolean buyEffect(IEffectFactory effect) {
        final float money = getRealMoney();
        if (effect.getPrice() > money)
            return false;

        setMoney(money - effect.getPrice());

        String bought = prefs.getString("boughtEffects", "");
        if (bought.equals(""))
            bought = effect.getName();
        else
            bought += ":" + effect.getName();

        prefs.putString("boughtEffects", bought);

        return true;
    }

    public void updateEffect(IEffectFactory newEffect) {
        prefs.putString("effectName", newEffect.getName()).flush();
        // Create a new effect, since the one passed through the parameter may dispose later
        effect = newEffect;
    }

    // Money related
    public static void addMoneyFromScore(int score) {
        setMoney(getRealMoney() + score * SCORE_TO_MONEY);
    }

    private static void setMoney(float money) {
        prefs.putFloat("money", money).flush();
    }

    public static int getMoney() {
        return (int) getRealMoney();
    }

    private static float getRealMoney() {
        return prefs.getFloat("money");
    }

    // Casino betting methods
    public static boolean canAffordBet(int betAmount) {
        return getMoney() >= betAmount;
    }

    public static boolean placeBet(int betAmount) {
        if (canAffordBet(betAmount)) {
            setMoney(getRealMoney() - betAmount);
            return true;
        }
        return false;
    }

    public static void addWinnings(int winnings) {
        setMoney(getRealMoney() + winnings);
    }

    // Get the current multiplier tier for a given score
    public static int getMultiplierTier(int score) {
        for (int i = MULTIPLIER_THRESHOLDS.length - 1; i >= 0; i--) {
            if (score >= MULTIPLIER_THRESHOLDS[i]) {
                return i;
            }
        }
        return -1; // No multiplier yet
    }

    // Get the multiplier value for a given tier
    public static float getMultiplierForTier(int tier) {
        if (tier >= 0 && tier < MULTIPLIERS.length) {
            return MULTIPLIERS[tier];
        }
        return 1.0f; // Base multiplier
    }

    // Calculate winnings for a bet and score
    public static int calculateWinnings(int betAmount, int score) {
        int tier = getMultiplierTier(score);
        float multiplier = getMultiplierForTier(tier);
        return (int)(betAmount * multiplier);
    }

    //endregion
}
