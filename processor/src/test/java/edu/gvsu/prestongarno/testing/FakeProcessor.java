package edu.gvsu.prestongarno.testing;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import edu.gvsu.prestongarno.processor.Loader;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Created by preston on 4/26/17.
 *
 * workaround for being able to use google's compile testing->
 * register javac task through initializing a proc and casting to impl. class
 ****************************************/
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public class FakeProcessor extends AbstractProcessor {
	@Override
	public synchronized void init(ProcessingEnvironment pe) {
		super.init(pe);
		Context   c = ((JavacProcessingEnvironment) pe).getContext();
		JavacTask t = c.get(JavacTask.class);
		t.addTaskListener(Loader.createTaskListener(t));
	}

	@Override
	public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
		return true;
	}
}
