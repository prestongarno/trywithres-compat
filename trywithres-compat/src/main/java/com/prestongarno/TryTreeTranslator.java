package com.prestongarno;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.*;

import static com.sun.tools.javac.code.Flags.FINAL;
import static com.sun.tools.javac.code.Flags.SYNTHETIC;
import static com.sun.tools.javac.code.Symbol.*;
import static com.sun.tools.javac.code.TypeTag.*;
import static com.sun.tools.javac.tree.JCTree.*;
import static com.sun.tools.javac.tree.JCTree.Tag.NE;
import static com.sun.tools.javac.util.JCDiagnostic.*;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javadoc.Messager;

import javax.tools.Diagnostic;
import java.util.*;

/****************************************
 * @author Preston Garno
 *
 * Translates the syntax tree
 ****************************************/
class TryTreeTranslator extends TreeTranslator {

	private final Context            context;
	private       TreeMaker          make;
	private final Symtab             symtab;
	private final Types              types;
	private final Names              names;
	private final Resolve            rs;
	private final Target             target;
	private       Env<AttrContext>   attrEnv;
	private       DiagnosticPosition make_pos;
	private       EndPosTable        endPosTable;

	private final Map<TypeSymbol, Env<AttrContext>> envs;

	/*****************************************
	 * A scope containing all unnamed resource variables/saved
	 * exception variables for translated TWR blocks
	 ****************************************/
	private Scope twrVars;

	/*****************************************
	 * The current method symbol.
	 ****************************************/
	private MethodSymbol currentMethodSym;

	TryTreeTranslator(Context context) {
		this.context = context;
		// util
		make = TreeMaker.instance(context);
		symtab = Symtab.instance(context);
		names = Names.instance(context);
		rs = Resolve.instance(context);
		target = Target.instance(context);
		types = Types.instance(context);
		twrVars = new Scope(symtab.noSymbol);
		this.envs = Util.getEnvs(context);
	}

	public void translateClass(JCTree o) {
		if (o instanceof JCClassDecl) {
			this.attrEnv = envs.get(((JCClassDecl) o).sym);
		}
		endPosTable = attrEnv == null ? endPosTable
												: attrEnv.toplevel.endPositions;
		try {
			this.translate(o);
		} catch (Throwable t) {
			JavacProcessingEnvironment.instance(context).getMessager()
					.printMessage(Diagnostic.Kind.ERROR,
									  "An unexpected error occurred processing the following: " +
											  o + "\n\nPlease file a report including both the source and following stacktrace at " +
											  "https://github.com/prestongarno/trywithres-compat/issues. Thank you.\n\n");
			t.printStackTrace();
		}
	}

	/*****************************************
	 * Visitor method: Translate a single node.
	 * Attach the source position from the old tree to its replacement tree.
	 ****************************************/
	public <T extends JCTree> T translate(T tree) {
		if (tree == null) {
			return null;
		} else {
			make_at(tree.pos());
			T result = super.translate(tree);
			if (endPosTable != null && result != tree) {
				endPosTable.replaceTree(tree, result);
			}
			return result;
		}
	}

	TreeMaker make_at(DiagnosticPosition pos) {
		make_pos = pos;
		return make.at(pos);
	}

	@Override
	public void visitTry(JCTry tree) {
		if (tree.resources.nonEmpty()) {
			result = transformTry(tree);
			return;
		}
		boolean hasBody     = tree.body.getStatements().nonEmpty();
		boolean hasCatchers = tree.catchers.nonEmpty();
		boolean hasFinally = tree.finalizer != null &&
				tree.finalizer.getStatements().nonEmpty();

		if (!hasCatchers && !hasFinally) {
			result = translate(tree.body);
			return;
		}
		if (!hasBody) {
			if (hasFinally) result = translate(tree.finalizer);
			else result = translate(tree.body);
			return;
		}
		super.visitTry(tree);
	}

	private JCTree transformTry(JCTry tree) {
		make_at(tree.pos());
		twrVars = twrVars.dup();
		JCBlock twrBlock = makeTwrBlock(tree.resources, tree.body, tree.finallyCanCompleteNormally, 0);
		if (tree.catchers.isEmpty() && tree.finalizer == null)
			result = translate(twrBlock);
		else
			result = translate(make.Try(twrBlock, tree.catchers, tree.finalizer));
		twrVars = twrVars.leave();
		return result;
	}

