package net.benelog.spring;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.time.LocalDateTime;

import net.benelog.spring.domain.Product;
import net.benelog.spring.persistence.ProductRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = JdbcApplication.class)
public class ProductIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	private ProductRepository repo;
	private Product product = new Product();

	@Before
	public void setUp() {
		product.setPrice(130000L);
		product.setRegisteredTime(LocalDateTime.now());
		product.setDescription("좋은 상품");
	}
	
	@Test
	public void shouldBeCreatedAndFound() {
		// given
		product.setName("키보드");

		// when
		Integer id = repo.create(product);

		// then
		Product found = repo.findById(id);
		assertThat(found.getName(), is("키보드"));
	}

	@Test
	public void shouldBeUpdated() {
		// given
		product.setName("키보드");
		product.setRegisteredTime(LocalDateTime.now().minusDays(1));

		Integer id = repo.create(product);

		// when
		product.setId(id);
		product.setName("무선 키보드");
		product.setRegisteredTime(LocalDateTime.now());
		boolean updated = repo.update(product);

		// then
		assertThat(updated, is(true));
		Product found = repo.findById(id);
		assertThat(found.getName(), is("무선 키보드"));
		System.out.println(found);
	}

	@Test
	public void shouldBeDeleted() {
		// given
		Integer id = repo.create(product);

		// when
		boolean deleted = repo.delete(id);

		// then
		assertThat(deleted, is(true));
		int countById = countRowsInTableWhere("product", "id = " +id);
		assertThat(countById, is(0));
	}
}
