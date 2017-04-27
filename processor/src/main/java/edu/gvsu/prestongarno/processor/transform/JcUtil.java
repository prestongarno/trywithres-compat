/*
 *       Copyright (c) 2017.  Preston Garno
 *
 *        Licensed under the Apache License, Version 2.0 (the "License");
 *        you may not use this file except in compliance with the License.
 *        You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *        Unless required by applicable law or agreed to in writing, software
 *        distributed under the License is distributed on an "AS IS" BASIS,
 *        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *        See the License for the specific language governing permissions and
 *        limitations under the License.
 ****************************************/

package edu.gvsu.prestongarno.processor.transform;

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static com.sun.tools.javac.code.Symbol.*;
import static com.sun.tools.javac.code.Type.ClassType;
import static com.sun.tools.javac.code.Type.MethodType;
import static com.sun.tools.javac.tree.JCTree.JCClassDecl;
import static com.sun.tools.javac.tree.JCTree.JCMethodDecl;


/**
 * *************************************************
 * Dynamic-MVP - edu.gvsu.prestongarno - by Preston Garno on 3/28/17
 * <p>
 * Helper methods to hijack the AST
 ***************************************************/
public class JcUtil {

	private static final Method memberEnterMethod;
	private static final Method classEnterMethod;

	static {
		Method memberEnterTemp = null;
		Method tmpClassEnter   = null;
		try {
			memberEnterTemp = MemberEnter.class
					.getDeclaredMethod("memberEnter", JCTree.class, Env.class);
			tmpClassEnter = Enter.class.getDeclaredMethod("classEnv", JCClassDecl.class, Env.class);
			tmpClassEnter.setAccessible(true);
		} catch (Exception ex) {}
		memberEnterMethod = memberEnterTemp;
		classEnterMethod = tmpClassEnter;
	}

	/*****************************************
	 * Enter any type of tree into the enclosing syntax tree of any
	 * class with one method call when providing the environment!
	 * @param tree the member
	 * @param envir the member environment
	 ****************************************/
	public static void inject(MemberEnter instance, JCTree tree, Env<AttrContext> envir) {
		try {
			memberEnterMethod.invoke(instance, tree, envir);
		} catch (Exception e) {}
	}

