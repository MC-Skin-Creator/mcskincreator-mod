/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Which way the head ends up pointing, which is the whole of what this class decides.
 *
 * <p>A head turned the wrong way looks exactly like a head turned the right way until
 * the mouse moves, and nothing in this environment can move a mouse. So the signs are
 * pinned here instead, against the one convention they have to agree with: the game
 * counts a body's turn the other way round from the screen, which is why
 * {@code bodyRot} goes <em>down</em> as the figure turns towards the right of the panel.
 */
class GazeTest {
    /** The middle of a 200x300 panel, with the figure fitted to it at one block to 100px. */
    private static final float CENTRE_X = 100;
    private static final float FEET_Y = 250;
    private static final float SCALE = 100;

    private static Gaze at(float mouseX, float mouseY) {
        return Gaze.towards(mouseX, mouseY, CENTRE_X, FEET_Y, SCALE);
    }

    @Test
    void theHeadTurnsTowardsAPointerOnTheRight() {
        // Turning towards the right of the panel lowers bodyRot, so the head's own turn
        // has to go the same way or it looks over the wrong shoulder.
        assertTrue(at(CENTRE_X + 60, FEET_Y).headTurn(0) < 0,
                "a pointer on the right turns the head right, which is a negative turn");
        assertTrue(at(CENTRE_X - 60, FEET_Y).headTurn(0) > 0,
                "and a pointer on the left turns it the other way");
    }

    @Test
    void theHeadNodsTowardsAPointerBelowIt() {
        // The game's pitch is positive downwards, the same way the screen's y runs.
        assertTrue(at(CENTRE_X, FEET_Y + 200).headNod() > 0, "a pointer underneath looks down");
        assertTrue(at(CENTRE_X, FEET_Y - 300).headNod() < 0, "and one overhead looks up");
    }

    @Test
    void pointingAtTheEyesLooksStraightAtYou() {
        float eyeY = FEET_Y - 1.62F * SCALE;
        Gaze gaze = at(CENTRE_X, eyeY);
        assertEquals(0, gaze.headTurn(0), 1.0E-4F);
        assertEquals(0, gaze.headNod(), 1.0E-4F);
    }

    @Test
    void theHeadGivesBackWhateverTheBodyHasBeenTurnedBy() {
        // The point of the whole class: the player turns the body, the head stays on the
        // pointer. A head that simply rode the body would report zero here for every turn.
        float eyeY = FEET_Y - 1.62F * SCALE;
        Gaze gaze = at(CENTRE_X, eyeY);
        for (float bodyTurn : new float[] {-40, -15, 0, 15, 40}) {
            assertEquals(-bodyTurn, gaze.headTurn(bodyTurn), 1.0E-4F,
                    "the head undoes the body's turn to keep facing you, at " + bodyTurn);
        }
    }

    @Test
    void theNeckNeverBendsFurtherThanANeckBends() {
        Gaze gaze = at(CENTRE_X + 4000, FEET_Y);
        for (float bodyTurn = -360; bodyTurn <= 360; bodyTurn += 5) {
            float turn = gaze.headTurn(bodyTurn);
            assertTrue(turn >= -Gaze.NECK_LIMIT && turn <= Gaze.NECK_LIMIT,
                    "the neck is bounded at " + bodyTurn + ", not " + turn);
        }
    }

    @Test
    void spinningTheFigureRoundDoesNotLeaveTheHeadStuckAtItsLimit() {
        // A body wrapped to just under a full turn is ten degrees off facing you, not
        // three hundred and fifty. Without the wrap the head sticks at the limit for most
        // of every revolution, which reads as a neck that has locked up.
        float eyeY = FEET_Y - 1.62F * SCALE;
        assertEquals(10, at(CENTRE_X, eyeY).headTurn(350), 1.0E-4F);
        assertEquals(-10, at(CENTRE_X, eyeY).headTurn(-350), 1.0E-4F);
    }

    @Test
    void theNodStopsShortOfAFoldedNeckOnItsOwn() {
        // It carries no bound of its own, so the falloff has to be the bound.
        assertTrue(Math.abs(at(CENTRE_X, FEET_Y + 100000).headNod()) < 45,
                "the nod is bounded by its falloff alone");
    }

    @Test
    void zoomingInKeepsTheEyesOnTheFace() {
        // The eyes are a fixed height up the figure, so they are a different number of
        // pixels up the panel at every zoom. Pointing at them is looking straight ahead
        // whichever zoom that is.
        for (float scale : new float[] {40, 100, 260}) {
            float eyeY = FEET_Y - 1.62F * scale;
            assertEquals(0, Gaze.towards(CENTRE_X, eyeY, CENTRE_X, FEET_Y, scale).headNod(),
                    1.0E-4F, "the eyes are still the eyes at " + scale + " pixels a block");
        }
    }

    @Test
    void aheadIsLookingAtNothing() {
        assertEquals(0, Gaze.AHEAD.headTurn(0), 1.0E-6F);
        assertEquals(0, Gaze.AHEAD.headNod(), 1.0E-6F);
    }
}
