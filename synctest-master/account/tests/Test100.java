package account.tests;
import org.junit.*;
import org.junit.Test;
import static org.junit.Assert.*;


public class Test100 {
    @Test
    public void test100() {
        account.src.Main m = new account.src.Main();
        m.Runner(new String[] {"100"});
        assertTrue((m.less==false)&&(m.more==false));
    }
}
