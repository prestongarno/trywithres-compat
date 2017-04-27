package edu.gvsu.prestongarno.processor;

import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;

import static com.sun.tools.javac.tree.JCTree.*;

import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;


/**
 * Created by preston on 4/26/17.
 ****************************************/
public class TryTreeTranslator extends TreeTranslator {

	private final Context   context;
	private final Trees     trees;
	private final TreeMaker make;

	private final Type           _exception_type;
	TryTreeTranslator(Context context, BasicJavacTask task) {
		this.context = context;
		trees = Trees.instance(task);
		make = TreeMaker.instance(context);
		_exception_type = Symtab.instance(context).exceptionType;
	}

	@Override
	public void visitTry(JCTree.JCTry jcTry) {
		//if it has resources translate
		if (jcTry.resources.nonEmpty()) {
			this.change(jcTry);
		}
		super.visitTry(jcTry);
	}


	/*****************************************
	 * Translate the try-with-resources statement to something compatible
	 *
	 * ORIGINAL:
	 * try(AutoCloseable res = new DangerousItem()){
	 * 	resource.doDangerousStuff();
	 * }
	 *
	 * NEW:
	 * try {
	 * 	AutoCloseable res = new DangerousItem();
	 *
	 * 	//==================
	 * 	try {
	 * 		resource.doDangerousStuff();
	 *   } catch (Exception e) {
	 * 		try{ res.close(); } catch (Exception ex) { throw ex.getThrowable(); }
	 *   }
	 *   //==================
	 *
	 * } // include user catch exceptions -> prepending a close() statement to the block
	 *
	 * @param jcTry the try statement to change
	 ****************************************/
	private void change(JCTry jcTry) {
		List<JCStatement> resList = List.nil();
		for (JCTree vd : jcTry.resources) {
			resList = resList.prepend((JCStatement) vd);
		}


		// construct a nested try/catch block for closing the resources
		List<JCTree>   _res          = List.nil();
		// the body shifted one block inside a try
		JCBlock        _nested_body  = jcTry.body;
		// the inner catch block construction
		Name           _nes_exc_name   = Names.instance(context).fromString("_nes_exc_name_");

		// Catch any exception
		JCVariableDecl     param      = make.Param(_nes_exc_name, _exception_type, _exception_type.tsym);

		// closing all of the resourcces with a null check
		List<JCStatement> _close_invokations = List.nil();

			// the method invokation on close
		for(JCStatement ex : resList) {
			JCVariableDecl varDec = ((JCVariableDecl) ex);
			JCExpression e = make.Ident(varDec.name); // identified same as the param in the try-w-res
			JCFieldAccess access = make.Select(e, Names.instance(context).fromString("close"));
			JCMethodInvocation _inv_close = make.Apply(List.nil(),access, List.nil());
			JCBinary _check_null = make.Binary(Tag.NE, make.Ident(((JCVariableDecl) ex).name), make.Literal(TypeTag.BOT, null));
			JCParens parens = make.Parens(_check_null); // conditional
			JCStatement exec = make.Exec(_inv_close);
			List<JCStatement> stats = List.of((JCStatement) exec);
			JCIf _not_null_check = make.If(parens, make.Block(0, stats), null);
			_close_invokations = _close_invokations.append(_not_null_check);
			System.out.println(_not_null_check);
		}

		// make a generalized catch statement that closes all and then throws the exception again
		JCCatch _nested_catch = make.Catch(param, make.Block(0, List.of(make.Throw(make.Ident(param.sym)))));
		_nested_catch.body.stats = _nested_catch.body.stats.prependList(_close_invokations);

		// make a finally block doing the same thins
		JCBlock _nested_fin_block = make.Block(0, _nested_catch.body.stats);

		// make the try block
		//Params are: 	List<JCTree> resources, JCBlock body, List<JCCatch> catchers, JCBlock finalizer)
		JCTry _shifted_try_block = make.Try(_res, _nested_body, List.of(_nested_catch), _nested_fin_block);

		//add the nested try/catch statement at beginning
		jcTry.body = make.Block(0, List.of(_shifted_try_block));
		// insert the variable declarations at the beginning of the block
		jcTry.body.stats = jcTry.body.stats.prependList(resList);
		jcTry.resources = List.nil();

		// replace with ours
		result = jcTry;
		this.translate(jcTry);
	}


}

