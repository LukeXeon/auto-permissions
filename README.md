# AutoPermissions
无需编写`requestPermissions`的代码，在第一个`Activity`启动时自动扫描`AndroidManifest`申请其中的权限（包括悬浮窗权限，但暂时不支持申请MIUI的悬浮窗权限），最低兼容到`API 14`，无依赖，没有使用`appcompat`，也不需要编写代码，唯一需要做的工作就是把他引入你的项目。
# Gradle引入
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
	dependencies {
	        implementation 'com.github.LukeXeon:auto-permissions:0.0.3'
	}
```
