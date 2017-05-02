import edu.gvsu.prestongarno.testing.util.OnCloseResourceException;
import edu.gvsu.prestongarno.testing.util.TestAcloseable;

public class TestInceptionTryCatch {

	public Object _ASSERT_NOT_NULL = null;

	public void INVOKE_ME() throws OnCloseResourceException {
		try (TestAcloseable something = new TestAcloseable()) {
			_ASSERT_NOT_NULL = something;
			something.doRiskyThings();
			new SampleInnerCompUnitIface(){
				@Override
				public void doStuff() {
					try (TestAcloseable something = new TestAcloseable(false, true)) {
						something.doRiskyThings();
					} catch (OnCloseResourceException ox) {
						(new SampleInnerCompUnitIface(){
							@Override
							public void doStuff() {
								try (TestAcloseable something = new TestAcloseable(false, true)) {
									something.doRiskyThings();
								} catch (OnCloseResourceException ox) {
									System.out.println("catching inside nested class x2 deep");
									new SampleInnerCompUnitIface(){
										@Override
										public void doStuff() {
											System.out.println("catching inside nested class x3 deep");
										}
									}.doStuff();
								}
							}
						}).doStuff();
						System.out.println("catching in an interface");
					}
				}
			}.doStuff();
		}
	}
}


interface SampleInnerCompUnitIface {
	void doStuff();
}
