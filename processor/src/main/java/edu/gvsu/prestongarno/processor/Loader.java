package edu.gvsu.prestongarno.processor;

import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.util.Map;

/*****************************************
 * Created by preston on 4/26/17.
 *
 * Plugin for the ServiceLoader
 ****************************************/
public class Loader implements Plugin {

	@Override
	public String getName() {
		return "Try-With-Resources<<";
	}

	/*****************************************
	 * @param javacTask the comp. task
	 * @param strings args
	 ****************************************/
	@Override
	public void init(JavacTask javacTask, String... strings) {
		javacTask.addTaskListener(createTaskListener(javacTask));
	}

	/*****************************************
	 * @param task the compilation task
	 * @return TaskListener that transforms try-with-res to
	 * a safe but backwards compatible version
	 ****************************************/
	public static TaskListener createTaskListener(JavacTask task) {
		return new TaskListenerImpl(task);
	}


	/*****************************************
	 * The tasklistener waits until the
	 * processing rounds are over to scan the AST
	 ****************************************/
	static final class TaskListenerImpl implements TaskListener {

		boolean transformed;
		private BasicJavacTask task;

		public TaskListenerImpl(JavacTask javacTask)
		{
			task = (BasicJavacTask) javacTask;
			transformed = false;
		}

		public void started(TaskEvent taskEvent) {}

		public void finished(TaskEvent taskEvent) {
			if(!transformed && taskEvent.getKind() == TaskEvent.Kind.ANALYZE) {
				TryTreeTranslator ttt = new TryTreeTranslator(task.getContext());
				//System.out.println("\n============<BEFORE>============");
				//taskEvent.getCompilationUnit().getTypeDecls().forEach(System.out::println);

				taskEvent.getCompilationUnit().getTypeDecls().forEach(o -> ttt.translateClass(((JCTree.JCClassDecl) o)));

				//System.out.println("\n============<AFTER>============");
				//taskEvent.getCompilationUnit().getTypeDecls().forEach(System.out::println);
				transformed = true;
			}
		}
	}
}
