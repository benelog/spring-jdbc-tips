package net.benelog.spring;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.List;

import net.benelog.spring.domain.Seller;
import net.benelog.spring.persistence.SellerRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = JdbcApplication.class)
public class SellerIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	private SellerRepository repo;
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
}
