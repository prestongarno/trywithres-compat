package edu.gvsu.prestongarno.testing.util;

/**
 * Created by preston on 4/28/17.
 */
public class Test {

    public static void main(String[] args) throws Exception {
        new Test().INVOKE_ME();
    }
public void INVOKE_ME() throws OnCloseResourceException {
    try {
        java.lang.RuntimeException _Scope_Runtime_Exception = new RuntimeException();
        TestAcloseable something = null;
        try {
            something = new TestAcloseable(false, false);
            something.doRiskyThings();
        } catch (java.lang.RuntimeException _nest_catch_throwable) {
            _Scope_Runtime_Exception.initCause(_nest_catch_throwable);
            if (something != null) {
                try {
                    something.close();
                    something = null;
                } catch (java.lang.Throwable _inner_inner_throwable_suppressed) {
                    _Scope_Runtime_Exception.addSuppressed(_inner_inner_throwable_suppressed);
                }
            }
        } finally {
            if (something != null) {
                try {
                    something.close();
                    something = null;
                } catch (java.lang.Throwable _inner_inner_throwable_suppressed) {
                    _Scope_Runtime_Exception.addSuppressed(_inner_inner_throwable_suppressed);
                }
            }
            if (_Scope_Runtime_Exception.getCause() != null || _Scope_Runtime_Exception.getSuppressed().length > 0) {
                throw _Scope_Runtime_Exception;
            }
        }
    } catch (java.lang.RuntimeException _something_no_one_should_ever_name_a_variable) {
        throw _something_no_one_should_ever_name_a_variable;
    }
    }
}
