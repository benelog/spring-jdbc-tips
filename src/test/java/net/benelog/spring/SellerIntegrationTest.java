package net.benelog.spring;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.benelog.spring.domain.Product;
import net.benelog.spring.domain.Seller;
import net.benelog.spring.persistence.ProductRepository;
import net.benelog.spring.persistence.SellerRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = JdbcApplication.class)
public class SellerIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	private SellerRepository repo;
	@Autowired
	private ProductRepository productRepo;
	private Seller seller = new Seller();

	@Before
	public void setUp() {
		seller.setHomepage("http://blog.naver.com");
		seller.setTelNo("010-1111-1111");
		seller.setAddress("서울시 마포구 용강동");
	}

	@Test
	public void shouldBeCreatedAndFound() {
		// given
		seller.setName("정유하");

		// when
		Integer id = repo.create(seller);

		// then
		Seller found = repo.findById(id);
		assertThat(found.getName(), is("정유하"));
	}

	@Test
	public void shouldBeFoundByIdList() {
		// given
		Integer id1 = repo.create(seller);
		Integer id2 = repo.create(seller);

		// when
		List<Seller> sellerList = repo.findByIdList(Arrays.asList(id1,id2));

		// then
		assertThat(sellerList.size(), is(2));
	}

	@Test
	public void shouldBeFoundByTelNo() {
		// given
		String telNo = "0101112123123123";
		seller.setTelNo(telNo);
		repo.create(seller);
		
		// when
		Seller condition = new Seller();
		condition.setTelNo(telNo);
		List<Seller> selected = repo.findBy(condition);

		// then	
		assertThat(selected.size(), is(1));
	}

	@Test
	public void shouldBeFoundByIdWithProduct() {
		// given
		Integer id = insertSellerAndProduct();

		// when
		Seller selected = repo.findByIdWithProduct(id);

		// then
		List<Product> productList = selected.getProductList();
		assertThat(productList.size(), is(1));
	}

	@Test
	public void shouldBeFoundByIdWithLazyProduct() {
		Integer id = insertSellerAndProduct();

		// when
		Seller selected = repo.findByIdWithLazyProduct(id);

		// then
		List<Product> productList = selected.getProductList();
		assertThat(productList.size(), is(1));
	}

	@Test
	public void shouldBeUpdated() {
		// given
		seller.setName("정유하");
		Integer id = repo.create(seller);

		// when
		seller.setId(id);
		seller.setName("정유하");
		boolean updated = repo.update(seller);

		// then
		assertThat(updated, is(true));
		Seller found = repo.findById(id);
		assertThat(found.getName(), is("정유하"));
	}

	@Test
	public void shouldBeDeleted() {
		// given
		Integer id = repo.create(seller);

		// when
		boolean deleted = repo.delete(id);

		// then
		assertThat(deleted, is(true));
		int countById = countRowsInTableWhere("seller", "id = " +id);
		assertThat(countById, is(0));
	}

	private Integer insertSellerAndProduct() {
		Integer id = repo.create(seller);
		seller.setId(id);
		Product product = new Product();
		product.setPrice(130000L);
		product.setRegisteredTime(LocalDateTime.now());
		product.setDescription("좋은 상품");
		
		product.setSeller(seller);
		productRepo.create(product);
		return id;
	}
}
