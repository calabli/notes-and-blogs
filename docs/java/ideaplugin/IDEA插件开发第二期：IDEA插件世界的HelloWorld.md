# 开发环境
## 1、JDK版本（JDK17）

![img.png](img.png)

2、Gradle版本（Gradle7.6）

![img_1.png](img_1.png)

3、IDEA开发工具版本（2022.3专业版）

![img_2.png](img_2.png)

IDEA版本信息

# 项目创建
得益于IDEA强大的功能使得我们创建一个IDEA插件项目变得非常简单，首先我们需要打开项目管理界面，新建一个项目：

![img_3.png](img_3.png)

新建项目

点击IDE Plugin可以填写插件项目的相关信息，项目名称填写hello-world-plugin，修改项目本地所在位置，选中
新建项目类型为Plugin，使用语言为Java语言，同时填写Group信息和选择JDK版本，然后点击Create进行项目的创建。

![img_4.png](img_4.png)

项目在创建过程中，会下载插件项目对应的依赖插件，主要包括两个：java和org.jetbrains.intellij，
具体内容可以在项目路径下的build.gradle.kts文件中的plugins标签中查看。初始项目构建完成之后
出现如下提示代表创建项目成功。

![img_5.png](img_5.png)

既然插件项目已经创建成功，那么我们接下来看一下项目的文件目录结构，了解各个目录的用处。

![img_6.png](img_6.png)

Gradle Wrapper解决了什么问题？

1、你本地安装的Gradle版本跟项目所需的Gradle版本不一致容易导致问题，为了解决该问题，
Gradle Wrapper应运而生，Gradle Wrapper是一个脚本，可调用Gradle的声明版本，
并在编译时下载。因此，开发人员可以快速启动并运行Gradle项目，而无需遵循手动安装Gradle过程。

2、使用项目根目录下的gradlew命令运行的Gradle版本是Gradle Wrapper中声明的Gradle版本，
不是本地安装的Gradle版本。

3、gradle/wrapper/gradle-wrapper.jar是实际下载对应Gradle版本的工具包。

下面我们看一下build.gradle.kts文件的具体内容：
```kts
// 项目依赖插件
plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.10.1"
}
// 项目Group信息
group = "com.codermonster"
// 版本信息
version = "1.0-SNAPSHOT"
// 依赖下载仓库
repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  // 运行插件的时候，启动的IDEA版本
    version.set("2022.1.4")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("221")
        untilBuild.set("231.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

```
我们继续来看一下resources/META-INF/plugin.xml文件的信息：

```xml
<!-- Plugin Configuration File. Read more: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html -->
<idea-plugin>
    <!-- Unique identifier of the plugin. It should be FQN. It cannot be changed between the plugin versions. -->
    <id>com.codermonster.hello-world-plugin</id>

    <!-- Public plugin name should be written in Title Case.
         Guidelines: https://plugins.jetbrains.com/docs/marketplace/plugin-overview-page.html#plugin-name -->
  <!-- 运行插件项目显示的插件名称 -->
    <name>Hello-world-plugin</name>

    <!-- A displayed Vendor name or Organization ID displayed on the Plugins Page. -->
    <!-- 联系信息 -->
    <vendor email="support@yourcompany.com" url="https://www.yourcompany.com">YourCompany</vendor>

    <!-- Description of the plugin displayed on the Plugin Page and IDE Plugin Manager.
         Simple HTML elements (text formatting, paragraphs, and lists) can be added inside of <![CDATA[ ]]> tag.
         Guidelines: https://plugins.jetbrains.com/docs/marketplace/plugin-overview-page.html#plugin-description -->
    <!-- 插件说明 -->
    <description><![CDATA[
    Enter short description for your plugin here.<br>
    <em>most HTML tags may be used</em>
  ]]></description>

    <!-- Product and plugin compatibility requirements.
         Read more: https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html -->
    <depends>com.intellij.modules.platform</depends>

    <!-- Extension points defined by the plugin.
         Read more: https://plugins.jetbrains.com/docs/intellij/plugin-extension-points.html -->
    <extensions defaultExtensionNs="com.intellij">

    </extensions>
</idea-plugin>

```

