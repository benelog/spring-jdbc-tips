package net.benelog.spring.domain;

import java.util.List;
import java.util.function.Function;

import org.springframework.beans.BeanUtils;

public class LazySeller extends Seller {
	private Function<Integer, List<Product>> productLoader;

	public LazySeller(Seller seller, Function<Integer, List<Product>> productLoader) {
		this.productLoader = productLoader;
		BeanUtils.copyProperties(seller, this);
	}

	@Override
	public List<Product> getProductList() {
		if(super.getProductList() != null) {
			return super.getProductList();
		}

		List<Product> productList = productLoader.apply(super.getId());
		super.setProductList(productList);
		return productList;
	}
}
