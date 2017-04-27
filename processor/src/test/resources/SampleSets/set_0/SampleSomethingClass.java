import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SampleSomethingClass {
    public void foo() {
        System.out.println("you just got foo!");
    }

    public void testTryNoThrows() throws IOException {
        try (FileReader r = new FileReader("test"),
             BufferedInputStream ac = new BufferedInputStream(null))
        {
            System.out.println("doing dangerous stuff in the try block...");

        } catch (UnknownError err) {
            System.out.println("Catching some domain specific error here...");
        }
    }

}