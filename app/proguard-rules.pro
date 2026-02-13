-renamesourcefileattribute SourceFile

-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-keep public class com.paywall.Paywall {
    public on_load();
}
-keep public class com.paywall.Utils