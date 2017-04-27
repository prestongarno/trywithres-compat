package edu.gvsu.prestongarno.testing;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.io.*;
import java.util.List;

import static com.google.testing.compile.Compiler.javac;
import static edu.gvsu.prestongarno.testing.CompilerUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by preston on 4/26/17.
 */
public class CompilerTests1 {
	@Test
	public void mTestServiceLoader() throws Exception {
		Compilation compilation = javac()
				.withProcessors(new FakeProcessor())
				.compile(loadClassSet(0));
		System.out.println(compilation);
	}

	@Test
	public void testTryBehaviourNormally() throws IOException {
		List<JavaFileObject> obj = loadClassSet(0);
		obj.forEach(file -> {
			InputStream saved = null;
			try(InputStream r = file.openInputStream()) {
				saved = r;
				r.skip(10000000L);
			} catch (IOException e) {} finally {
				try {
					System.out.println(saved.available());
				} catch (IOException e1) { assertTrue(e1.getMessage().contains("closed"));}
			}
		});
	}
}
