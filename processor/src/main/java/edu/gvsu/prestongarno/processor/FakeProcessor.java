package edu.gvsu.prestongarno.processor;

import com.sun.source.util.JavacTask;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Created by preston on 4/26/17.
 * <p>
 * workaround for being able to use google's compile testing->
 * register javac task through initializing a proc and casting to impl. class
 ****************************************/
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public class FakeProcessor extends AbstractProcessor {
	@Override
	public synchronized void init(ProcessingEnvironment pe) {
		super.init(pe);
		JavacTask task = JavacTask.instance(pe);
		task.addTaskListener(new Loader.TaskListenerImpl(task));
	}

	@Override
	public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
		return false;
	}
}
