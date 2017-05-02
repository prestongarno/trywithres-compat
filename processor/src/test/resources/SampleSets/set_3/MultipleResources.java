import edu.gvsu.prestongarno.testing.util.OnCloseResourceException;
import edu.gvsu.prestongarno.testing.util.TestAcloseable;

/**
 * Created by preston on 4/28/17.
 */
public class MultipleResources {

	public Object _ASSERT_NOT_NULL = null;
	public Object _ASSERT_NOT_NULL_2 = null;
	public Object _ASSERT_NOT_NULL_3 = null;
	public boolean finalBlockCompleted;

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
