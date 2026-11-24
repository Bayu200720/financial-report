# Keep WebView JS bridge
-keepclassmembers class com.example.financereport.MainActivity$FileBridge {
    @android.webkit.JavascriptInterface <methods>;
}
