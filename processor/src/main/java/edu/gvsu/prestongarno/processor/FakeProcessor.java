package edu.gvsu.prestongarno.processor;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Created by preston on 4/26/17.
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
/*		roundEnvironment.getRootElements().stream()
				.map(o -> Trees.instance(this.processingEnv).getTree(o))
				.filter(tree -> tree instanceof JCTree.JCClassDecl)
				.map(tree -> ((JCTree.JCClassDecl) tree))
				.forEach(tree -> new TreeTranslator(){
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
					}.translate(tree));*/

		return false;
	}
}
