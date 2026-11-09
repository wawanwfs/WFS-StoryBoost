package com.wfs.storyboost;

import static org.junit.Assert.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Independent safety contract, never relies on a real low-RAM phone. */
@RunWith(AndroidJUnit4.class)
public final class DncnnMemorySafetyQaTest {
    private static final long MiB=1024L*1024L;
    @Test public void memoryHeadroomGuardRejectsPressuredAndLowRamDevices(){
        assertEquals(512*MiB,AiDenoiseEngine.MIN_AVAILABLE_HEADROOM_BYTES);
        assertFalse(AiDenoiseEngine.hasSafeMemoryBudget(true,false,8192*MiB));
        assertFalse(AiDenoiseEngine.hasSafeMemoryBudget(false,true,8192*MiB));
        assertFalse(AiDenoiseEngine.hasSafeMemoryBudget(false,false,0));
        assertFalse(AiDenoiseEngine.hasSafeMemoryBudget(false,false,511*MiB));
        assertTrue(AiDenoiseEngine.hasSafeMemoryBudget(false,false,512*MiB));
        assertTrue(AiDenoiseEngine.hasSafeMemoryBudget(false,false,2048*MiB));
    }
}
