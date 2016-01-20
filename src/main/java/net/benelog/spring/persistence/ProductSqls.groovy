package net.benelog.spring.persistence;

public class ProductSqls {
	public static final String SELECT_BY_ID =  """
		SELECT id, name, price, desc, reg_time
		FROM product
		WHERE id = :id
	"""

	public static final String DELETE_BY_ID = """
		DELETE FROM product 
		WHERE id = :id
	"""

	public static final String UPDATE = """
		UPDATE product
		SET name = :name,
			 desc = :desc,
			 price = :price,
			 reg_time = :reg_time
		WHERE id = :id
	"""
}
