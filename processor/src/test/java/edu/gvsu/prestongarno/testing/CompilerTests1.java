package edu.gvsu.prestongarno.testing;

import com.google.testing.compile.Compilation;
import edu.gvsu.prestongarno.testing.util.TestAcloseable;
import edu.gvsu.prestongarno.testing.util.TestRuntimeException;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static edu.gvsu.prestongarno.testing.CompilerUtil.*;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/****************************************
 * Created by preston on 4/26/17.
 ****************************************/
public class CompilerTests1 {
	@Test
	public void mTestServiceLoader() throws Exception {
		Compilation compilation = javac()
				.withProcessors(new FakeProcessor())
				.compile(loadClassSet(0));
		compilation.diagnostics().forEach(System.out::println);
		assertThat(compilation).succeededWithoutWarnings();
	}

	/** test set #0: make sure the source is closed by attempting
	 * to write using resource after exception already thrown
	 */
	@Test//(expected = RuntimeException.class)
	public void testLoadingClassesAndAssertClosed() throws Exception {
		Compilation compilation = javac()
				.withProcessors(new FakeProcessor())
				.compile(loadClassSet(0));
		compilation.diagnostics().forEach(System.out::println);
		assertThat(compilation).succeededWithoutWarnings();

		// test the resource being closed reflectively call method
		Field _resource;
		Method _modified_method;

		//load class, create instance, and get method & resource fields
		Class sample = CompilerUtil.createClassLoader(compilation).loadClass("SampleSomethingClass");
		_resource = sample.getField("_ASSERT_NOT_NULL");
		_modified_method = sample.getMethod("INVOKE_ME");
		Object inst = sample.newInstance();
		assertNotNull(_resource);
		assertNotNull(_modified_method);
		assertNotNull(inst);

		try {
			_modified_method.invoke(inst);
		} catch (TestRuntimeException expected) { // Expected: testexc.
			expected.printStackTrace();
			assertTrue(expected.getCause() instanceof TestRuntimeException);
			// get the field and check the resource if it's closed
		} catch (Throwable r) {
			r.printStackTrace();
			Arrays.stream(r.getSuppressed()).forEach(throwable -> System.out.println(throwable.getMessage()));
			throw new RuntimeException(r);
		} finally {
			assertTrue(((TestAcloseable) _resource.get(inst)).isClosed());
		}
	}

}
