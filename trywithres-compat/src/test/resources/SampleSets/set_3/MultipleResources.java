import com.prestongarno.testing.util.OnCloseResourceException;
import com.prestongarno.testing.util.TestAcloseable;
import com.prestongarno.testing.util.TestObject;
import com.sun.tools.javac.util.List;

/**
 * Created by preston on 4/28/17.
 */
public class MultipleResources extends TestObject {

	public TestAcloseable _ASSERT_NOT_NULL = null;
	public TestAcloseable _ASSERT_NOT_NULL_2 = null;
	public TestAcloseable _ASSERT_NOT_NULL_3 = null;
	public boolean finalBlockCompleted;

	@Override
	public List<TestAcloseable> getCloseable() {
		return List.of(_ASSERT_NOT_NULL, _ASSERT_NOT_NULL_2, _ASSERT_NOT_NULL_3);
	}

	public void INVOKE_ME() throws OnCloseResourceException {
		finalBlockCompleted = false;
		try (TestAcloseable something = new TestAcloseable();
			  TestAcloseable somethingElse = new TestAcloseable();
			  TestAcloseable somethingElseer = new TestAcloseable(true, false);
			  TestAcloseable somethingElsest = new TestAcloseable(true, true)) {
			_ASSERT_NOT_NULL = something;
			_ASSERT_NOT_NULL_2 = somethingElse;
			_ASSERT_NOT_NULL_3 = somethingElseer;

			somethingElse.doRiskyThings();

			something.doRiskyThings();

			somethingElseer.doRiskyThings();

			somethingElsest.doRiskyThings();
		} finally {
			System.out.println("Running user finally block...");
			finalBlockCompleted = true;
		}
	}
}
