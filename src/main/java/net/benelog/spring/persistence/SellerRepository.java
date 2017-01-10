package net.benelog.spring.persistence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.OneToManyResultSetExtractor.ExpectedResults;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcInsertOperations;
import org.springframework.stereotype.Repository;

import net.benelog.spring.domain.LazySeller;
import net.benelog.spring.domain.Product;
import net.benelog.spring.domain.Seller;

@Repository
public class SellerRepository {
	private NamedParameterJdbcOperations db;
	private SimpleJdbcInsertOperations sellerInsertion;
	private RowMapper<Seller> sellerMapper = BeanPropertyRowMapper.newInstance(Seller.class);

	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private RowMapper<Product> productMapper =  (rs, rowNum) -> {
		Product product = new Product();
		product.setId(rs.getInt("product_id")); // 필수값
		product.setName(rs.getString("product_name"));
		product.setPrice(rs.getLong("price"));
		product.setDescription(rs.getString("desc"));
		LocalDateTime regTime = LocalDateTime.parse(rs.getString("reg_time"), formatter);
		product.setRegisteredTime(regTime);
		return product;
	};

	@Autowired
	public SellerRepository(DataSource dataSource) {
		this.db = new NamedParameterJdbcTemplate(dataSource); 

		this.sellerInsertion = new SimpleJdbcInsert(dataSource)
			.withTableName("seller")
			.usingGeneratedKeyColumns("id");
	}

	public Integer create(Seller seller) {
		SqlParameterSource params = new BeanPropertySqlParameterSource(seller);
		return sellerInsertion.executeAndReturnKey(params).	intValue();
	}

	public Seller findById(Integer id) {
		Map<String, Integer> params = Collections.singletonMap("id", id);
		return db.queryForObject(SellerSqls.SELECT_BY_ID, params, sellerMapper);
	}

	public List<Seller> findByIdList(List<Integer> idList) {
		Map<String, Object> params = Collections.singletonMap("idList", idList);
		return db.query(SellerSqls.SELECT_BY_ID_LIST, params, sellerMapper);
	}

	public Seller findByIdWithProduct(Integer id) {
		Map<String, Integer> params = Collections.singletonMap("id", id);

		ResultSetExtractor<List<Seller>> extractor = 
				new SellerProductExtractor(sellerMapper, productMapper, ExpectedResults.ONE_AND_ONLY_ONE);
		return db.query(SellerSqls.SELECT_BY_ID_WITH_PRODUCT, params, extractor)
				.get(0);
	}

	public Seller findByIdWithLazyProduct(Integer id) {
		Seller seller = findById(id);
		Map<String, Integer> params = Collections.singletonMap("seller_id", id);

		return new LazySeller(
			seller,
			() -> db.query(ProductSqls.SELECT_PRODUCT_LIST_BY_SELLER_ID, params, productMapper)
		);
	}

	public List<Seller> findBy(Seller condition) {
		SqlParameterSource params = new BeanPropertySqlParameterSource(condition);
		String sql = SellerSqls.selectByCondition(condition);
		return db.query(sql, params, sellerMapper);
	}

	public boolean update(Seller seller) {
		SqlParameterSource params = new BeanPropertySqlParameterSource(seller);
		int affected = db.update(SellerSqls.UPDATE, params);
		return affected == 1;
	}

	public boolean delete(Integer id) {
		Map<String, Integer> params = Collections.singletonMap("id", id);
		int affected = db.update(SellerSqls.DELETE_BY_ID, params);
		return affected == 1;
	}
}
