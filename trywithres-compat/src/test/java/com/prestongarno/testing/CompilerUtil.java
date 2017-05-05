package com.prestongarno.testing;

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


public class CompilerUtil {


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
				.map(CompilerUtil.converter::toJavaFileObject)
				.collect(toList());
	}

	private static Stream<? extends File> flattenDir(File dir) {
		File[] contents = dir.listFiles();

		if (contents == null || contents.length == 0) return Stream.of(dir);

		else return Arrays.stream(contents)
				.flatMap(CompilerUtil::flattenDir);
	}

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
			throw new IllegalArgumentException("No such file or file is not a .java source file");
		}
	};

	/*****************************************
	 * Creates a custom classloader with the files created by the google compilation
	 * @param compilation the compilation object that was run
	 * @return a classloader that can be used to load and create instances of compiled source
	 ****************************************/
	static ClassLoader createClassLoader(com.google.testing.compile.Compilation compilation)
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
	 * Hackish solution reflection to access the generated class files from the compiler
	 * @param compilation the compilation instance
	 * @return a list of files from the compilation
	 ****************************************/
	@SuppressWarnings("unchecked")
	private static List<JavaFileObject> getFiles(Compilation compilation) throws
			NoSuchFieldException,
			IllegalAccessException {
		Field f = compilation.getClass().getDeclaredField("generatedFiles");
		f.setAccessible(true);
		final ImmutableList<JavaFileObject> files   = (ImmutableList<JavaFileObject>) f.get(compilation);
		List<JavaFileObject>                objects = new ArrayList<>();
		objects.addAll(files);
		return objects;
	}
}

