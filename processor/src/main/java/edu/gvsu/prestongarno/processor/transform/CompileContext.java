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
 */

package edu.gvsu.prestongarno.processor.transform;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;

import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/** **************************************************
 * Dynamic-MVP - edu.gvsu.prestongarno.transformation - by Preston Garno on 4/11/17
 * ***************************************************/
public class CompileContext {
	
	Map<String, ArrayList<JCTree>> sourceMap;
	JavacProcessingEnvironment env;
	Trees trees;
	TreeMaker mod;
	Names names;
	JavacElements elements;
	Symtab symtab;
	Symbol.ClassSymbol translateSym;
	final Symbol.PackageSymbol ROOT_PKG;
	
	private static CompileContext instance;
	
	private CompileContext(JavacProcessingEnvironment environment){
		env = environment;
		// symbol for the "TranslateView" interface that returns a presenter
		translateSym = env.getElementUtils().getTypeElement("edu.gvsu.prestongarno.annotations.TranslateView");
		trees = Trees.instance(environment);
		mod = TreeMaker.instance(environment.getContext());
		elements = JavacElements.instance(environment.getContext());
		names = Names.instance(environment.getContext());
		symtab = Symtab.instance(environment.getContext());
		sourceMap = getSourceMap(environment);
		ROOT_PKG = symtab.rootPackage;
	}
	
	public static CompileContext getInstance() {
		return instance;
	}
	
	public static CompileContext createInstance(JavacProcessingEnvironment environment) {
		return instance == null ? instance = new CompileContext(environment) : instance;
	}


	public JCTree getTree(Element element) {
		return ((JCTree) trees.getTree(element));
	}

	/*****************************************
	 * Get all AST nodes through reflection
	 * @param environment the javac environment
	 * @return a map of all compiling classes and all nodes in the AST for each class
	 ****************************************/
	@SuppressWarnings("unchecked")
	private Map<String, ArrayList<JCTree>> getSourceMap(JavacProcessingEnvironment environment) {
		Context context = environment.getContext();
		Field f;
		try {
			f = context.getClass().getDeclaredField("ht");
			f.setAccessible(true);
			
			//noinspection unchecked
			Map<Context.Key, Object> map = (Map<Context.Key, Object>) f.get(context);
			
			final Log logger = map.entrySet().stream()
					.filter(entry -> entry.getValue() instanceof Log)
					.map(entry -> ((Log) entry.getValue()))
					.findAny().orElseThrow(IllegalStateException::new);
			
			f = logger.getClass()
					.getSuperclass()
					.getDeclaredField("sourceMap");
			f.setAccessible(true);
			
			Map<JavaFileObject, DiagnosticSource> sourceMap =
					(Map<JavaFileObject, DiagnosticSource>) f.get(logger);
			
			Map<String, ArrayList<JCTree>> classes = new HashMap<>();
			
			for (DiagnosticSource ds : sourceMap.values()) {
				String className = ds.getFile().getName();
				
				f = ds.getClass().getDeclaredField("endPosTable");
				f.setAccessible(true);
				EndPosTable obj = (EndPosTable) f.get(ds);
				f = obj.getClass().getDeclaredField("endPosMap");
				f.setAccessible(true);
				IntHashTable table = (IntHashTable) f.get(obj);
				f = table.getClass().getDeclaredField("objs");
				f.setAccessible(true);
				
				Object[] source = (Object[]) f.get(table);
				
				ArrayList<JCTree> trees = new ArrayList<JCTree>();
				
				for (int i = 0; i < source.length; i++) {
					trees.add((JCTree) source[i]);
				}
				classes.put(className, trees);
			}
			
			return classes;
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
}
