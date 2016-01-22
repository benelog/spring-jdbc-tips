package net.benelog.spring.persistence;

import static org.apache.commons.lang.StringUtils.*;

import net.benelog.spring.domain.Seller;


public class SellerSqls {
	public static final String SELECT_BY_ID = """
		SELECT id, name, tel_no, address, homepage
				FROM seller 
				WHERE id = :id
	"""

	public static final String DELETE_BY_ID = """
		DELETE FROM seller
		WHERE id = :id
	"""
	
	public static final String UPDATE = """
		UPDATE seller \n
		SET name = :name,
			 tel_no = :telNo,
			 address = :address,
			 homepage = :homepage
		WHERE id = :id
	"""

	public static final String ADDRESS_CONDITION = 
			"AND address = :address \n";

	public static final String NAME_CONDITION = 
			"AND name = :name \n";

	public static final String TEL_NO_CONDITION = 
			"AND tel_no = :telNo \n";
	

	public static String selectByCondition(Seller seller) {
		String selectPart = """
			SELECT id, name, tel_no, address, homepage 
			FROM seller
			WHERE 1=1
		""";

		StringBuilder sql = new StringBuilder(selectPart);

		if (isNotBlank(seller.getName())) {
			sql.append("AND name = :name \n");
		}
		
		if (isNotBlank(seller.getAddress())) {
			sql.append("AND address = :address \n");
		}

		if (isNotBlank(seller.getTelNo())) {
			sql.append("AND tel_no = :telNo \n");
		}

		return sql.toString();
	}
}
