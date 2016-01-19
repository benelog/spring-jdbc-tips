native SQL쿼리를 실행하는 프레임워크로 Spring JDBC가 iBatis/MyBatis에 대비해서 가지는 장점을 정리해봅니다.

## 단순한 초기설정
Spring의 JdbcTemplate 계열은 DataSource 객체만 있으면 코드 1줄로 생성할 수 있습니다.

```java
NamedParameterJdbcOperations jdbc = new NamedParameterJdbcTemplate(dataSource);
```

DataSource와 트랙잭션 설정 등 어느 프레임워크를 쓰더라도 들어가는 영역을 제외한다면, 위의 한줄이 필요한 선언의 전부입니다. iBatis/MyBatis는 설정파일의 위치 등을 지정해야하기 때문에 이보다는 초기 설정이 이보다는 복잡합니다.

JdbcTemplate 계열은 멀티스레드에서 접근해도 안된합니다. 따라서 매번 객체를 생성할 필요는 없습니다. DAO등에서는 멤버변수로 저장해 둡니다. 보통 DataSoure 객체만 외부에서 주입받아서 아래와 같이 설정합니다.

```java
public class SellerRepository {
	private NamedParameterJdbcOperations db;
	public setDataSource(DataSource dataSource) {
		this.db = new NamedParameterJdbcTemplate(dataSource);
	}
...
```

[NamedParameterJdbcDaoSupport](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/namedparam/NamedParameterJdbcDaoSupport.html) 를 활용하면 `setDataSource()` 메서드과 멤버변수를 직접 선언하지 않아도 됩니다. 대신 `getNamedParameterJdbcTemplate()`으로 NamedParameterJdbcTemplate을 얻어옵니다. `getNamedParameterJdbcTemplate()`는 메서드 이름이 긴 편이라 짧은 이름으로 따로 멤버변수를 지정하는 편이 편할수도 있습니다. 


```java
public class SellerRepository extends NamedParameterJdbcDaoSupport {
	public int update(Seller seller) {
		SqlParameterSource params = new BeanPropertySqlParameterSource(seller);
		return getNamedParameterJdbcTemplate().update(SellerSqls.UPDATE, params);
	}

```

`component-scan`과 생성자 주입을 같이 쓸 때는 아래와 같이 선언합니다.

```java
@Repository
public class SellerRepository extends NamedParameterJdbcDaoSupport {
	private NamedParameterJdbcOperations db;
	@Autowired
	public SellerRepository(DataSource dataSource) {
		this.db = new NamedParameterJdbcTemplate(dataSource);
	}
```

DataSource가 여러 개일때는 `@Qualifier`등으로 원하는 DataSource를 하나만 찍어서 지정해야 합니다.

## INSERT 구문을 자동생성
SimpleJdbcInsert 클래스는 INSERT 구문을 자동으로 생성해줍니다. DB 컬럼명과 객체의 속성명이 일치한다면 아래와 같은 단순한 코드로 DB에 1건을 입력할 수 있습니다.

```java
SimpleJdbcInsertOperations insertion = new SimpleJdbcInsert(dataSource).withTableName("seller")
SqlParameterSource params = new BeanPropertySqlParameterSource(seller);
insertion.execute(params);
```

이때 DB컬럼명의 snake_case는 java객체에서는 caseCase로 자동으로 바꾸어줍니다. 즉, DB컬럼명이 tel_no이지만 Java 객체의 속성명은 telNo이라도 이를 수동으로 지정해 줄 필요가 없습니다.

데이터를 입력하는 시점에 DB에서 값을 증가시켜서 자동으로 PK가 결정되는 경우가 있습니다. 예를 들면 DB스키마가 아래와 같을 경우입니다.

```sql
CREATE TABLE seller (
	id INT IDENTITY NOT NULL PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(20) , 
	tel_no VARCHAR(50),
	address VARCHAR(255),
	homepage VARCHAR(255)
);

```

이때는 `usingGeneratedKeyColumns()`, `executeAndReturnKey()` 메서드를 활용하면 됩니다.

```java
SimpleJdbcInsertOperations insertion = new SimpleJdbcInsert(dataSource)
		.withTableName("seller")
		.usingGeneratedKeyColumns("id");
SqlParameterSource params = new BeanPropertySqlParameterSource(seller);
Integer id = insertion.executeAndReturnKey(params).intValue();
```

XML 설정없이 Java코드만을 이용합니다. iBatis/MyBatis 등에서 쓰는 방식보다 오타를 찾기가 쉽고, API를 탐색하기에도 편합니다.

