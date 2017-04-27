package edu.gvsu.prestongarno.testing;

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

import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.tools.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.tools.JavaFileObject;

import java.io.IOException;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;


/**
 * *************************************************
 * Dynamic-MVP - edu.gvsu.prestongarno.sourcegentests.TestUtil - by Preston Garno on 3/10/17
 ****************************************/
public class CompilerUtil {


	private static final String MAGIC_NUMBER = "CAFEBABE";

	private static final String TEST_CLASS_SET_DIRECTORY = "/src/test/resources/SampleSets/";

	/*****************************************
	 * Loads a raw text source files into memory for compilation
	 * @param index
	 * @return
	 * @throws IOException
	 ****************************************/
	public static List<JavaFileObject> loadClassSet(int index) throws IOException {

		return Files.list((new File(System.getProperty("user.dir") + TEST_CLASS_SET_DIRECTORY + "/set_" + index)
				.toPath()))
				.map(Path::toFile)
				.flatMap(CompilerUtil::flattenDir)
				.filter(File::canRead)
				.filter(o -> {
					String[] ch = o.getAbsoluteFile().getAbsolutePath().split("/");
					return !ch[ch.length - 1].contains(".swp");
				})
				.map(File::toPath)
				.map(CompilerUtil.logPath::print)
				.map(CompilerUtil.converter::toJavaFileObject)
				.collect(toList());
	}

	private static Stream<? extends File> flattenDir(File dir) {
		File[] contents = dir.listFiles();

		if (contents == null || contents.length == 0) return Stream.of(dir);

		else return Arrays.stream(contents)
				.flatMap(CompilerUtil::flattenDir);
	}

	@FunctionalInterface
	private interface Logger {


		Path print(Path path);
	}

	private static final Logger logPath = path -> {
		System.out.println("Loading class file: " + path.toString());
		return path;
	};

	@FunctionalInterface
	private interface PathConverter {
		JavaFileObject toJavaFileObject(Path path);
	}

	/*****************************************
	 * Debugging purposes - lists the URLS of the test file objects being loaded for test compilation
	 ****************************************/
	private static final PathConverter converter = (Path path) -> {
		try {
			return JavaFileObjects.forResource(path.toUri().toURL());
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("No such file");
		}
	};

	public static void outputDiagnostics(Compilation compilation) {
		List<? extends Diagnostic> diagnostics = compilation.diagnostics();
		diagnostics.forEach(d -> {
			System.out.println("================================================");
			System.out.println("Diagnostic: " + d.getKind().toString());
			System.out.println(d.getMessage(Locale.ENGLISH));
		});
	}

	/*****************************************
	 * Creates a custom classloader with the files created by the google compilation
	 * @param compilation the compilation object that was run
	 * @return a classloader that can be used to load and create instances of compiled source
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws NoSuchFieldException
	 ****************************************/
	public ClassLoader createClassLoader(com.google.testing.compile.Compilation compilation)
			throws InstantiationException,
			IllegalAccessException,
			ClassNotFoundException,
			IOException, NoSuchFieldException {

		return new ClassLoader() {

			final List<JavaFileObject> files = getFiles(compilation);

			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				System.out.println("Loading compiled class: " + name);

				JavaFileObject mc = this.getFileFromName(name, files);

				if (mc != null) {

					try {

						byte[] bytes = new byte[500000];

						int len = mc.openInputStream().read(bytes);
						files.remove(mc);

						return defineClass(name, bytes, 0, len);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				return super.findClass(name);
			}

			private JavaFileObject getFileFromName(
					String name,
					List<JavaFileObject> input) {
				return input.stream().filter(object -> {
					final URI normalize = object.toUri().normalize();
					String    curr      = normalize.toString();
					curr = curr.substring(("mem:///CLASS_OUTPUT/").length(),
												 curr.length() - ".class".length());
					curr = curr.replace("/", ".");
					return Objects.equals(curr, name);
				}).findAny().orElse(null);
			}
		};
	}

	/*****************************************
	 * Hacky solution, reflection to access the generated files from the google compiler
	 * @param compilation the compilation instance
	 * @return a list of files from the compilation
	 ****************************************/
	@SuppressWarnings("unchecked")
	private List<JavaFileObject> getFiles(Compilation compilation) throws
			NoSuchFieldException,
			IllegalAccessException {

		Field f = compilation.getClass().getDeclaredField("generatedFiles");
		f.setAccessible(true);
		final ImmutableList<JavaFileObject> files   = (ImmutableList<JavaFileObject>) f.get(compilation);
		List<JavaFileObject>                objects = new ArrayList<>();
		objects.addAll(files);
		return objects;
	}

	/*****************************************
	 * Returns the full classname for the class in the test source set
	 * @param shortName the simple name of the class
	 * @return the full class name to load from a classloader
	 ****************************************/
	static String getFullClassName(String shortName) {
		return "edu.gvsu.prestongarno.sourcegentests." + shortName;
	}

	public static String prettyPrintElement(Element element) {
		List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
		String                           prettyMirrors     = "";
		if (annotationMirrors.isEmpty())
			prettyMirrors = "none";
		else {
			for (AnnotationMirror mirror : annotationMirrors) {
				prettyMirrors = prettyMirrors.concat("\n\t\t\t" + mirror.getAnnotationType().toString());

				//annotation values
				for (AnnotationValue x : mirror.getElementValues().values()) {
					prettyMirrors = prettyMirrors.concat("\n\t\t\t\t\\- " +
																			 x.toString() + "\n\t\t\t\t\tvalue = " + x.getValue().toString());
				}
			}
		}
		Set<Modifier> modifiers        = element.getModifiers();
		String        modifierToString = "" + modifiers.size();
		for (Modifier mod : modifiers) {
			modifierToString = modifierToString.concat(mod.toString() + ",");
		}
		return "\nElement:\t" + element.getKind().toString() + "\n\t\tName = " + element.getSimpleName()
				+ "\n\t\tAs type: " + element.asType().toString()
				+ "\n\t\tModifiers: " + modifiers
				+ "\n\t\tAnnotations: " + prettyMirrors
				+ "\n\t\tEnclosing Element = " + element.getEnclosingElement().asType();
	}
}

