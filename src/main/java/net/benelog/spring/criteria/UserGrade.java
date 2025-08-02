package net.benelog.spring.criteria;

public enum UserGrade {
	SPECIAL("S"),LOCKED("L");

	private String code;

	UserGrade(String code) {
		this.code = code;
	}
}
