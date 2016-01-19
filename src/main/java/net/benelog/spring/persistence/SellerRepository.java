package net.benelog.spring.persistence;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import net.benelog.spring.domain.Seller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcInsertOperations;
import org.springframework.stereotype.Repository;

@Repository
public class SellerRepository {
	private NamedParameterJdbcOperations db;
	private SimpleJdbcInsertOperations sellerInsertion;
	private RowMapper<Seller> sellerMapper = BeanPropertyRowMapper.newInstance(Seller.class);

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
