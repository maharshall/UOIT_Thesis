package account.tests;
import org.junit.*;
import org.junit.Test;
import static org.junit.Assert.*;


public class Test1K {
    @Test
    public void test1K() {
        account.src.Main m = new account.src.Main();
        m.Runner(new String[] {"1000"});
        assertTrue((m.less==false)&&(m.more==false));
    }
}
