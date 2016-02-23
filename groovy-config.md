# Java 프로젝트에 Groovy를 섞어 쓰는 설정

## Build tool

### Maven
`/src/main/java` 아래에 `.groovy` 파일을 놓아도 컴파일을 할 수 있는 설정입니다. 

Dependencies에 groovy에 대한 의존성을 추가합니다.

```xml
<dependency>
    <groupId>org.codehaus.groovy</groupId>
    <artifactId>groovy-all</artifactId>
    <version>2.4.5</version>
</dependency>
```

build-plugins 에 maven-compiler-plugin에 아래 내용을 추가합니다.

```xml
    <plugin>
		<groupId>org.apache.maven.plugins</groupId>
		<artifactId>maven-compiler-plugin</artifactId>
		<version>2.3.2</version>
		<configuration>
			<compilerId>groovy-eclipse-compiler</compilerId>
			<meminitial>128m</meminitial>
			<maxmem>512m</maxmem>
			<source>1.8</source>
			<target>1.8</target>
			<encoding>utf-8</encoding>
		</configuration>
		<dependencies>
			<dependency>
				<groupId>org.codehaus.groovy</groupId>
				<artifactId>groovy-eclipse-compiler</artifactId>
				<version>2.7.0-01</version>
			</dependency>
		</dependencies>
		</plugin>
    <plugin>
```

### Gradle
`/src/main/java` 아래에 `.groovy` 파일을 놓아도 컴파일을 할 수 있는 설정입니다. 

```groovy
apply plugin: 'java'
apply plugin: 'groovy'

[compileGroovy, compileTestGroovy]*.options*.encoding = 'UTF-8' 

compileJava {
	sourceCompatibility = 1.8
	targetCompatibility = 1.8
}

compileJava {
	dependsOn compileGroovy
}

sourceSets {
	main {
		groovy {
			srcDirs = ['src/main/java']
		}
		java {
			srcDirs = []
		}
	}
}

tasks.withType(GroovyCompile) {
	dependsOn = []
}

tasks.withType(JavaCompile) { task ->
	dependsOn task.name.replace("Java", "Groovy")
}

```

## IDE
### Eclipse

#### 방법1 : STS(SpringSource Tools Suite)의 Extension 기능으로 설치
STS를 쓰고 있다면 'Dashboard > Extensions'에서 플러그인을  선택해서 설치할 수 있습니다.

아래 3가지 plugin을 선택해서 설치합니다.

1. Groovy 2.x Compiler for Groovy-Eclipse
	- pom.xml에 선언된 Groovy 버전과 가급적 맞추어서 선택
2. Groovy-Eclipse
3. Groovy-Eclipse Configurator for M2Eclipse (Maven)
	- 3번은 Maven을 쓸 때만 필요.  pom.xml에 빨간 줄이 뜨면 Ctrl +1 을 눌러서도 설치할수 있음.

이미 설치된 Plugin이 있을지도 모르므로 'Show installed' 옵션을 켜고 확인을 하는 것이 좋습니다.

![sts-groovy-support](sts-groovy-support.png)


#### 방법2 : 직접 찾아서 설치
Eclipse Market place에서 검색해서 추가하거나 Update site URL로 설치합니다.

- Groovy Eclipse
	- Eclipse 4.5 (Mars)
		- Update site : http://dist.springsource.org/snapshot/GRECLIPSE/e4.5/
		- Marketplace : https://marketplace.eclipse.org/content/groovygrails-tool-suite-ggts-eclipse
	- Eclipse 4.4 (Luna)
		- Update site : http://dist.springsource.org/release/GRECLIPSE/e4.4/
		- Marketplace : https://marketplace.eclipse.org/content/groovygrails-tool-suite-ggts-eclipse	
	- Eclipse 4.3 (Kepler)
		- Update site : http://dist.springsource.org/release/GRECLIPSE/e4.3/
		- Marketplace : https://marketplace.eclipse.org/content/groovygrails-tool-suite-ggts-eclipse
	- Eclipse 4.2 (Juno)
		- Update site : http://dist.springsource.org/release/GRECLIPSE/e4.2/
		- Marketplace : [groovy-eclipse-juno](https://marketplace.eclipse.org/content/groovy-eclipse-juno)
	- Eclipse 3.7 (Indigo)
		- Update site : http://dist.springsource.org/release/GRECLIPSE/e3.7/
		- Marketplace : [groovy-eclipse-indigo](https://marketplace.eclipse.org/content/groovy-eclipse-indigo)
	- Eclipse 3.6 (Helios)
		- Update site : http://dist.springsource.org/release/GRECLIPSE/e3.6/


#### 클래스 추가 방법
New-> groovy class를 선택하여서 java 파일 작성하듯이 클래스를 만듭니다.

![new groovy](http://file.benelog.net/include/sql_without_xml/new_groovy_class.png)

### IntelliJ
JetBrains의 Groovy plugin은  IDEA community Edition에서도 사용할 수 있습니다.

'File > Settings'(단축키 Ctrl + Alt + S) 로 이동해서 Plugin 메뉴를 클립합니다. Groovy Plugin이 보인다면 선택합니다.

![groovy plugin](images/idea-groovy.png) 

없다면 [Install JetBrains plugins...]를 클릭합니다.  Groovy로 검색해서 설치합니다.

참고로 groovy-eclipse-compiler를 maven설정으로 써도 IntelliJ에서도 잘 실행됩니다.


