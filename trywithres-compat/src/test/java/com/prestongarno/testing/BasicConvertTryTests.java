package com.prestongarno.testing;

import com.google.testing.compile.Compilation;
import com.prestongarno.FakeProcessor;
import com.prestongarno.testing.util.OnCloseResourceException;
import com.prestongarno.testing.util.TestAcloseable;
import com.prestongarno.testing.util.TestObject;
import com.prestongarno.testing.util.TestRuntimeException;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static com.prestongarno.testing.CompilerUtil.loadClassSet;
import static junit.framework.TestCase.assertTrue;

/****************************************
 * Created by preston on 4/26/17.
 ****************************************/
@SuppressWarnings("unchecked")
public class BasicConvertTryTests {

	private static final String DIAGNOSTICS = "-g:source";

	@Test
	public void mTestServiceLoader() throws Exception {
		Compilation compilation = javac()
				.withOptions(DIAGNOSTICS)
				.withProcessors(new FakeProcessor())
				.compile(loadClassSet(4));
		assertThat(compilation).succeededWithoutWarnings();
	}

	@Test
	public void testLoadingClassesAndAssertClosed() throws Exception {
		Compilation compilation = javac()
				.withOptions(DIAGNOSTICS)
				.withProcessors(new FakeProcessor())
				.compile(loadClassSet(0));
		compilation.diagnostics().forEach(System.out::println);
		assertThat(compilation).succeededWithoutWarnings();

		// test the resource being closed & call method
		Method _modified_method;

		//load class, create instance, and get method & resource fields
		Class sample = CompilerUtil.createClassLoader(compilation).loadClass("SampleSomethingClass");
		_modified_method = sample.getMethod("INVOKE_ME");
		TestObject inst = (TestObject) sample.newInstance();

		_modified_method.invoke(inst);
		inst.getCloseable().forEach(testAcloseable -> assertTrue(testAcloseable.isClosed()));
	}

	@Test(expected = InvocationTargetException.class)
	public void testExceptionThrownFromTry() throws Exception {
		Compilation compilation = javac()
				.withOptions(DIAGNOSTICS)
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
		TestObject inst = (TestObject) sample.newInstance();

		try {
			_modified_method.invoke(inst);
		} catch (TestRuntimeException expected) { // Expected: testexc.
			assertTrue(expected.getCause() instanceof TestRuntimeException);
			assertTrue(((TestAcloseable) _resource.get(inst)).isClosed());
		}
	}

	@Test
	public void exceptionThrownFromAutoClose() throws Exception {
		Compilation compilation = javac()
				.withOptions(DIAGNOSTICS)
				.withProcessors(new FakeProcessor())
				.compile(loadClassSet(2));

		assertThat(compilation).succeededWithoutWarnings();

		// test the resource being closed reflectively call method
		Method _modified_method;

		//load class, create instance, and get method & resource fields
		Class sample = CompilerUtil.createClassLoader(compilation).loadClass("TestInceptionTryCatch");
		_modified_method = sample.getMethod("INVOKE_ME");
		TestObject inst = (TestObject) sample.newInstance();

		try {
			_modified_method.invoke(inst);
		} catch (InvocationTargetException exc) {
			assertTrue(exc.getCause() instanceof OnCloseResourceException);
			inst.getCloseable().forEach(testAcloseable -> assertTrue(testAcloseable.isClosed()));
		}
	}

	/**
	 * Run with 4 resources, first 2 run normally, 3rd fails on block, 4th fails on close
	 */
	@Test
	public void testMultipleResource() throws Exception {
		Compilation compilation = javac()
				.withOptions(DIAGNOSTICS)
				.withProcessors(new FakeProcessor())
				.compile(loadClassSet(3));
		assertThat(compilation).succeededWithoutWarnings();

		// test the resource being closed reflectively call method
		Field _finally_block_complete;
		Method _modified_method;

		//load class, create instance, and get method & resource fields
		Class sample = CompilerUtil.createClassLoader(compilation).loadClass("MultipleResources");
		_finally_block_complete = sample.getField("finalBlockCompleted");
		_modified_method = sample.getMethod("INVOKE_ME");
		TestObject inst = (TestObject) sample.newInstance();

		try {
			_modified_method.invoke(inst);
		} catch (InvocationTargetException exc) {
			inst.getCloseable().forEach(testAcloseable -> assertTrue(testAcloseable.isClosed()));
			assertTrue(((boolean) _finally_block_complete.get(inst)));
			assertTrue(exc.getCause().getSuppressed().length > 0 && exc.getCause().getSuppressed()[0] instanceof OnCloseResourceException);
		}
	}
}

