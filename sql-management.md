## SQL 관리 방안
Spring JDBC의 `NamedParameterJdbcTemplate`을 쓸 때 SQL관리를 하는 방법을 정리해봤습니다.

### (추천) 다른 JVM 언어의 Multiline String 활용
Groovy, Scala, Kotlin, xTend, Ceylon 등의 JVM 언어에는 여러 줄에 걸친 문자열 선언을 하는 문법이 있습니다. 이 중 Groovy를 활용하는 것을 가장 권장합니다. Java와 동일한 문법이 가장 많이 지원되고, Maven, IntelliJ, Maven, Gradle 등의 Plugin들도 안정되었기 때문입니다.

아래와 같이 따옴표 세 개 문법만 쓰고, 나머지는 Java와 동일한 문법으로 사용할 수도 있습니다.

```groovy
// 정적 쿼리
public staic final String DELETE_BY_ID = """
	DELETE FROM seller
	WHERE id = :id
""";

// 동적 쿼리
public static String buildSelectSql(Seller seller) {
	StringBuilder sql = new StringBuilder();
	sql.append("""
		SELECT name, address
		FROM seller
		WHERE 1=1
	""");

	isNotEmpty(seller.getName(), sql, """
		AND  name = :name
	""");

	isNotEmpty(seller.getAddress(), sql, """
		AND  address = :address
	""");

	return sql.toString();
}

private static void isNotEmpty(String param, StringBuilder sql, String part) {
	if(StringUtils.isNotEmpty(param)) {
		sql.append(part);
	}
}
```

미래에 Java에도 멀티라인 스트링이 도입이 된다면 Groovy에 대한 의존을 제거할 수도 있습니다. 멀티라인 스트링의 문법은 다른 JVM 언어인 Groovy, Scala, Kotlin, Ceylon이 동일하기에 Java도 같은 형식의 문법을 도입할 가능성이 높습니다. 그때가 온다면 아래 명령어로 일괄적으로 Groovy 파일을 Java로 전환하면 됩니다.

```
find . -name '*.groovy' -print0 | xargs -0 rename 's/.groovy$/.java/'
```

- 장점
	- 컴파일 타임의 검증
		- 쿼리 ID가 자동완성 되고 오타를 칠 수가 없음
		- 쿼리 문자열로 IDE에서 Ctrl + 클릭으로 바로 이동 가능
	- XML 파싱 비용이 없음
	- 다른 도구에서 SQL 쿼리를 복사&붙여넣기에 편함
	- 학습 비용이 거의 없음. ( `"""` 문법만 알면 됨)
	- Java 조건/반복문으로 동적 쿼리를 만들 수 있음.
		- 테스트 코드 커버리지를 측정 가능
- 단점
	- Eclipse와 IntelliJ Community Edition에서는 별도의 IDE Plugin 설치 필요

IDE와 빌드도구에서 설정하는 방법은 [Java 프로젝트에 Groovy를 섞어 쓰는 설정](groovy-config.md)을 참조하실 수 있습니다.

### Spring의 속성 관리 기능 활용
Spring의 속성관리 기능으로 SQL파일을 따로 빼서 선언할 수도 있습니다.

#### Properties XML 선언활용
보통 속성관리에는 .properties 파일을 쓰지만 SQL을 편집하기에는 XML파일이 SQL편집에는 더 편리합니다. 아래와 같은 형식으로 선언을 합니다.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd" >
<properties>
	<entry key="seller.selectById">
		SELECT id, name, address, email
		FROM seller
		WHERE id = :id
	</entry>
	<entry key="seller.deleteById">
		DELETE FROM seller
		WHERE id = :id
	</entry>
</properties>
```

위의 파일은 Spring 설정에서 `@PropertySource("classpath:/sellerSqls.xml")` , `<context:property-placeholder location="classpath:sellerSqls.xml"/>` 같은 선언으로 로딩할 수 있습니다.

쿼리 문자열은 아래와 같이 `@Value("#{key}")` 선언으로 참조합니다.

```java
	@Value("${seller.selectById}")
	private String selectById;
```

#### `<utils:properties>` 활용
Spring의 ApplicationContext 설정 파일에 아래와 같이 쿼리를 선언할 수 있습니다.

```xml
<util:properties id="sellerSqls">
	<prop key="selectById">
		SELECT id, name, address, email
		FROM seller
		WHERE id = :id
	</prop>
	<prop key="deleteById">
		DELETE FROM seller
		WHERE id = :id
	</prop>
</util:properties>
```

또는  `<utils:properties location="classpath:sqls.xml">`와 같이 다른 .propeties 파일이나 propeties XML파일을 지정할 수도 있습니다.

참조하는 쪽에서는 `@Value("#{beanId.key}")`로 선언을 합니다.

```java
	@Value("#{sellerSqls.selectById}")
	private String selectById;
