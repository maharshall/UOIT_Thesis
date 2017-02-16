package account.tests;
import org.junit.*;
import org.junit.Test;
import static org.junit.Assert.*;


public class Test50K {
    @Test
    public void test50K() {
        account.src.Main m = new account.src.Main();
        m.Runner(new String[] {"50000"});
        assertTrue((m.less==false)&&(m.more==false));
    }
}
