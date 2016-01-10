package net.benelog.spring.domain;

import java.time.LocalDateTime;

public class Product {
	private Integer id;
	private String name;
	private Long price;
	private String description;
	private LocalDateTime registeredTime;
	
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

	public Long getPrice() {
		return price;
	}

	public void setPrice(Long price) {
		this.price = price;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDateTime getRegisteredTime() {
		return registeredTime;
	}

	public void setRegisteredTime(LocalDateTime registeredTime) {
		this.registeredTime = registeredTime;
	}

	@Override
	public String toString() {
		return "Product [id=" + id + ", name=" + name + ", price=" + price
				+ ", description=" + description + ", registeredTime="
				+ registeredTime + "]";
	}
}