	private JCBlock makeTwrBlock(
			List<JCTree> resources, JCBlock block,
			boolean finallyCanCompleteNormally, int depth) {
		if (resources.isEmpty())
			return block;

		// Add resource declaration or expression to block statements
		ListBuffer<JCStatement> stats    = new ListBuffer<>();
		JCTree                  resource = resources.head;
		JCExpression            expr;
		if (resource instanceof JCVariableDecl) {
			JCVariableDecl var = (JCVariableDecl) resource;
			if (var.sym == null) var.sym = new VarSymbol(0, var.name, var.type == null ? Type.noType : var.type,
																		var.type == null ? symtab.noSymbol : var.type.tsym);
			expr = make.Ident(var.sym);
			expr.setType(resource.type);
			stats.add(var);
		} else {
			Assert.check(resource instanceof JCExpression);
			VarSymbol syntheticTwrVar =
					new VarSymbol(SYNTHETIC | FINAL,
									  makeSyntheticName(names.fromString("twrVar" + depth), twrVars),
									  (resource.type.hasTag(BOT)) ?
									  symtab.autoCloseableType : resource.type,
									  currentMethodSym);
			twrVars.enter(syntheticTwrVar);
			JCVariableDecl syntheticTwrVarDecl =
					make.VarDef(syntheticTwrVar, (JCExpression) resource);
			expr = make.Ident(syntheticTwrVar);
			stats.add(syntheticTwrVarDecl);
		}

		// Add primaryException declaration
		VarSymbol primaryException =
				new VarSymbol(SYNTHETIC,
								  makeSyntheticName(names.fromString("primaryException" +
																						 depth), twrVars),
								  symtab.throwableType,
								  currentMethodSym);
		twrVars.enter(primaryException);
		JCVariableDecl primaryExceptionTreeDecl = make.VarDef(primaryException, makeNull());
		stats.add(primaryExceptionTreeDecl);

		// Create catch clause that saves exception and then rethrows it
		VarSymbol param =
				new VarSymbol(FINAL | SYNTHETIC,
								  names.fromString("t" +
																 target.syntheticNameChar()),
								  symtab.throwableType,
								  currentMethodSym);
		JCVariableDecl paramTree   = make.VarDef(param, null);
		JCStatement    assign      = make.Assignment(primaryException, make.Ident(param));
		JCStatement    rethrowStat = make.Throw(make.Ident(param));
		JCBlock        catchBlock  = make.Block(0L, List.of(assign, rethrowStat));
		JCCatch        catchClause = make.Catch(paramTree, catchBlock);

		int oldPos = make.pos;
		make.at(TreeInfo.endPos(block));
		JCBlock finallyClause = makeFinClause(primaryException, expr);
		make.at(oldPos);
		JCTry outerTry = make.Try(makeTwrBlock(resources.tail, block,
															finallyCanCompleteNormally, depth + 1),
										  List.of(catchClause),
										  finallyClause);
		outerTry.finallyCanCompleteNormally = finallyCanCompleteNormally;
		stats.add(outerTry);
		return make.Block(0L, stats.toList());
	}

	private JCBlock makeFinClause(Symbol primaryException, JCExpression resource) {
		// primaryException.addSuppressed(catchException);
		VarSymbol catchException =
				new VarSymbol(SYNTHETIC, make.paramName(2),
								  symtab.throwableType,
								  currentMethodSym);
		JCStatement addSuppressionStatement =
				make.Exec(makeCall(make.Ident(primaryException),
										 names.addSuppressed,
										 List.of(make.Ident(catchException))));

		// try { resource.close(); } catch (e) { primaryException.addSuppressed(e); }
		JCBlock        tryBlock           = make.Block(0L, List.of(generateCloseInvoke(resource)));
		JCVariableDecl catchExceptionDecl = make.VarDef(catchException, null);
		JCBlock        catchBlock         = make.Block(0L, List.of(addSuppressionStatement));
		List<JCCatch>  catchClauses       = List.of(make.Catch(catchExceptionDecl, catchBlock));
		JCTry          tryTree            = make.Try(tryBlock, catchClauses, null);
		tryTree.finallyCanCompleteNormally = true;

		// if (primaryException != null) {try...} else resourceClose;
		JCIf closeIfStatement = make.If(makeNonNullCheck(make.Ident(primaryException)),
												  tryTree,
												  generateCloseInvoke(resource));

		return make.Block(0L, List.of(make.If(makeNonNullCheck(resource),
														  closeIfStatement,
														  null)));
	}

