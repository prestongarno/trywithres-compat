#Try-With-Resources Compatibility: Java & Android API <19 (KitKat)

###Standalone dependency to support migrating Android apps from [retrolambda](https://github.com/orfjackal/retrolambda) to Android Studio 2.4, which now comes with support for [some Java 8 features out of the box]().  While google is rumored to be providing support for try-with-resources ['hopefully sometime soon'](https://www.reddit.com/r/androiddev/comments/65f2rb/java_8_language_features_support_update/dgaqpak/), I decided to go ahead and build a standalone tool to do this until 2.4 provides a fix.  

##How to use with gradle:

trywithresources-compat will only work with source version 1.8. This is also a *compileOnly* dependency. In your build.gradle file add this to your dependencies closure\*:

    dependencies {
      compileOnly ':trywithresources-compat:0.1'
    }

\* <sub>I'm currently waiting on JIRA approval so I can deploy to Maven Central. Currently the only way to use it is to add the direct repository URL (jcenter/Bintray) at https://dl.bintray.com/prestongarno/trywithresources-compat/.  Until then, add this to your repositories closure.</sub>
     
##How it works:

trywithresources-compat leverages the Java [`ServiceLoader`](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to attatch to the javac compilation process. this processor runs a single pass over the Abstract Syntax Tree and translates any try-with-resources blocks to standard try-catch blocks. 

The code generation process uses synthetic local variables in order to avoid naming conflicts with existing symbols. The transformed block follows the [JLS canonical process](https://docs.oracle.com/javase/specs/jls/se7/html/jls-14.html#jls-14.20.3).

####Example code:

    try(AutoCloseableImplImpl example = new AutoCloseableImpl()) {
      example.doRiskyThings();
    }

####Result:

    {
      final AutoCloseableImplImpl example = new AutoCloseableImpl()
      Throwable primaryException0$ = null;

      try {
         example.doRiskyThings();
      } catch (synthetic final Throwable t$) {
          primaryException0$ = t$;
          throw t$;
      } finally {
          if (something != null) 
              if (primaryException0$ != null) try { something.close();
          } catch (synthetic final Throwable x2$) {
              primaryException0$.addSuppressed(x2$);
          } else something.close();
      }
   }

###Possible Issues:
   * Hotswap/InstantRun on emulators running Android \<19 won't work *only if you add a TWR and instant run before a clean build*. A clean build will most likely be required if testing on \<API 19 and you add a try-with-resource statement. Release APK won't be affected.