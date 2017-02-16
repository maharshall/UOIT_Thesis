package account.tests;
import org.junit.*;
import org.junit.Test;
import static org.junit.Assert.*;


public class Test10 {
    @Test
    public void test10() {
        account.src.Main m = new account.src.Main();
        m.Runner(new String[] {"10"});
        assertTrue((m.less==false)&&(m.more==false));
    }
}
