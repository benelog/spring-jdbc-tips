Spring JDBC는  iBatis/Mybatis와 유사하게 native SQL쿼리를 실행하는 프레임워크입니다.

Spring JDBC의 장점을 잘 드러내는 기능을 위주로 사용법을 정리해봤습니다.

- 대표적인 클래스와 인터페이스
	- NamedParameterJdbcTemplate의 초기화
	- RowMapper : 쿼리 결과를 객체로 변환
	- SqlParameterSource : 쿼리의 파라미터 지정
- 간단한 ORM 기능
	- SimpleJdbcInsert : Insert 구문을 자동 생성
	- SimpleJdbcUpdate : Update 구문을 자동 생성
	- OneToManyResultSetExtractor : 1개의 SELECT 문에서 1대 다 관계 추출
	- 다대일 매핑, Lazy loading 기법 소개
- SQL 쿼리 관리
	- 정적 SQL선언
	- Java 코드로 정적 SQL선언
	- SQL 선언 파일로 Groovy 활용

Spring JDBC보다 더 발전한 프레임워크를 원한다면 Jooq, QueryDSL, JPA등을 고려해볼만합니다.

## 대표적인 클래스와 인터페이스
Spring JDBC에서는 DB에 접근할때 대표적으로 아래 2개의 클래스를 사용합니다.

- [JdbcTemplate](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html)
- [NamedParameterJdbcTemplate](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/namedparam/NamedParameterJdbcTemplate.html) 

실무에서는 SQL 안에 파라미터를 넣어서 쓰는 것이 편리하므로 JdbcTemplate보다는 NamedParameterJdbcTemplate를 더 권장합니다. NamedParameterJdbcTemplate의 동작은 NamedParameterJdbcOpertaion라는 인터페이스에 정의되어 있기도 합니다. 참조타입으로 그 인터페이스를 활용하기도 합니다.

SQL 쿼리와 Java객체를 연결하는 역할로 아래의 인터페이스가 쓰입니다.

- [RowMapper](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/RowMapper.html) : ResultSet 에서 값을 추출하여 원하는 객체로 변환
- [SqlParameterSource](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/namedparam/SqlParameterSource.html) : Java에서 값을 추출하여 SQL에 들어갈 파라미터로 변환

RowMapper와 SqlParameterSource 인터페이스를 구현한 기본 클래스들도 함께 제공됩니다.

| 변환 대상  | RowMapper | SqlParamerSource |
|---------|-----------|------------------|
| bean | BeanPropertyRowMapper |  BeanPropertySqlParameterSource |
| Map | ColumnMapRowMapper | MapSqlParameterSource |

앞에서 설명한 클래스/인터페이스의 자세한 사용법을 살펴보겠습니다.

### NamedParameterJdbcTemplate의 초기화
NamedParameterJdbcTemplate은 DataSource 객체만 미리 정의되어 있다면 코드 1줄로 생성할 수 있습니다.

```java
NamedParameterJdbcOperations jdbc = new NamedParameterJdbcTemplate(dataSource);
```

DataSource와 트랙잭션 설정 등 어느 프레임워크를 쓰더라도 들어가는 영역을 제외한다면, 위의 한줄이 필요한 선언의 전부입니다. iBatis/MyBatis는 설정파일의 위치 등을 지정해야하기 때문에 초기 설정이 이보다는 복잡합니다.

JdbcTemplate 계열은 멀티스레드에서 접근해도 안전합니다. 따라서 매번 객체를 생성할 필요는 없습니다. DAO등에서는 멤버변수로 저장해 둡니다. 보통 DataSoure 객체만 외부에서 주입받아서 아래와 같이 설정합니다.

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
public class SellerRepository {
	private NamedParameterJdbcOperations db;
	@Autowired
	public SellerRepository(DataSource dataSource) {
		this.db = new NamedParameterJdbcTemplate(dataSource);
	}
```

DataSource가 여러 개일때는 `@Qualifier`등으로 원하는 DataSource를 하나만 찍어서 지정해야 합니다.

## RowMapper : 쿼리 결과를 객체로 변환
RowMapper는 JDBC의 명세인 ResultSet에서 원하는 객체로 타입을 변환하는 역할을 합니다. 기본적인 전략을 구현할 클래스는 Spring JDBC에서 제공을 합니다.

DB의 컬럼명과 bean 객체의 속성명이 일치하다면 BeanPropertyRowMapper를 이용하여 자동으로 객체변환을 할 수 있습니다. snake_case는 camelCase로 알아서 맞춰줍니다.

예를 들어 아래와 같은 속성을 가진 객체가 있을 때,
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

다음과 같은 코드로 ResultSet에서 Seller로 타입을 변환하여 쿼리 결과를 받아옵니다.
```java
	public static final String SELECT_BY_ID =
			"SELECT id, name, tel_no, address, homepage FROM seller WHERE id = :id";

