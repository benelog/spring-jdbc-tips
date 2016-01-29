package net.benelog.spring.persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcInsertOperations;
import org.springframework.stereotype.Repository;

import net.benelog.spring.domain.Product;
import net.benelog.spring.domain.Seller;

@Repository
public class ProductRepository {
	private NamedParameterJdbcOperations db;
	private SimpleJdbcInsertOperations productInsertion;
	private RowMapper<Product> productMapper;
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	@Autowired
	public ProductRepository(DataSource dataSource) {
		this.db = new NamedParameterJdbcTemplate(dataSource); 

		this.productInsertion = new SimpleJdbcInsert(dataSource)
			.withTableName("product")
			.usingGeneratedKeyColumns("id");

		this.productMapper = (rs, rowNum) -> {
			Product product = new Product();
			product.setId(rs.getInt("product_id")); // PK는 필수값. getInt를 써도 문제는 없음
			product.setName(rs.getString("product_name"));
			product.setPrice((Long) rs.getObject("price"));
			product.setDescription(rs.getString("desc"));
			LocalDateTime regTime = LocalDateTime.parse(rs.getString("reg_time"), formatter);
			product.setRegisteredTime(regTime);

			Seller seller = new Seller();
			seller.setId((Integer)rs.getObject("seller_id"));
			seller.setName(rs.getString("seller_name"));
			seller.setHomepage(rs.getString("homepage"));
			seller.setAddress(rs.getString("address"));
			product.setSeller(seller);

			return product;
		};
	}
	
	private Map<String, Object> mapColumns(Product product) {
		Map<String, Object> params = new HashMap<>();
		params.put("id", product.getId());
		params.put("name", product.getName());
		params.put("desc", product.getDescription());
		params.put("price", product.getPrice());
		params.put("seller_id", product.getSeller().getId());
		params.put("reg_time", product.getRegisteredTime().format(formatter));
		return params;
	}

	public Integer create(Product product) {
		Map<String, Object> params = mapColumns(product);
		return productInsertion.executeAndReturnKey(params).intValue();
	}

	public Product findById(Integer id) {
		Map<String, Integer> params = Collections.singletonMap("id", id);
		return db.queryForObject(ProductSqls.SELECT_BY_ID, params, productMapper);
	}

	public boolean update(Product product) {
		Map<String, Object> params = mapColumns(product);
		int affected = db.update(ProductSqls.UPDATE, params);
		return affected == 1;
	}

	public boolean delete(Integer id) {
		Map<String, Integer> params = Collections.singletonMap("id", id);
		int affected = db.update(ProductSqls.DELETE_BY_ID, params);
		return affected == 1;
	}
}
