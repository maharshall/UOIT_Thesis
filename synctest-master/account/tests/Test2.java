package account.tests;
import org.junit.*;
import org.junit.Test;
import static org.junit.Assert.*;


public class Test2 {
    @Test
    public void test2() {
        account.src.Main m = new account.src.Main();
        m.Runner(new String[] {"2"});
        assertTrue((m.less==false)&&(m.more==false));
    }
}
