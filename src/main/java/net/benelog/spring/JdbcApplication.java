package net.benelog.spring;

import java.time.LocalDateTime;

import net.benelog.spring.domain.Product;
import net.benelog.spring.domain.Seller;
import net.benelog.spring.persistence.ProductRepository;
import net.benelog.spring.persistence.SellerRepository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class JdbcApplication {
	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(JdbcApplication.class, args);

		Seller seller = new Seller();
		seller.setName("정유하");
		seller.setHomepage("http://blog.naver.com");
		seller.setTelNo("010-1111-1111");
		seller.setAddress("서울시 마포구 용강동");

		SellerRepository sellerRepo = context.getBean(SellerRepository.class);
		Integer sellerId = sellerRepo.create(seller);
		seller.setName("정유현");
		seller.setId(sellerId);
		sellerRepo.update(seller);
		System.out.println(sellerRepo.findById(sellerId));
		
		ProductRepository productRepo = context.getBean(ProductRepository.class);
		Product product = new Product();
		product.setName("키보드");
		product.setPrice(130000L);
		product.setRegisteredTime(LocalDateTime.now());
		product.setDescription("좋은 상품");
		Integer productId = productRepo.create(product);
		System.out.println(productRepo.findById(sellerId));

		product.setRegisteredTime(LocalDateTime.now().minusDays(1));
		product.setId(productId);
		productRepo.update(product);
		System.out.println(productRepo.findById(sellerId));
	}
}
