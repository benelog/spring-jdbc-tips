## 연관 관계 지연 로딩 기법
지연 로딩(lazy loading)은 객체를 로딩하는 시점을 실제로 그 객체가 쓰이는 시점까지 미루는 패턴입니다. ORM에서는 불필요한 SQL 쿼리가 미리 호출되지 않도록 이 기법이 자주 사용됩니다.

예를 들어 설명해보겠습니다. 아래와 같이 'Seller'객체가 'List<Product>' 타입의 속성을 참조를 하는 객체 관계가 있습니다.

```java
public class Seller {
	private Integer id;
	private String name;
	....
	private List<Product> productList;
	// getter, setter 생략
```

DB에는 'seller'와 'product'가 각각의 테이블에 저장되어 있습니다. 'Seller' 객체를 로딩할 때 'productList'까지 함께 로딩한다면 2번의 쿼리가 필요합니다.  그런데 'productList'속성이 필요한 화면도 있고, 아닌 곳도 있다고 가정을 해보겠습니다. 따라서 'productList'를 미리 로딩해두는 것은 불필요한 쿼리를 실행하는 것일수도 있습니다.

이런 문제를 해결하는 가장 전통적인 방법은 화면에 따라서 서비스 레이어의 메서드를 분리하는 것입니다. 'SellerService.findSellerWithProductList()'처럼 'productList'를 함께 로딩하는 메서드를 따로 하나 더 두는 것입니다. 이런 방식은 코드를 추적할 때 SQL 호출의 흐름이 명확히 보인다는 장점이 있습니다. 반면 서비스 레이어의 메서드가 많이 늘어날 수 있고, UI 레이어의 변경이 있을 때 서비스 레이어까지 신경을 써야한다는 단점도 있습니다. 지연 로딩 패턴은 이런 단점을 보완할 수 있습니다.

Hibernate 같은 ORM에서는 간단한 옵션으로 'getProductList()'가 호출될 때 SQL 쿼리를 실행하는 지연 로딩 기능을 제공합니다. Spring JDBC같이 본격적인 ORM이 아닌 프레임워크를 쓸 때도 수동으로 이를 흉내내어 볼 수도 있습니다. 아래와 같이 Seller를 상속한 하위 클래스를 만듭니다.


```java
public class LazySeller extends Seller {
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

'productList'를 로딩하는 역할을 'productLoader' 멤버 변수에 위임을 했습니다. 'productLoader'은 `java.util.function.Supplier`타입으로 정의했습니다.

서비스 레이어 혹은 Repository 레이어에서 'productLoader'를 람다표현식으로 선언할 수 있습니다.

```java
	public Seller findById(Integer id) {
		Seller seller = dao.findSellerById(id);
		return new LazySeller(
			seller,
			() -> dao.findProductListBySellerId(id);
		);
	}
```

위의 예제를 실행하면 `LazySeller`가 생성되는 시점에서는 `LazySeller.productLoader` 변수에 람다표현식으로 정의한 객체를 할당하기만 합니다. `LazySeller.getProductList()`가 호출되는 시점에서야 `productLoader.get();`이 실행되어서 `dao.findProductListBySellerId(id);`이 호출됩니다.

아래 링크에서 같은 패턴으로 구현한 예제를 참조하실 수 있습니다.

- [LazySeller](/src/main/java/net/benelog/spring/domain/LazySeller.java)
- [SellerRepository](/src/main/java/net/benelog/spring/persistence/SellerRepository.java)
- [SellerIntegrationTest](/src/test/java/net/benelog/spring/SellerIntegrationTest.java)