	private RowMapper<Seller> sellerMapper = BeanPropertyRowMapper.newInstance(Seller.class);

	public Seller findById(Integer id) {
		Map<String, Integer> params = Collections.singletonMap("id", id);
		return db.queryForObject(SELECT_BY_ID, params, sellerMapper);
	}
```

DB컬럼의 이름인 `tel_no`는 snake_case였는데, Seller객체의 속성이름인 `telNo`는 camelCase입니다. 별다른 설정이 없어도 자동으로 매핑이 되었습니다. MyBatis에서는 `mapUnderscoreToCamelCase`라는 값을 XML으로 설정해서 이런 동작을 수행할 수 있습니다. iBatis에서는 비슷한 기능이 없습니다.

RowMapper의 또다른 기본 구현체인 ColumnMapRowMapper은 ResultSet을 java.util.Map으로 반환합니다. 앞선 예제에서 Seller 타입 대신에 Map으로 변환을 하고자한다면 다음과 같이 코드를 씁니다.

```java
	private RowMapper<Map<String,Object>> sellerMapper = new ColumnMapRowMapper();

	public Map<String,Object> findById(Integer id) {
		Map<String, Integer> params = Collections.singletonMap("id", id);
		return db.queryForObject(SELECT_BY_ID, params, sellerMapper);
	}
```

만약 자동변환이 될 수 없다면 RowMapper를 직접 구현을 합니다. RowMapper는 메서드가 1개인 인터페이스이기 때문에 Java8에서는 람다표현식으로 간단히 선언할 수 있습니다.

```java
		private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss"); // DB에 문자열로 들어간 날짜 데이터 변환에 사용

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

RowMapper를 정의하는 코드는 Java코드이기 때문에 컴파일 시점에 검증되는 영역이 많습니다. 그래서 오타를 빨리 발견하고, 자동완성, 코드추적, 리팩토링에도 유리합니다. RowMapper를 쓸 때 한번만 쓰이는 로직은 람다표현식으로 메서드 안에 바로 작성해도 됩니다. 그러다 여러 곳에서 쓰이면 멤벼변수로 올리고 별도의 파일로 뺀 클래스로도 정의할 수 있습니다. iBatis나 MyBatis와 같이 XML로 매핑선언을 하는 것보다 실수를 미리 발견하고 코드를 개선하기에 편리합니다.

RowMapper를 직접 구현하면서 적극적인 타입변환을 간편하게 할 수도 있습니다. 예를 들면 DB에는 reg_time라는 컬럼이 VARCHAR(14)와 '20150101120000'처럼 저장되어 있을때 Java객체는 LocalDateTime으로 쓰고 싶다고 합시다. 이 때 RowMapper안에서 바로 `LocalDateTime.parse()` 메서드를 호출하면 그런 작업은 간단히 끝납니다. Enum, Java8의 Optional과 같은 클래스를 쓰기에도 좋습니다. MyBatis/IBatis를 쓸 때는 이런 작업을 DAO의 코드에서 하거나 별도의 Conveter등을 전역적으로 등록해야합니다. Spring JDBC에도 필요하다면 그런 전역적인 방법을 동원할 수 있지만, 지역적인 변환을 할 때는 RowMapper 안에서 바로 수행할수 있습니다.

### SqlParameterSource : 쿼리의 파라미터 지정
앞선 예제에서는 SQL에  `:id`와 같은 이름이 붙여진 파라미터(named parameter)가 포함되어 있습니다. 앞선 예제에서 그렇게 했듯이 Map으로 그런 파라미터를 지정할 수 있습니다.

하나의 파라미터로 여러개의 값을 넘길 때에는 java.util.LIst를 값으로 넣으면 됩니다. `IN` 절에서 여러개의 파라미터를 받는 코드를 예로 들겠습니다. MyBatis를 쓸때는 아래와 같이 XML 안에서 foreach태그로 반복문을 써줘야합니다. java문법 대신 foreach 태그로 제어문 프로그래밍을 하는 격입니다.

```xml
<select id="seletIdByList" resultType="domain.Seller">
	SELECT id, name, tel_no, address, homepage
	FROM seller
	WHERE id IN
  <foreach item="id" index="index" collection="idList" open="(" separator="," close=")">
     #{item}
  </foreach>
</select>

```

Spring JDBC로는 아래와 같이 옮길 수 있습니다. 쿼리 선언을 비슷하게 문자열로 하고,

```groovy
public class SellerSqls {
	public static final String UPDATE = """
		SELECT id, name, tel_no, address, homepage
				FROM seller
				WHERE id IN (:idList)
	"""
...
```

쿼리를 호출하는 코드에서는 "idList"라는 파리미터 이름으로 List 객체만 남기면 됩니다.

```java

	public List<Seller> findByIdList(List<Integer> idList) {
		Map<String, Object> params = Collections.singletonMap("idList", idList);
		return db.query(SellerSqls.SELECT_BY_ID_LIST, params, sellerMapper);
	}
```

SqlParmameterSource는 앞선 예제의 Map과 비슷한 역할을 합니다. 기본 구현체로 MapSqlParameterSource를 제공합니다. 그런데 NamedParameterJdbcTemplate에서는 직접 Map을 파라미터로 받는 메서드가 많기에 MapSqlParameterSource를 쓰지 않아도 Map으로 파라미터를 넘기는데 불편함이 없기는 합니다.

기본 구현체인 BeanPropertySqlParameterSource은 getter/setter가 있는 bean 객체로부터 파라미터를 추줄합니다. 

아래와 같은 Update구문을 실행할 때를 예를 들어보겠습니다.

```groovy
public class SellerSqls {
	public static final String UPDATE = """
		UPDATE seller \n
		SET name = :name,
			 tel_no = :telNo,
			 address = :address,
			 homepage = :homepage
		WHERE id = :id
	"""
...
```

파라미터인 `:name`, `:telNo` 등은 Seller 객체의 속성명과 동일합니다. 이럴 때는 SqlParameterSource 인터페이스를 구현한 BeanPropertySqlParameterSource 클래스를 이용할 수 있습니다.

```java
	public boolean update(Seller seller) {
		SqlParameterSource params = new BeanPropertySqlParameterSource(seller);
		int affected = db.update(SellerSqls.UPDATE, params);
		return affected == 1;
	}
```

## 간단한 ORM 기능
Spring JDBC에는 Insert구문을 자동으로 생성해주는 기능이 있습니다. Update구문을 자동생성해주는 클래스도 4.3버전에서는 들어갈 예정입니다. DELETE구문은 원래 단순하니, Spring JDBC를 잘 활용하면 CRUD 중 앞으로 CUD까지는 큰 노력없이 작성을 끝낼수 있습니다.

1대다 관계의 객체를 조회할 때는 Spring Data JDBC에서 제공하는 [OneToManyResultSetExtractor](http://docs.spring.io/spring-data/jdbc/docs/current/api/org/springframework/data/jdbc/core/OneToManyResultSetExtractor.html) 클래스를 활용할 수 있습니다.

프레임워크에서 제공되는 기능은 아니지만 다대1관례의 매핑과 레이지 로딩을 수동으로 처리하는 기법에 대해서도 정리해봅니다.

### SimpleJdbcInsert : INSERT 구문을 자동 생성
SimpleJdbcInsert 클래스는 INSERT 구문을 자동으로 만들어 줍니다. DB 컬럼명과 객체의 속성명이 일치한다면 아래와 같은 단순한 코드로 DB에 1건을 입력할 수 있습니다.

```java
SimpleJdbcInsertOperations insertion = new SimpleJdbcInsert(dataSource).withTableName("seller")
SqlParameterSource params = new BeanPropertySqlParameterSource(seller);
insertion.execute(params);
```

BeanPropertyRowMapper로 마찬가지로 이때 DB컬럼명의 snake_case는 java객체에서는 caseCase로 자동으로 바꾸어줍니다.

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

로그에는 어떤 SQL이 날아갔는지 당연히 나옵니다. `org.springframework.jdbc` 패키지의 로그레벨을 'DEBUG'로 설정해야 합니다.

```
2016-01-11 06:22:54.551 DEBUG 300 --- [           main] o.s.jdbc.core.simple.SimpleJdbcInsert    :
	The following parameters are used for call INSERT INTO product (NAME, DESC, PRICE, SELLER_ID, REG_TIME) VALUES(?, ?, ?, ?, ?) with: [키보드, 좋은 상품, 130000, null, 20160111062254]
```

멀티스레드에서 접근해도 안전하기 때문에 DAO 등의 멤버변수로 저장할수도 있습니다.

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

BeanUtils를 이용해서 Bean -> Map으로 자동으로 변환한 후 DB컬럼명과 객체 속성명이 다른 부분만 수동으로 처리할수도 있습니다. 반복되는 코드는 메소드를 추출해서 재활용할수도 있습니다.

### SimpleJdbcUpdate : Update 구문을 자동 생성
SimpleJdbcInsert와 유사하게 Update구문을 작성해주는 SimpleJdbcUpdate라는 클래스도 있습니다. 이 클래스는 Spring 4.3버전에 ( [SPR-4691](https://jira.spring.io/browse/SPR-4691) 참조 ) 정식으로 Spring 내부에 포함될 예정입니다. 현재는 https://github.com/florentp/spring-simplejdbcupdate 에 올라와있는 별도의 라이브러리로 이용할 수 있습니다.

### OneToManyResultSetExtractor : 1개의 SELECT문으로 1대 다 관계 추출
(작성 중)

```groovy
public class SellerSqls {
	public static final String SELECT_BY_ID_WITH_PRODUCT =  """
		SELECT
			S.id, S.name, S.tel_no, S.address, S.homepage,
			P.id AS product_id, P.name AS product_name, P.price, P.desc, P.seller_id, P.reg_time
		FROM seller S
			LEFT OUTER JOIN product P ON P.seller_id = S.id
		WHERE S.id = :id
	"""
...
```


```java
public class Seller {
	private Integer id;
	private String name;
	private String address;
	private String telNo;
	private String homepage;
	private List<Product> productList;
```


```java
public class SellerProductExtractor extends OneToManyResultSetExtractor<Seller, Product, Integer> {

	public SellerProductExtractor(RowMapper<Seller> rootMapper, RowMapper<Product> childMapper,
			org.springframework.data.jdbc.core.OneToManyResultSetExtractor.ExpectedResults expectedResults) {
		super(rootMapper, childMapper, expectedResults);
	}

	@Override
	protected Integer mapPrimaryKey(ResultSet rs) throws SQLException {
		return rs.getInt("id");
	}

	@Override
	protected Integer mapForeignKey(ResultSet rs) throws SQLException {
		return rs.getInt("seller_id");
	}

	@Override
	protected void addChild(Seller seller, Product product) {
		seller.addProduct(product);
	}

}

```

```java
	public Seller findByIdWithProduct(Integer id) {
		Map<String, Integer> params = Collections.singletonMap("id", id);

		ResultSetExtractor<List<Seller>> extractor = 
				new SellerProductExtractor(sellerMapper, productMapper, ExpectedResults.ONE_AND_ONLY_ONE);
		return db.query(SellerSqls.SELECT_BY_ID_WITH_PRODUCT, params, extractor).get(0);
	}
```

[OneToManyResultSetExtractor](http://docs.spring.io/spring-data/jdbc/docs/current/api/org/springframework/data/jdbc/core/OneToManyResultSetExtractor.html)
 
### 연관 관계 직접 매핑 기법
(작성 중)

```java
public class Product {
	Integer id;
	Integer name;
	Seller seller;
...

}
```

객체간의 관계가 있을 때에도 Java코드로 이를 기술할 수 있습니다. 예를 들어  Product가 seller라는 속성을 가지고 있다고 합시다.

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

그런 점이 불편해서 iBatis/MyBatis를 쓸때는 쿼리 결과대로 펴서 아래와 같이 객체선언을 하는 경우가 많이 보입니다.

```java
public class Product {
	Integer id;
	String name;
	Integer sellerId;
	String sellerName;
...
}
```

위와 같이 쿼리결과에 맞춰 모든 속성을 다 추가하다보면 객체마다 중복속성이 생깁니다. 객체가 커져서 코드를 파악하기도 불편해지고, DB의 컬럼의 변경/추가 때마다 많은 객체를 수정해서 변경할 부분도 더 많아집니다.  

### 연관관계 지연 로딩 기법
(작성 중)
```java
ublic class LazySeller extends Seller {
	private Supplier<List<Product>> productLoader;

	public LazySeller(Seller seller, Supplier<List<Product>> productLoader) {
		this.productLoader = productLoader;
		BeanUtils.copyProperties(seller, this);
	}

	@Override
	public List<Product> getProductList() {
		if(super.getProductList() != null) {
			return super.getProductList();
		}

		List<Product> productList = productLoader.get();
		super.setProductList(productList);
		return productList;
	}
}
```

```java
	public Seller findByIdWithLazyProduct(Integer id) {
		Seller seller = findById(id);
		Map<String, Integer> params = Collections.singletonMap("id", id);

		return new LazySeller(
			seller,
			() -> db.query(ProductSqls.SELECT_PRODUCT_LIST_BY_SELLER_ID, params, productMapper)
		);
	}
```

## SQL 쿼리 관리
iBatis, MyBatis에서는 보통 쿼리의 ID를 문자열로 씁니다. Spring JDBC에서는 Java의 상수로 선언할 수 있습니다.

![typing_error.png](http://file.benelog.net/include/sql_without_xml/typing_error.png)

Dynamic SQL을 생성할 때도 Java에서 쓰는 조건/반복문을 자연스럽게 쓸 수 있습니다.

```java
	prvate static final String SELECT_PART = "SELECT id, name, tel_no, address, homepage\n" +
			"	FROM seller " +
			"	WHERE 1=1";

	public static String selectByCondition(Seller seller) {

		StringBuilder sql = new StringBuilder(SELECT_PART);

		if (isNotBlank(seller.getName())) {
			sql.append("AND name = :name \n");
		}

		if (isNotBlank(seller.getAddress())) {
			sql.append("AND address = :address \n");
		}

		if (isNotBlank(seller.getTelNo())) {
			sql.append("AND tel_no = :telNo \n");
		}

		return sql.toString();
	}
```

iBatis/MyBatis에서는 XML안에서 조건/반복문을 나름대로의 표현식으로 써야합니다. 이를 위해 별도의 문법을 학습해야 합니다. 자동완성과 오타 예방, 코드 추적에도 풀리합니다.

그런데, Java에서는 여러줄에 걸친 문자열 선언을 지원하지 않기에 SQL이 길어지면 편집이 불편합니다. SQL 선언을 Groovy로 한다면 이를 극복할 수 있습니다.

### Groovy를 이용한 쿼리 관리 (작성중)
- [IDE와 빌드툴 설정 방법](./groovy-config.md)
- 참고 commit : [spring-jdbc-examples/commit/88805](https://github.com/benelog/spring-jdbc-examples/commit/88805b26b950612da2b378f49a77519d9b437db2) 참조

## 더 발전한 프레임워크

### SQL 명령 스타일을 계속 쓰면서 컴파일러와 IDE의 장점을 이용하고 싶다면?
Spring JDBC에서 컴파일타임에 검증되는 부분이 많은 점이 마음에 들었다면 그 특징을 더 강화한 프레임워크를 검토해볼만합니다. [Querydsl Sql](https://github.com/querydsl/querydsl/tree/master/querydsl-sql)과 
[JOOQ](http://www.jooq.org/)는 SQL의 선언도 Java의 타입을 살린 코드로 작성하는 프레임워크입니다. 그래서 Spring JDBC에서는 문자열일 뿐이였던 SQL을 작성할 때도 오타를 더 많이 검증하고 자동 완성을 활용할 수 있습니다.


### SQL을 자동 생성하는 부분을 늘이고 싶다면?
Spring JDBC에서는 자동 Insert, Update와 관계매핑을 지원하는 클래스가 있기는 합니다. 그러나 Spring JDBC는 JDBC를 단순하게 매핑하는데 초점을 맞춘 프레임워크이기에 많은 기능을 제공하는 ORM과는 거리가 멉니다. 앞에서 소개한 단순한 ORM 기능에서 더 많은 부분을 자동화한 방식에 관심이 있다면, JPA 스펙과 Hibernate, [Spring Data JPA](http://projects.spring.io/spring-data-jpa/)에 관심을 가질만합니다.

다만 ORM 프레임워크를 쓴다고해서 DB를 객체로 어떻게 매핑할지의 문제가 자동으로 풀리는 것은 아닙니다. 어떤 전략을 써서 객체를 매핑할지 오히려 더 많은 고민이 생깁니다. 앞에서 소개한 클래스와 기법으로 객체관계 매핑을 어떻게 할지 충분히 고민하는 습관을 들이고, ORM의 필요성을 느낀후 점진적으로 ORM을 도입하는 것도 괜찮은 전략입니다.


