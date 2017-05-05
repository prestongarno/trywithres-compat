import com.prestongarno.testing.util.OnCloseResourceException;
import com.prestongarno.testing.util.TestAcloseable;
import com.prestongarno.testing.util.TestObject;
import com.sun.tools.javac.util.List;

public class SampleSomethingClass extends TestObject {

	public static void main(String[] args) throws Exception {
		new SampleSomethingClass().INVOKE_ME();
	}

	public TestAcloseable _ASSERT_NOT_NULL = null;

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

	@Override
	public List<TestAcloseable> getCloseable() {
		return List.of(this._ASSERT_NOT_NULL);
	}
}