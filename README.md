Try-With-Resources Compatibility: Java & Android API <19 (KitKat)
======

#### Standalone dependency to support migrating Android apps from [retrolambda](https://github.com/orfjackal/retrolambda) to Android Studio 2.4, which now comes with support for [some Java 8 features out of the box]().  While Google is rumored to be providing support for try-with-resources ['hopefully sometime soon'](https://www.reddit.com/r/androiddev/comments/65f2rb/java_8_language_features_support_update/dgaqpak/), I decided make a standalone tool to do this until Google provides a fix for Android Studio 2.4.



## How to use with gradle:\
trywithresources-compat will only work with source version 1.8. This is also a *compileOnly dependency. In your module build.gradle file add this to your dependencies closure:\*
    
    repositories {
      mavenCentral()
    }
    
    dependencies {
      compileOnly 'com.prestongarno:trywithres-compat:0.1'
    }
    
**NOTE**: Android Studio may fail your build saying that "annotation processors must be explicitly declared".  If this happens, simply replace "compileOnly" with "annotationProccessor" and build will proceed as normal.

#### Options:
*  **Pretty-print**: compile with the `-g:source` argument to print before/after processing.  To do this with gradle:
 ```
     compileJava { 
       options.compilerArgs.add("-g:source")  
     }
```
     
#### Possible Issues:
   * Hotswap/InstantRun on emulators running Android \<19 won't work *after adding a try-with-resource and instant run before a clean build*. A clean build will most likely be required if testing on \<API 19 and you add a try-with-resource statement. Release APK won't be affected.
<br><br><br><br>
     
     
##### **How it works:**

* trywithresources-compat leverages the Java [`ServiceLoader`](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to attach itself to the javac compilation process and use the (admittedly, half internal/unsupported) API com.sun.tools.javac 
* After annotation processing, it runs a single pass over the Abstract Syntax Tree and translates any try-with-resources blocks to standard try-catch blocks
* The code generation process uses synthetic local variables in order to avoid naming conflicts with existing symbols. The transformed block follows the [JLS canonical process](https://docs.oracle.com/javase/specs/jls/se7/html/jls-14.html#jls-14.20.3) for de-sugaring as shown below:

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
               } catch (synthetic final Throwable xI$) {
                 primaryException0$.addSuppressed(xI$);
               } 
             else something.close();
      }
    } // if applicable: catch/finally blocks are inserted here

    

