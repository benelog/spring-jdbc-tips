## Spring JDBC 확장 라이브러리

### SimpleJdbcUpdate : Update 구문을 자동 생성
`SimpleJdbcUpdate`는 Update 구문을 자동으로 생성해줍니다. Insert 구문을 자동으로 만들어주는 `SimpleJdbcInsert`와 유사합니다.

사용법은 https://github.com/florentp/spring-simplejdbcupdate 을 참조하시기 랍니다.

이 라이브러리는 Spring 4.3버전에 포함될 예정이였다가 현재 보류 중에 있습니다.

- 관련 이슈 : [SPR-4691](https://jira.spring.io/browse/SPR-4691)
- 관련 Pull Request : https://github.com/spring-projects/spring-framework/pull/1075

### OneToManyResultSetExtractor : 1개의 SELECT문에서 1대 다 관계의 객체 추출
[OneToManyResultSetExtractor](http://docs.spring.io/spring-data/jdbc/docs/current/api/org/springframework/data/jdbc/core/OneToManyResultSetExtractor.html)는 [Spring Data JDBC Extensions](http://projects.spring.io/spring-data-jdbc-ext/) 라는 별도의 모듈에서 제공되는 클래스입니다. '1 대 다' 관계의 객체를 조회할 때 쿼리 호출 횟수를 줄일 수 있습니다.

아래와 같이 `Seller`와 `Product`객체가 1대다 관계를 가지고 있는 경우를 예로 들어보겠습니다.

```java
public class Seller {
	private Integer id;
	....
	private List<Product> productList;

	public void addProduct(Product product) {
		if (productList == null) {
			productList = new ArrayList<>();
		}
		productList.add(product);
	}
```

위의 `Seller` 객체에 `productList`필드를 채워서 조회하기 위해서는 'seller 테이블'과 'product 테이블'를 각각 조회하는 쿼리를 호출해야 합니다. [연관 관계 지연 로딩 기법](lazy-loading.md)을 이용하면 불필요한 호출을 막을 수는 있습니다. 그러나 다수의 Seller를 productList와 함께 조회하는 화면이 있다면 지연 로딩도 효과적이지 않습니다. 그럴 때는 Seller 건수 만큼 Product를 조회하는 SQL이 실행됩니다. 이런 문제를 N+1 쿼리라고 부릅니다.

N+1 쿼리를 효율하기 위해서 1대 다에서 다 쪽인 product의 건수만큼 데이터를 조회하는 SQL을 아래와 같이 작성할 수 있습니다.

```groovy
public class SellerSqls {
	public static final String SELECT_BY_NAME_WITH_PRODUCT =  """
		SELECT
			S.id, S.name, S.tel_no, S.address, S.homepage,
			P.id AS product_id, P.name AS product_name, P.price, P.desc, P.seller_id, P.reg_time
		FROM seller S
			LEFT OUTER JOIN product P ON P.seller_id = S.id
		WHERE S.name = :name
	""";
...
```
그런데 위와 같은 쿼리를 다시 `Seller` 객체에 매핑을 할 때 어려움이 생깁니다. `Seller` 1 건을 조회하고자 해도 연관된 `Product`가 2건이라면, 위의 쿼리는 2건을 반홥합니다.

'OneToManyResultSetExtractor'는 이런 상황에서 '1 대 다' 관계에 맞춰서 객체를 매핑합니다. 아래와 같이 `OneToManyResultSetExtractor`를 상속해서 Primary Key 등의 연관관계를 유추할 수 있는 정보들을 지정합니다.

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

`OneToManyResultSetExtractor`는 Spring JDBC의 [ResultSetExtractor](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/jdbc/core/ResultSetExtractor.html
) 인터페이스의 구현체입니다. `NamedParameterJdbcTemplate`, `JdbcTemplate`의 `query()`메서드는 이 `ResultSetExtractor`을 파라미터로 받아줍니다. 앞선 예제의 `SellerProductExtractor`는 아래와 같이 활용할 수 있습니다.

```java
	public List<Seller> findByNameWithProduct(String name) {
		Map<String, String> params = Collections.singletonMap("name", name);

		ResultSetExtractor<List<Seller>> extractor =
				new SellerProductExtractor(sellerMapper, productMapper, ExpectedResults.ONE_AND_ONLY_ONE);
		return jdbc.query(SellerSqls.SELECT_BY_NAME_WITH_PRODUCT, params, extractor);
	}
```

참고로 `OneToManyResultSetExtractor`의 객체 매핑 방식은 JPA의 'fetch join'과도 유사합니다.
