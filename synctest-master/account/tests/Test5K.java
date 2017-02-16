package account.tests;
import org.junit.*;
import org.junit.Test;
import static org.junit.Assert.*;


public class Test5K {
    @Test
    public void test5K() {
        account.src.Main m = new account.src.Main();
        m.Runner(new String[] {"5000"});
        assertTrue((m.less==false)&&(m.more==false));
    }
}
