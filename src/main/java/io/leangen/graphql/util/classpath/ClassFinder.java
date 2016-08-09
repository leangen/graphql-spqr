package io.leangen.graphql.util.classpath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

/**
 * <p>A <tt>ClassFinder</tt> object is used to find classes. By default, an
 * instantiated <tt>ClassFinder</tt> won't find any classes; you have to
 * add the classpath (via a call to {@link #addClassPath}), add jar files,
 * add zip files, and/or add directories to the <tt>ClassFinder</tt> so it
 * knows where to look. Adding a jar file to a <tt>ClassFinder</tt> causes
 * the <tt>ClassFinder</tt> to look at the jar's manifest for a
 * "Class-Path" entry; if the <tt>ClassFinder</tt> finds such an entry, it
 * adds the contents to the search path, as well. After the
 * <tt>ClassFinder</tt> has been "primed" with things to search, you call
 * its {@link #findClasses findClasses()} method to have it search for
 * the classes, optionally passing a {@link ClassFilter} that can be used
 * to filter out classes you're not interested in.</p>
 *
 * <p>The following example illustrates how you might use a
 * <tt>ClassFinder</tt> to locate all non-abstract classes that implement
 * the <tt>ClassFilter</tt> interface, searching the classpath as well
 * as anything specified on the command line.</p>
 *
 * <blockquote><pre>
 * import org.clapper.util.classutil.*;
 *
 * public class Test
 * {
 *     public static void main (String[] args) throws Throwable
 *     {
 *         ClassFinder finder = new ClassFinder();
 *         for (String arg : args)
 *             finder.add(new File(arg));
 *
 *         ClassFilter filter =
 *             new AndClassFilter
 *                 // Must not be an interface
 *                 (new NotClassFilter (new InterfaceOnlyClassFilter()),
 *
 *                 // Must implement the ClassFilter interface
 *                 new SubclassClassFilter (ClassFilter.class),
 *
 *                 // Must not be abstract
 *                 new NotClassFilter (new AbstractClassFilter()));
 *
 *         Collection&lt;ClassInfo&gt; foundClasses = new ArrayList&lt;ClassInfo&gt;();
 *         finder.findClasses (foundClasses, filter);
 *
 *         for (ClassInfo classInfo : foundClasses)
 *             System.out.println ("Found " + classInfo.getClassName());
 *     }
 * }
 * </pre></blockquote>
 *
 * <p>This class, and the {@link ClassInfo} class, rely on the ASM
 * byte-code manipulation library. If that library is not available, this
 * package will not work. See
 * <a href="http://asm.objectweb.org"><i>asm.objectweb.org</i></a>
 * for details on ASM.</p>
 *
 * <p><b>WARNING: This class is not thread-safe.</b></p>
 *
 * @version <tt>$Revision$</tt>
 *
 * @author Copyright &copy; 2006 Brian M. Clapper
 */
public class ClassFinder
{
    /*----------------------------------------------------------------------*\
                            Private Data Items
    \*----------------------------------------------------------------------*/

	/**
	 * Places to search.
	 */
	private LinkedHashMap<String,File> placesToSearch =
			new LinkedHashMap<String,File>();

	/**
	 * Found classes. Cleared after every call to findClasses()
	 */
	private Map<String,ClassInfo> foundClasses =
			new LinkedHashMap<String,ClassInfo>();


    /*----------------------------------------------------------------------*\
                              Public Methods
    \*----------------------------------------------------------------------*/

	/**
	 * Add the contents of the system classpath for classes, skipping java.library.path and java.home.
	 *
	 * @return this ClassFinder instance to allow call chaining
	 */
	public ClassFinder addPrunedClassPath() {
		String path = System.getProperty ("java.class.path");
		String[] libPaths = System.getProperty ("java.library.path").split(File.pathSeparator);
		String javaHome = System.getProperty ("java.home");

		return add(Arrays.stream(path.split(File.pathSeparator))
				.filter(part -> !Arrays.stream(libPaths).anyMatch(part::startsWith) && !part.startsWith(javaHome)));
	}

