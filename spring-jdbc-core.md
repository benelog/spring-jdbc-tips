## Spring JDBC의 핵심 사용법
Spring JDBC에서 자주 쓰이는 클래스/인터페이스의 사용법을 정리합니다.

(링크는 Javadoc)

- [NamedParameterJdbcTemplate](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/namedparam/NamedParameterJdbcTemplate.html) : 이름이 붙여진 파라미터가 들어간 SQL을 호출
- [RowMapper](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/RowMapper.html) : ResultSet 에서 값을 추출하여 원하는 객체로 변환
		- [BeanPropertyRowMapper](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/BeanPropertyRowMapper.html) : ResultSet -> Bean 으로 변환
		- [ColumnMapRowMapper](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/ColumnMapRowMapper.html) :  ResultSet -> Map 으로 변환
- [SqlParameterSource](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/namedparam/SqlParameterSource.html) : SQL에 파라미터 전달
		- [BeanPropertySqlParameterSource](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/namedparam/BeanPropertySqlParameterSource.html) : Bean 객체로 파리미터 전달
		- [MapSqlParameterSource](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/namedparam/MapSqlParameterSource.html) : Map으로 파라미터 전달
- [SimpleJdbcInsert](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/simple/SimpleJdbcInsert.html) : Insert 쿼리를 자동생성


### NamedParameterJdbcTemplate
`NamedParameterJdbcTemplate`을 사용하면 SQL 쿼리 안에서 `?`로 표현되던 파라미터를 `:productName`과 같이 이름을 붙여서 지정할 수 있습니다. 여러 개의 파라미터가 있는 쿼리를 실행할 때는 `JdbcTemplate`보다 `NamedParameterJdbcTemplate`을 사용하기를 권장합니다.

`NamedParameterJdbcTemplate`의 동작은 `NamedParameterJdbcOpertaion`라는 인터페이스에 정의되어 있기도 합니다. 참조타입으로 그 인터페이스를 활용할 수도 있습니다.

NamedParameterJdbcTemplate은 DataSource 객체를 필요로 합니다. 생성자에서 DataSource를 전달받을 수도 있습니다.

```java
NamedParameterJdbcOperations jdbc = new NamedParameterJdbcTemplate(dataSource);
```

JdbcTemplate 계열은 멀티스레드에서 접근해도 안전합니다. 따라서 매번 객체를 생성할 필요는 없습니다. DAO등에서는 멤버변수로 저장해 둡니다. DataSoure 객체만 외부에서 주입받아서 아래와 같이 설정할 수 있습니다.

```java
public class SellerRepository {
	private NamedParameterJdbcOperations jdbc;
	public setDataSource(DataSource dataSource) {
		this.jdbc = new NamedParameterJdbcTemplate(dataSource);
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
public class SellerRepository {
	private NamedParameterJdbcOperations jdbc;
	@Autowired
	public SellerRepository(DataSource dataSource) {
		this.jdbc = new NamedParameterJdbcTemplate(dataSource);
	}
```

DataSource가 여러 개일때는 `@Qualifier`나 `@Resource` 선언을 이용해서 원하는 DataSource를 하나만 찍어서 지정해야 합니다.

### RowMapper : 쿼리 결과를 객체로 변환
`RowMapper`는 JDBC의 인터페이스인 `ResultSet`에서 원하는 객체로 타입을 변환하는 역할을 합니다. 기본적인 전략을 구현한 클래스는 Spring JDBC에서 제공을 합니다.

#### BeanPropertyRowMapper
DB의 컬럼명과 bean 객체의 속성명이 일치하다면 `BeanPropertyRowMapper`를 이용하여 자동으로 객체변환을 할 수 있습니다. DB 컬럼명이 'snake_case'로 되어 있어도 'camelCase'로 선언된 클래스의 필드로 매핑이 됩니다.

예를 들어 아래와 같은 `Seller` 객체가 있을 때,

```java
public class Seller {
	private Integer id;
	private String name;
	private String address;`
	private String telNo;
	private String homepage;
	...
}
```

다음과 같은 코드로 `ResultSet`에서 `Seller`로 타입을 변환하여 쿼리 결과를 받아옵니다.
```java
	public static final String SELECT_BY_ID =
			"SELECT id, name, tel_no, address, homepage FROM seller WHERE id = :id";

	private RowMapper<Seller> sellerMapper = BeanPropertyRowMapper.newInstance(Seller.class);

	public Seller findById(Integer id) {
		Map<String, Integer> params = Collections.singletonMap("id", id);
		return jdbc.queryForObject(SELECT_BY_ID, params, sellerMapper);
	}
