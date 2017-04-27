package edu.gvsu.prestongarno.processor;

import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree;
import edu.gvsu.prestongarno.processor.transform.JcUtil;

import java.util.Map;

import static com.sun.tools.javac.code.Symbol.*;

/**
 * Created by preston on 4/26/17.
 */
public class Loader implements Plugin {
	@Override
	public String getName() {
		return "Try-With-Resources<<";
	}

	@Override
	public void init(JavacTask javacTask, String... strings) {
		javacTask.addTaskListener(createTaskListener(javacTask));
	}

	public static TaskListener createTaskListener(JavacTask task) {
		return new TaskListenerImpl(task);
	}


	/**
	 * The tasklistener waits until processing is over to scan the AST
	 */
	static final class TaskListenerImpl implements TaskListener {

		private BasicJavacTask task;

		public TaskListenerImpl(JavacTask javacTask) { task = (BasicJavacTask) javacTask; }

		public void started(TaskEvent taskEvent) {
			if(taskEvent.getKind() == TaskEvent.Kind.ANALYZE) {
				TryTreeTranslator ttt = new TryTreeTranslator(task.getContext(), task);
				taskEvent.getCompilationUnit().getTypeDecls().forEach(o -> ttt.translate((JCTree) o));
			}

		}

		public void finished(TaskEvent e) {}
	}
}
