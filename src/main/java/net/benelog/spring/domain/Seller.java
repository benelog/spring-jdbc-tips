package net.benelog.spring.domain;

import java.util.ArrayList;
import java.util.List;

public class Seller {
	private Integer id;
	private String name;
	private String address;
	private String telNo;
	private String homepage;
	private List<Product> productList = new ArrayList<>();

	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getTelNo() {
		return telNo;
	}
	public void setTelNo(String telNo) {
		this.telNo = telNo;
	}
	public String getHomepage() {
		return homepage;
	}
	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}

	public List<Product> getProductList() {
		return productList;
	}

	@Override
	public String toString() {
		return "Seller [id=" + id + ", name=" + name + ", address=" + address
				+ ", telNo=" + telNo + ", homepage=" + homepage + "]";
	}
	public void addProduct(Product product) {
		productList.add(product);
	}
}
