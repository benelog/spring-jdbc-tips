package net.benelog.spring.domain;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;

public class LazySeller extends Seller {
	private Supplier<List<Product>> productLoader;

	public LazySeller(Seller seller, Supplier<List<Product>> productLoader) {
		this.productLoader = productLoader;
		BeanUtils.copyProperties(seller, this);
	}

	@Override
	public List<Product> getProductList() {
		if(super.getProductList() != null) {
			return super.getProductList();
		}

		List<Product> productList = productLoader.get();
		super.setProductList(productList);
		return productList;
	}
}