로그에는 어떤 SQL이 날아갔는지 당연히 나옵니다. `org.springframework.jdbc` 패키지의 로그레벨을 'DEBUG'로 설정해야 합니다.

```
2016-01-11 06:22:54.551 DEBUG 300 --- [           main] o.s.jdbc.core.simple.SimpleJdbcInsert    : 
	The following parameters are used for call INSERT INTO product (NAME, DESC, PRICE, SELLER_ID, REG_TIME) VALUES(?, ?, ?, ?, ?) with: [키보드, 좋은 상품, 130000, null, 20160111062254]
```

멀티스레드에서 접근해도 안전하기 때문에 DAO 등의 멤버변수로 저장할수도 있습니다.

```java
public class SellerRepository extends NamedParameterJdbcDaoSupport {
	private SimpleJdbcInsertOperations sellerInsertion;

	@Autowired
	public SellerRepository(DataSource dataSource) {
		this.sellerInsertion = new SimpleJdbcInsert(dataSource)
			.withTableName("seller")
			.usingGeneratedKeyColumns("id");
	}
```

`usingColumns()` 메서드를 쓰면 입력할 컬럼을 명시적으로 지정할 수도 있습니다.


만약 DB컬럼명과 클래스의 속성명이 자동으로 매핑될수 없다면 아래와 같이 Map을 이용해서 수동으로 선언할수 있습니다.

```java
	public Integer create(Product product) {
		Map<String, Object> params = mapColumns(product);
		return productInsertion.executeAndReturnKey(params).intValue();
		// DB 컬럼에 대응되는 항목은 Wrapper type을 반환하는 관례를 사용
	}

	private Map<String, Object> mapColumns(Product product) {
		Map<String, Object> params = new HashMap<>();
		...
		params.put("desc", product.getDescription());
		...
		return params;
	}
	
```

이런 경우에라도 getter를 호출할때는 오타를 치면 컴파일이 되지 않으므로 MyBatis/IBatis의 방식보다 생산성에서 유리합니다. 코드를 개선하기에도 좋은 구조입니다. 중복이 될만한 부분을 메서드를 추출할 수도 있고, BeanUtils를 이용해서 Bean -> Map으로 자동변환후 다른 부분만 수동으로 처리할수도 있습니다.


## 유연한 DB컬럼명 -> 객체속성명 매핑
DB에서 조회를 할 때도 컬럼명과 속성명이 일치하다면 별도로 매핑선언을 할 필요가 없습니다. BeanPropertyRowMapper를 활용합니다. 
snake_case는 camelCase로 자동변환됩니다.

```java
	public static final String SELECT_BY_ID = 
			"SELECT id, name, tel_no, address, homepage\n" + 
			"		FROM seller " + 
			"		WHERE id = :id";

	private RowMapper<Seller> sellerMapper = BeanPropertyRowMapper.newInstance(Seller.class);

	public Seller findById(Integer id) {
		Map<String, Integer> params = Collections.singletonMap("id", id);
		return db.queryForObject(SELECT_BY_ID, params, sellerMapper);
	}
```

만약 자동변환이 될 수 없다면 RowMapper를 직접 선언해서 변환할수 있습니다. RowMapper는 메서드가 1개인 인터페이스이기 때문에 Java8에서는 람다표현식으로 써도 됩니다.

```java
		RowMapper<Product>  productMapper = (rs, rowNum) -> {
			Product product = new Product();
			product.setId(rs.getInt("id"));
			product.setName(rs.getString("name"));
			product.setPrice(rs.getLong("price"));
			product.setDescription(rs.getString("desc"));
			LocalDateTime regTime = LocalDateTime.parse(rs.getString("reg_time"), formatter);
			product.setRegisteredTime(regTime);
			return product;
		};

```

RowMapper를 정의하는 코드는 Java코드이기 때문에 iBatis나 MyBatis에서 XML로 매핑선언을 하는것보다 비해서 여러 장점이 있습니다.

첫째, 컴파일 시점에 더 많은 것을 검증합니다. 변환할 객체의 setter를 직접 호출한다면 객체 속성명은 IDE에서 자동완성되고, 오타를 치면 빨간 줄로 바로 알려줍니다. 

