import edu.gvsu.prestongarno.testing.util.OnCloseResourceException;
import edu.gvsu.prestongarno.testing.util.TestAcloseable;

public class SampleSomethingClass {

	public static void main(String[] args) throws Exception {
		new SampleSomethingClass().INVOKE_ME();
	}

	public Object _ASSERT_NOT_NULL = null;

	public void INVOKE_ME() {
		try (TestAcloseable something = new TestAcloseable(false, false)) {
			_ASSERT_NOT_NULL = something;
			something.doRiskyThings();
		} catch (OnCloseResourceException on) {
			System.out.println("closable failed...");
		} finally {
			System.out.println("user's finally block is running..");
		}
	}

}