```

Spring 속성 관리 기능을 활용할 때의 장단점은 아래와 같습니다.

- 장점
	- 라이브러리 의존성을 더 추가할 필요없음.
	- SQL 쿼리를 복사&붙여넣기에 편함
	- 쿼리 ID가 틀리면 어플리케이션 시작 시점에 알려주게 할 수 있음.
- 단점
	- 쿼리 ID가 틀려도 컴파일 타임에 검증되지 않음.
	- 동적 쿼리는 XML 안에 선언할 수 없음
	- `@Value`로 참조된 쿼리 문자열은 IDE에서 Ctrl + 클릭으로 바로 이동할 수 없음

### 템플릿 엔진 도입
HTML 파일을 만들 때 템플릿 엔진을 쓰는 것과 유사한 방식으로 SQL을 관리할 수도 있습니다. Freemarker를 활용한 사례는 http://kwon37xi.egloos.com/7048211 을 참조하실 수 있습니다. 이 글에서는 SQL 전용 템플릿 엔진 2가지를 소개합니다.


#### ElSql
EqSql의 자체적인 문법으로 Query의 Id와 조건문 등을 기술할수 있습니다.

```
-- ==========================================================================
@NAME(deleteSeller)
		DELETE FROM seller
		WHERE id = :id

-- ==========================================================================
@NAME(selectSeller)
	SELECT id, name, address, email
	FROM seller
	@WHERE oid = :doc_oid
		@AND(:name)
			name = :name
		@AND(:address)
			address = :address
```

자세한 사용법은 https://github.com/OpenGamma/ElSql 을 참조하시기 바랍니다.

#### JIRM-Core
JIRM-Core에서는 별도의 파일로 선언된 SQL파일을 String으로 읽어오는 기능을 제공합니다.

```java
String sql = PlainSql.fromResource(TestBean.class, "select-test-bean.sql").getSql();

```

JIRM-Core는 자체적으로도 Named parameter를 지원합니다. 그렇지만, NamedParameterJdbcTemplate을 함께 쓴다면 굳이 그 기능이 필요하지는 않습니다.

자세한 사용법은 https://github.com/agentgt/jirm/tree/master/jirm-core

템플릿 엔진을 활용할 때의 장단점은 아래와 같습니다.

- 장점
	- SQL 쿼리를 복사&붙여넣기에 편함.
	- 쿼리 ID가 틀리면 어플리케이션 시작 시점에 알려주게 할 수 있음.
	- (ELSql) 동적 쿼리까지 템플릿 파일 안에 선언할 수 있음.

- 단점
	- 쿼리 ID가 틀려도 컴파일 타임에 검증되지 않음.
	- 동적 쿼리를 만드는 표현식이 틀려도 컴파일 타임에 검증되지 않음
	- IDE 안에서 Ctrl + 클릭으로 바로 이동을 할 수 없음.


### SQL Builder 도입
MyBatis나 JOOQ는 Spring JDBC와 마찬가지로 SQL 쿼리를 실행하는 프레임워크입니다. 그런데 이 프레임워크들의 일부 기능인 SQL Builder만을 활용할 수 있습니다. SQL 쿼리가 단긴 String 문자열은 다른 프레임워크의 기능으로 생성하고, 쿼리를 실행하는 역할은 Spring JDBC에 맡기는 방식입니다.


#### MyBatis의 SQL 클래스
MyBatis에서 제공하는 SQL클래스도 SQL구문을 Java의 String으로 생성하는것을 도와줍니다.

```java
// 정적쿼리
public String deleteById() {
	return new SQL()
		.DELETE_FROM("seller");
		.WHERE("id = :id");
		.toString();
}


// 동적쿼리
public String selectByCondition(Seller seller) {
	return new SQL() {{
		SELECT("name, address");
		FROM("seller");
		if (StringUtils.isNotEmpty(seller.getName())) {
			WHERE("name = #{name}");
		}
		if (StringUtils.isNotEmpty(seller.getAddress())) {
			WHERE("address = #{address}");
		}
  }}.toString();
}
```

자세한 사용법은 http://www.mybatis.org/mybatis-3/ko/statement-builders.html 을 참고합니다. 위의 동적쿼리에서 사용한 내부 익명 클래스 문법이 클래스 로더에 부담이 된다는 주장도 있습니다. 그 주장에 대해서는 https://blog.jooq.org/2014/12/08/dont-be-clever-the-double-curly-braces-anti-pattern/ 을 참조하시기 바랍니다. MyBatis의 라이벌기술이라고 할 수 있는 JOOQ의 블로그에 올라온 글이라는 점이 흥미롭습니다.

### JOOQ
JOOQ에서도 아래와 같이 SQL 구문을 String으로 생성할 수 있습니다.

```java
public String selectById() {
	return create.select(field("name"), field("address")
		.from(table("seller"))
		.where(field("id").equal(":id"))
		.getSQL();
}
```

자세한 소개는 https://www.jooq.org/doc/3.9/manual/getting-started/use-cases/jooq-as-a-standalone-sql-builder/ 을 참조하시기 바랍니다.

MyBatis나 Jooq의 SQL Builder를 도입할 때의 장단점은 아래와 같습니다.

- 장점
	- 컴파일 타임의 검증
		- 쿼리를 생성하는 메서드는 자동 완성 되고 오타를 칠 수가 없음
		- `SELECT`, `FROM` 같은 SQL 예약어의 오타까지 예방해 줌
		- 쿼리를 생성하는 메서드로 IDE에서 Ctrl + 클릭으로 바로 이동 가능
	- Java 조건/반복문으로 동적쿼리를 만들 수 있음
- 단점
	- Native SQL을 다른 툴에서 바로 복사&붙여넣기 하기가 불편함

위와 같은 라이브러리를 쓰지 않더라도 StringBuilder를 이용한 SqlBuilder를 직접 만드는 것은 어렵지 않습니다. 아래 사례를 참조하실 수 있습니다.

- [동적 Native SQL 생성 어떻게 할까 - 순수 Java 코드로 생성하기 ](http://kwon37xi.egloos.com/7092965)
