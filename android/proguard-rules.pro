# La web llama a QuizznHost.postMessage(): R8 no debe borrar ni renombrar los métodos expuestos a JavaScript
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
