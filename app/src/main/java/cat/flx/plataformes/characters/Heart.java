package cat.flx.plataformes.characters;

import cat.flx.plataformes.GameEngine;

public class Heart extends Character {

    public Heart(GameEngine gameEngine, int x, int y) {
        super(gameEngine, x, y);
    }


    private static final int[][] ANIMATIONS = new int[][] {
            new int[] { 100, 101, 102, 103, 104, 105 }
    };

    @Override int[][] getAnimations() { return ANIMATIONS; }

    @Override void updatePhysics(int delta) { }

    @Override void updateCollisionRect() {
        collisionRect.set(x, y, x + 12, y + 12);
    }

}
