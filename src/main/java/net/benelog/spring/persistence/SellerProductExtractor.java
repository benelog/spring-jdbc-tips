package net.benelog.spring.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.data.jdbc.core.OneToManyResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import net.benelog.spring.domain.Product;
import net.benelog.spring.domain.Seller;

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
