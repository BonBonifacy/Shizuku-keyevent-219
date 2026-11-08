# 保留 Shizuku 内部 newProcess 方法供反射调用
-keep class rikka.shizuku.Shizuku { *; }
-keep class rikka.shizuku.ShizukuProvider { *; }
-dontwarn rikka.shizuku.**
