package com.victorvalentim.zividomelive.core.lifecycle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivationStateTest {

    @Test
    void reloadRequestsCoalesceAndCanBeConsumedAgainLater() {
        ActivationState state = new ActivationState();
        state.requestReload();
        state.requestReload();
        assertTrue(state.consumeReloadRequest());
        assertFalse(state.consumeReloadRequest());
        state.requestReload();
        assertTrue(state.consumeReloadRequest());
    }

    @Test
    void pauseAndResumeDoNotConsumeReloadRequest() {
        ActivationState state = new ActivationState();
        state.requestReload();
        state.pause();
        assertTrue(state.isPaused());
        assertTrue(state.isAccepting());
        state.resume();
        assertFalse(state.isPaused());
        assertTrue(state.consumeReloadRequest());
    }

    @Test
    void stoppingClearsReloadAndPermanentlyRejectsWork() {
        ActivationState state = new ActivationState();
        state.requestReload();
        state.beginStopping();
        state.beginStopping();
        assertFalse(state.isAccepting());
        assertFalse(state.consumeReloadRequest());
        assertThrows(IllegalStateException.class, state::requestReload);
        assertThrows(IllegalStateException.class, state::pause);
        assertThrows(IllegalStateException.class, state::resume);
    }

    @Test
    void closeIsTerminalAndIdempotent() {
        ActivationState state = new ActivationState();
        state.close();
        state.close();
        assertTrue(state.isClosed());
        assertFalse(state.isAccepting());
    }

    @Test
    void closeClearsPauseAndPendingReloadBeforeRejectingEveryAdmissionCall() {
        ActivationState state = new ActivationState();
        state.requestReload();
        state.pause();

        state.close();

        assertFalse(state.isPaused());
        assertFalse(state.consumeReloadRequest());
        assertThrows(IllegalStateException.class, state::requestReload);
        assertThrows(IllegalStateException.class, state::pause);
        assertThrows(IllegalStateException.class, state::resume);
    }

    @Test
    void stoppingThenClosingIsAStableTwoPhaseTransition() {
        ActivationState state = new ActivationState();
        state.beginStopping();
        assertFalse(state.isClosed());
        assertFalse(state.isAccepting());

        state.close();
        state.beginStopping();

        assertTrue(state.isClosed());
        assertFalse(state.isAccepting());
        assertFalse(state.isPaused());
    }
}