	public static Env<AttrContext> injectClassDefinition(Context context, JCClassDecl classDecl, Env<AttrContext> env) {
		try {
			return (Env<AttrContext>) classEnterMethod.invoke(Enter.instance(context), classDecl, env);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		throw new RuntimeException();
	}

	/*****************************************
	 * Workaround for getting type environments by manually
	 * loading the package private TypeEnvs class which is
	 * loaded initially on a differend classloader
	 ****************************************/
	@SuppressWarnings("unchecked")
	public static Map<TypeSymbol, Env<AttrContext>> getEnvs(Context c) {
		try {
			Field f = Context.class.getDeclaredField("ht");
			f.setAccessible(true);
			Map<Context.Key, Object> m = (Map<Context.Key, Object>) f.get(c);
			Object o = m.entrySet().stream()
					.filter(entry -> entry.getValue().getClass().getName().equals("com.sun.tools.javac.comp.TypeEnvs"))
					.findAny().get().getValue();

			Field _map_field = o.getClass().getDeclaredField("map");
			_map_field.setAccessible(true);
			Map<TypeSymbol, Env<AttrContext>> _stolen = (Map<TypeSymbol, Env<AttrContext>>) _map_field.get(o);

			return new HashMap<>(_stolen);

		} catch (Exception e) { e.printStackTrace(); }
		throw new IllegalStateException("error acquiring the types & type environments...");
	}


	public static JCClassDecl injectInterface(
			Context context,
			Env<AttrContext> env,
			JCClassDecl classDecl,
			ClassSymbol interfaceToAdd,
			List<JCMethodDecl> methods) {

		TreeMaker maker = TreeMaker.instance(context);

		//TODO make sure this is an interface
		java.util.List<Symbol> interfaceSymbols = SymbolScopeUtil.getMembersList(interfaceToAdd);

		classDecl.implementing = classDecl.implementing.append(maker.Ident(interfaceToAdd));

		ClassSymbol symbol = classDecl.sym;
		ClassType   TYPE   = (ClassType) symbol.type;
		TYPE.all_interfaces_field = TYPE.all_interfaces_field.append(interfaceToAdd.type);
		TYPE.interfaces_field = TYPE.interfaces_field.append(interfaceToAdd.type);

		for (JCMethodDecl tr : methods) {
			//classDecl = resolveSignature(context,classDecl, tr);
			inject(MemberEnter.instance(context), tr, env);
		}

		// change the interfaces on the type
/*		TYPE.tsym = symbol;
		classDecl.type = TYPE;*/
		return classDecl;
	}

	public static JCClassDecl resolveSignature(Context context, JCClassDecl classDecl, JCMethodDecl method) {

		classDecl.defs = classDecl.defs.append(method);

		ClassSymbol symbol = classDecl.sym;

		method.sym = fixMethodMirror(
				context,
				symbol,
				method.getModifiers().flags,
				method.name,
				List.from(method.getParameters().stream()
									 .map(decl1 -> decl1.type)
									 .collect(Collectors.toList())),
				method.getReturnType().type);

		//method.sym.owner = classDecl.sym;

		return classDecl;
	}


	/*****************************************
	 * Fixes other references/scopes to this method without closing the class for modification
	 * @param cs
	 * @param access
	 * @param methodName
	 * @param paramTypes
	 * @param returnType
	 ****************************************/
	private static MethodSymbol fixMethodMirror(
			Context context,
			ClassSymbol cs,
			long access,
			Name methodName,
			List<Type> paramTypes,
			Type returnType) {

		final MethodType methodSym = new MethodType(paramTypes,
																  returnType,
																  List.nil(),
																  Symtab.instance(context).methodClass);

		MethodSymbol methodSymbol = new MethodSymbol(access,
																	methodName,
																	methodSym,
																	cs);

		JcUtil.SymbolScopeUtil.enter(cs, methodSymbol);

		return methodSymbol;
	}

	/*****************************************
	 * This injection method doesn't work as well as the one above
	 ****************************************/
	static class SymbolScopeUtil {
		private static final Field  membersField;
		private static final Field  scopeEntryArray;
		private static final Method removeMethod;
		private static final Method enterMethod;

		static {
			Field  f = null;
			Method r = null;
			Method e = null;
			Field  d = null;
			try {
				f = ClassSymbol.class.getField("members_field");
				r = f.getType().getMethod("remove", Symbol.class);
				e = f.getType().getMethod("enter", Symbol.class);
				d = Scope.class.getDeclaredField("table");
			} catch (Exception ex) {}
			membersField = f;
			removeMethod = r;
			enterMethod = e;
			scopeEntryArray = d;
		}

		static void remove(ClassSymbol from, Symbol toRemove) {
			if (from == null) return;
			try {
				Scope scope = getField(membersField, Scope.class, from);
				removeMethod.invoke(scope, toRemove);
			} catch (Exception e) { e.printStackTrace();}
		}

		static void enter(ClassSymbol from, Symbol toEnter) {
			if (from == null) return;
			try {
				Scope scope = getField(membersField, Scope.class, from);
				enterMethod.invoke(scope, toEnter);
			} catch (Exception e) {e.printStackTrace();}
		}

		static java.util.List<Symbol> getMembersList(Symbol symbol) {
			Scope         c       = getField(membersField, Scope.class, symbol);
			Scope.Entry[] entries = getField(scopeEntryArray, Scope.Entry[].class, c);
			return Arrays
					.stream(entries)
					.filter(Objects::nonNull)
					.map(entry -> entry.sym)
					.collect(Collectors.toCollection(ArrayList::new));
		}

		@SuppressWarnings("unchecked")
		private static <V> V getField(Field field, Class<V> type, Object object) {
			try {
				field.setAccessible(true);
				return (V) field.get(object);
			} catch (IllegalAccessException e) { e.printStackTrace(); }
			throw new IllegalArgumentException("No such Field " + field.getName() + " of type " + type);
		}
	}

}
