package net.benelog.spring.persistence;

import org.junit.Test;

import net.benelog.spring.domain.Seller;

public class SellerSqlsTest {

	@Test
	public void selectByCondition() {
		Seller condition = new Seller();
		condition.setName("jyh");
		condition.setTelNo("010-2841-1383");

		String sql = SellerSqls.selectByCondition(condition);
		System.out.println(sql);

	}
}
