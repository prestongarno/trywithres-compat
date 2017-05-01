import edu.gvsu.prestongarno.testing.util.TestAcloseable;
import edu.gvsu.prestongarno.testing.util.TestRuntimeException;

public class Test {

    public static void main(String[] args) throws Exception {
        new Test().INVOKE_ME();
    }
    public void INVOKE_ME() throws Exception {

        try (SubCloseable something = new SubCloseable()) {
            something.doRiskyThings();
        }
    }

}

class SubTestRTExc extends TestRuntimeException {
    @Override
    public String toString() {
        return "<<SubTestRTExc>>";
    }
}

class SubCloseable extends TestAcloseable implements AutoCloseable {

    private boolean failOnAction;
    private boolean failOnClose;
    private boolean closed;

    public SubCloseable() { this(false, false); }

    public SubCloseable(boolean failOnAction, boolean failOnClose) {
        this.failOnAction = failOnAction;
        this.failOnClose = failOnClose;
    }

    public boolean isClosed() {
        return closed;
    }

    public void doRiskyThings() {
        closed = false;
        if (failOnAction) {
            throw new SubTestRTExc();
        } else System.out.println("Performing work normally...");
    }

}
