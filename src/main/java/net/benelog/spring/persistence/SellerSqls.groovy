package net.benelog.spring.persistence;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import static org.apache.commons.lang.StringUtils.*;

import net.benelog.spring.domain.Seller;

public class SellerSqls {
	public static final String SELECT_BY_ID = """
		SELECT id, name, tel_no, address, homepage
				FROM seller 
				WHERE id = :id
	""";

	public static final String SELECT_BY_ID_LIST = """
		SELECT id, name, tel_no, address, homepage
				FROM seller
				WHERE id IN (:idList)
	""";

	public static final String DELETE_BY_ID = """
		DELETE FROM seller
		WHERE id = :id
	""";
	
	public static final String UPDATE = """
		UPDATE seller \n
		SET name = :name,
			 tel_no = :telNo,
			 address = :address,
			 homepage = :homepage
		WHERE id = :id
	""";

	public static final String ADDRESS_CONDITION = 
			"AND address = :address \n";

	public static final String NAME_CONDITION = 
			"AND name = :name \n";

	public static final String TEL_NO_CONDITION = 
			"AND tel_no = :telNo \n";
	

	public static String selectByCondition(Seller seller) {
		return """
			SELECT id, name, tel_no, address, homepage 
			FROM seller
			""" +
		whereAnd (
			notEmpty(seller.getName(), "name = :name"),
			notEmpty(seller.getAddress(), "address = :address"),
			notEmpty(seller.getTelNo(), "tel_no = :telNo")
		);
	}

	public static final String SELECT_BY_ID_WITH_PRODUCT =  """
		SELECT
			S.id, S.name, S.tel_no, S.address, S.homepage,
			P.id AS product_id, P.name AS product_name, P.price, P.desc, P.seller_id, P.reg_time
		FROM seller S
			LEFT OUTER JOIN product P ON P.seller_id = S.id
		WHERE S.id = :id
	""";

	private static String notEmpty(String param, String condition) {
		return StringUtils.isEmpty(param)? null: condition;
	}

	private static String whereAnd(String ... conditions) {
		List<String> finalCond = conditions.findAll({it != null});
		Assert.notEmpty(finalCond);
		return "WHERE " + finalCond.join("\nAND ");
	}
}