	private JCStatement generateCloseInvoke(JCExpression resource) {
		// convert to AutoCloseable if needed
		if (types.asSuper(resource.type, symtab.autoCloseableType.tsym) == null) {
			resource = (JCExpression) convert(resource, symtab.autoCloseableType);
		}
		// create resource.close() method invocation
		JCExpression resourceClose = makeCall(resource, names.close, List.nil());
		return make.Exec(resourceClose);
	}

	private JCMethodInvocation makeCall(JCExpression left, Name name, List<JCExpression> args) {
		Assert.checkNonNull(left.type);
		Symbol funcsym = lookupMethod(make_pos, name, left.type,
												TreeInfo.types(args));
		return make.App(make.Select(left, funcsym), args);
	}

	private JCTree convert(JCTree tree, Type pt) {
		if (tree.type == pt || tree.type.hasTag(BOT))
			return tree;
		JCTree result = make_at(tree.pos()).TypeCast(make.Type(pt), (JCExpression) tree);
		result.type = (tree.type.constValue() != null) ? Util.coerce(symtab, tree.type, pt)
																	  : pt;
		return result;
	}

	public void visitMethodDef(JCMethodDecl tree) {
		super.visitMethodDef(tree);
		currentMethodSym = tree.sym;
	}

	/*****************************************
	 * Create a fresh synthetic name within a given scope - the unique name is
	 * obtained by appending '$' chars at the end of the name until no match
	 * is found.
	 *
	 * @param name base name
	 * @param s    scope in which the name has to be unique
	 * @return fresh synthetic name
	 ****************************************/
	private Name makeSyntheticName(Name name, Scope s) {
		do {
			name = name.append(
					target.syntheticNameChar(),
					names.empty);
		} while (lookupSynthetic(name, s) != null);
		return name;
	}

	/*****************************************
	 * Look up a synthetic name in a given scope.
	 *
	 * @param s    The scope.
	 * @param name The name.
	 ****************************************/
	private Symbol lookupSynthetic(Name name, Scope s) {
		Symbol sym = s.lookup(name).sym;
		return (sym == null || (sym.flags() & SYNTHETIC) == 0) ? null : sym;
	}

	/*****************************************
	 * Make an attributed tree representing a literal. This will be an
	 * Ident node in the case of boolean literals, a Literal node in all
	 * other cases.
	 *
	 * @param type  The literal's type.
	 * @param value The literal's value.
	 ****************************************/
	private JCExpression makeLit(Type type, Object value) {
		return make.Literal(type.getTag(), value).setType(type.constType(value));
	}

	/*****************************************
	 * Make an attributed tree representing null.
	 ****************************************/
	private JCExpression makeNull() {
		return makeLit(symtab.botType, null);
	}

	private JCExpression makeNonNullCheck(JCExpression expression) {
		return makeBinary(NE, expression, makeNull());
	}

	/*****************************************
	 * Make an attributed binary expression.
	 *
	 * @param optag The operators tree tag.
	 * @param lhs   The operator's left argument.
	 * @param rhs   The operator's right argument.
	 ****************************************/
	private JCBinary makeBinary(JCTree.Tag optag, JCExpression lhs, JCExpression rhs) {
		JCBinary tree = make.Binary(optag, lhs, rhs);
		tree.operator = Util.resolveBinaryOperator(Resolve.instance(context), make_pos, optag, attrEnv, lhs.type, rhs.type);
		tree.type = tree.operator.type.getReturnType();
		return tree;
	}

	/*****************************************
	 * Look up a method in a given scope.
	 ****************************************/
	private MethodSymbol lookupMethod(DiagnosticPosition pos, Name name, Type qual, List<Type> args) {
		return rs.resolveInternalMethod(pos, attrEnv, qual, name, args, List.nil());
	}
}