둘째, 적극적인 타입변환을 간편하게 할 수 있습니다. 예를 들면 DB에는 reg_time라는 컬럼이 VARCHAR(14)와 '20150101120000'처럼 저장되어 있을때 Java객체는 LocalDateTime으로 쓰고 싶다고 합시다. 이 때 RowMapper안에서 바로 `LocalDateTime.parse()` 메서드를 호출하면 그런 작업은 간단히 끝납니다. Enum, Java8의 Optional과 같은 클래스를 쓰기에도 좋습니다. MyBatis/IBatis를 쓸 때는 이런 작업을 DAO의 코드에서 하거나 별도의 Conveter등을 번거롭게 등록해야합니다.

셋째, java코드리팩토링에 유리합니다. 한번만 쓰이는 변환로직은 람다표현식으로 메서드 안에 바로 써도 됩니다. 그러다  여러곳에서 쓰이면 멤벼변수로 올리고 별도의 클래스로도 정의할 수 있습니다. 중복되는 로직을 한곳으로 모이기에도 편합니다.

그리고 CoumnMapRowMapper를 이용하면 getter/setter가 있는 객체대신 map으로 변환한수도 있습니다.

객체간의 관계가 있을 때에도 Java코드로 이를 기술할 수 있습니다. 예를 들어  Product가 seller라는 속성을 가지고 있다고 합시다.

```java
public class Product {
	Integer id;
	Integer name;
	Seller seller;
...

}
```

이를 한번의 SQL로 조회해서 가지고 왔다고 하면, RowMapper를 아래처럼 선언해서 매핑할 수 있습니다.


```java
		RowMapper<Product>  productMapper = (rs, rowNum) -> {
			Product product = new Product();
			product.setId(rs.getInt("id"));
			product.setName(rs.getString("name"));
			
			Seller seller = new Seller();
			seller.setId(rs.getInt("seller_id"));
			seller.setName(rs.getString("seller_name"));
			product.setSeller(seller);

			return product;
		};

```

iBats/MyBatis를 쓸때는 이런 객체관의 관계를 설정할때  아래와 같이 충첩된 XML선언을 해야 합니다.

```xml
<resultMap id="ProductResultMap" type="Product">
  <id property="id" column="id"/>
  <result property="name" column="name"/>
  <association property="seller" javaType="Seller">
    <id property="id" column="seller_id"/>
    <result property="name" column="seller_name"/>
  </association>
```

그런 점이 불편해서 iBatis/MyBatis를 쓸때는 쿼리결과대로 펴서 아래와 같이 객체선언을 하는 경우가 많이 보입니다.

```java
public class Product {
	Integer id;
	String name;
	Integer sellerId;
	String sellerName;
...
}
```

결국에는 객체마다 중복속성이 생기고, 하나의 객체가 커져서 코드파악과 변경에 불리해집니다. RowMapper를 직접 구현한다면 객체 매핑 전략을 구현하고 최적화하기에 유리합니다. 물론 본격적으로 객체매핑을 하기위해서는 ORM이 필요합니다.


## 쿼리 생성에 Java 코드를 활용
iBatis, MyBatis에서는 보통 쿼리의 ID를 문자열로 씁니다. Spring JDBC에서는 이를 상수로 선언할 수 있기 때문에  쓰기 때문에 자동완성과 오타 예방, 코드 추적에 더 유리합니다.

![typing_error.png](http://file.benelog.net/include/sql_without_xml/typing_error.png)

Dynamic SQL을 생성할 때도 Java에서 쓰는 조건/반복문을 자연스럽게 쓸 수 있습니다. 

```java
	public static String selectByCondition(Seller seller) {
		String selectPart = "SELECT id, name, tel_no, address, homepage\n" + 
				"	FROM seller " +
				"	WHERE 1=1";
		
		StringBuilder sql = new StringBuilder(selectPart);

		if (!isEmpty(seller.getName())) {
			sql.append("AND name = :name \n");
		}
		
		if (!isEmpty(seller.getAddress())) {
			sql.append("AND address = :address \n");
		}

		if (!isEmpty(seller.getTelNo())) {
			sql.append("AND tel_no = :telNo \n");
		}

		return sql.toString();
	}
```

iBatis/MyBatis에서는 XML안에서 조건/반복문을 나름대로의 표현식으로 써야합니다. 이를 위해 별도의 문법을 학습해야 합니다. Java로 조건/반복문을 짤 때와 비해서는 오타를 치기 쉽고 자동완성이 되는 범위도 좁습니다.

그런데, Java에서 Multiline String이 지원 안되기 때문에 SQL이 길어지면 불편해집니다. 이를 극복하는 방법은 [Java에서 XML없이 SQL개발하기](http://blog.benelog.net/2708621)을 참조합니다.

