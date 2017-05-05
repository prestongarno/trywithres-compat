import com.prestongarno.testing.util.OnCloseResourceException;
import com.prestongarno.testing.util.TestAcloseable;
import com.prestongarno.testing.util.TestObject;
import com.sun.tools.javac.util.List;

public class SampleExceptionInTry extends TestObject {

	public TestAcloseable _ASSERT_NOT_NULL = null;

	public void INVOKE_ME() throws OnCloseResourceException {

		try (TestAcloseable something = new TestAcloseable(true, false))
		{
			_ASSERT_NOT_NULL = something;
			something.doRiskyThings();
		}
	}

	@Override
	public List<TestAcloseable> getCloseable() {
		return List.of(this._ASSERT_NOT_NULL);
	}

}