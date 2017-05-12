Try-With-Resources Compatibility: Java & Android API <19 (KitKat)
======

[![GPL License](https://badges.frapsoft.com/os/gpl/gpl.svg?v=103)](https://opensource.org/licenses/GPL-3.0/)   [![GitHub version](https://badge.fury.io/gh/boennemann%2Fbadges.svg)](https://repo1.maven.org/maven2/com/prestongarno/trywithres-compat/)

### Standalone dependency to support migrating Android apps from [retrolambda](https://github.com/orfjackal/retrolambda) to Android Studio 2.4, which now comes with support for [some Java 8 features out of the box]().  While Google is rumored to be providing support for try-with-resources ['hopefully sometime soon'](https://www.reddit.com/r/androiddev/comments/65f2rb/java_8_language_features_support_update/dgaqpak/), I decided make a standalone tool to do this until Google provides a fix for Android Studio 2.4.

<br>

**Quick note about the GPL license:** with this being a [compile only](https://blog.gradle.org/introducing-compile-only-dependencies) dependency with no runtime library/linkage, the same classpath exception applies here that covers javac: unless you extend this library, you are free in order to create an executable application, covered under whatever license you want. I.N.A.L., though:)


## How to use with gradle:
trywithresources-compat will only work with source version 1.8. This is also a *compileOnly dependency. In your module build.gradle file add this to your module build.gradle:\*
    
    repositories {
      mavenCentral()
    }
    
    dependencies {
      compileOnly 'com.prestongarno:trywithres-compat:0.1'
    }
    
**NOTE**: Android Studio may fail your build saying that "annotation processors must be explicitly declared".  If this happens, simply replace "compileOnly" with "annotationProccessor" and build will proceed as normal.

## Options:
*  **Pretty-print**: compile with the `-g:source` argument to print before/after processing.  To do this with gradle:
 ```
     compileJava { 
       options.compilerArgs.add("-g:source")  
     }
```
     
### Possible Issues:
   * Hotswap/InstantRun on emulators running Android \<19 won't work *after adding a try-with-resource and instant run before a clean build*. A clean build will most likely be required if testing on \<API 19 and you add a try-with-resource statement. Release APK won't be affected.
<br><br><br><br>
     
     
##### **How it works:**

* trywithresources-compat leverages the Java [`ServiceLoader`](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to attach itself to the javac compilation process and use the (admittedly, half internal/unsupported) API com.sun.tools.javac 
* After annotation processing, it runs a single pass over the the code structure and translates any try-with-resources blocks to standard try-catch blocks
* The code generation process uses synthetic local variables in order to avoid naming conflicts with existing symbols. The transformed block follows the [JLS canonical process](https://docs.oracle.com/javase/specs/jls/se7/html/jls-14.html#jls-14.20.3) (minus the call to `Throwable#addSuppressed`) for de-sugaring as shown below. This guarantees runtime stability with zero performance costs, and also provides that sweet, sweet, syntactical sugar.

##### Example code:

    try(AutoCloseableImplImpl example = new AutoCloseableImpl()) {
      example.doRiskyThings();
    } //optional catch/finally

##### Result:

    {
      final AutoCloseableImplImpl example = new AutoCloseableImpl()
      Throwable primaryException0$ = null;

      try {
         example.doRiskyThings();
      } catch (synthetic final Throwable t$) {
          primaryException0$ = t$;
          throw t$;
      } finally {
          if (something != null) {
             if (primaryException0$ != null) 
               try { 
                 something.close();
               } catch (synthetic final Throwable xI$) { } 
             else something.close();
          }
      }
    } // if applicable: catch/finally blocks are inserted here

    