Gradle Wrapper的配置文件是gradle/wrapper/gradle-wrapper.properties，我们继续看一下该文件的信息：
```properties
# 解压Gradle压缩包之后的文件存放处
distributionBase=GRADLE_USER_HOME
# 解压Gradle压缩包之后的文件存放处，相对于distributionBase设置的路径基础之上的文件路径
distributionPath=wrapper/dists
# 下载地址
distributionUrl=https\://services.gradle.org/distributions/gradle-7.5.1-bin.zip
# Gradle压缩包文件存放处
zipStoreBase=GRADLE_USER_HOME
# Gradle压缩包文件存放处，相对于zipStoreBase设置的路径基础之上的文件路径
zipStorePath=wrapper/dists
```

至此，我们已经完成了一个插件项目的创建，在项目的右上方可以直接运行项目，点击运行之后，
会在沙盒中新打开一个IDEA窗口。

![img_7.png](img_7.png)

我们可以看到该IDEA窗口的版本信息和配置文件build.gradle.kts中规定的版本信息一致。
除此之外，点击Plugins，可以看到新创建的插件已经被安装在新打开的IDEA中了

![img_8.png](img_8.png)

关闭新创建IDEA的窗口即可结束插件项目的运行。

# 增加功能Action

Action 是插件开发中核心的一个概念，Action 可以理解为插件中某个功能的具体实现，举个例子，IDEA 中，
默认通过快捷键 Command+Option+L ，来对代码文件进行格式 Format 。又比如，我们在 IDE 中，点击鼠标右键，
会弹出一个功能菜单，这个菜单中的每一个选项，都对应着一个具体的功能操作，这个功能操作所做的事情，
也是一个 Action 在代码中所做的事情。

调用插件中的功能，最常见方式就是基于 Action 来实现的，Antion 代表一个具体的功能动作。Action 可通过快捷键、
菜单选项来触发。

使用Action的方式是通过一个实现类继承AnAction抽象类，实现其中的actionPerformed方法，在该方法中定义功能逻辑，
即当快捷键或者菜单选项触发时，会执行方法中的内容。

定义一个实现类HelloWorldAction，代码如下：

```java
public class HelloWorldAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("功能被触发");
        // 在IDEA中发送系统通知
        Notifications.Bus.notify(new Notification("HelloWorldPlugin", "欢迎来到插件世界！", NotificationType.INFORMATION), e.getProject());
        // Notifications.Bus.notify方法接收两个参数，第一个参数是一个通知，第二个参数是项目
        // Notification构造器包括三个参数
        // 1、Group信息
        // 2、通知信息
        // 3、通知消息的类型
        // e.getProject()的作用是在多个项目IDE窗口打开的时候，让通知只在当前项目的IDE窗口右下角显示
    }
}
```
如果插件想使用这个Action，那我们必须将这个Action注册到IDE，让IDE感知到才可以，
这个过程是通过resources/META-INF/plugin.xml文件进行配置，具体配置如下：

```xml
<idea-plugin>
    <actions>
        <!-- 在此处添加你的Action -->
        <action class="com.codermonster.helloworldplugin.HelloWorldAction" description="Notification Send">
            <!-- 该标签表示通过快捷键触发Action，注意快捷键可能会冲突，导致功能无法被触发，更换绑定的快捷键即可 -->
            <keyboard-shortcut first-keystroke="control shift 9" keymap="$default"/>
        </action>
    </actions>
</idea-plugin>
```
运行插件项目，在新窗口中随便打开一个项目，然后触发快捷键，可以得到如下的通知：

![img_9.png](img_9.png)

注意因为Windows和Mac键盘存在差异，所以在Mac上绑定上面的快捷键，但是当你按下对应快捷键的时候并不会触发，
这是因为Control被绑定成了Command，所以按下Command+Shift+9的时候，在Mac上会触发上面的通知信息。
如果不知道绑定了哪些快捷键，可以在应用偏好设置中搜索KeyMap进行查看。

![img_10.png](img_10.png)
