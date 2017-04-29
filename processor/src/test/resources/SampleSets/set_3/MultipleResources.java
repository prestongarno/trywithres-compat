import edu.gvsu.prestongarno.testing.util.OnCloseResourceException;
import edu.gvsu.prestongarno.testing.util.TestAcloseable;

/**
 * Created by preston on 4/28/17.
 */
public class MultipleResources {

	public Object _ASSERT_NOT_NULL = null;
	public Object _ASSERT_NOT_NULL_2 = null;

	public void INVOKE_ME() throws OnCloseResourceException {
		try (TestAcloseable something = new TestAcloseable(true, false);
			  TestAcloseable somethingElse = new TestAcloseable(false, false)) {
			_ASSERT_NOT_NULL = something;
			_ASSERT_NOT_NULL_2 = somethingElse;
			somethingElse.doRiskyThings();
			something.doRiskyThings();
		}
	}
}