	/**
	 * Add the contents of the system classpath for classes.
	 *
	 * @return this ClassFinder instance to allow call chaining
	 */
	public ClassFinder addClassPath() {
		return add(Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)));
	}

	/**
	 * Add a jar file, zip file or directory to the list of places to search
	 * for classes.
	 *
	 * @param files  directories, jar or zip files to look for classes in
	 *
	 * @return this ClassFinder instance to allow call chaining
	 */
	public ClassFinder add(File... files) {
		for (File file : files) {
			if (isPossibleRoot(file) && !placesToSearch.containsKey(file.getAbsolutePath())) {
				String absPath = file.getAbsolutePath();
				placesToSearch.put(absPath, file);
				if (isJar(absPath)) {
					try {
						loadJarClassPathEntries(file);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

		return this;
	}

	public ClassFinder add(Collection<File> files) {
		File[] array = new File[files.size()];
		return add(files.toArray(array));
	}

	private ClassFinder add(Stream<String> paths) {
		return add(paths.map(File::new).collect(Collectors.toList()));
	}

	/**
	 * Search all classes in the search areas, keeping only those that
	 * pass the specified filter.
	 *
	 * @param filter  the filter, or null for no filter
	 *
	 * @return descriptors for found classes
	 */
	public Collection<ClassInfo> findClasses (ClassFilter filter) throws ClassReadingException {
		foundClasses.clear();

		// Load all the classes first.

		for (File file : placesToSearch.values()) {
			String name = file.getPath();

			System.out.println ("Finding classes in " + name);
			if (isJar(name))
				processJar (name, foundClasses);
			else if (isZip (name))
				processZip (name, foundClasses);
			else
				processDirectory (file, foundClasses);
		}

		System.out.println ("Read " + foundClasses.size() + " classes.");

		// Next, weed out the ones we don't want.

		Collection<ClassInfo> result = new HashSet<>();
		for (ClassInfo classInfo : foundClasses.values()) {
			String className = classInfo.getClassName();
			String locationName = classInfo.getClassLocation().getPath();
//			System.out.println ("Looking at " + locationName + " (" + className + ")");

			if ((filter == null) || (filter.accept (classInfo, this))) {
				System.out.println ("Filter accepted " + className);
				result.add (classInfo);
			}
		}

		foundClasses.clear();
		return result;
	}

	/**
	 * Intended to be called only from a {@link ClassFilter} object's
	 * {@link ClassFilter#accept accept()} method, this method attempts to
	 * find all the superclasses (except <tt>java.lang.Object</tt>for a
	 * given class, by checking all the currently-loaded class data.
	 *
	 * @param classInfo     the {@link ClassInfo} objects for the class
	 * @param superClasses  where to store the {@link ClassInfo} objects
	 *                      for the superclasses. The map is indexed by
	 *                      class name.
	 *
	 * @return the number of superclasses found
	 */
	public int findAllSuperClasses(ClassInfo classInfo, Map<String, ClassInfo> superClasses) {
		int total = 0;

		String superClassName = classInfo.getSuperClassName();
		if (superClassName != null) {
			ClassInfo superClassInfo = foundClasses.get (superClassName);
			if (superClassInfo != null) {
				superClasses.put (superClassName, superClassInfo);
				total++;
				total += findAllSuperClasses (superClassInfo, superClasses);
			}
		}

		return total;
	}

	/**
	 * Intended to be called only from a {@link ClassFilter} object's
	 * {@link ClassFilter#accept accept()} method, this method attempts to
	 * find all the interfaces implemented by given class (directly and
	 * indirectly), by checking all the currently-loaded class data.
	 *
	 * @param classInfo     the {@link ClassInfo} objects for the class
	 * @param interfaces    where to store the {@link ClassInfo} objects
	 *                      for the interfaces. The map is indexed by
	 *                      class name
	 *
	 * @return the number of interfaces found
	 */
	public int findAllInterfaces(ClassInfo classInfo, Map<String, ClassInfo> interfaces) {
		int total = 0;
		String superClassName = classInfo.getSuperClassName();
		if (superClassName != null) {
			ClassInfo superClassInfo = foundClasses.get (superClassName);
			if (superClassInfo != null) {
				total += findAllInterfaces (superClassInfo, interfaces);
			}
		}

		String[] interfaceNames = classInfo.getInterfaces();
		if (interfaces != null) {
			for (String interfaceName : interfaceNames) {
				ClassInfo intfClassInfo = foundClasses.get (interfaceName);
				if (intfClassInfo != null) {
					interfaces.put (interfaceName, intfClassInfo);
					total++;
					total += findAllInterfaces (intfClassInfo, interfaces);
				}
			}
		}

		return total;
	}

    /*----------------------------------------------------------------------*\
                              Private Methods
    \*----------------------------------------------------------------------*/

	private void processJar (String jarName, Map<String, ClassInfo> foundClasses) {
		JarFile jar = null;
		try {
			jar = new JarFile (jarName);
			File jarFile = new File (jarName);
			processOpenZip (jar, jarFile,
					new ClassInfoClassVisitor (foundClasses, jarFile));
		} catch (IOException ex) {
			System.out.println ("Can't open jar file '" + jarName + "'"  + ex.getMessage());
		}

		finally {
			try {
				if (jar != null) jar.close();
			} catch (IOException ex) {
				System.out.println ("Can't close " + jarName + ": " + ex.getMessage());
			}
		}
	}

	private void processZip (String zipName, Map<String, ClassInfo> foundClasses) {
		ZipFile zip = null;

		try {
			zip = new ZipFile (zipName);
			File zipFile = new File (zipName);
			processOpenZip (zip, zipFile,
					new ClassInfoClassVisitor (foundClasses, zipFile));
		} catch (IOException ex) {
			System.out.println ("Can't open jar file '" + zipName + "'"  + ex.getMessage());
		}

		finally {
			try {
				if (zip != null) zip.close();
			} catch (IOException ex) {
				System.out.println ("Can't close " + zipName + ": "  + ex.getMessage());
			}

		}
	}

	private void processOpenZip(ZipFile zip, File zipFile, ClassVisitor classVisitor) {
		String zipName = zipFile.getPath();
		for (Enumeration<? extends ZipEntry> e = zip.entries();
		     e.hasMoreElements(); ) {
			ZipEntry entry = e.nextElement();

			if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".class")) {
				try {
					System.out.println ("Reading " + zipName + "(" + entry.getName() +
							")");
					readClass(zip.getInputStream (entry), classVisitor);
				} catch (IOException ex) {
					System.out.println ("Can't open \"" + entry.getName() +
							"\" in zip file \"" + zipName + "\": "
							+ ex.getMessage());
				}
			}
		}
	}

	private void processDirectory (File dir, Map<String,ClassInfo> foundClasses) throws ClassReadingException {
		Collection<Path> files = findFiles(dir, Files::isRegularFile, path -> path.toString().endsWith(".class"));
		ClassVisitor classVisitor = new ClassInfoClassVisitor(foundClasses, dir);

		for (Path path : files) {
			InputStream classInput = null;
			try {
				classInput = Files.newInputStream(path);
				readClass(classInput, classVisitor);
			} catch (IOException e) {
				throw new ClassReadingException("File " + path.toString() + " does not exist", e);
			} finally {
				if (classInput != null) try {classInput.close();} catch(Exception e){/*no-op*/}
			}
		}
	}

	private void loadJarClassPathEntries (File jarFile) throws IOException {
		JarFile jar = new JarFile (jarFile);
		Manifest manifest = jar.getManifest();
		if (manifest == null) {
			return;
		}

		Attributes attrs = manifest.getMainAttributes();
		Set<Object> keys = attrs.keySet();

		for (Object key : keys) {
			String value = (String) attrs.get (key);

			if (key.toString().equals ("Class-Path")) {
				String jarName = jar.getName();
				System.out.println ("Adding Class-Path from jar " + jarName);

				StringBuilder buf = new StringBuilder();
				StringTokenizer tok = new StringTokenizer (value);
				while (tok.hasMoreTokens()) {
					buf.setLength (0);
					String element = tok.nextToken();
					String parent = jarFile.getParent();
					if (parent != null) {
						buf.append (parent);
						buf.append (File.separator);
					}

					buf.append (element);
				}

				String element = buf.toString();
				System.out.println ("From " + jarName + ": " + element);

				add (new File (element));
			}
		}
	}

	private void readClass(InputStream classStream, ClassVisitor classVisitor) throws ClassReadingException {
		try {
			ClassReader cr = new ClassReader (classStream);
			cr.accept(classVisitor, ClassInfo.ASM_CR_ACCEPT_CRITERIA);
		} catch (Exception ex) {
			throw new ClassReadingException("Unable to read class from open input stream", ex);
		}
	}

	private boolean isJar (String fileName) {
		return fileName.toLowerCase().endsWith (".jar");
	}

	private boolean isZip (String fileName) {
		return fileName.toLowerCase().endsWith (".zip");
	}

	private boolean isPossibleRoot (File file) {
		String path = file.getPath();
		return file.exists() && isJar(path) || isZip(path) || file.isDirectory();
	}

	private Collection<Path> findFiles(File rootDir, PathFilter... filters) {
		try (Stream<Path> paths = Files.walk(rootDir.toPath(), FileVisitOption.FOLLOW_LINKS)) {
			return paths.filter(path -> Arrays.stream(filters).allMatch(filter -> filter.accept(path)))
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}