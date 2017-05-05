package com.prestongarno;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Options;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * Created by preston on 4/26/17.
 ****************************************/
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public class FakeProcessor extends AbstractProcessor {
	private boolean INJECT_STDOUT_STATEMENT;

	/*****************************************
	 * Add task to the javac compilation process
	 * @param pe the processing environment
	 ****************************************/
	@Override
	public synchronized void init(ProcessingEnvironment pe) {
		JavacTask task = JavacTask.instance(pe);
		if (!(task instanceof BasicJavacTask)) {
			pe.getMessager()
					.printMessage(Diagnostic.Kind.WARNING,
									  "Unfortunately you are using an incompatible " +
											  "version of the javac.  Please report this at " +
											  "https://github.com/prestongarno/trywithres-compat/issues. Thank you.");
			return;
		}

		super.init(pe);
		task.addTaskListener(new Loader.TaskListenerImpl(task));
		String v = Options.instance(((BasicJavacTask) task).getContext()).get("-g:source");
		INJECT_STDOUT_STATEMENT = v != null;
	}

	/*****************************************
	 * @param set set of type elements
	 * @param roundEnvironment the roundenv
	 * @return false
	 ****************************************/
	@Override
	public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
/*		if(INJECT_STDOUT_STATEMENT) {
			roundEnvironment.getRootElements().stream()
					.map(o -> Trees.instance(this.processingEnv).getTree(o))
					.filter(tree -> tree instanceof JCTree.JCClassDecl)
					.map(tree -> ((JCTree.JCClassDecl) tree))
					.forEach(tree -> new TreeTranslator() {
						final BasicJavacTask task = (BasicJavacTask) JavacTask.instance(processingEnv);
						final TreeMaker make = TreeMaker.instance(task.getContext());
						final Symtab symtab = Symtab.instance(task.getContext());
						final Names names = Names.instance(task.getContext());

						@Override
						public void visitTry(JCTree.JCTry jcTry) {
							super.visitTry(jcTry);
							Name                 print          = names.fromString("println");
							JCTree.JCFieldAccess out            = make.Select(make.Ident(symtab.systemType.tsym).setType(symtab.systemType), names.fromString("out"));
							JCTree.JCLiteral     literal        = make.Literal("Injected finally statement closing resources...").setType(symtab.stringType);
							JCTree.JCStatement   _invoke_stdout = make.Exec(make.Apply(null, make.Select(out, print), List.of(literal)));
							jcTry.body.stats = jcTry.body.stats.prepend(_invoke_stdout);
							result = jcTry;
							super.visitTry(jcTry);
						}
					}.translate(tree));
		}*/

		return false;
	}
}
