package net.benelog.spring.persistence;

public class ProductSqls {
	public static final String SELECT_BY_ID =  """
		SELECT
			P.id AS product_id, P.name AS product_name, P.price, P.desc, P.seller_id, P.reg_time,
			S.name AS seller_name, S.tel_no, S.address, S.homepage
		FROM product P LEFT OUTER JOIN seller S ON S.id = P.seller_id
		WHERE P.id = :id
	""";

	public static final String DELETE_BY_ID = """
		DELETE FROM product 
		WHERE id = :id
	""";

	public static final String UPDATE = """
		UPDATE product
		SET name = :name,
			desc = :desc,
			price = :price,
			seller_id = :seller_id,
			reg_time = :reg_time
		WHERE id = :id
	""";

	public static final String SELECT_PRODUCT_LIST_BY_SELLER_ID =  """
		SELECT
			P.id AS product_id, P.name AS product_name, P.price, P.desc, P.seller_id, P.reg_time,
			S.name AS seller_name, S.tel_no, S.address, S.homepage
		FROM product P LEFT OUTER JOIN seller S ON S.id = P.seller_id
		WHERE P.seller_id = :seller_id
	""";
}
