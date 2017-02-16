package account.tests;
import org.junit.*;
import org.junit.Test;
import static org.junit.Assert.*;


public class Test15K {
    @Test
    public void test15K() {
        account.src.Main m = new account.src.Main();
        m.Runner(new String[] {"15000"});
        assertTrue((m.less==false)&&(m.more==false));
    }
}
