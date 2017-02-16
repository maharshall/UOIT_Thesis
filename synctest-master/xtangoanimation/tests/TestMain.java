package xtangoanimation.tests;

import org.junit.*;
import org.junit.Test;
import static org.junit.Assert.*;


public class TestMain {

    @Test
    public void testMain() {
        System.out.println("Testing XtangoAnimator");
        xtangoanimation.src.XtangoAnimator xa = new xtangoanimation.src.XtangoAnimator();
        xa.execute();
        assertTrue(true);
    }

}
