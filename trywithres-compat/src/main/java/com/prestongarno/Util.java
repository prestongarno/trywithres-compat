package com.prestongarno;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.util.*;

import static com.sun.tools.javac.tree.JCTree.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.sun.tools.javac.code.Symbol.*;

/**
 * Created by preston on 4/29/17.
 ****************************************/
public class Util {

	private static final Method resolveBinOpMethod;
	private static final Field  _context_component_map_field;

	static {
		Method tmpClassEnter         = null;
		Method tmpBinopRes           = null;
		Field  tmp_context_component = null;
		try {
			tmpClassEnter = Enter.class.getDeclaredMethod("classEnv", JCClassDecl.class, Env.class);
			tmp_context_component = Context.class.getDeclaredField("ht");
			tmpBinopRes = Resolve.class.getDeclaredMethod("resolveBinaryOperator",
																		 JCDiagnostic.DiagnosticPosition.class,
																		 Tag.class, Env.class,
																		 Type.class, Type.class);
			tmpBinopRes.setAccessible(true);
			tmp_context_component.setAccessible(true);
			tmp_context_component.setAccessible(true);
			tmpClassEnter.setAccessible(true);
		} catch (Exception ex) { throw new RuntimeException("Error accessing javac internal methods!"); }
		resolveBinOpMethod = tmpBinopRes;
		_context_component_map_field = tmp_context_component;
	}

	static Symbol resolveBinaryOperator(
			Resolve resolve,
			JCDiagnostic.DiagnosticPosition make_pos,
			Tag optag,
			Env<AttrContext> attrEnv,
			Type type,
			Type type1) {

		try {
			return (Symbol) resolveBinOpMethod.invoke(resolve, make_pos, optag, attrEnv, type, type1);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("Failure resolving binary operator!");
		}
	}

	/*****************************************
	 * Workaround for getting type environments by manually
	 * loading the package private TypeEnvs class which is
	 * loaded initially on a differend classloader
	 ****************************************/
	@SuppressWarnings("unchecked")
	static Map<TypeSymbol, Env<AttrContext>> getEnvs(Context c) {
		try {
			Map<Context.Key, Object> m = (Map<Context.Key, Object>) _context_component_map_field.get(c);
			Object o = m.entrySet().stream()
					.filter(entry -> entry.getValue().getClass().getName().equals("com.sun.tools.javac.comp.TypeEnvs"))
					.findAny().get().getValue();

			Field _map_field = o.getClass().getDeclaredField("map");
			_map_field.setAccessible(true);
			Map<Symbol.TypeSymbol, Env<AttrContext>> _stolen = (Map<Symbol.TypeSymbol, Env<AttrContext>>) _map_field.get(o);

			return new HashMap<>(_stolen);

		} catch (Exception e) {
			throw new RuntimeException("Error acquiring the types & type environments");
		}
	}

	/*****************************************
	 * Coerce constant type to target type.
	 *
	 * @param etype The source type of the coercion,
	 *              which is assumed to be a constant type compatible with
	 *              ttype.
	 * @param ttype The target type of the coercion.
	 ****************************************/
	static Type coerce(Symtab syms, Type etype, Type ttype) {
		if (etype.tsym.type == ttype.tsym.type)
			return etype;
		if (etype.isNumeric()) {
			Object n = etype.constValue();
			switch (ttype.getTag()) {
				case BYTE: return syms.byteType.constType((byte) intValue(n));
				case CHAR: return syms.charType.constType((char) intValue(n));
				case SHORT: return syms.shortType.constType((short) intValue(n));
				case INT: return syms.intType.constType(intValue(n));
				case LONG: return syms.longType.constType(longValue(n));
				case FLOAT: return syms.floatType.constType(floatValue(n));
				case DOUBLE: return syms.doubleType.constType(doubleValue(n));
			}
		}
		return ttype;
	}

	private static int intValue(Object x) { return ((Number) x).intValue(); }

	private static long longValue(Object x) { return ((Number) x).longValue(); }

	private static float floatValue(Object x) { return ((Number) x).floatValue(); }

	private static double doubleValue(Object x) { return ((Number) x).doubleValue(); }

}