```

DB컬럼의 이름인 `tel_no`는 snake_case였는데, `Seller`객체의 속성이름인 `telNo`는 camelCase입니다. 별다른 설정이 없어도 자동으로 매핑이 되었습니다.

'BeanPropertyRowMapper'의 타입 변환 전략을 확장하고 싶다면
[BeanPropertyRowMapper.setConversionService()](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/BeanPropertyRowMapper.html#setConversionService-org.springframework.core.convert.ConversionService-) 메서드를 통해 직접 구현한 `Converter`를 등록할 수 있습니다. `java.sql.TimeStamp`를 `ZonedDateTime`으로 변환하는 예를 들어보겠습니다.

아래와 같이 `Converter`를 구현하고,

```java
public class ZonedDateTimeConverter implements Converter<Timestamp, ZonedDateTime> {
	@Override
	public ZonedDateTime convert(Timestamp source) {
		return ZonedDateTime.ofInstant(source.toInstant(), ZoneId.of("UTC"));
	}
}
```

`DefaultConversionService.addConverter()`, `BeanPropertyRowMapper.setConversionService()`를 호출해서 `Converter` 구현체를 등록합니다.

```java
public static <T> RowMapper<T> getRowMapper(Class<T> mappedClass) {
		BeanPropertyRowMapper<T> mapper = BeanPropertyRowMapper.newInstance(mappedClass);
		DefaultConversionService service = new DefaultConversionService();
		service.addConverter(new ZonedDateTimeConverter());
		mapper.setConversionService(service);
		return mapper;
	}

```

위의  getRowMapper메서드를 Utiltity 클래스 같은 곳에 넣어두고 `BeanPropertyRowMapper.newInstance(...)` 메서드 대신에 호출해서 쓰면 됩니다.


#### ColumnMapRowMapper
`ColumnMapRowMapper`은 `ResultSet`을 `java.util.Map`으로 반환합니다. 앞선 예제에서 'Seller' 타입 대신에 `java.util.Map`으로 변환을 하려면 다음과 같이 코드를 씁니다.

```java
	private RowMapper<Map<String,Object>> sellerMapper = new ColumnMapRowMapper();

	public Map<String,Object> findById(Integer id) {
		Map<String, Integer> params = Collections.singletonMap("id", id);
		return db.queryForObject(SELECT_BY_ID, params, sellerMapper);
	}
```

#### 수동변환
자동변환을 할 수 없다면 `RowMapper`를 직접 구현합니다. `RowMapper`는 메서드가 1개인 인터페이스이기 때문에 Java 8에서는 람다표현식으로 간단히 선언할 수 있습니다.

```java
RowMapper<Product>  productMapper = (rs, rowNum) -> {
	Product product = new Product();
	product.setId(rs.getInt("id"));
	product.setName(rs.getString("name"));
	product.setDescription(rs.getString("desc"));
	return product;
};
```

### SqlParameterSource : 쿼리의 파라미터 지정
앞선 예제에서는 SQL에  `:id`와 같은 이름이 붙여진 파라미터(named parameter)가 포함되어 있습니다. `SqlParmameterSource` 인터페이스는 그런 파라미터에 값을 지정하는 역할을 합니다. 앞선 예제에서는 `java.util.Map`으로 파리미터에 값을 지정했었습니다.

#### BeanPropertySqlParameterSource
기본 구현체인 `BeanPropertySqlParameterSource`은 getter/setter가 있는 bean 객체로부터 파라미터를 추줄합니다.

아래와 같은 Update 구문을 실행할 때를 예를 들어보겠습니다.

```
public class SellerSqls {
	public static final String UPDATE = """
		UPDATE seller \n
		SET name = :name,
			 tel_no = :telNo,
			 address = :address,
			 homepage = :homepage
		WHERE id = :id
	""";
```

파라미터인 `:name`, `:telNo` 등은 Seller 객체의 속성명과 동일합니다. 이럴 때는 `BeanPropertySqlParameterSource`을 활용하면 파리미터 이름과 대응되는 getter 메서드를 호출하여 값을 전달하게 됩니다.

```java
	public int update(Seller seller) {
		SqlParameterSource params = new BeanPropertySqlParameterSource(seller);
		return jdbc.update(SellerSqls.UPDATE, params);
	}
```

`BeanPropertySqlParameterSource`의 타입변환 전략이 충분하지 않은 경우도 있습니다. 예를 들면 Mysql에서 DB컬럼이 timestamp 타입일때 이에 대응된는 java객체의 필드가 ZonedDateTime로 선언되었을 경우입니다. `BeanPropertySqlParameterSource`의 기본적인 동작으로는 ZonedDateTime-> timstamp 변환이 되지 않습니다. 그런 때에는 `BeanPropertySqlParameterSource`를 상속한 클래스를 만들어서 문제를 해결할 수 있습니다.


```java
static class ExtendedSqlParameterSource extends BeanPropertySqlParameterSource {
		public BridgeSqlParameterSource(Object object) {
			super(object);
		}

