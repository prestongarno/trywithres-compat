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
 *        limitations under the License
 *        .
 */

package edu.gvsu.prestongarno.processor;

import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.TreeTranslator;

import java.util.Map;

import static com.sun.tools.javac.code.Symbol.*;

public class TryResPlugin extends TreeTranslator {

	private final Map<TypeSymbol, Env<AttrContext>> map;

	public TryResPlugin(Map<TypeSymbol, Env<AttrContext>> map) {
		this.map = map;

		//System.out.println(javacTask.getElements());
		//System.out.println("COMPILING!");
		//this.task = javacTask;
		// get context --> start hacking away at the AST
		//if (javacTask instanceof BasicJavacTask)
			//System.out.println("IT EES!!!" + javacTask.getClass().toGenericString());
		//HashMap<Symbol.TypeSymbol, Env<AttrContext>> map;
		Object                                       _TypeEnvs = null;

/*		map = (HashMap<Symbol.TypeSymbol, Env<AttrContext>>)
				JcUtil.getEnvs(
						((BasicJavacTask) javacTask)
								.getContext().get(MemberEnter.class));*/

		map.entrySet().forEach(System.out::println);
/*		try {
			Class<?> aClass = javacTask.getClass()
					.getClassLoader().loadClass("com.sun.tools.javac.comp.TypeEnvs");
			_TypeEnvs = ((BasicJavacTask) javacTask).getContext()
					.get(aClass);

			Field envField = _TypeEnvs.getClass().getField("map");
			envField.setAccessible(true);
			//noinspection unchecked
			map = (HashMap<Symbol.TypeSymbol, Env<AttrContext>>) envField.get(_TypeEnvs);
		} catch (Exception e) {e.printStackTrace();}*/


	}

	public void start() {
		map.entrySet().forEach(System.out::print);
	}
}