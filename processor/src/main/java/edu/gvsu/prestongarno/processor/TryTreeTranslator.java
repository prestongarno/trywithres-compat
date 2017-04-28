package edu.gvsu.prestongarno.processor;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;

import static com.sun.tools.javac.tree.JCTree.*;

import com.sun.tools.javac.tree.TreeCopier;
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

	/**true=print the before->after contents of the AST for each try-with-res
	 */
	private final boolean VERBOSE = true;

	private final Context    context;
	private final TreeMaker  make;
	private final TreeCopier copier;
	private final Symtab symtab;
	private final Names names;

	private final Type _runt_exception_type;
	private final Type _throwable_exception_type;

	TryTreeTranslator(Context context) {
		this.context = context;
		// util
		make = TreeMaker.instance(context);
		copier = new TreeCopier(make);
		symtab = Symtab.instance(context);
		names = Names.instance(context);

		// useful constant types/sym
		_runt_exception_type = symtab.runtimeExceptionType;
		_throwable_exception_type = symtab.throwableType;
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
	 * This is the translation when running set_0
	 *
	 *  ======================>Before<======================
	 *
	 *  try (final TestAcloseable something = new TestAcloseable(false, false);) {
	 *      something.doRiskyThings();
	 *  }
	 *
	 *  ======================>After<======================
	 *
	 *  try {
	 *      java.lang.RuntimeException _Scope_Runtime_Exception = new RuntimeException();
	 *      TestAcloseable something = new TestAcloseable(false, false);
	 *      try {
	 *          something.doRiskyThings();
	 *      } catch (java.lang.Throwable _nest_catch_throwable) {
	 *          _Scope_Runtime_Exception.initCause(_nest_catch_throwable);
	 *          if (something != null) {
	 *              try {
	 *                  something.close();
	 *                  something = null;
	 *              } catch (java.lang.Throwable _inner_inner_throwable_suppressed) {
	 *                  _Scope_Runtime_Exception.addSuppressed(_inner_inner_throwable_suppressed);
	 *              }
	 *          }
	 *      } finally {
	 *          if (something != null) {
	 *              try {
	 *                  something.close();
	 *                  something = null;
	 *              } catch (java.lang.Throwable _inner_inner_throwable_suppressed) {
	 *                  _Scope_Runtime_Exception.addSuppressed(_inner_inner_throwable_suppressed);
	 *              }
	 *          }
	 *          if (_Scope_Runtime_Exception.getCause() != null || _Scope_Runtime_Exception.getSuppressed().length > 0) {
	 *              throw _Scope_Runtime_Exception;
	 *          }
	 *      }
	 *  }
	 *
	 *  ======================>======<======================
	 *
	 *  	 * @param jcTry the try statement to change
	 ****************************************/
	private void change(JCTry jcTry) {
		if(VERBOSE) {
			System.out.println("======================>Before<======================");
			System.out.println(jcTry);
		}
		result = jcTry = (JCTry) copier.copy(jcTry); //dnt know why but this is useless without it
		List<JCStatement> resList = List.nil();
		for (JCTree vd : jcTry.resources) {
			((JCVariableDecl) vd).mods.flags = 0;
			resList = resList.prepend((JCStatement) vd);
		}

		jcTry.resources = List.nil(); //clear the resources from the other try statement
		jcTry.resources.clear();

		JCBlock _nested_body = jcTry.body; // the body shifted one block inside a try
		jcTry.body = null;

		// exception handling scope needed for the finally blck
		Name _name_exception_runtime = names.fromString("_Scope_Runtime_Exception");
		JCVariableDecl _exception_generic = // varsymbol defs: flag, names, type, symbol
				make.VarDef(new Symbol.VarSymbol(0, _name_exception_runtime, this._runt_exception_type, _runt_exception_type.tsym),
								make.NewClass(null, List.nil(), make.Ident(_runt_exception_type.tsym), List.nil(), null));

		// the inner catch block construction
		Name _nest_catch_throwable = names.fromString("_nest_catch_throwable");
		// Catch any exception
		JCVariableDecl param = make.Param(_nest_catch_throwable, _throwable_exception_type, _throwable_exception_type.tsym);

		// closing all of the resourcces with a null check
		List<JCStatement> _close_invokations = List.nil();

		// the method invokations to close resources.  Wraps each close() call in a null check,
		// a try/catch, and suppresses any exceptions that are thrown before moving to the next and throwing
		for (JCStatement ex : resList) {
			JCVariableDecl     varDec            = ((JCVariableDecl) ex);
			JCFieldAccess      access            = make.Select(make.Ident(varDec.name), names.fromString("close"));
			JCMethodInvocation _inv_close        = make.Apply(List.nil(), access, List.nil());
			JCBinary           _check_null       = make.Binary(Tag.NE, make.Ident(((JCVariableDecl) ex).name), make.Literal(TypeTag.BOT, null));
			JCParens           parens            = make.Parens(_check_null); // conditional
			JCStatement        exec              = make.Exec(_inv_close);
			JCStatement        _nullify_resource = make.Exec(make.Assign(make.Ident(varDec.name), make.Ident(names.fromString("null"))));
			// surround each close() method with a try catch
			Name _inner_inner_exc_close_name = names.fromString("_inner_inner_throwable_suppressed");
			JCVariableDecl _throwable_fromclose_call = make.Param(_inner_inner_exc_close_name,
																					_throwable_exception_type,
																					_throwable_exception_type.tsym);
			JCFieldAccess      _put_suppressed_close_exc = make.Select(make.Ident(_exception_generic.sym), names.fromString("addSuppressed"));
			JCMethodInvocation _add_suppress_invoke      = make.Apply(null, _put_suppressed_close_exc, List.of(make.Ident(_inner_inner_exc_close_name)));
			JCCatch            _catch_exc_on_close       = make.Catch(_throwable_fromclose_call, make.Block(0, List.of(make.Exec(_add_suppress_invoke))));

			JCTry             _try_inner_close = make.Try(List.nil(), make.Block(0, List.of(exec, _nullify_resource)), List.of(_catch_exc_on_close), null);
			List<JCStatement> stats            = List.of(_try_inner_close);
			JCIf              _not_null_check  = make.If(parens, make.Block(0, stats), null);
			_close_invokations = _close_invokations.append(_not_null_check);
		}

		// make a generalized catch statement that closes all, suppresses exceptions and throws runtime exceptions
		JCCatch _nested_catch = make.Catch(param, make.Block(0, List.nil()));
		_nested_catch.body.stats = _nested_catch.body.stats.prependList(_close_invokations);

		// make a finally block doing the same thins
		//_nested_catch.body.stats.
		JCBlock _nested_fin_block = make.Block(0, _close_invokations);

		// make the try block; Params are: 	List<JCTree> resources, JCBlock body, List<JCCatch> catchers, JCBlock finalizer)
		JCTry _shifted_try_block = make.Try(List.nil(), _nested_body, List.of(_nested_catch), _nested_fin_block);

		// +3 down: first set the cause of the exception as the initial one in the user's code that caused the exception
		JCFieldAccess      _add_suppress_exc    = make.Select(make.Ident(_name_exception_runtime), names.fromString("initCause"));
		JCMethodInvocation _add_suppress_invoke = make.Apply(null, _add_suppress_exc, List.of(make.Ident(_nest_catch_throwable)));
		_nested_catch.body.stats = _nested_catch.body.stats.prepend(make.Exec(_add_suppress_invoke));//used to assign

		// check if there were any suppressed exceptions from the try block or closing resources in the catch/finally block
		JCFieldAccess _get_suppressed = make.Select(make.Ident(_exception_generic.sym), names.fromString("getSuppressed"));
		// invoke_getSuppressed()
		JCMethodInvocation _inv_get_suppressed = make.Apply(null, _get_suppressed, List.nil());
		// invoke_getSuppressed().length
		JCFieldAccess _array_type_length = make.Select(_inv_get_suppressed, symtab.lengthVar.name);
		// Binary getCause != null
		JCMethodInvocation _inv_get_cause_ = make.Apply(null, make.Select(make.Ident(_exception_generic), names.fromString("getCause")), List.nil());
		JCBinary           _cause_not_null = make.Binary(Tag.NE, _inv_get_cause_, make.Literal(TypeTag.BOT, null));
		// Binary length_of_exceptions > 0
		JCBinary _compare_suppressed_zero = make.Binary(Tag.GT, _array_type_length, make.Literal(TypeTag.INT, "0"));
		// Binary OR cause exists <> has suppressed
		JCBinary _bin_or_exceptions = make.Binary(Tag.OR, _cause_not_null, _compare_suppressed_zero);
		// if(suppressed > 0) throw RuntimeException (wrapping the suppressed ones)
		JCIf _if_suppressed_exists = make.If(make.Parens(_bin_or_exceptions), make.Block(0, List.of(make.Throw(make.Ident(_exception_generic.sym)))), null);
		// append this to the nested finally block
		_nested_fin_block.stats = _nested_fin_block.stats.append(_if_suppressed_exists);

		jcTry.body = make.Block(0, List.of(_exception_generic)); // construct the thing
		jcTry.body.stats = jcTry.body.stats.appendList(resList); // add in all of the resource declarations
		jcTry.body.stats = jcTry.body.stats.append(_shifted_try_block);

		result = jcTry; // set result
		this.translate(jcTry); // translate again to make sure everything's updated

		if(VERBOSE) {
			System.out.println("======================>After<======================");
			System.out.println(jcTry);
			System.out.println("======================>======<======================");
		}
	}
}

