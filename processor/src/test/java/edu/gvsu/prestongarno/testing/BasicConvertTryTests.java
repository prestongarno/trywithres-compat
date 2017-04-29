package edu.gvsu.prestongarno.testing;

import com.google.testing.compile.Compilation;
import edu.gvsu.prestongarno.processor.FakeProcessor;
import edu.gvsu.prestongarno.testing.util.OnCloseResourceException;
import edu.gvsu.prestongarno.testing.util.TestAcloseable;
import edu.gvsu.prestongarno.testing.util.TestRuntimeException;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static edu.gvsu.prestongarno.testing.CompilerUtil.*;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/****************************************
 * Created by preston on 4/26/17.
 ****************************************/
public class BasicConvertTryTests {
	@Test
	public void mTestServiceLoader() throws Exception {
		Compilation compilation = javac()
				.withProcessors(new FakeProcessor())
				.compile(loadClassSet(0));
		assertThat(compilation).succeededWithoutWarnings();
	}

	@Test
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

		_modified_method.invoke(inst);
		assertTrue(((TestAcloseable) _resource.get(inst)).isClosed());
	}

	@Test(expected = InvocationTargetException.class)
	public void testExceptionThrownFromTry() throws Exception {
		Compilation compilation = javac()
				.withProcessors(new FakeProcessor())
				.compile(loadClassSet(1));
		compilation.diagnostics().forEach(System.out::println);
		assertThat(compilation).succeededWithoutWarnings();

		// test the resource being closed reflectively call method
		Field _resource;
		Method _modified_method;

		//load class, create instance, and get method & resource fields
		Class sample = CompilerUtil.createClassLoader(compilation).loadClass("SampleExceptionInTry");
		_resource = sample.getField("_ASSERT_NOT_NULL");
		_modified_method = sample.getMethod("INVOKE_ME");
		Object inst = sample.newInstance();

		try {
			_modified_method.invoke(inst);
		} catch (TestRuntimeException expected) { // Expected: testexc.
			assertTrue(expected.getCause() instanceof TestRuntimeException);
			assertTrue(((TestAcloseable) _resource.get(inst)).isClosed());
		}
	}

	@Test
	public void exceptionThrownFromAutoClose() throws Exception {
		Compilation compilation = javac().withProcessors(new FakeProcessor()).compile(loadClassSet(2));
		assertThat(compilation).succeededWithoutWarnings();

		// test the resource being closed reflectively call method
		Field _resource;
		Method _modified_method;

		//load class, create instance, and get method & resource fields
		Class sample = CompilerUtil.createClassLoader(compilation).loadClass("ExceptionOnlyInClose");
		_resource = sample.getField("_ASSERT_NOT_NULL");
		_modified_method = sample.getMethod("INVOKE_ME");
		Object inst = sample.newInstance();

		try {
			_modified_method.invoke(inst);
		} catch (InvocationTargetException exc) {
			assertTrue(exc.getCause() instanceof OnCloseResourceException);
		}
	}

	@Test
	public void testMultipleResource() throws Exception {
		Compilation compilation = javac().withProcessors(new FakeProcessor()).compile(loadClassSet(3));
		assertThat(compilation).succeededWithoutWarnings();

		// test the resource being closed reflectively call method
		Field _resource;
		Field _resource_2;
		Method _modified_method;

		//load class, create instance, and get method & resource fields
		Class sample = CompilerUtil.createClassLoader(compilation).loadClass("MultipleResources");
		_resource = sample.getField("_ASSERT_NOT_NULL");
		_resource_2 = sample.getField("_ASSERT_NOT_NULL_2");
		_modified_method = sample.getMethod("INVOKE_ME");
		Object inst = sample.newInstance();

		try {
			_modified_method.invoke(inst);
		} catch (InvocationTargetException exc) {
			assertTrue(((TestAcloseable) _resource.get(inst)).isClosed());
			assertTrue(((TestAcloseable) _resource_2.get(inst)).isClosed());
		}

	}
}