		@Override
		public Object getValue(String paramName) throws IllegalArgumentException {
			Object value = super.getValue(paramName);
			if ( value instanceof ZonedDateTime) {
				value = Timestamp.from(((ZonedDateTime) value).toInstant() );
			}
			return value;
		}
}
```

#### MapSqlParameterSource
`MapSqlParameterSource`는 이름처럼 Map과 비슷한 형식으로 파라미터를 지정할 때 쓸 수 있습니다.  `NamedParameterJdbcTemplate`에서는 직접 Map을 파라미터로 받는 메서드가 많기에 `MapSqlParameterSource`를 쓰지 않고 Map으로 바로 파라미터를 넘길 수도 있습니다. `MapSqlParameterSource` 클래스를 사용하면 메서드 체인 형식으로 파라미터를 정의할수 있는 장점이 있기는 합니다.

```java
SqlParameterSource params = new MapSqlParameterSource()
    .addValue("name", "판매자1")
    .addValue("address", "마포구 용강동");
```

### SimpleJdbcInsert : INSERT 구문을 자동 생성
`SimpleJdbcInsert` 클래스를 활용하면 직접 INSERT 구문을 쓰지 않고도 DB에 데이터를 저장할 수 있습니다. DB 컬럼명과 객체의 속성명이 일치한다면 아래와 같은 단순한 코드로 DB에 데이터 1건을 입력할 수 있습니다.

```java
SimpleJdbcInsertOperations insertion = new SimpleJdbcInsert(dataSource).withTableName("seller")
SqlParameterSource params = new BeanPropertySqlParameterSource(seller);
insertion.execute(params);
```

`BeanPropertyRowMapper`로 마찬가지로 이때 DB컬럼명의 snake_case는 java객체에서는 caseCase로 자동으로 바꾸어줍니다.

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

그런 경우에는 `usingGeneratedKeyColumns()`, `executeAndReturnKey()` 메서드를 활용하면 됩니다.

```java
SimpleJdbcInsertOperations insertion = new SimpleJdbcInsert(dataSource)
		.withTableName("seller")
		.usingGeneratedKeyColumns("id");
SqlParameterSource params = new BeanPropertySqlParameterSource(seller);
Integer id = insertion.executeAndReturnKey(params).intValue();
```

로그로 어떤 SQL이 실행되었는지 확인할 수 있습니다. `org.springframework.jdbc` 패키지의 로그레벨을 'DEBUG'로 설정하면 아래와 같이 실행된 쿼리가 나옵니다.

```
2016-01-11 06:22:54.551 DEBUG 300 --- [           main] o.s.jdbc.core.simple.SimpleJdbcInsert    :
	The following parameters are used for call INSERT INTO product (NAME, DESC, PRICE, SELLER_ID, REG_TIME) VALUES(?, ?, ?, ?, ?) with: [키보드, 좋은 상품, 130000, null, 20160111062254]
```

`SimpleJdbcInsert`의 `excute()`, `executeAndReturnKey()` 메서드는 멀티스레드에서 접근해도 안전합니다. 따라서 아래와 같이 클래스의 멤버변수로 지정해도 됩니다.

```java
public class SellerRepository {
	private SimpleJdbcInsertOperations sellerInsertion;

	@Autowired
	public SellerRepository(DataSource dataSource) {
		this.sellerInsertion = new SimpleJdbcInsert(dataSource)
			.withTableName("seller")
			.usingGeneratedKeyColumns("id");
	}
```

만약 DB컬럼명과 클래스의 속성명이 자동으로 매핑될수 없다면 `java.util.Map`이나 `MapSqlParameterSource`을 이용해서 수동으로 선언할수 있습니다.  Map을 쓰는 예제는 아래와 같습니다.

```java
	public Integer create(Product product) {
		Map<String, Object> params = mapColumns(product);
		return productInsertion.executeAndReturnKey(params).intValue();
	}

	private Map<String, Object> mapColumns(Product product) {
		Map<String, Object> params = new HashMap<>();
		...
		params.put("desc", product.getDescription());
		...
		return params;
	}

```

참고로 `SimpleJdbcInsert.execute()`를 호출할 때 `MapSqlParameterSource`를 파라미터로 넘기면 `BeanPropertySqlParameterSource`처럼 camelCase와 snake_case 간의 자동변환이 이루어집니다.

예를 들어 DB컬럼명이 'seller_id'로 되어 있더라도 아래와 같이 'sellerId'를 파리미터의 이름으로 지정할 수 있습니다.

````java
SqlParameterSource params = new MapSqlParameterSource()
	.addValue("sellerId",1)
	.addValue("address", "마포구 용강동");
````
